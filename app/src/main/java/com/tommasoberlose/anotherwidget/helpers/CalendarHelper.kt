package com.tommasoberlose.anotherwidget.helpers

import android.Manifest
import android.content.Context
import android.provider.CalendarContract
import com.tommasoberlose.anotherwidget.services.EventListenerJob
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.services.UpdateCalendarService
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import me.everything.providers.android.calendar.CalendarProvider
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by tommaso on 08/10/17.
 */

object CalendarHelper {
    fun updateEventList(context: Context) {
        UpdateCalendarService.enqueueWork(context)
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
        return Preferences.calendarFilter.split(",").map { it.replace(" ", "") }
            .filter { it != "" }.map { it.toLong() }
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

    fun List<Event>.applyFilters() : List<Event> {
        return this
            .asSequence()
            .filter { (Preferences.showDeclinedEvents || it.selfAttendeeStatus != CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) }
            .filter { (Preferences.showAcceptedEvents || it.selfAttendeeStatus != CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED) }
            .filter { (Preferences.showInvitedEvents || it.selfAttendeeStatus != CalendarContract.Attendees.ATTENDEE_STATUS_INVITED) }
            .filter { (Preferences.calendarAllDay || !it.allDay) }
            .filter { (!Preferences.showOnlyBusyEvents || it.availability != CalendarContract.EventsEntity.AVAILABILITY_FREE) }
            .toList()
    }

    fun List<Event>.sortEvents(): List<Event> {
        return sortedWith { event: Event, event1: Event ->
            val date = Calendar.getInstance().apply { timeInMillis = event.startDate }
            val date1 = Calendar.getInstance().apply { timeInMillis = event1.startDate }

            if (date.get(Calendar.DAY_OF_YEAR) == date1.get(Calendar.DAY_OF_YEAR) && date.get(
                    Calendar.YEAR) == date1.get(Calendar.YEAR)
            ) {
                if (event.allDay && event1.allDay) {
                    event.startDate.compareTo(event1.startDate)
                } else if (event.allDay) {
                    1
                } else if (event1.allDay) {
                    -1
                } else {
                    event.startDate.compareTo(event1.startDate)
                }
            } else {
                event.startDate.compareTo(event1.startDate)
            }
        }
    }
}