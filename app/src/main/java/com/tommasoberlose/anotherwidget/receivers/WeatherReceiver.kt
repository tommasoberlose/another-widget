package com.tommasoberlose.anotherwidget.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.services.WeatherWorker
import java.util.*


class WeatherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Actions.ACTION_WEATHER_UPDATE -> WeatherWorker.setUpdates(context)
        }
    }

    companion object {

//        fun setUpdates(context: Context) {
//            removeUpdates(context)
//
//            if (Preferences.showWeather && Preferences.weatherProviderApi != "") {
//                WeatherHelper.updateWeather(context)
//                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
//                    "WEATHER_JOB_PERIODIC",
//                    ExistingPeriodicWorkPolicy.KEEP,
//                    PeriodicWorkRequestBuilder<WeatherWorker>(
//                        when (Preferences.weatherRefreshPeriod) {
//                            0 -> 30
//                            1 -> 60
//                            2 -> 60L * 3
//                            3 -> 60L * 6
//                            4 -> 60L * 12
//                            5 -> 60L * 24
//                            else -> 60
//                        }
//                        , TimeUnit.MINUTES
//                    )
//                        .addTag(JOB_TAG)
//                        .setConstraints(constraints)
//                        .build()
//                )
//            }
//        }
//
//        fun setOneTimeUpdate(context: Context) {
//
//            listOf(10L, 20L, 30L).forEach {
//                with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
//                    setRepeating(
//                        AlarmManager.RTC,
//                        if (event.endDate > 60 *1000) event.endDate else now.timeInMillis + 120000,
//                        PendingIntent.getBroadcast(context, 0, Intent(context, UpdatesReceiver::class.java).apply { action = Actions.ACTION_WEATHER_UPDATE }, 0)
//                    )
//                }
//                workManager.enqueueUniqueWork(
//                    "WEATHER_JOB_ONE_TIME_$it", ExistingWorkPolicy.KEEP,
//                    OneTimeWorkRequestBuilder<WeatherWorker>()
//                        .setInitialDelay(it, TimeUnit.MINUTES)
//                        .setConstraints(constraints)
//                        .addTag(JOB_TAG)
//                        .build()
//                )
//            }
//        }
//
//        fun removeUpdates(context: Context) {
//            with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
//                cancel(PendingIntent.getBroadcast(context, 0, Intent(context, WeatherReceiver::class.java).apply { action = Actions.ACTION_WEATHER_UPDATE }, 0))
//                listOf(10L, 20L, 30L).forEach {
//                    cancel(PendingIntent.getBroadcast(context, it.toInt(), Intent(context, WeatherReceiver::class.java).apply { action = Actions.ACTION_WEATHER_UPDATE }, 0))
//                }
//            }
//        }
    }
}
