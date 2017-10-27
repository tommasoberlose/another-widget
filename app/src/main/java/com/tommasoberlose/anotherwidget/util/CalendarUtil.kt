package com.tommasoberlose.anotherwidget.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.CalendarContract
import android.util.Log
import android.util.TimeUtils
import android.widget.Toast
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.`object`.CalendarSelector
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.`object`.Event
import me.everything.providers.android.calendar.CalendarProvider
import me.everything.providers.android.contacts.ContactsProvider
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Comparator

/**
 * Created by tommaso on 08/10/17.
 */

object CalendarUtil {

    fun updateEventList(context: Context) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (SP.getBoolean(Constants.PREF_SHOW_EVENTS, true)) {
            val eventList = ArrayList<Event>()

            val now = Calendar.getInstance()
            val limit = Calendar.getInstance()
            when (SP.getInt(Constants.PREF_SHOW_UNTIL, 1)) {
                0 -> limit.add(Calendar.HOUR, 3)
                1 -> limit.add(Calendar.HOUR, 6)
                2 -> limit.add(Calendar.HOUR, 12)
                3 -> limit.add(Calendar.DAY_OF_MONTH, 1)
                4 -> limit.add(Calendar.DAY_OF_MONTH, 3)
                5 -> limit.add(Calendar.DAY_OF_MONTH, 7)
                6 -> limit.add(Calendar.HOUR, 1)
                else -> limit.add(Calendar.HOUR, 6)
            }


            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, now.timeInMillis)
            ContentUris.appendId(builder, limit.timeInMillis)

            if (!Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
                resetNextEventData(context)
            } else {
                val provider = CalendarProvider(context)
                val instances = provider.getInstances(now.timeInMillis, limit.timeInMillis).list
                for (instance in instances) {
                    val e = provider.getEvent(instance.eventId)
                    if (e != null && (SP.getBoolean(Constants.PREF_CALENDAR_ALL_DAY, false) || !e.allDay) && !(SP.getString(Constants.PREF_CALENDAR_FILTER, "").contains(" " + e.calendarId + ",")) && (SP.getBoolean(Constants.PREF_SHOW_DECLINED_EVENTS, true) || !e.selfAttendeeStatus.equals(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED))) {
                        eventList.add(Event(e.id.toInt(), e.title, e.dTStart, e.dTend, e.calendarId.toInt(), e.allDay, e.eventLocation))
                    }
                }

                if (eventList.isEmpty()) {
                    resetNextEventData(context)
                } else {
                    eventList.sortWith(Comparator { event: Event, event1: Event ->
                        if (event.allDay && event1.allDay) {
                            0
                        } else {
                            if (event.allDay) {
                                Log.d("AW1", event.title + " " + event.startDate + " - " + event1.title + " " + event1.startDate)
                                if (TimeUnit.MILLISECONDS.toMinutes(event1.startDate - now.timeInMillis) > 31 || now.timeInMillis > event1.endDate) {
                                    1
                                } else {
                                    -1
                                }
                            } else if (event1.allDay) {
                                Log.d("AW2", event.title + " " + event.startDate + " - " + event1.title + " " + event1.startDate)
                                if (TimeUnit.MILLISECONDS.toMinutes(event.startDate - now.timeInMillis) > 31 || now.timeInMillis > event.endDate) {
                                    -1
                                } else {
                                    1
                                }
                            } else {
                                if (event.startDate > event1.startDate) {
                                    1
                                } else if (event.startDate < event1.startDate) {
                                    -1
                                }
                                0
                            }
                        }
                    })
                    saveNextEventData(context, eventList.get(0))
                }
            }
        } else {
            resetNextEventData(context)
        }

    }

    fun getCalendarList(context: Context): List<me.everything.providers.android.calendar.Calendar> {
        val calendarList = ArrayList<me.everything.providers.android.calendar.Calendar>()

        if (!Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
            return calendarList
        }
        val provider = CalendarProvider(context)
        return provider.calendars.list

/*
        try {
            val calendarCursor = context.contentResolver.query(Uri.parse("content://com.android.calendar/calendars"),
                    arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME),
                    null,
                    null,
                    null)

            if (calendarCursor != null && calendarCursor.count > 0) {

                calendarCursor.moveToFirst()

                for (j in 0 until calendarCursor.count) {
                    val id = calendarCursor.getInt(0)
                    val name = calendarCursor.getString(1)
                    val account = calendarCursor.getString(2)
                    calendarList.add(CalendarSelector(id, name, account))
                    calendarCursor.moveToNext()
                }

                calendarCursor.close()
            } else {
                Toast.makeText(context, R.string.error_no_calendar, Toast.LENGTH_SHORT).show()
            }
        } catch (ignored: Exception) {
            ignored.printStackTrace()
            try {
                val calendarCursor = context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                        arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME),
                        null,
                        null,
                        null)

                if (calendarCursor != null && calendarCursor.count > 0) {

                    calendarCursor.moveToFirst()

                    for (j in 0 until calendarCursor.count) {
                        calendarList.add(CalendarSelector(calendarCursor.getInt(0), calendarCursor.getString(1), calendarCursor.getString(2)))
                        calendarCursor.moveToNext()
                    }

                    calendarCursor.close()
                }
            } catch (ignore: Exception) {
                ignore.printStackTrace()
            } finally {
                return calendarList
            }
        } finally {
            return calendarList
        }*/
    }

    @SuppressLint("ApplySharedPref")
    fun resetNextEventData(context: Context) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        SP.edit()
                .remove(Constants.PREF_NEXT_EVENT_ID)
                .remove(Constants.PREF_NEXT_EVENT_NAME)
                .remove(Constants.PREF_NEXT_EVENT_START_DATE)
                .remove(Constants.PREF_NEXT_EVENT_END_DATE)
                .remove(Constants.PREF_NEXT_EVENT_ALL_DAY)
                .remove(Constants.PREF_NEXT_EVENT_CALENDAR_ID)
                .remove(Constants.PREF_NEXT_EVENT_LOCATION)
                .commit()
        Util.updateWidget(context)
    }

    @SuppressLint("ApplySharedPref")
    fun saveNextEventData(context: Context, event: Event) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        SP.edit()
                .putInt(Constants.PREF_NEXT_EVENT_ID, event.id)
                .putString(Constants.PREF_NEXT_EVENT_NAME, event.title)
                .putLong(Constants.PREF_NEXT_EVENT_START_DATE, event.startDate)
                .putLong(Constants.PREF_NEXT_EVENT_END_DATE, event.endDate)
                .putBoolean(Constants.PREF_NEXT_EVENT_ALL_DAY, event.allDay)
                .putInt(Constants.PREF_NEXT_EVENT_CALENDAR_ID, event.calendarID)
                .putString(Constants.PREF_NEXT_EVENT_LOCATION, event.address)
                .commit()
        Util.updateWidget(context)
    }

    fun getNextEvent(context: Context): Event {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return Event(SP.getInt(Constants.PREF_NEXT_EVENT_ID, 0), SP.getString(Constants.PREF_NEXT_EVENT_NAME, ""), SP.getLong(Constants.PREF_NEXT_EVENT_START_DATE, 0), SP.getLong(Constants.PREF_NEXT_EVENT_END_DATE, 0), SP.getInt(Constants.PREF_NEXT_EVENT_CALENDAR_ID, 0), SP.getBoolean(Constants.PREF_NEXT_EVENT_ALL_DAY, false), SP.getString(Constants.PREF_NEXT_EVENT_LOCATION, ""))
    }
}