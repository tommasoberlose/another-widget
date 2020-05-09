package com.tommasoberlose.anotherwidget.db

import android.content.Context
import android.util.Log
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import io.realm.Realm
import io.realm.RealmResults
import java.util.*
import kotlin.collections.ArrayList

class EventRepository(val context: Context) {
    private val realm by lazy { Realm.getDefaultInstance() }

    fun saveEvents(eventList: ArrayList<Event>) {
        realm.executeTransactionAsync { realm ->
            realm.where(Event::class.java).findAll().deleteAllFromRealm()
            realm.copyToRealm(eventList)
        }
    }

    fun resetNextEventData() {
        realm.executeTransactionAsync {
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
    }

    fun saveNextEventData(event: Event) {
        Preferences.nextEventId = event.eventID
    }

    fun getNextEvent(): Event? {
        val nextEvent = getEventByEventId(Preferences.nextEventId)
        return if (nextEvent != null && nextEvent.endDate > Calendar.getInstance().timeInMillis) {
            nextEvent
        } else {
            val events = getEvents()
            if (events.isNotEmpty()) {
                val newNextEvent = events.first()
                Preferences.nextEventId = newNextEvent!!.eventID
                newNextEvent
            } else {
                resetNextEventData()
                null
            }
        }
    }

    fun getEventByEventId(id: Long): Event? = realm.where(Event::class.java).equalTo("eventID", id).findFirst()

    fun goToNextEvent() {
        val eventList = getEvents()
        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.eventID == Preferences.nextEventId }
            if (index > -1 && index < eventList.size - 1) {
                Preferences.nextEventId = eventList[index + 1]!!.eventID
            } else {
                Preferences.nextEventId = eventList.first()!!.eventID
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
                Preferences.nextEventId = eventList[index - 1]!!.eventID
            } else {
                Preferences.nextEventId = eventList.last()!!.eventID
            }
        } else {
            resetNextEventData()
        }
        UpdatesReceiver.setUpdates(context)
        MainWidget.updateWidget(context)
    }

    fun getEvents(): RealmResults<Event> {
        val now = Calendar.getInstance().timeInMillis
        return realm.where(Event::class.java).greaterThan("endDate", now).findAll()
    }

    fun getEventsCount(): Int = getEvents().size
}