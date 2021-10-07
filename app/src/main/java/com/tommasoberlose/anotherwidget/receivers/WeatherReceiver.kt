package com.tommasoberlose.anotherwidget.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.services.WeatherWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class WeatherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> setUpdates(context)

            Actions.ACTION_WEATHER_UPDATE -> {
                WeatherWorker.enqueue(context)
            }
        }
    }

    companion object {
        fun setUpdates(context: Context) {
            if (Preferences.showWeather) {
                val interval = when (Preferences.weatherRefreshPeriod) {
                    0 -> 30
                    1 -> 60
                    2 -> 60L * 3
                    3 -> 60L * 6
                    4 -> 60L * 12
                    5 -> 60L * 24
                    else -> 60
                }
                WeatherWorker.enqueuePeriodic(context, interval, TimeUnit.MINUTES)
            }
        }

        fun removeUpdates(context: Context) {
            WeatherWorker.cancelPeriodic(context)
        }
    }
}
