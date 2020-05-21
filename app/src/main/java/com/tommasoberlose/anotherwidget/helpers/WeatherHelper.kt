package com.tommasoberlose.anotherwidget.helpers

import android.Manifest
import android.content.Context
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.network.WeatherNetworkApi
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import org.greenrobot.eventbus.EventBus


/**
 * Created by tommaso on 08/10/17.
 */

object WeatherHelper {

    fun updateWeather(context: Context) {
        val networkApi = WeatherNetworkApi(context)
        if (Preferences.customLocationAdd != "") {
            networkApi.updateWeather()
        } else if (context.checkGrantedPermission(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else Manifest.permission.ACCESS_FINE_LOCATION)) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val location = task.result
                    if (location != null) {
                        Preferences.customLocationLat = location.latitude.toString()
                        Preferences.customLocationLon = location.longitude.toString()

                        networkApi.updateWeather()
                        EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                    }
                }
            }
        }
    }

    fun removeWeather(context: Context) {
        Preferences.remove(Preferences::weatherTemp)
        Preferences.remove(Preferences::weatherRealTempUnit)
        MainWidget.updateWidget(context)
    }

    fun getWeatherIconResource(icon: String, style: Int = Preferences.weatherIconPack): Int {
        return when (icon) {
            "01d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.clear_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.clear_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.clear_day_4
                    else -> R.drawable.clear_day
                }
            }
            "02d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.partly_cloudy_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.partly_cloudy_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.partly_cloudy_4
                    else -> R.drawable.partly_cloudy
                }
            }
            "03d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.mostly_cloudy_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.mostly_cloudy_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.mostly_cloudy_4
                    else -> R.drawable.mostly_cloudy
                }
            }
            "04d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.cloudy_weather_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.cloudy_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.cloudy_weather_4
                    else -> R.drawable.cloudy_weather
                }
            }
            "09d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.storm_weather_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.storm_weather_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.storm_weather_day_4
                    else -> R.drawable.storm_weather_day
                }
            }
            "10d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.rainy_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.rainy_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.rainy_day_4
                    else -> R.drawable.rainy_day
                }
            }
            "11d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.thunder_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.thunder_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.thunder_day_4
                    else -> R.drawable.thunder_day
                }
            }
            "13d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.snow_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.snow_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.snow_day_4
                    else -> R.drawable.snow_day
                }
            }
            "50d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.haze_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.haze_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.haze_day_4
                    else -> R.drawable.haze_day
                }
            }
            "80d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.windy_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.windy_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.windy_day_4
                    else -> R.drawable.windy_day
                }
            }
            "81d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.rain_snow_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.rain_snow_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.rain_snow_day_4
                    else -> R.drawable.rain_snow_day
                }
            }
            "82d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.haze_weather_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.haze_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.haze_weather_4
                    else -> R.drawable.haze_weather
                }
            }



            "01n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.clear_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.clear_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.clear_night_4
                    else -> R.drawable.clear_night
                }
            }
            "02n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.partly_cloudy_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.partly_cloudy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.partly_cloudy_night_4
                    else -> R.drawable.partly_cloudy_night
                }
            }
            "03n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.mostly_cloudy_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.mostly_cloudy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.mostly_cloudy_night_4
                    else -> R.drawable.mostly_cloudy_night
                }
            }
            "04n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.cloudy_weather_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.cloudy_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.cloudy_weather_4
                    else -> R.drawable.cloudy_weather
                }
            }
            "09n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.storm_weather_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.storm_weather_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.storm_weather_night_4
                    else -> R.drawable.storm_weather_night
                }
            }
            "10n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.rainy_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.rainy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.rainy_night_4
                    else -> R.drawable.rainy_night
                }
            }
            "11n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.thunder_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.thunder_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.thunder_night_4
                    else -> R.drawable.thunder_night
                }
            }
            "13n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.snow_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.snow_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.snow_night_4
                    else -> R.drawable.snow_night
                }
            }
            "50n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.haze_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.haze_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.haze_night_4
                    else -> R.drawable.haze_night
                }
            }
            "80n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.windy_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.windy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.windy_night_4
                    else -> R.drawable.windy_night
                }
            }
            "81n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.rain_snow_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.rain_snow_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.rain_snow_night_4
                    else -> R.drawable.rain_snow_night
                }
            }
            "82n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.haze_weather_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.haze_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.haze_weather_4
                    else -> R.drawable.haze_weather
                }
            }
            else -> {
                return R.drawable.unknown
            }
        }
    }
}