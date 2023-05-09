package com.tommasoberlose.anotherwidget.services

import android.Manifest
import android.content.Context
import android.os.Build
import android.provider.CalendarContract
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper.applyFilters
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper.sortEvents
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import java.util.*
import me.everything.providers.android.calendar.CalendarProvider
import org.greenrobot.eventbus.EventBus

class UpdateCalendarWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val context = applicationContext
        UpdatesReceiver.removeUpdates(context)
        val eventRepository = EventRepository(context)

        if (Preferences.showEvents) {
            if (!context.checkGrantedPermission(Manifest.permission.READ_CALENDAR)) {
                eventRepository.resetNextEventData()
                eventRepository.clearEvents()
            } else {
                // fetch all events from now to next ACTION_CALENDAR_UPDATE + limit
                val now = Calendar.getInstance()
                val limit = Calendar.getInstance().apply {
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.HOUR_OF_DAY, 0)
                    add(Calendar.DATE, 1)
                    when (Preferences.showUntil) {
                        0 -> add(Calendar.HOUR, 3)
                        1 -> add(Calendar.HOUR, 6)
                        2 -> add(Calendar.HOUR, 12)
                        3 -> add(Calendar.DAY_OF_MONTH, 1)
                        4 -> add(Calendar.DAY_OF_MONTH, 3)
                        5 -> add(Calendar.DAY_OF_MONTH, 7)
                        6 -> add(Calendar.MINUTE, 30)
                        7 -> add(Calendar.HOUR, 1)
                        else -> add(Calendar.HOUR, 6)
                    }
                }

                try {
                    val eventList = ArrayList<Event>()
                    val provider = CalendarProvider(context)
                    // apply time zone offset to correctly fetch all-day events
                    val data = provider.getInstances(
                        now.timeInMillis + now.timeZone.getOffset(now.timeInMillis).coerceAtMost(0),
                        limit.timeInMillis + limit.timeZone.getOffset(limit.timeInMillis).coerceAtLeast(0)
                    )
                    if (data != null) {
                        val filteredCalendarIdList = CalendarHelper.getFilteredCalendarIdList()
                        for (instance in data.list) {
                            try {
                                val e = provider.getEvent(instance.eventId)
                                if (e == null || e.deleted || filteredCalendarIdList.contains(e.calendarId))
                                    continue
                                if (e.allDay) {
                                    val start = Calendar.getInstance()
                                    start.timeInMillis = instance.begin
                                    val end = Calendar.getInstance()
                                    end.timeInMillis = instance.end
                                    instance.begin =
                                        start.timeInMillis - start.timeZone.getOffset(start.timeInMillis)
                                    instance.end =
                                        end.timeInMillis - end.timeZone.getOffset(end.timeInMillis)
                                }
                                if (instance.begin <= limit.timeInMillis && now.timeInMillis < instance.end) {
                                    /* Following check may result in "fake" all-day events with
                                     * non-UTC start/end time, and therefore cannot be found by
                                     * Calendar when tapped to open details.
                                    // Check all day events
                                    val startDate = Calendar.getInstance()
                                    startDate.timeInMillis = instance.begin
                                    val endDate = Calendar.getInstance()
                                    endDate.timeInMillis = instance.end

                                    val isAllDay = e.allDay || (
                                            startDate.get(Calendar.MILLISECOND) == 0
                                                    && startDate.get(Calendar.SECOND) == 0
                                                    && startDate.get(Calendar.MINUTE) == 0
                                                    && startDate.get(Calendar.HOUR_OF_DAY) == 0
                                                    && endDate.get(Calendar.MILLISECOND) == 0
                                                    && endDate.get(Calendar.SECOND) == 0
                                                    && endDate.get(Calendar.MINUTE) == 0
                                                    && endDate.get(Calendar.HOUR_OF_DAY) == 0
                                            )
                                    */

                                    eventList.add(
                                        Event(
                                            id = instance.id,
                                            eventID = e.id,
                                            title = e.title ?: "",
                                            startDate = instance.begin,
                                            endDate = instance.end,
                                            calendarID = e.calendarId,
                                            allDay = e.allDay,
                                            address = e.eventLocation ?: "",
                                            selfAttendeeStatus = e.selfAttendeeStatus.toInt(),
                                            availability = e.availability
                                        )
                                    )
                                }
                            } catch (ignored: Exception) {
                            }
                        }
                    }

                    val sortedEvents = eventList.sortEvents()
                    val filteredEventList = sortedEvents.applyFilters()

                    if (filteredEventList.isEmpty()) {
                        eventRepository.resetNextEventData()
                        eventRepository.clearEvents()
                    } else {
                        val first = filteredEventList.first()
                        if (Preferences.nextEventId != first.id && (
                            //Preferences.showWeatherAsGlanceProvider || !Preferences.showNextEvent ||
                            eventRepository.getEventById(first.id)?.startDate != first.startDate))
                            eventRepository.saveNextEventData(first)
                        eventRepository.saveEvents(filteredEventList)
                    }
                } catch (ignored: java.lang.Exception) {
                }
            }
        } else {
            eventRepository.resetNextEventData()
            eventRepository.clearEvents()
        }
        eventRepository.close()
        UpdatesReceiver.setUpdates(context)

        MainWidget.updateWidget(context)
        EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())

        if (Preferences.showEvents)
            enqueueTrigger(context)
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "updateEventList",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<UpdateCalendarWorker>().build()
            )
        }

        fun enqueueTrigger(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    "updateEventListTrigger",
                    ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<Trigger>().setConstraints(
                        Constraints.Builder().addContentUriTrigger(
                            CalendarContract.CONTENT_URI,
                            true
                        ).build()
                    ).build()
                )
            }
        }

        fun cancelTrigger(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                WorkManager.getInstance(context).cancelUniqueWork(
                    "updateEventListTrigger"
                )
            }
        }
    }

    class Trigger(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            if (Preferences.showEvents && !isStopped)
                enqueue(applicationContext)
            return Result.success()
        }
    }
}
