package com.tommasoberlose.anotherwidget.helpers

import android.app.AlarmManager
import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object AlarmHelper {
    fun getNextAlarm(context: Context): String = with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
        val alarm = nextAlarmClock
        return if (
            alarm != null
            && alarm.triggerTime - Calendar.getInstance().timeInMillis > 2 * 60 * 1000
            && alarm.triggerTime - Calendar.getInstance().timeInMillis < 12 * 60 * 60 * 1000
        ) {
            "%s %s".format(
                SimpleDateFormat("EEE", Locale.getDefault()).format(alarm.triggerTime),
                DateFormat.getTimeFormat(context).format(Date(alarm.triggerTime))
            )
        } else {
            ""
        }
    }
}