package com.tommasoberlose.anotherwidget.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.AlarmManager
import android.app.PendingIntent
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.util.Util


class UpdatesReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED) || intent.action.equals(Intent.ACTION_INSTALL_PACKAGE) || intent.action.equals(Constants.ACTION_TIME_UPDATE)) {
            Util.updateWidget(context)
        }
    }

    fun setUpdates(context: Context) {
        removeUpdates(context)

        if (Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
            Util.updateWidget(context)

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, UpdatesReceiver::class.java)
            i.action = Constants.ACTION_TIME_UPDATE
            val pi = PendingIntent.getBroadcast(context, 0, i, 0)
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), (1000 * 60).toLong(), pi)
        }
    }

    fun removeUpdates(context: Context) {
        val intent = Intent(context, UpdatesReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, 0, intent, 0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(sender)
    }
}
