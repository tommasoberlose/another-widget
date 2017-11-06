package com.tommasoberlose.anotherwidget.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
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
import io.realm.Realm
import me.everything.providers.android.calendar.CalendarProvider
import me.everything.providers.android.contacts.ContactsProvider
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Comparator
import kotlin.collections.ArrayList

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
                6 -> limit.add(Calendar.MINUTE, 30)
                7 -> limit.add(Calendar.HOUR, 1)
                else -> limit.add(Calendar.HOUR, 6)
            }


            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, now.timeInMillis)
            ContentUris.appendId(builder, limit.timeInMillis)

            if (!Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
                resetNextEventData(context)
            } else {
                val provider = CalendarProvider(context)
                val data = provider.getInstances(now.timeInMillis, limit.timeInMillis)
                if (data != null) {
                    val instances = data.list
                    for (instance in instances) {
                        val e = provider.getEvent(instance.eventId)
                        if (e != null && instance.begin <= limit.timeInMillis && (SP.getBoolean(Constants.PREF_CALENDAR_ALL_DAY, false) || !e.allDay) && !(SP.getString(Constants.PREF_CALENDAR_FILTER, "").contains(" " + e.calendarId + ",")) && (SP.getBoolean(Constants.PREF_SHOW_DECLINED_EVENTS, true) || !e.selfAttendeeStatus.equals(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED))) {
                            if (e.allDay) {
                                val start = Calendar.getInstance()
                                start.timeInMillis = instance.begin
                                val end = Calendar.getInstance()
                                end.timeInMillis = instance.end
                                instance.begin = start.timeInMillis - start.timeZone.getOffset(start.timeInMillis)
                                instance.end = end.timeInMillis - end.timeZone.getOffset(end.timeInMillis)
                            }
                            eventList.add(Event(instance.id, e.id, e.title, instance.begin, instance.end, e.calendarId.toInt(), e.allDay, e.eventLocation ?: ""))
                        }
                    }
                }

                if (eventList.isEmpty()) {
                    resetNextEventData(context)
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
                    saveEvents(context, eventList)
                    saveNextEventData(context, eventList[0])
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
        val data = provider.calendars
        if (data != null) {
            return data.list
        } else {
            return calendarList
        }
    }

    fun saveEvents(context: Context, eventList: ArrayList<Event>) {
        val db = Util.getRealInstance(context)
        db.executeTransaction { realm ->
            realm.where(Event::class.java).findAll().deleteAllFromRealm()
            realm.copyToRealm(eventList)
        }
    }

    @SuppressLint("ApplySharedPref")
    fun resetNextEventData(context: Context) {
        val db = Util.getRealInstance(context)
        db.executeTransaction {
            db.where(Event::class.java).findAll().deleteAllFromRealm()
        }
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        SP.edit()
                .remove(Constants.PREF_EVENT_ID)
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
                .putLong(Constants.PREF_EVENT_ID, event.id)
                .commit()
        Util.updateWidget(context)
    }

    fun getNextEvent(context: Context): Event {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val db = Util.getRealInstance(context)
        val nextEvent = db.where(Event::class.java).equalTo("id", SP.getLong(Constants.PREF_EVENT_ID, 0)).findFirst()
        return if (nextEvent != null) {
            nextEvent
        } else {
            val eventList = db.where(Event::class.java).findAll()
            if (eventList.isNotEmpty()) {
                eventList[0] ?: Event()
            } else {
                Event()
            }
        }
    }

    @SuppressLint("ApplySharedPref")
    fun goToNextEvent(context: Context) {
        val db = Util.getRealInstance(context)
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val eventList = db.where(Event::class.java).findAll()

        if (eventList.isNotEmpty()) {
            var found = false
            for (e in eventList) {
                if (e.id == SP.getLong(Constants.PREF_EVENT_ID, 0)) {
                    if (eventList.indexOf(e) < eventList.size - 1) {
                        SP.edit()
                                .putLong(Constants.PREF_EVENT_ID, eventList[eventList.indexOf(e) + 1]?.id ?: 0)
                                .commit()
                    } else {
                        SP.edit()
                                .putLong(Constants.PREF_EVENT_ID, eventList[0]?.id ?: 0)
                                .commit()
                    }
                    found = true
                    break
                }
            }

            if (!found) {
                SP.edit()
                        .putLong(Constants.PREF_EVENT_ID, eventList[0]?.id ?: 0)
                        .commit()
            }
        } else {
            resetNextEventData(context)
        }

        context.sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
    }

    fun getEventsCount(context: Context): Int {
        val db = Util.getRealInstance(context)
        return db.where(Event::class.java).findAll().size
    }
}