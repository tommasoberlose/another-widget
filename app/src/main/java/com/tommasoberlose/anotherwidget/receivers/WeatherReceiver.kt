package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.services.WeatherWorker

class WeatherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Actions.ACTION_WEATHER_UPDATE -> {
                WeatherHelper.updateWeather(context)
            }
        }
    }

    companion object {
        fun setUpdates(context: Context) {
            if (Preferences.showWeather) {
                WeatherWorker.enqueueTrigger(context)
            }
        }

        fun removeUpdates(context: Context) {
            WeatherWorker.cancelTrigger(context)
        }
    }
}
