package com.tommasoberlose.anotherwidget.services

import android.Manifest
import android.app.*
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.IBinder
import android.util.Log
import androidx.core.app.*
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.WeatherNetworkApi
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

class LocationService : Service() {

    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(LOCATION_ACCESS_NOTIFICATION_ID, getLocationAccessNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(LOCATION_ACCESS_NOTIFICATION_ID, getLocationAccessNotification())
        job?.cancel()
        job = GlobalScope.launch(Dispatchers.IO) {
            if (ActivityCompat.checkSelfPermission(
                    this@LocationService,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (com.google.android.gms.common.GoogleApiAvailability.getInstance()
                        .isGooglePlayServicesAvailable(this@LocationService)
                    == com.google.android.gms.common.ConnectionResult.SUCCESS
                ) {
                    LocationServices.getFusedLocationProviderClient(this@LocationService).lastLocation
                } else {
                    val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
                    var location: android.location.Location? = null
                    for (provider in arrayOf(
                        "fused",    // LocationManager.FUSED_PROVIDER,
                        android.location.LocationManager.GPS_PROVIDER,
                        android.location.LocationManager.NETWORK_PROVIDER,
                        android.location.LocationManager.PASSIVE_PROVIDER
                    )) {
                        if (lm.isProviderEnabled(provider)) {
                            location = lm.getLastKnownLocation(provider)
                            if (location != null) break
                        }
                    }
                    com.google.android.gms.tasks.Tasks.forResult(location)
                }.addOnCompleteListener { task ->
                    val networkApi = WeatherNetworkApi(this@LocationService)
                    if (task.isSuccessful) {
                        val location = task.result
                        if (location != null) {
                            Preferences.customLocationLat = location.latitude.toString()
                            Preferences.customLocationLon = location.longitude.toString()
                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            networkApi.updateWeather()
                            withContext(Dispatchers.Main) {
                                stopSelf()
                            }
                        }
                        EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                    } else {
                        CoroutineScope(Dispatchers.IO).launch {
                            networkApi.updateWeather()
                            withContext(Dispatchers.Main) {
                                stopSelf()
                            }
                        }
                        EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                    }
                }
            } else {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        job = null
    }

    companion object {
        const val LOCATION_ACCESS_NOTIFICATION_ID = 28465

        @JvmStatic
        fun requestNewLocation(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, LocationService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getLocationAccessNotification(): Notification {
        with(NotificationManagerCompat.from(this)) {
            // Create channel
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                createNotificationChannel(
                    NotificationChannel(
                        getString(R.string.location_access_notification_channel_id),
                        getString(R.string.location_access_notification_channel_name),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = getString(R.string.location_access_notification_channel_description)
                    }
                )
            }

            val builder = NotificationCompat.Builder(this@LocationService, getString(R.string.location_access_notification_channel_id))
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(getString(R.string.location_access_notification_title))
                .setOngoing(true)
                .setColor(ContextCompat.getColor(this@LocationService, R.color.colorAccent))

            // Main intent that open the activity
            builder.setContentIntent(PendingIntent.getActivity(this@LocationService, 0, Intent(this@LocationService, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT))

            return builder.build()
        }
    }
}