package com.tommasoberlose.anotherwidget.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.models.Event
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import org.joda.time.Period
import java.util.*


class UpdatesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Actions.ACTION_CALENDAR_UPDATE -> CalendarHelper.updateEventList(context)

            "com.sec.android.widgetapp.APPWIDGET_RESIZE",
            Intent.ACTION_DATE_CHANGED,
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            Actions.ACTION_TIME_UPDATE -> {
                Log.d("ciao", "arrivata notifica")
                MainWidget.updateWidget(context)
                if (intent.hasExtra(EVENT_ID)) {
                    setUpdates(context, intent.getLongExtra(EVENT_ID, -1))
                }
            }
        }
    }

    companion object {
        const val EVENT_ID = "EVENT_ID"

        fun setUpdates(context: Context, eventId: Long? = null) {
            val eventRepository = EventRepository(context)
            if (eventId == null) {
                removeUpdates(context)

                eventRepository.getEvents().forEach { event ->
                    setEventUpdate(context, event)
                }
            } else {
                val event = eventRepository.getEventByEventId(eventId)
                if (event != null) {
                    setEventUpdate(context, event)
                }
            }
        }

        private fun setEventUpdate(context: Context, event: Event) {
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                val now = Calendar.getInstance().apply {
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diff = Period(now.timeInMillis, event.startDate)
                if (event.startDate > now.timeInMillis) {
                    // Update the widget every hour till the event
                    Log.d("ciao", "${event.title} hours: ${diff.hours} - ${diff.minutes}")
                    setExactAndAllowWhileIdle(
                        AlarmManager.RTC,
                        if (event.startDate - diff.hours * 1000 * 60 * 60 > (now.timeInMillis + 120 * 1000)) event.startDate - diff.hours * 1000 * 60 * 60 else now.timeInMillis + 120000,
                        PendingIntent.getBroadcast(
                            context,
                            event.eventID.toInt(),
                            Intent(context, UpdatesReceiver::class.java).apply {
                                action = Actions.ACTION_TIME_UPDATE
                                putExtra(EVENT_ID, event.eventID)
                            },
                            0
                        )
                    )
                } else {
                    // Update the widget one second after the event is finished
                    Log.d(
                        "ciao",
                        "${event.title} end: ${Date(if (event.endDate > now.timeInMillis + 120 * 1000) event.endDate else now.timeInMillis + 120000)}"
                    )
                    val fireTime =
                        if (event.endDate > now.timeInMillis + 120 * 1000) event.endDate else now.timeInMillis + 120000
                    setExactAndAllowWhileIdle(
                        AlarmManager.RTC,
                        fireTime,
                        PendingIntent.getBroadcast(
                            context,
                            event.eventID.toInt(),
                            Intent(context, UpdatesReceiver::class.java).apply {
                                action = Actions.ACTION_TIME_UPDATE
                            },
                            0
                        )
                    )
                }
            }
        }

        fun removeUpdates(context: Context) {
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                EventRepository(context).getEvents().forEach {
                    cancel(PendingIntent.getBroadcast(context, it.eventID.toInt(), Intent(context, UpdatesReceiver::class.java), 0))
                }
            }
        }
    }
}
