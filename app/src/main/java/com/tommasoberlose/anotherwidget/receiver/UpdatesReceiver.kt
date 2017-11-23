package com.tommasoberlose.anotherwidget.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import android.icu.text.LocaleDisplayNames
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.`object`.Event
import com.tommasoberlose.anotherwidget.util.CalendarUtil
import com.tommasoberlose.anotherwidget.util.CrocodileService
import com.tommasoberlose.anotherwidget.util.Util
import java.sql.Time
import java.util.*
import java.util.concurrent.TimeUnit


class UpdatesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED) || intent.action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) || intent.action.equals("android.intent.action.PACKAGE_REPLACED") || intent.action.equals("android.intent.action.PACKAGE_ADDED")) {
            setUpdates(context)
        } else if (intent.action.equals(Constants.ACTION_TIME_UPDATE) || intent.action.equals("com.sec.android.widgetapp.APPWIDGET_RESIZE") || intent.action == "android.intent.action.USER_PRESENT") {
            val e: Event = CalendarUtil.getNextEvent(context)
            if (e.id == 0.toLong() || e.endDate <= Calendar.getInstance().timeInMillis) {
                CalendarUtil.updateEventList(context)
            } else {
                Util.updateWidget(context)
            }
        } else if (intent.action.equals(Constants.ACTION_CALENDAR_UPDATE)) {
            CalendarUtil.updateEventList(context)
        }
    }

    fun setUpdates(context: Context) {
        CalendarUtil.updateEventList(context)
        removeUpdates(context)
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(Intent(context, CrocodileService::class.java))
        } else {
            context.startService(Intent(context, CrocodileService::class.java))
        }*/

        val now = Calendar.getInstance()
        now.set(Calendar.MILLISECOND, 0)
        now.set(Calendar.SECOND, 0)

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, UpdatesReceiver::class.java)
        i.action = Constants.ACTION_TIME_UPDATE
        val pi = PendingIntent.getBroadcast(context, 0, i, 0)
        am.setRepeating(AlarmManager.RTC_WAKEUP, now.timeInMillis, (1000 * 60).toLong(), pi)
    }

    fun removeUpdates(context: Context) {
        val intent = Intent(context, UpdatesReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, 0, intent, 0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(sender)
    }
}
