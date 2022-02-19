package com.tommasoberlose.anotherwidget.db

import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper.applyFilters
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper.sortEvents
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

class EventRepository(val context: Context) {
    private val db by lazy { EventDatabase.getDatabase(context) }

    fun saveEvents(eventList: List<Event>) {
        db.runInTransaction{
            db.dao().run {
                deleteAll()
                insertAll(eventList)
            }
        }
    }

    fun clearEvents() {
        db.dao().deleteAll()
    }

    fun resetNextEventData() {
        Preferences.bulk {
            remove(Preferences::nextEventId)
        }
    }

    fun saveNextEventData(event: Event) {
        Preferences.nextEventId = event.id
    }

    fun getNextEvent(): Event? {
        val nextEvent = getEventById(Preferences.nextEventId)
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
        return if (nextEvent != null && nextEvent.endDate > now && nextEvent.startDate <= limit.timeInMillis) {
            nextEvent
        } else {
            val events = getEvents()
            if (events.isNotEmpty()) {
                val newNextEvent = events.first()
                saveNextEventData(newNextEvent)
                newNextEvent
            } else {
                resetNextEventData()
                null
            }
        }
    }

    fun getEventById(id: Long): Event? {
        return db.dao().findById(id)
    }

    fun goToNextEvent() {
        val eventList = getEvents()
        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.id == Preferences.nextEventId }
            if (index > -1 && index < eventList.size - 1) {
                saveNextEventData(eventList[index + 1])
            } else {
                saveNextEventData(eventList.first())
            }
        } else {
            resetNextEventData()
        }
        MainWidget.updateWidget(context)
        org.greenrobot.eventbus.EventBus.getDefault().post(
            com.tommasoberlose.anotherwidget.ui.fragments.MainFragment.UpdateUiMessageEvent()
        )
    }

    fun goToPreviousEvent() {
        val eventList = getEvents()
        if (eventList.isNotEmpty()) {
            val index = eventList.indexOfFirst { it.id == Preferences.nextEventId }
            if (index > 0) {
                saveNextEventData(eventList[index - 1])
            } else {
                saveNextEventData(eventList.last())
            }
        } else {
            resetNextEventData()
        }
        MainWidget.updateWidget(context)
        org.greenrobot.eventbus.EventBus.getDefault().post(
            com.tommasoberlose.anotherwidget.ui.fragments.MainFragment.UpdateUiMessageEvent()
        )
    }

    fun getFutureEvents(): List<Event> {
        return db.dao().findFuture(Calendar.getInstance().timeInMillis).sortEvents()
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
        return db.dao().find(now, limit.timeInMillis).sortEvents()
    }

    fun getEventsCount(): Int = db.dao().countAll()

    fun close() {
        // db.close()
    }

    @Dao
    interface EventDao {
        @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
        fun findById(id: Long) : Event?

        @Query("SELECT * FROM events WHERE end_date > :from")
        fun findFuture(from: Long) : List<Event>

        @Query("SELECT * FROM events WHERE end_date > :from AND start_date <= :to")
        fun find(from: Long, to: Long) : List<Event>

        @Query("SELECT count(*) FROM events")
        fun countAll() : Int

        @Insert
        fun insertAll(events: List<Event>)

        @Query("DELETE FROM events")
        fun deleteAll()
    }

    @Database(entities = arrayOf(Event::class), version = 1, exportSchema = false)
    abstract class EventDatabase : RoomDatabase() {
        abstract fun dao(): EventDao

        companion object {
            private var INSTANCE: EventDatabase? = null

            fun getDatabase(context: Context): EventDatabase {
                // if the INSTANCE is not null, then return it,
                // if it is, then create the database
                return INSTANCE ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        EventDatabase::class.java,
                        "events"
                    ).allowMainThreadQueries().build()
                    INSTANCE = instance
                    // return instance
                    instance
                }
            }
        }
    }
}
