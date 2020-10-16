package com.tommasoberlose.anotherwidget.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import androidx.core.app.JobIntentService
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
import me.everything.providers.android.calendar.CalendarProvider
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class UpdateCalendarJob : JobIntentService() {

    companion object {
        private const val jobId = 1200

        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, UpdateCalendarJob::class.java, jobId, work)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val eventRepository = EventRepository(this)
        if (Preferences.showEvents) {
            val eventList = ArrayList<Event>()

            val now = Calendar.getInstance()
            val begin = Calendar.getInstance().apply {
                set(Calendar.MILLISECOND, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.HOUR_OF_DAY, 0)
            }
            val limit = Calendar.getInstance().apply {
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

            if (!checkGrantedPermission(
                    Manifest.permission.READ_CALENDAR
                )
            ) {
                eventRepository.resetNextEventData()
            } else {
                try {
                    val provider = CalendarProvider(this)
                    val data = provider.getInstances(begin.timeInMillis, limit.timeInMillis)
                    if (data != null) {
                        val instances = data.list
                        for (instance in instances) {
                            try {
                                val e = provider.getEvent(instance.eventId)
                                if (e != null && !e.deleted && instance.begin <= limit.timeInMillis && now.timeInMillis < instance.end && !CalendarHelper.getFilteredCalendarIdList()
                                        .contains(e.calendarId)
                                ) {
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
                                    eventList.add(
                                        Event(
                                            id = instance.id,
                                            eventID = e.id,
                                            title = e.title ?: "",
                                            startDate = instance.begin,
                                            endDate = instance.end,
                                            calendarID = e.calendarId.toInt(),
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
                    val filteredEventList = sortedEvents
                        .applyFilters()

                    if (filteredEventList.isEmpty()) {
                        eventRepository.resetNextEventData()
                        eventRepository.clearEvents()
                    } else {
                        eventRepository.saveEvents(
                            sortedEvents
                        )
                        eventRepository.saveNextEventData(filteredEventList.first())
                    }
                } catch (ignored: java.lang.Exception) {

                }
            }
        } else {
            eventRepository.resetNextEventData()
        }

        UpdatesReceiver.setUpdates(this)
        MainWidget.updateWidget(this)

        EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
        eventRepository.close()
    }
}