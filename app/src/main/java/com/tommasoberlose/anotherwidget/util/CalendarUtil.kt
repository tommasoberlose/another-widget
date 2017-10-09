package com.tommasoberlose.anotherwidget.util

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.provider.CalendarContract
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.`object`.Event
import java.util.*

/**
 * Created by tommaso on 08/10/17.
 */

object CalendarUtil {

    fun getNextEvent(context: Context): List<Event> {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val eventList = ArrayList<Event>()

        val now = Calendar.getInstance()
        val hourLimit = Calendar.getInstance()
        hourLimit.add(Calendar.HOUR, 6)

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now.timeInMillis)
        ContentUris.appendId(builder, hourLimit.timeInMillis)

        if (!Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
            return eventList
        }

        val instanceCursor = context.contentResolver.query(builder.build(), arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END), null, null, null) ?: return eventList
        instanceCursor.moveToFirst()

        for (i in 0 until instanceCursor.count) {
            val ID = instanceCursor.getInt(0)

            val eventCursor = context.contentResolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.ALL_DAY),
                    CalendarContract.Events._ID + " is ?",
                    arrayOf(Integer.toString(ID)), null) ?: return eventList
            eventCursor.moveToFirst()

            for (j in 0 until eventCursor.count) {
                val e = Event(eventCursor, instanceCursor)
                val allDay: Boolean = !eventCursor.getString(1).equals("0")
                if (e.endDate - now.timeInMillis > 1000 * 60 * 30 && (SP.getBoolean(Constants.PREF_CALENDAR_ALL_DAY, false) || !allDay)) {
                    eventList.add(e)
                }
                eventCursor.moveToNext()
            }

            eventCursor.close()

            instanceCursor.moveToNext()
        }

        instanceCursor.close()
        return eventList
    }
    fun getCalendarList(context: Context): List<com.tommasoberlose.anotherwidget.`object`.Calendar> {
        val calendarList = ArrayList<com.tommasoberlose.anotherwidget.`object`.Calendar>()

        if (!Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
            return calendarList
        }

        val calendarCursor = context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.NAME, CalendarContract.Calendars.ACCOUNT_NAME),
                null,
                null,
                null) ?: return calendarList

        calendarCursor.moveToFirst()

        for (j in 0 until calendarCursor.count) {
            calendarList.add(com.tommasoberlose.anotherwidget.`object`.Calendar(calendarCursor.getInt(0), calendarCursor.getString(1), calendarCursor.getString(2)))
            calendarCursor.moveToNext()
        }

        calendarCursor.close()

        return calendarList
    }
}