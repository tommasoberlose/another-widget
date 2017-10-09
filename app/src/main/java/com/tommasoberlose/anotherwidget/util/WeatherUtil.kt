package com.tommasoberlose.anotherwidget.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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

/**
 * Created by tommaso on 08/10/17.
 */

object WeatherUtil {


    fun getWeather(context: Context) {
        if (!Util.checkGrantedPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return
        }
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        getCurrentWeather(context, locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, object: LocationListener {
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

    @SuppressLint("ApplySharedPref")
    fun getCurrentWeather(context: Context, location: Location?) {
        if (location != null) {
            val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

            try {
                val config = WeatherConfig()
                config.unitSystem = if (SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F").equals("C")) WeatherConfig.UNIT_SYSTEM.M else WeatherConfig.UNIT_SYSTEM.I
                config.lang = "en" // If you want to use english
                config.maxResult = 1 // Max number of cities retrieved
                config.numDays = 1 // Max num of days in the forecast
                config.ApiKey = "43e744ad8ff91b09ea62dbc7d0e7c1dd";

                val client = WeatherClient.ClientBuilder().attach(context)
                        .httpClient(com.survivingwithandroid.weather.lib.client.volley.WeatherClientDefault::class.java)
                        .provider(OpenweathermapProviderType())
                        .config(config)
                        .build()

                client.getCurrentCondition(WeatherRequest(location.longitude, location.latitude), object : WeatherClient.WeatherEventListener {
                    @SuppressLint("ApplySharedPref")
                    override fun onWeatherRetrieved(currentWeather: CurrentWeather) {
                        SP.edit()
                                .putFloat(Constants.PREF_WEATHER_TEMP, currentWeather.weather.temperature.temp)
                                .putString(Constants.PREF_WEATHER_ICON, currentWeather.weather.currentCondition.icon)
                                .commit()
                        Util.updateWidget(context)
                    }

                    @SuppressLint("ApplySharedPref")
                    override fun onWeatherError(e: WeatherLibException?) {
                    }

                    @SuppressLint("ApplySharedPref")
                    override fun onConnectionError(throwable: Throwable?) {
                    }
                })
            } catch (t: Exception) {
            }
        }
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