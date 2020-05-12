package com.tommasoberlose.anotherwidget.helpers

import android.Manifest
import android.content.Context
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.db.EventRepository
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

    fun getWeatherIconResource(icon: String): Int {
        when (icon) {
            "01d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.clear_day else R.drawable.clear_day_2
            }
            "02d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.partly_cloudy else R.drawable.partly_cloudy_2
            }
            "03d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.mostly_cloudy else R.drawable.mostly_cloudy_2
            }
            "04d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.cloudy_weather else R.drawable.cloudy_weather_2
            }
            "09d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.storm_weather_day else R.drawable.storm_weather_day_2
            }
            "10d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.rainy_day else R.drawable.rainy_day_2
            }
            "11d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.thunder_day else R.drawable.thunder_day_2
            }
            "13d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.snow_day else R.drawable.snow_day_2
            }
            "50d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.haze_day else R.drawable.haze_day_2
            }
            "80d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.windy_day else R.drawable.windy_day_2
            }
            "81d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.rain_snow_day else R.drawable.rain_snow_day_2
            }
            "82d" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.haze_weather else R.drawable.haze_weather_2
            }



            "01n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.clear_night else R.drawable.clear_night_2
            }
            "02n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.partly_cloudy_night else R.drawable.partly_cloudy_night_2
            }
            "03n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.mostly_cloudy_night else R.drawable.mostly_cloudy_night_2
            }
            "04n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.cloudy_weather else R.drawable.cloudy_weather_2
            }
            "09n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.storm_weather_night else R.drawable.storm_weather_night_2
            }
            "10n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.rainy_night else R.drawable.rainy_night_2
            }
            "11n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.thunder_night else R.drawable.thunder_night_2
            }
            "13n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.snow_night else R.drawable.snow_night_2
            }
            "50n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.haze_night else R.drawable.haze_night_2
            }
            "80n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.windy_night else R.drawable.windy_night_2
            }
            "81n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.rain_snow_night else R.drawable.rain_snow_night_2
            }
            "82n" -> {
                return if (Preferences.weatherIconPack == Constants.WeatherIconPack.DEFAULT.value) R.drawable.haze_weather else R.drawable.haze_weather_2
            }
            else -> {
                return R.drawable.unknown
            }
        }
    }
}