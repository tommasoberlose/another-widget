package com.tommasoberlose.anotherwidget.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.*
import android.os.Bundle
import android.preference.PreferenceManager
import com.survivingwithandroid.weather.lib.WeatherClient
import com.survivingwithandroid.weather.lib.WeatherConfig
import com.survivingwithandroid.weather.lib.exception.WeatherLibException
import com.survivingwithandroid.weather.lib.model.CurrentWeather
import com.survivingwithandroid.weather.lib.provider.openweathermap.OpenweathermapProviderType
import com.survivingwithandroid.weather.lib.request.WeatherRequest
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.`object`.Constants
import android.content.DialogInterface
import android.support.v4.content.ContextCompat.startActivity
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.content.Intent
import android.location.LocationManager
import android.util.Log


/**
 * Created by tommaso on 08/10/17.
 */

object WeatherUtil {

    fun updateWeather(context: Context) {
        Util.showLocationNotification(context, false)
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (SP.getString(Constants.PREF_CUSTOM_LOCATION_ADD, "").equals("") || SP.getString(Constants.PREF_CUSTOM_LOCATION_LAT, "").equals("") || SP.getString(Constants.PREF_CUSTOM_LOCATION_LON, "").equals("")) {

            if (!Util.checkGrantedPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                return
            }
            var gpsEnabled = false
            var networkEnabled = false
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            try {
                gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            } catch (ex: Exception) {
            }

            try {
                networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            } catch (ex: Exception) {
            }

            if (!gpsEnabled && !networkEnabled) {
                Util.showLocationNotification(context, true)
            } else {
                if (gpsEnabled) {
                    getCurrentWeather(context, locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER))
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            locationManager.removeUpdates(this)
                            getCurrentWeather(context, location)
                        }

                        @SuppressLint("ApplySharedPref")
                        override fun onProviderDisabled(p0: String?) {
                        }

                        @SuppressLint("ApplySharedPref")
                        override fun onProviderEnabled(p0: String?) {
                        }

                        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
                        }
                    })
                }

                if (networkEnabled) {
                    getCurrentWeather(context, locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            locationManager.removeUpdates(this)
                            getCurrentWeather(context, location)
                        }

                        @SuppressLint("ApplySharedPref")
                        override fun onProviderDisabled(p0: String?) {
                        }

                        @SuppressLint("ApplySharedPref")
                        override fun onProviderEnabled(p0: String?) {
                        }

                        override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
                        }
                    })
                }
            }
        } else {
            weatherNetworkRequest(context, SP.getString(Constants.PREF_CUSTOM_LOCATION_LAT, "").toDouble(), SP.getString(Constants.PREF_CUSTOM_LOCATION_LON, "").toDouble())
        }
    }

    @SuppressLint("ApplySharedPref")
    fun getCurrentWeather(context: Context, location: Location?) {
        if (location != null) {
            weatherNetworkRequest(context, location.latitude, location.longitude)
        }
    }

    fun weatherNetworkRequest(context: Context, latitude: Double, longitude: Double) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (!SP.getString(Constants.PREF_WEATHER_PROVIDER_API_KEY, "").equals("")) {
            try {
                val config = WeatherConfig()
                config.unitSystem = if (SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F").equals("C")) WeatherConfig.UNIT_SYSTEM.M else WeatherConfig.UNIT_SYSTEM.I
                config.lang = "en"
                config.maxResult = 1
                config.numDays = 1
                config.ApiKey = SP.getString(Constants.PREF_WEATHER_PROVIDER_API_KEY, "")

                val client = WeatherClient.ClientBuilder().attach(context)
                        .httpClient(com.survivingwithandroid.weather.lib.client.volley.WeatherClientDefault::class.java)
                        .provider(OpenweathermapProviderType())
                        .config(config)
                        .build()

                client.getCurrentCondition(WeatherRequest(longitude, latitude), object : WeatherClient.WeatherEventListener {
                    @SuppressLint("ApplySharedPref")
                    override fun onWeatherRetrieved(currentWeather: CurrentWeather) {
                        SP.edit()
                                .putFloat(Constants.PREF_WEATHER_TEMP, currentWeather.weather.temperature.temp)
                                .putString(Constants.PREF_WEATHER_ICON, currentWeather.weather.currentCondition.icon)
                                .putString(Constants.PREF_WEATHER_REAL_TEMP_UNIT, SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F"))
                                .commit()
                        Util.updateWidget(context)
                    }

                    @SuppressLint("ApplySharedPref")
                    override fun onWeatherError(e: WeatherLibException?) {
                        // removeWeather(context, SP)
                    }

                    @SuppressLint("ApplySharedPref")
                    override fun onConnectionError(throwable: Throwable?) {
                        // removeWeather(context, SP)
                    }
                })
            } catch (t: Exception) {
                // removeWeather(context, SP)
            }
        } else {
            removeWeather(context, SP)
        }
    }

    @SuppressLint("ApplySharedPref")
    fun removeWeather(context: Context, SP: SharedPreferences) {
        SP.edit().
                remove(Constants.PREF_WEATHER_TEMP)
                .remove(Constants.PREF_WEATHER_ICON)
                .commit()
        Util.updateWidget(context)
    }

    fun getWeatherIconResource(icon: String): Int {
        when (icon) {
            "01d" -> {
                return R.drawable.clear_day
            }
            "02d" -> {
                return R.drawable.partly_cloudy
            }
            "03d" -> {
                return R.drawable.mostly_cloudy
            }
            "04d" -> {
                return R.drawable.cloudy_weather
            }
            "09d" -> {
                return R.drawable.storm_weather_day
            }
            "10d" -> {
                return R.drawable.rainy_day
            }
            "11d" -> {
                return R.drawable.thunder_day
            }
            "13d" -> {
                return R.drawable.snow_day
            }
            "50d" -> {
                return R.drawable.haze_day
            }
            "01n" -> {
                return R.drawable.clear_night
            }
            "02n" -> {
                return R.drawable.partly_cloudy_night
            }
            "03n" -> {
                return R.drawable.mostly_cloudy_night
            }
            "04n" -> {
                return R.drawable.cloudy_weather
            }
            "09n" -> {
                return R.drawable.storm_weather_night
            }
            "10n" -> {
                return R.drawable.rainy_night
            }
            "11n" -> {
                return R.drawable.thunder_night
            }
            "13n" -> {
                return R.drawable.snow_night
            }
            "50n" -> {
                return R.drawable.haze_night
            }
            else -> {
                return -1
            }
        }
    }
}