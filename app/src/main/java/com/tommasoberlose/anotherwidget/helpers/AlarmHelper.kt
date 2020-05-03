package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.content.Context
import android.text.format.DateFormat
import java.util.*

object AlarmHelper {
    fun getNextAlarm(context: Context): String = with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
        return if (
            nextAlarmClock != null
            && nextAlarmClock.triggerTime - Calendar.getInstance().timeInMillis > 2 * 60 * 1000
            && nextAlarmClock.triggerTime - Calendar.getInstance().timeInMillis < 24 * 60 * 60 * 1000
        ) {
            DateFormat.getTimeFormat(context).format(Date(nextAlarmClock.triggerTime))
        } else {
            ""
        }
    }
}