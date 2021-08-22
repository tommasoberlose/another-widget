package com.tommasoberlose.anotherwidget.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class WeatherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> setUpdates(context)

            Actions.ACTION_WEATHER_UPDATE -> {
                GlobalScope.launch(Dispatchers.IO) {
                    WeatherHelper.updateWeather(context)
                }
            }
        }
    }

    companion object {
        private const val MINUTE = 60 * 1000L
        fun setUpdates(context: Context) {
            removeUpdates(context)

            if (Preferences.showWeather) {
                val interval = MINUTE * when (Preferences.weatherRefreshPeriod) {
                    0 -> 30
                    1 -> 60
                    2 -> 60L * 3
                    3 -> 60L * 6
                    4 -> 60L * 12
                    5 -> 60L * 24
                    else -> 60
                }
                with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                    setRepeating(
                        AlarmManager.RTC,
                        Calendar.getInstance().timeInMillis,
                        interval,
                        PendingIntent.getBroadcast(context, 0, Intent(context, WeatherReceiver::class.java).apply { action = Actions.ACTION_WEATHER_UPDATE }, PendingIntent.FLAG_IMMUTABLE)
                    )
                }
            }
        }

        fun removeUpdates(context: Context) {
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                cancel(PendingIntent.getBroadcast(context, 0, Intent(context, WeatherReceiver::class.java).apply { action = Actions.ACTION_WEATHER_UPDATE }, PendingIntent.FLAG_IMMUTABLE))
                listOf(10, 20, 30).forEach {
                    cancel(PendingIntent.getBroadcast(context, it, Intent(context, WeatherReceiver::class.java).apply { action = Actions.ACTION_WEATHER_UPDATE }, PendingIntent.FLAG_IMMUTABLE))
                }
            }
        }
    }
}
