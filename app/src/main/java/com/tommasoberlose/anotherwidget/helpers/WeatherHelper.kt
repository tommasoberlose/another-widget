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
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
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

    fun getWeatherIconResource(context: Context, icon: String, style: Int = Preferences.weatherIconPack): Int {
        return when (icon) {
            "01d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.clear_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.clear_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.clear_day_4
                    else -> if (context.isDarkTheme()) R.drawable.clear_day_5 else R.drawable.clear_day_5_light
                }
            }
            "02d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.partly_cloudy_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.partly_cloudy_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.partly_cloudy_4
                    else -> if (context.isDarkTheme()) R.drawable.partly_cloudy_5 else R.drawable.partly_cloudy_5_light
                }
            }
            "03d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.mostly_cloudy_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.mostly_cloudy_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.mostly_cloudy_4
                    else -> if (context.isDarkTheme()) R.drawable.mostly_cloudy_5 else R.drawable.mostly_cloudy_5_light
                }
            }
            "04d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.cloudy_weather_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.cloudy_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.cloudy_weather_4
                    else -> if (context.isDarkTheme()) R.drawable.cloudy_weather_5 else R.drawable.cloudy_weather_5_light
                }
            }
            "09d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.storm_weather_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.storm_weather_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.storm_weather_day_4
                    else -> if (context.isDarkTheme()) R.drawable.storm_weather_day_5 else R.drawable.storm_weather_day_5_light
                }
            }
            "10d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.rainy_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.rainy_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.rainy_day_4
                    else -> if (context.isDarkTheme()) R.drawable.rainy_day_5 else R.drawable.rainy_day_5_light
                }
            }
            "11d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.thunder_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.thunder_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.thunder_day_4
                    else -> if (context.isDarkTheme()) R.drawable.thunder_day_5 else R.drawable.thunder_day_5_light
                }
            }
            "13d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.snow_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.snow_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.snow_day_4
                    else -> if (context.isDarkTheme()) R.drawable.snow_day_5 else R.drawable.snow_day_5_light
                }
            }
            "50d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.haze_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.haze_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.haze_day_4
                    else -> if (context.isDarkTheme()) R.drawable.haze_day_5 else R.drawable.haze_day_5_light
                }
            }
            "80d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.windy_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.windy_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.windy_day_4
                    else -> if (context.isDarkTheme()) R.drawable.windy_day_5 else R.drawable.windy_day_5_light
                }
            }
            "81d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.rain_snow_day_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.rain_snow_day_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.rain_snow_day_4
                    else -> if (context.isDarkTheme()) R.drawable.rain_snow_day_5 else R.drawable.rain_snow_day_5_light
                }
            }
            "82d" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.haze_weather_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.haze_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.haze_weather_4
                    else -> if (context.isDarkTheme()) R.drawable.haze_weather_5 else R.drawable.haze_weather_5_light
                }
            }



            "01n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.clear_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.clear_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.clear_night_4
                    else -> if (context.isDarkTheme()) R.drawable.clear_night_5 else R.drawable.clear_night_5_light
                }
            }
            "02n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.partly_cloudy_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.partly_cloudy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.partly_cloudy_night_4
                    else -> if (context.isDarkTheme()) R.drawable.partly_cloudy_night_5 else R.drawable.partly_cloudy_night_5_light
                }
            }
            "03n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.mostly_cloudy_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.mostly_cloudy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.mostly_cloudy_night_4
                    else -> if (context.isDarkTheme()) R.drawable.mostly_cloudy_night_5 else R.drawable.mostly_cloudy_night_5_light
                }
            }
            "04n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.cloudy_weather_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.cloudy_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.cloudy_weather_4
                    else -> if (context.isDarkTheme()) R.drawable.cloudy_weather_5 else R.drawable.cloudy_weather_5_light
                }
            }
            "09n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.storm_weather_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.storm_weather_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.storm_weather_night_4
                    else -> if (context.isDarkTheme()) R.drawable.storm_weather_night_5 else R.drawable.storm_weather_night_5_light
                }
            }
            "10n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.rainy_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.rainy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.rainy_night_4
                    else -> if (context.isDarkTheme()) R.drawable.rainy_night_5 else R.drawable.rainy_night_5_light
                }
            }
            "11n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.thunder_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.thunder_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.thunder_night_4
                    else -> if (context.isDarkTheme()) R.drawable.thunder_night_5 else R.drawable.thunder_night_5_light
                }
            }
            "13n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.snow_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.snow_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.snow_night_4
                    else -> if (context.isDarkTheme()) R.drawable.snow_night_5 else R.drawable.snow_night_5_light
                }
            }
            "50n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.haze_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.haze_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.haze_night_4
                    else -> if (context.isDarkTheme()) R.drawable.haze_night_5 else R.drawable.haze_night_5_light
                }
            }
            "80n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.windy_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.windy_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.windy_night_4
                    else -> if (context.isDarkTheme()) R.drawable.windy_night_5 else R.drawable.windy_night_5_light
                }
            }
            "81n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.rain_snow_night_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.rain_snow_night_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.rain_snow_night_4
                    else -> if (context.isDarkTheme()) R.drawable.rain_snow_night_5 else R.drawable.rain_snow_night_5_light
                }
            }
            "82n" -> {
                when (style) {
                    Constants.WeatherIconPack.COOL.value -> R.drawable.haze_weather_3
                    Constants.WeatherIconPack.MINIMAL.value -> R.drawable.haze_weather_2
                    Constants.WeatherIconPack.GOOGLE_NEWS.value -> R.drawable.haze_weather_4
                    else -> if (context.isDarkTheme()) R.drawable.haze_weather_5 else R.drawable.haze_weather_5_light
                }
            }
            else -> {
                return if (context.isDarkTheme()) R.drawable.unknown_dark else R.drawable.unknown_light
            }
        }
    }
}