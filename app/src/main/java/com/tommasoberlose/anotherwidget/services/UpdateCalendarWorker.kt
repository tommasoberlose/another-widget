package com.tommasoberlose.anotherwidget.services

import android.Manifest
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.everything.providers.android.calendar.CalendarProvider
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.collections.ArrayList

class UpdateCalendarWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            UpdatesReceiver.removeUpdates(context)
            val eventRepository = EventRepository(context)

            if (Preferences.showEvents) {
                if (!context.checkGrantedPermission(Manifest.permission.READ_CALENDAR)) {
                    eventRepository.resetNextEventData()
                    eventRepository.clearEvents()
                    Preferences.showEvents = false
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
                            val instances = data.list
                            for (instance in instances) {
                                try {
                                    val e = provider.getEvent(instance.eventId)
                                    if (e == null || e.deleted || CalendarHelper.getFilteredCalendarIdList().contains(e.calendarId))
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
                            eventRepository.saveEvents(sortedEvents)
                            //eventRepository.saveNextEventData(filteredEventList.first())
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
        }

        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "UpdateCalendarWorker",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<UpdateCalendarWorker>().build()
            )
        }
    }
}
