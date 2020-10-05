package com.tommasoberlose.anotherwidget.network

import android.content.Context
import android.util.Log
import com.google.gson.internal.LinkedTreeMap
import com.haroldadmin.cnradapter.NetworkResponse
import com.haroldadmin.cnradapter.executeWithRetry
import com.kwabenaberko.openweathermaplib.constants.Units
import com.kwabenaberko.openweathermaplib.implementation.OpenWeatherMapHelper
import com.kwabenaberko.openweathermaplib.implementation.callbacks.CurrentWeatherCallback
import com.kwabenaberko.openweathermaplib.models.currentweather.CurrentWeather
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.network.repository.WeatherGovRepository
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.lang.Exception

class WeatherNetworkApi(val context: Context) {
    fun updateWeather() {
        Preferences.weatherProviderError = ""
        Preferences.weatherProviderLocationError = ""

        if (Preferences.showWeather && Preferences.customLocationLat != "" && Preferences.customLocationLon != "") {
            when (Constants.WeatherProvider.fromInt(Preferences.weatherProvider)) {
                Constants.WeatherProvider.OPEN_WEATHER -> useOpenWeatherMap(context)
                Constants.WeatherProvider.WEATHER_GOV -> useWeatherGov(context)
            }
        } else {
            WeatherHelper.removeWeather(
                context
            )
        }
    }

    private fun useOpenWeatherMap(context: Context) {
        if (Preferences.weatherProviderApi != "" ) {
            val helper = OpenWeatherMapHelper(Preferences.weatherProviderApi)
            helper.setUnits(if (Preferences.weatherTempUnit == "F") Units.IMPERIAL else Units.METRIC)
            helper.getCurrentWeatherByGeoCoordinates(Preferences.customLocationLat.toDouble(), Preferences.customLocationLon.toDouble(), object :
                CurrentWeatherCallback {
                override fun onSuccess(currentWeather: CurrentWeather?) {
                    currentWeather?.let {
                        Preferences.weatherTemp = currentWeather.main.temp.toFloat()
                        Preferences.weatherIcon = currentWeather.weather[0].icon
                        Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                        MainWidget.updateWidget(context)

                        EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                    }
                }

                override fun onFailure(throwable: Throwable?) {
                }

            })
        } else {
            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_missing_key)
            Preferences.weatherProviderLocationError = ""

            WeatherHelper.removeWeather(
                context
            )
        }
    }

    private fun useWeatherGov(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = WeatherGovRepository()
            val pointsResponse = executeWithRetry(times = 5) {
                repository.getGridPoints(
                    Preferences.customLocationLat,
                    Preferences.customLocationLon
                )
            }

            when (pointsResponse) {
                is NetworkResponse.Success -> {
                    try {
                        val pp = pointsResponse.body["properties"] as LinkedTreeMap<*, *>
                        val gridId = pp["gridId"] as String
                        val gridX = pp["gridX"] as Double
                        val gridY = pp["gridY"] as Double

                        when (val weatherResponse = repository.getWeather(
                            gridId,
                            gridX,
                            gridY,
                            if (Preferences.weatherTempUnit == "F") "us" else "si"
                        )) {
                            is NetworkResponse.Success -> {
                                try {
                                    val props =
                                        weatherResponse.body["properties"] as LinkedTreeMap<*, *>
                                    val periods = props["periods"] as List<*>
                                    val now = periods[0] as LinkedTreeMap<*, *>

                                    val temp = now["temperature"] as Double
                                    val fullIcon = now["icon"] as String
                                    val isDaytime = now["isDaytime"] as Boolean

                                    Preferences.weatherTemp = temp.toFloat()
                                    Preferences.weatherIcon = WeatherHelper.getWeatherGovIcon(fullIcon, isDaytime)
                                    Preferences.weatherRealTempUnit = Preferences.weatherTempUnit

                                    MainWidget.updateWidget(context)

                                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                                } catch (ex: Exception) {
                                }
                            }
                        }
                    } catch(ex: Exception) {

                    }
                }
                is NetworkResponse.ServerError -> {
                    if (pointsResponse.body?.containsKey("status") == true && (pointsResponse.body?.get("status") as Double).toInt() == 404) {
                        Preferences.weatherProviderError = ""
                        Preferences.weatherProviderLocationError = context.getString(R.string.weather_provider_error_wrong_location)

                        WeatherHelper.removeWeather(
                            context
                        )
                    }
                }
            }

        }
    }
}