package com.tommasoberlose.anotherwidget.helpers

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.util.Log
import com.tommasoberlose.anotherwidget.services.EventListenerJob
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.services.UpdateCalendarJob
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
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
        UpdateCalendarJob.enqueueWork(context, Intent())
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
}