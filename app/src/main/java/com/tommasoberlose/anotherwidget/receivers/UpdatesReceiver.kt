package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import org.joda.time.Period
import java.text.DateFormat
import java.util.*


class UpdatesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Actions.ACTION_CALENDAR_UPDATE -> CalendarHelper.updateEventList(context)

            "com.sec.android.widgetapp.APPWIDGET_RESIZE",
            Intent.ACTION_DATE_CHANGED,
            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED -> MainWidget.updateWidget(context)
        }
    }

    companion object {
        fun setUpdates(context: Context) {
            removeUpdates(context)

            val eventRepository = EventRepository(context)
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                eventRepository.getEvents().forEach { event ->
                    val hoursDiff = Period(Calendar.getInstance().timeInMillis, event.startDate).hours

                    // Update the widget every hour till the event
                    (0 .. hoursDiff).forEach {
                        setExact(
                            AlarmManager.RTC_WAKEUP,
                            (event.startDate + 1000) - it * 1000 * 60* 60,
                            PendingIntent.getBroadcast(context, 0, Intent(context, UpdatesReceiver::class.java).apply { action = Actions.ACTION_TIME_UPDATE }, 0)
                        )
                    }

                    // Update the widget one second after the event is finished
                    setExact(
                        AlarmManager.RTC_WAKEUP,
                        event.endDate + 1000,
                        PendingIntent.getBroadcast(context, 0, Intent(context, UpdatesReceiver::class.java).apply { action = Actions.ACTION_TIME_UPDATE }, 0)
                    )
                }
            }
        }

        fun removeUpdates(context: Context) {
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                cancel(PendingIntent.getBroadcast(context, 0, Intent(context, UpdatesReceiver::class.java), 0))
            }
        }
    }
}
