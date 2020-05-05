package com.tommasoberlose.anotherwidget.helpers

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.tommasoberlose.anotherwidget.services.EventListenerJob
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import me.everything.providers.android.calendar.CalendarProvider
import org.greenrobot.eventbus.EventBus
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * Created by tommaso on 08/10/17.
 */

object CalendarHelper {

    fun updateEventList(context: Context) {
        val eventRepository = EventRepository(context)
        if (Preferences.showEvents) {
            val eventList = ArrayList<Event>()

            val now = Calendar.getInstance()
            val limit = Calendar.getInstance()
            when (Preferences.showUntil) {
                0 -> limit.add(Calendar.HOUR, 3)
                1 -> limit.add(Calendar.HOUR, 6)
                2 -> limit.add(Calendar.HOUR, 12)
                3 -> limit.add(Calendar.DAY_OF_MONTH, 1)
                4 -> limit.add(Calendar.DAY_OF_MONTH, 3)
                5 -> limit.add(Calendar.DAY_OF_MONTH, 7)
                6 -> limit.add(Calendar.MINUTE, 30)
                7 -> limit.add(Calendar.HOUR, 1)
                else -> limit.add(Calendar.HOUR, 6)
            }

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, now.timeInMillis)
            ContentUris.appendId(builder, limit.timeInMillis)

            if (!context.checkGrantedPermission(
                    Manifest.permission.READ_CALENDAR
                )
            ) {
                eventRepository.resetNextEventData()
            } else {
                try {
                    val provider = CalendarProvider(context)
                    val data = provider.getInstances(now.timeInMillis, limit.timeInMillis)
                    if (data != null) {
                        val instances = data.list
                        for (instance in instances) {
                            try {
                                val e = provider.getEvent(instance.eventId)
                                if (e != null && !e.deleted && instance.begin <= limit.timeInMillis && (Preferences.calendarAllDay || !e.allDay) && !getFilteredCalendarIdList().contains(
                                        e.calendarId
                                    ) && (Preferences.showDeclinedEvents || e.selfAttendeeStatus.toInt() != CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED)
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
                                            instance.id,
                                            e.id,
                                            e.title ?: "",
                                            instance.begin,
                                            instance.end,
                                            e.calendarId.toInt(),
                                            e.allDay,
                                            e.eventLocation ?: ""
                                        )
                                    )
                                }
                            } catch (ignored: Exception) {
                            }
                        }
                    }

                    if (eventList.isEmpty()) {
                        eventRepository.resetNextEventData()
                    } else {
                        eventList.sortWith(Comparator { event: Event, event1: Event ->
                            if (event.allDay && event1.allDay) {
                                event.startDate.compareTo(event1.startDate)
                            } else if (event.allDay) {
                                1
                            } else if (event1.allDay) {
                                -1
                            } else {
                                event1.startDate.compareTo(event.startDate)
                            }
                        })
                        eventList.reverse()
                        eventRepository.saveEvents(
                            eventList
                        )
                        eventRepository.saveNextEventData(
                            eventList[0]
                        )
                    }
                } catch (ignored: java.lang.Exception) {

                }
            }
        } else {
            eventRepository.resetNextEventData()
        }

        UpdatesReceiver.setUpdates(context)
        Log.d("ciao", "force update? 7")
        MainWidget.updateWidget(context)

        EventBus.getDefault().post(MainActivity.UpdateUiMessageEvent())
    }

    fun getCalendarList(context: Context): List<me.everything.providers.android.calendar.Calendar> {
        val calendarList = ArrayList<me.everything.providers.android.calendar.Calendar>()

        if (!context.checkGrantedPermission(
                Manifest.permission.READ_CALENDAR
            )
        ) {
            return calendarList
        }
        val provider = CalendarProvider(context)
        val data = provider.calendars
        return if (data != null) {
            data.list
        } else {
            calendarList
        }
    }

    fun getFilteredCalendarIdList(): List<Long> {
        return Preferences.calendarFilter.split(",").map { it.replace(" ", "") }.filter { it != "" }.map { it.toLong() }
    }

    fun filterCalendar(list: List<Long>) {
        Preferences.calendarFilter = list.joinToString(separator = ",", prefix = " ")
    }

    fun setEventUpdatesAndroidN(context: Context) {
        EventListenerJob.schedule(context)
    }

    fun removeEventUpdatesAndroidN(context: Context) {
        EventListenerJob.remove(context)
    }
}