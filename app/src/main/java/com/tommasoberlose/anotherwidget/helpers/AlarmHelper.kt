package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import java.text.SimpleDateFormat
import java.util.*

object AlarmHelper {
    fun getNextAlarm(context: Context): String = with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
        val alarm = nextAlarmClock
        return if (
            alarm != null
            && alarm.triggerTime - Calendar.getInstance().timeInMillis > 5 * 60 * 1000
        ) {
            setTimeout(context, alarm.triggerTime)
            "%s %s".format(
                SimpleDateFormat("EEE", Locale.getDefault()).format(alarm.triggerTime),
                DateFormat.getTimeFormat(context).format(Date(alarm.triggerTime))
            )
        } else {
            ""
        }
    }

    fun isAlarmProbablyWrong(context: Context): Boolean {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val alarm = nextAlarmClock
            return (
                alarm != null
                && alarm.triggerTime - Calendar.getInstance().timeInMillis < 5 * 60 * 1000
            )
        }
    }

    private fun setTimeout(context: Context, trigger: Long) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val intent = Intent(context, UpdatesReceiver::class.java).apply {
                action = Actions.ACTION_ALARM_UPDATE
            }
            cancel(PendingIntent.getBroadcast(context, ALARM_UPDATE_ID, intent,
                                              PendingIntent.FLAG_IMMUTABLE))
            setExact(
                AlarmManager.RTC,
                trigger,
                PendingIntent.getBroadcast(
                    context,
                    ALARM_UPDATE_ID,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
    }

    private const val ALARM_UPDATE_ID = 24953
}