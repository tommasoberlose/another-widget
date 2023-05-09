package com.tommasoberlose.anotherwidget.services

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.WeatherNetworkApi
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class WeatherWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (Preferences.customLocationAdd == "" &&
            context.checkGrantedPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) {
            if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
                == ConnectionResult.SUCCESS
            ) {
                suspendCancellableCoroutine { continuation ->
                    LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnCompleteListener {
                        continuation.resume(if (it.isSuccessful) it.result else null)
                    }
                }
            } else {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                var location: Location? = null
                for (provider in lm.getProviders(true)) {
                    lm.getLastKnownLocation(provider)?.let {
                        if (location == null ||
                            it.time - location!!.time > 2 * 60 * 1000 ||
                            (it.time - location!!.time > -2 * 60 * 1000 && it.accuracy < location!!.accuracy))
                            location = it
                    }
                }
                location
            }?.let { location ->
                Preferences.customLocationLat = location.latitude.toString()
                Preferences.customLocationLon = location.longitude.toString()
            }
        }
        withContext(Dispatchers.IO) {
            WeatherNetworkApi(context).updateWeather()
        }

        if (Preferences.showWeather)
            enqueueTrigger(context)
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context, replace: Boolean = false) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "updateWeather",
                if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<WeatherWorker>().build()
            )
        }

        fun enqueueTrigger(context: Context) {
            val interval = when (Preferences.weatherRefreshPeriod) {
                0 -> 30
                1 -> 60
                2 -> 60L * 3
                3 -> 60L * 6
                4 -> 60L * 12
                5 -> 60L * 24
                else -> 60
            }
            WorkManager.getInstance(context).enqueueUniqueWork(
                "updateWeatherTrigger",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<Trigger>().setInitialDelay(
                    interval, TimeUnit.MINUTES
                ).build()
            )
        }

        fun cancelTrigger(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(
                "updateWeatherTrigger"
            )
        }
    }

    class Trigger(context: Context, params: WorkerParameters) : Worker(context, params) {
        override fun doWork(): Result {
            if (Preferences.showWeather && !isStopped)
                enqueue(applicationContext)
            return Result.success()
        }
    }
}
