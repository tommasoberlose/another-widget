package com.tommasoberlose.anotherwidget.utils

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import com.chibatching.kotpref.blockingBulk
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.components.events.Event
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import io.realm.Realm
import io.realm.RealmResults
import me.everything.providers.android.calendar.CalendarProvider
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

/**
 * Created by tommaso on 08/10/17.
 */

object CalendarUtil {

    fun updateEventList(context: Context) {
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

            if (!Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
                resetNextEventData(context)
            } else {
                val provider = CalendarProvider(context)
                val data = provider.getInstances(now.timeInMillis, limit.timeInMillis)
                if (data != null) {
                    val instances = data.list
                    for (instance in instances) {
                        try {
                            val e = provider.getEvent(instance.eventId)
                            if (e != null && instance.begin <= limit.timeInMillis && (Preferences.calendarAllDay || !e.allDay) && !(Preferences.calendarFilter.contains(" " + e.calendarId + ",")) && (Preferences.showDeclinedEvents || e.selfAttendeeStatus.toInt() != CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED)) {
                                if (e.allDay) {
                                    val start = Calendar.getInstance()
                                    start.timeInMillis = instance.begin
                                    val end = Calendar.getInstance()
                                    end.timeInMillis = instance.end
                                    instance.begin = start.timeInMillis - start.timeZone.getOffset(start.timeInMillis)
                                    instance.end = end.timeInMillis - end.timeZone.getOffset(end.timeInMillis)
                                }
                                eventList.add(Event(instance.id, e.id, e.title ?: "", instance.begin, instance.end, e.calendarId.toInt(), e.allDay, e.eventLocation ?: ""))
                            }
                        } catch (ignored: Exception) {}
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
                    saveEvents(eventList)
                    saveNextEventData(context, eventList[0])
                }
            }
        } else {
            resetNextEventData(context)
        }

        UpdatesReceiver.setUpdates(context)
        Util.updateWidget(context)
    }

    fun getCalendarList(context: Context): List<me.everything.providers.android.calendar.Calendar> {
        val calendarList = ArrayList<me.everything.providers.android.calendar.Calendar>()

        if (!Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
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

    private fun saveEvents(eventList: ArrayList<Event>) {
        Realm.getDefaultInstance().executeTransactionAsync { realm ->
            realm.where(Event::class.java).findAll().deleteAllFromRealm()
            realm.copyToRealm(eventList)
        }
    }

    private fun resetNextEventData(context: Context) {
        Realm.getDefaultInstance().executeTransactionAsync {
            it.where(Event::class.java).findAll().deleteAllFromRealm()
        }

        Preferences.bulk {
            remove(Preferences::nextEventId)
            remove(Preferences::nextEventName)
            remove(Preferences::nextEventStartDate)
            remove(Preferences::nextEventAllDay)
            remove(Preferences::nextEventLocation)
            remove(Preferences::nextEventEndDate)
            remove(Preferences::nextEventCalendarId)
        }

        Util.updateWidget(context)
    }

    private fun saveNextEventData(context: Context, event: Event) {
        Preferences.nextEventId = event.id
        Util.updateWidget(context)
    }

    fun getNextEvent(): Event? {
        val realm = Realm.getDefaultInstance()
        return realm.where(Event::class.java).equalTo("id", Preferences.nextEventId).findFirst() ?: realm.where(Event::class.java).findFirst()
    }

    fun goToNextEvent(context: Context) {
        val eventList = Realm.getDefaultInstance().where(Event::class.java).findAll()

        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.id == Preferences.nextEventId }
            if (index > -1 && index < eventList.size - 1) {
                Preferences.nextEventId = eventList[index + 1]!!.id
            } else {
                Preferences.nextEventId = eventList.first()!!.id
            }
        } else {
            resetNextEventData(context)
        }
        UpdatesReceiver.setUpdates(context)
        Util.updateWidget(context)
    }

    fun goToPreviousEvent(context: Context) {
        val eventList = Realm.getDefaultInstance().where(Event::class.java).findAll()

        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.id == Preferences.nextEventId }
            if (index > 0) {
                Preferences.nextEventId = eventList[index - 1]!!.id
            } else {
                Preferences.nextEventId = eventList.last()!!.id
            }
        } else {
            resetNextEventData(context)
        }
        UpdatesReceiver.setUpdates(context)
        Util.updateWidget(context)
    }

    fun getEvents(): RealmResults<Event> = Realm.getDefaultInstance().where(Event::class.java).findAll()
    fun getEventsCount(): Int = Realm.getDefaultInstance().where(Event::class.java).findAll().size
}