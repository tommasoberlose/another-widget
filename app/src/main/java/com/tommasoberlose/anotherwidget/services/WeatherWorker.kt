package com.tommasoberlose.anotherwidget.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.receivers.WeatherReceiver
import java.util.*
import java.util.concurrent.TimeUnit


class WeatherWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        Log.d("ciao1", "weather ok")
        WeatherHelper.updateWeather(applicationContext)
        return Result.success()
    }

    companion object {
        private const val JOB_TAG = "WEATHER_WORKER"

        fun setUpdates(context: Context) {
            removeUpdates(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            if (Preferences.showWeather && Preferences.weatherProviderApi != "") {
                WeatherHelper.updateWeather(context)
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "WEATHER_JOB_PERIODIC",
                    ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<WeatherWorker>(
                        when (Preferences.weatherRefreshPeriod) {
                            0 -> 30
                            1 -> 60
                            2 -> 60L * 3
                            3 -> 60L * 6
                            4 -> 60L * 12
                            5 -> 60L * 24
                            else -> 60
                        }
                        , TimeUnit.MINUTES
                    )
                        .addTag(JOB_TAG)
                        .setConstraints(constraints)
                        .build()
                )
            }
        }

        fun setOneTimeUpdate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workManager = WorkManager.getInstance(context)
            listOf(10L, 20L, 30L).forEach {
                workManager.enqueueUniqueWork("WEATHER_JOB_ONE_TIME_$it", ExistingWorkPolicy.KEEP,
                    OneTimeWorkRequestBuilder<WeatherWorker>()
                        .setInitialDelay(it, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .addTag(JOB_TAG)
                        .build()
                )
            }
        }

        fun removeUpdates(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(JOB_TAG)
        }
    }
}