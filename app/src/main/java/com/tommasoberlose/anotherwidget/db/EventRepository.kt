package com.tommasoberlose.anotherwidget.db

import android.content.Context
import android.util.Log
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.services.UpdatesWorker
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import io.realm.Realm
import io.realm.RealmResults

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
        Preferences.nextEventId = event.id
    }

    fun getNextEvent(): Event? = realm.where(Event::class.java).equalTo("id", Preferences.nextEventId).findFirst() ?: realm.where(Event::class.java).findFirst()

    fun goToNextEvent() {
        val eventList = realm.where(Event::class.java).findAll()

        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.id == Preferences.nextEventId }
            if (index > -1 && index < eventList.size - 1) {
                Preferences.nextEventId = eventList[index + 1]!!.id
            } else {
                Preferences.nextEventId = eventList.first()!!.id
            }
        } else {
            resetNextEventData()
        }
        UpdatesWorker.setUpdates(context)
        MainWidget.updateWidget(context)
    }

    fun goToPreviousEvent() {
        val eventList = realm.where(Event::class.java).findAll()

        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.id == Preferences.nextEventId }
            if (index > 0) {
                Preferences.nextEventId = eventList[index - 1]!!.id
            } else {
                Preferences.nextEventId = eventList.last()!!.id
            }
        } else {
            resetNextEventData()
        }
        UpdatesWorker.setUpdates(context)
        MainWidget.updateWidget(context)
    }

    fun getEvents(): RealmResults<Event> = realm.where(Event::class.java).findAll()

    fun getEventsCount(): Int = realm.where(Event::class.java).findAll().size
}