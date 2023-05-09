package com.tommasoberlose.anotherwidget.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.*
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.setExactIfCanSchedule
import java.util.*
import org.joda.time.Period


class UpdatesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED -> {
                CalendarHelper.updateEventList(context)
                MediaPlayerHelper.updatePlayingMediaInfo(context)
                ActiveNotificationsHelper.clearLastNotification(context)
                GreetingsHelper.toggleGreetings(context)
            }

            Intent.ACTION_DATE_CHANGED,
            Actions.ACTION_CALENDAR_UPDATE -> {
                CalendarHelper.updateEventList(context)
            }

            Actions.ACTION_TIME_UPDATE -> {
                MainWidget.updateWidget(context)
                if (intent.hasExtra(EVENT_ID)) {
                    setUpdates(context, intent.getLongExtra(EVENT_ID, -1))
                }
            }

            "com.sec.android.widgetapp.APPWIDGET_RESIZE",
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Actions.ACTION_ALARM_UPDATE,
            Actions.ACTION_UPDATE_GREETINGS -> {
                MainWidget.updateWidget(context)
            }

            Actions.ACTION_CLEAR_NOTIFICATION -> {
                ActiveNotificationsHelper.clearLastNotification(context)
            }

            Actions.ACTION_REFRESH -> {
                CalendarHelper.updateEventList(context)
                MediaPlayerHelper.updatePlayingMediaInfo(context)
                WeatherHelper.updateWeather(context)
            }
        }
    }

    companion object {
        const val EVENT_ID = "EVENT_ID"

        fun setUpdates(context: Context, eventId: Long? = null) {
            if (!Preferences.showEvents)
                return
            val eventRepository = EventRepository(context)
            if (eventId == null) {
                // schedule ACTION_CALENDAR_UPDATE at midnight (ACTION_DATE_CHANGED no longer works)
                with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                    setExactIfCanSchedule(
                        AlarmManager.RTC,
                        Calendar.getInstance().apply {
                            set(Calendar.MILLISECOND, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.HOUR_OF_DAY, 0)
                            add(Calendar.DATE, 1)
                        }.timeInMillis,
                        PendingIntent.getBroadcast(
                            context,
                            0,
                            Intent(context, UpdatesReceiver::class.java).apply {
                                action = Actions.ACTION_CALENDAR_UPDATE
                            },
                            PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }

                eventRepository.getFutureEvents().forEach { event ->
                    setEventUpdate(context, event)
                }
            } else {
                val event = eventRepository.getEventById(eventId)
                if (event != null) {
                    setEventUpdate(context, event)
                }
            }
            eventRepository.close()
        }

        private fun setEventUpdate(context: Context, event: Event) {
            val now = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diff = Period(now.timeInMillis, event.startDate, org.joda.time.PeriodType.time())
            val limit = when (Preferences.showUntil) {
                0 -> 1000 * 60 * 60 * 3
                1 -> 1000 * 60 * 60 * 6
                2 -> 1000 * 60 * 60 * 12
                3 -> 1000 * 60 * 60 * 24
                4 -> 1000 * 60 * 60 * 24 * 3
                5 -> 1000 * 60 * 60 * 24 * 7
                6 -> 1000 * 60 * 30
                7 -> 1000 * 60 * 60
                else -> 1000 * 60 * 60 * 6
            }
            val fireTime = when {
                event.startDate <= now.timeInMillis
                    -> event.endDate
                event.startDate > now.timeInMillis + limit
                    -> event.startDate - limit
                !Preferences.showDiffTime
                    -> return
                event.allDay
                    -> event.startDate
                diff.hours > 12
                    -> event.startDate - 12 * 1000 * 60 * 60 + 1000 * 60
                diff.hours > 0
                    -> event.startDate - diff.hours * 1000 * 60 * 60 + 1000 * 60
                else
                    -> event.startDate - 1000 * 60 * when (Preferences.widgetUpdateFrequency) {
                        Constants.WidgetUpdateFrequency.DEFAULT.rawValue -> {
                            when {
                                diff.minutes >= 45 -> 44
                                diff.minutes >= 30 -> 29
                                diff.minutes >= 15 -> 14
                                else -> 0
                            }
                        }
                        Constants.WidgetUpdateFrequency.HIGH.rawValue -> {
                            when {
                                diff.minutes >= 5 -> diff.minutes - diff.minutes % 5 - 1
                                else -> 0
                            }
                        }
                        else -> 0
                    }
            }
            // no need to schedule updates after the next ACTION_CALENDAR_UPDATE
            if (Calendar.getInstance().apply {
                set(Calendar.MILLISECOND, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.HOUR_OF_DAY, 0)
                add(Calendar.DATE, 1)
            }.timeInMillis <= fireTime) return
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                setExactIfCanSchedule(
                    AlarmManager.RTC,
                    fireTime.coerceAtLeast(now.timeInMillis + 1000 * 60),
                    PendingIntent.getBroadcast(
                        context,
                        event.id.toInt(),
                        Intent(context, UpdatesReceiver::class.java).apply {
                            action = Actions.ACTION_TIME_UPDATE
                            if (event.startDate > now.timeInMillis)
                                putExtra(EVENT_ID, event.id)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
            }
        }

        fun removeUpdates(context: Context) {
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                cancel(PendingIntent.getBroadcast(context, 0, Intent(context, UpdatesReceiver::class.java).apply {
                    action = Actions.ACTION_CALENDAR_UPDATE
                }, PendingIntent.FLAG_IMMUTABLE))
                val eventRepository = EventRepository(context)
                eventRepository.getFutureEvents().forEach {
                    cancel(PendingIntent.getBroadcast(context, it.id.toInt(), Intent(context, UpdatesReceiver::class.java).apply {
                        action = Actions.ACTION_TIME_UPDATE
                    }, PendingIntent.FLAG_IMMUTABLE))
                }
                eventRepository.close()
            }
        }
    }
}
