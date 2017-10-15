package com.tommasoberlose.anotherwidget.receiver

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.util.Util
import com.tommasoberlose.anotherwidget.util.WeatherUtil

class WeatherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED) || intent.action.equals(Intent.ACTION_MY_PACKAGE_REPLACED) || intent.action.equals("android.intent.action.PACKAGE_REPLACED") || intent.action.equals("android.intent.action.PACKAGE_ADDED")) {
            setUpdates(context)
        } else if (intent.action.equals(Constants.ACTION_WEATHER_UPDATE) || intent.action.equals("android.location.PROVIDERS_CHANGED")) {
            WeatherUtil.updateWeather(context)
        }
    }

    fun setUpdates(context: Context) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        removeUpdates(context)
        WeatherUtil.updateWeather(context)

        if (SP.getBoolean(Constants.PREF_SHOW_WEATHER, true)) {
            Util.showWeatherNotification(context, SP.getString(Constants.PREF_WEATHER_PROVIDER_API_KEY, "").equals(""))
        }

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, WeatherReceiver::class.java)
        i.action = Constants.ACTION_WEATHER_UPDATE
        val pi = PendingIntent.getBroadcast(context, 1, i, 0)
        val refresh: Long = when (SP.getInt(Constants.PREF_WEATHER_REFRESH_PERIOD, 1)) {
            0 -> 30
            1 -> 60
            2 -> 60 * 3
            3 -> 60 * 6
            4 -> 60 * 12
            5 -> 60 * 24
            else -> 60
        }
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 1000 * 60 * refresh, pi)
    }

    fun setOneTimeUpdate(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, WeatherReceiver::class.java)
        i.action = Constants.ACTION_WEATHER_UPDATE
        val pi = PendingIntent.getBroadcast(context, 1, i, 0)
        am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 60 * 10, pi)
        am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 60 * 15, pi)
        am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * 60 * 20, pi)
    }

    fun removeUpdates(context: Context) {
        val intent = Intent(context, WeatherReceiver::class.java)
        val sender = PendingIntent.getBroadcast(context, 1, intent, 0)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(sender)
    }
}
