package com.tommasoberlose.anotherwidget.utils

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.kwabenaberko.openweathermaplib.constants.Units
import com.kwabenaberko.openweathermaplib.implementation.OpenWeatherMapHelper
import com.kwabenaberko.openweathermaplib.implementation.callbacks.CurrentWeatherCallback
import com.kwabenaberko.openweathermaplib.models.currentweather.CurrentWeather
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Preferences


/**
 * Created by tommaso on 08/10/17.
 */

object WeatherUtil {

    fun updateWeather(context: Context) {
        if (Preferences.customLocationAdd != "") {
            weatherNetworkRequest(
                context
            )
        } else {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener {
                Preferences.customLocationLat = it.latitude.toString()
                Preferences.customLocationLon = it.longitude.toString()

                weatherNetworkRequest(context)
            }
        }
    }

    private fun weatherNetworkRequest(context: Context) {
        if (Preferences.showWeather && Preferences.weatherProviderApi != "" && Preferences.customLocationLat != "" && Preferences.customLocationLon != "") {
            val helper = OpenWeatherMapHelper(Preferences.weatherProviderApi)
            helper.setUnits(if (Preferences.weatherTempUnit == "F") Units.IMPERIAL else Units.METRIC)
            helper.getCurrentWeatherByGeoCoordinates(Preferences.customLocationLat.toDouble(), Preferences.customLocationLon.toDouble(), object : CurrentWeatherCallback {
                override fun onSuccess(currentWeather: CurrentWeather?) {
                    currentWeather?.let {
                        Preferences.weatherTemp = currentWeather.main.temp.toFloat()
                        Preferences.weatherIcon = currentWeather.weather[0].icon
                        Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                        Util.updateWidget(context)
                    }
                }

                override fun onFailure(throwable: Throwable?) {
                }

            })
        } else {
            removeWeather(context)
        }
    }

    private fun removeWeather(context: Context) {
        Preferences.remove(Preferences::weatherTemp)
        Preferences.remove(Preferences::weatherTempUnit)
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