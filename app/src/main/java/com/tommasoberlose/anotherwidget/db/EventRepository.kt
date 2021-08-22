package com.tommasoberlose.anotherwidget.db

import android.content.Context
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper.applyFilters
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import io.realm.Realm
import java.util.*

class EventRepository(val context: Context) {
    private val realm by lazy { Realm.getDefaultInstance() }

    fun saveEvents(eventList: List<Event>) {
        realm.executeTransaction { realm ->
            realm.where(Event::class.java).findAll().deleteAllFromRealm()
            realm.copyToRealm(eventList)
        }
    }

    fun clearEvents() {
        realm.executeTransaction { realm ->
            realm.where(Event::class.java).findAll().deleteAllFromRealm()
        }
    }

    fun resetNextEventData() {
        Preferences.bulk {
            remove(Preferences::nextEventId)
            remove(Preferences::nextEventName)
            remove(Preferences::nextEventStartDate)
            remove(Preferences::nextEventAllDay)
            remove(Preferences::nextEventLocation)
            remove(Preferences::nextEventEndDate)
            remove(Preferences::nextEventCalendarId)
        }
    }

    fun saveNextEventData(event: Event) {
        Preferences.nextEventId = event.eventID
    }

    fun getNextEvent(): Event? {
        val nextEvent = getEventByEventId(Preferences.nextEventId)
        val now = Calendar.getInstance().timeInMillis
        val limit = Calendar.getInstance().apply {
            timeInMillis = now
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
        val event = if (nextEvent != null && nextEvent.endDate > now && nextEvent.startDate < limit.timeInMillis) {
            nextEvent
        } else {
            val events = getEvents()
            if (events.isNotEmpty()) {
                val newNextEvent = events.first()
                Preferences.nextEventId = newNextEvent.eventID
                newNextEvent
            } else {
                resetNextEventData()
                null
            }
        }
        return try {
            realm.copyFromRealm(event!!)
        } catch (ex: Exception) {
            event
        }
    }

    fun getEventByEventId(id: Long): Event? {
        val event = realm.where(Event::class.java).equalTo("eventID", id).findFirst()
        return try {
            realm.copyFromRealm(event!!)
        } catch (ex: Exception) {
            event
        }
    }

    fun goToNextEvent() {
        val eventList = getEvents()
        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.eventID == Preferences.nextEventId }
            if (index > -1 && index < eventList.size - 1) {
                Preferences.nextEventId = eventList[index + 1].eventID
            } else {
                Preferences.nextEventId = eventList.first().eventID
            }
        } else {
            resetNextEventData()
        }
        UpdatesReceiver.setUpdates(context)
        MainWidget.updateWidget(context)
    }

    fun goToPreviousEvent() {
        val eventList = getEvents()
        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.eventID == Preferences.nextEventId }
            if (index > 0) {
                Preferences.nextEventId = eventList[index - 1].eventID
            } else {
                Preferences.nextEventId = eventList.last().eventID
            }
        } else {
            resetNextEventData()
        }
        UpdatesReceiver.setUpdates(context)
        MainWidget.updateWidget(context)
    }

    fun getFutureEvents(): List<Event> {
        val now = Calendar.getInstance().timeInMillis
        realm.refresh()
        return realm
            .where(Event::class.java)
            .greaterThan("endDate", now)
            .findAll()
            .applyFilters()
    }

    private fun getEvents(): List<Event> {
        val now = Calendar.getInstance().timeInMillis
        val limit = Calendar.getInstance().apply {
            timeInMillis = now
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
        realm.refresh()
        return realm
            .where(Event::class.java)
            .greaterThan("endDate", now)
            .lessThanOrEqualTo("startDate", limit.timeInMillis)
            .findAll()
            .applyFilters()
    }

    fun getEventsCount(): Int = getEvents().size

    fun close() {
        realm.close()
    }
}