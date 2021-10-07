package com.tommasoberlose.anotherwidget.services

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.network.WeatherNetworkApi
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class WeatherWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        when {
            Preferences.customLocationAdd != "" -> {
                withContext(Dispatchers.IO) {
                    WeatherNetworkApi(context).updateWeather()
                }
            }
            context.checkGrantedPermission(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
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
                }.let { location ->
                    if (location != null) {
                        Preferences.customLocationLat = location.latitude.toString()
                        Preferences.customLocationLon = location.longitude.toString()
                    }
                    withContext(Dispatchers.IO) {
                        WeatherNetworkApi(context).updateWeather()
                    }
                }
            }
            else -> {
                Preferences.weatherProviderLocationError = context.getString(R.string.weather_provider_error_missing_location)
                Preferences.weatherProviderError = ""
                WeatherHelper.removeWeather(context)
                EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
            }
        }
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "updateWeather",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<WeatherWorker>().build()
            )
        }

        fun enqueuePeriodic(context: Context, interval: Long, unit: TimeUnit) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "updateWeatherPeriodically",
                ExistingPeriodicWorkPolicy.REPLACE,
                PeriodicWorkRequestBuilder<WeatherWorker>(interval, unit).build()
            )
        }

        fun cancelPeriodic(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(
                "updateWeatherPeriodically"
            )
        }
    }
}
