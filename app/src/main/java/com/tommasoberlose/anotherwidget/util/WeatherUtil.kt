package com.tommasoberlose.anotherwidget.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.UiModeManager
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
import android.support.annotation.NonNull
import android.support.v7.app.AppCompatDelegate
import android.util.Log
import com.google.android.gms.awareness.Awareness
import com.google.android.gms.awareness.snapshot.WeatherResponse
import com.google.android.gms.awareness.snapshot.WeatherResult
import com.google.android.gms.awareness.state.Weather
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.util.*


/**
 * Created by tommaso on 08/10/17.
 */

object WeatherUtil {

    fun updateWeather(context: Context) {
        Util.showLocationNotification(context, false)
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        if (SP.getString(Constants.PREF_CUSTOM_LOCATION_ADD, "").equals("") || SP.getString(Constants.PREF_CUSTOM_LOCATION_LAT, "").equals("") || SP.getString(Constants.PREF_CUSTOM_LOCATION_LON, "").equals("")) {
            if (SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) == Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) {
                newWeatherProvider(context)
            } else {
                if (!Util.checkGrantedPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    return
                }
                val mGoogleApiClient = GoogleApiClient.Builder(context)
                        .addApi(Awareness.API)
                        .build()
                mGoogleApiClient.connect()
                Awareness.SnapshotApi.getLocation(mGoogleApiClient)
                        .setResultCallback({ locationResult ->
                            if (locationResult.status.isSuccess) {
                                getCurrentWeather(context, locationResult.location)
                            }
                        })
            }
        } else {
            weatherNetworkRequest(context, SP.getString(Constants.PREF_CUSTOM_LOCATION_LAT, "").toDouble(), SP.getString(Constants.PREF_CUSTOM_LOCATION_LON, "").toDouble())
        }
    }

    @SuppressLint("ApplySharedPref")
    fun newWeatherProvider(context: Context) {
        if (!Util.checkGrantedPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            return
        }
        val mGoogleApiClient = GoogleApiClient.Builder(context)
                .addApi(Awareness.API)
                .build();
        mGoogleApiClient.connect()

        Awareness.SnapshotApi.getWeather(mGoogleApiClient)
                    .setResultCallback({ weatherResult ->
                        if (weatherResult.status.isSuccess) {
                            val weather: Weather = weatherResult.weather
                            val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
                            SP.edit()
                                    .putFloat(Constants.PREF_WEATHER_TEMP, weather.getTemperature(if (SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F").equals("F")) Weather.FAHRENHEIT else Weather.CELSIUS))
                                    .putString(Constants.PREF_WEATHER_ICON, getIconCodeFromAwareness(context, weather.conditions))
                                    .putString(Constants.PREF_WEATHER_REAL_TEMP_UNIT, SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F"))
                                    .commit()
                            Util.updateWidget(context)
                        }
                        mGoogleApiClient.disconnect()
                    })

    }

    @SuppressLint("ApplySharedPref")
    fun getCurrentWeather(context: Context, location: Location?) {
        if (location != null) {
            weatherNetworkRequest(context, location.latitude, location.longitude)
        }
    }

    fun weatherNetworkRequest(context: Context, latitude: Double, longitude: Double) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (!SP.getString(when (SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS)) {
            Constants.WEATHER_PROVIDER_OPEN_WEATHER -> Constants.PREF_OPEN_WEATHER_API_KEY
            else -> Constants.PREF_OPEN_WEATHER_API_KEY
        }, "").equals("")) {
            try {
                val config = WeatherConfig()
                config.unitSystem = if (SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F").equals("C")) WeatherConfig.UNIT_SYSTEM.M else WeatherConfig.UNIT_SYSTEM.I
                config.lang = "en"
                config.maxResult = 1
                config.numDays = 1
                config.ApiKey = SP.getString(when (SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS)) {
                    Constants.WEATHER_PROVIDER_OPEN_WEATHER -> Constants.PREF_OPEN_WEATHER_API_KEY
                    else -> Constants.PREF_OPEN_WEATHER_API_KEY
                }, "")

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

    fun getIconCodeFromAwareness(context: Context, conditions: IntArray): String {
        var icon = ""
        return if (conditions.contains(Weather.CONDITION_UNKNOWN)) {
            ""
        } else {
            if (conditions.contains(Weather.CONDITION_CLEAR)) {
                icon = "01"
            } else if (conditions.contains(Weather.CONDITION_CLOUDY)) {
                icon = "02"
            } else if (conditions.contains(Weather.CONDITION_RAINY)) {
                icon = "10"
            } else if (conditions.contains(Weather.CONDITION_STORMY)) {
                icon = "09"
            } else if (conditions.contains(Weather.CONDITION_SNOWY)) {
                icon = "13"
            } else if (conditions.contains(Weather.CONDITION_WINDY)) {
                icon = "80"
            } else if (conditions.contains(Weather.CONDITION_HAZY)) {
                icon = "50"
            } else if (conditions.contains(Weather.CONDITION_ICY)) {
                icon = "81"
            } else if (conditions.contains(Weather.CONDITION_FOGGY)) {
                icon = "82"
            }

            val uiManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
            return when {
                uiManager.nightMode == UiModeManager.MODE_NIGHT_YES -> icon + "n"
                uiManager.nightMode == UiModeManager.MODE_NIGHT_NO -> icon + "d"
                else -> return if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 19 || Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 7) {
                    icon + "n"
                } else {
                    icon + "d"
                }
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
            "80d" -> {
                return R.drawable.windy_day
            }
            "81d" -> {
                return R.drawable.rain_snow_day
            }
            "82d" -> {
                return R.drawable.haze_weather
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
            "80n" -> {
                return R.drawable.windy_night
            }
            "81n" -> {
                return R.drawable.rain_snow_night
            }
            "82n" -> {
                return R.drawable.haze_weather
            }
            else -> {
                return R.drawable.unknown
            }
        }
    }
}