package com.tommasoberlose.anotherwidget.util

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.`object`.Constants

class WeatherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED) || intent.action.equals(Intent.ACTION_INSTALL_PACKAGE) || intent.action.equals(Constants.ACTION_WEATHER_UPDATE)) {
            Util.getWeather(context)
        }
    }

    fun setUpdates(context: Context) {
        removeUpdates(context)

        if (Util.checkGrantedPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Util.getWeather(context)

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, WeatherReceiver::class.java)
            i.action = Constants.ACTION_WEATHER_UPDATE
            val pi = PendingIntent.getBroadcast(context, 1, i, 0)
            am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), (1000 * 60 * 60 * 2).toLong(), pi) // 2 hour
        }
    }

    fun removeUpdates(context: Context) {
        val intent = Intent(context, WeatherReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, 1, intent, 0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(sender)
    }
}
