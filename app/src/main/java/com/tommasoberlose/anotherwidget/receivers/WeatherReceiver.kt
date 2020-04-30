package com.tommasoberlose.anotherwidget.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.utils.Util
import com.tommasoberlose.anotherwidget.utils.WeatherUtil
import java.util.*


class WeatherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            setUpdates(context)
        } else if (intent.action == Actions.ACTION_WEATHER_UPDATE) {
            WeatherUtil.updateWeather(context)
        }
    }

    companion object {
        fun setUpdates(context: Context) {
            removeUpdates(context)

            if (Preferences.showWeather && Preferences.weatherProviderApi != "") {
                WeatherUtil.updateWeather(context)

                with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                    val pi = PendingIntent.getBroadcast(
                        context,
                        1,
                        Intent(context, WeatherReceiver::class.java).apply {
                            action = Actions.ACTION_WEATHER_UPDATE
                        },
                        0
                    )

                    val refresh: Long = when (Preferences.weatherRefreshPeriod) {
                        0 -> 30
                        1 -> 60
                        2 -> 60 * 3
                        3 -> 60 * 6
                        4 -> 60 * 12
                        5 -> 60 * 24
                        else -> 60
                    }
                    val now = Calendar.getInstance().apply {
                        set(Calendar.MILLISECOND, 0)
                        set(Calendar.SECOND, 0)
                    }

                    setRepeating(AlarmManager.RTC_WAKEUP, now.timeInMillis, 1000 * 60 * refresh, pi)
                }
            }
        }

        fun setOneTimeUpdate(context: Context) {
            // Update the weather in a few minuter when the api key has been changed
            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
                val pi = PendingIntent.getBroadcast(context, 1, Intent(context, WeatherReceiver::class.java).apply { action = Actions.ACTION_WEATHER_UPDATE }, 0)
                val now = Calendar.getInstance().apply {
                    set(Calendar.MILLISECOND, 0)
                    set(Calendar.SECOND, 0)
                }

                listOf(10, 15, 20).forEach {
                    setExact(AlarmManager.RTC_WAKEUP, now.timeInMillis + 1000 * 60 * it, pi)
                }
            }
        }

        fun removeUpdates(context: Context) {
            val intent = Intent(context, WeatherReceiver::class.java)
            val sender = PendingIntent.getBroadcast(context, 1, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(sender)
        }
    }
}
