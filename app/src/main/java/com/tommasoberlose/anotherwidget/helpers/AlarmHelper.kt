package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.content.Context
import android.text.format.DateFormat
import java.util.*

object AlarmHelper {
    fun getNextAlarm(context: Context): String = with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
        val alarm = nextAlarmClock
        return if (
            alarm != null
            && alarm.triggerTime - Calendar.getInstance().timeInMillis > 2 * 60 * 1000
            && alarm.triggerTime - Calendar.getInstance().timeInMillis < 24 * 60 * 60 * 1000
        ) {
            DateFormat.getTimeFormat(context).format(Date(alarm.triggerTime))
        } else {
            ""
        }
    }
}