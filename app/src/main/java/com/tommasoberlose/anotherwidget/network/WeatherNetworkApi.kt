package com.tommasoberlose.anotherwidget.network

import android.content.Context
import android.util.Log
import com.chibatching.kotpref.Kotpref
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
import com.tommasoberlose.anotherwidget.network.repository.*
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class WeatherNetworkApi(val context: Context) {
    suspend fun updateWeather() {
        Kotpref.init(context)
        Preferences.weatherProviderError = "-"
        Preferences.weatherProviderLocationError = ""

        if (Preferences.showWeather && Preferences.customLocationLat != "" && Preferences.customLocationLon != "") {
            when (Constants.WeatherProvider.fromInt(Preferences.weatherProvider)) {
                Constants.WeatherProvider.OPEN_WEATHER -> useOpenWeatherMap(context)
                Constants.WeatherProvider.WEATHER_GOV -> useWeatherGov(context)
                Constants.WeatherProvider.WEATHER_BIT -> useWeatherBitProvider(context)
                Constants.WeatherProvider.WEATHER_API -> useWeatherApiProvider(context)
                Constants.WeatherProvider.HERE -> useHereProvider(context)
                Constants.WeatherProvider.ACCUWEATHER -> useAccuweatherProvider(context)
                Constants.WeatherProvider.YR -> useYrProvider(context)
            }
        } else {
            WeatherHelper.removeWeather(
                context
            )

            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
        }
    }

    private fun useOpenWeatherMap(context: Context) {
        if (Preferences.weatherProviderApiOpen != "") {
            val helper = OpenWeatherMapHelper(Preferences.weatherProviderApiOpen)
            helper.setUnits(if (Preferences.weatherTempUnit == "F") Units.IMPERIAL else Units.METRIC)
            helper.getCurrentWeatherByGeoCoordinates(Preferences.customLocationLat.toDouble(), Preferences.customLocationLon.toDouble(), object :
                CurrentWeatherCallback {
                override fun onSuccess(currentWeather: CurrentWeather?) {
                    currentWeather?.let {
                        Preferences.weatherTemp = currentWeather.main.temp.toFloat()
                        Preferences.weatherIcon = currentWeather.weather[0].icon
                        Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                        MainWidget.updateWidget(context)
                    }

                    Preferences.weatherProviderError = ""
                    Preferences.weatherProviderLocationError = ""
                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                }

                override fun onFailure(throwable: Throwable?) {
                    Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                    Preferences.weatherProviderLocationError = ""
                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                }
            })
        } else {
            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_missing_key)
            Preferences.weatherProviderLocationError = ""

            WeatherHelper.removeWeather(
                context
            )
            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
        }
    }

    private suspend fun useWeatherGov(context: Context) {
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

                                Preferences.weatherProviderError = ""
                                Preferences.weatherProviderLocationError = ""

                                MainWidget.updateWidget(context)
                            } catch (ex: Exception) {
                                Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                                Preferences.weatherProviderLocationError = ""
                            } finally {
                                EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                            }
                        }
                        else -> {
                            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_connection)
                            Preferences.weatherProviderLocationError = ""
                        }
                    }
                } catch(ex: Exception) {
                    Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                    Preferences.weatherProviderLocationError = ""
                } finally {
                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                }
            }
            is NetworkResponse.ServerError -> {
                if (pointsResponse.body?.containsKey("status") == true && (pointsResponse.body?.get("status") as Double).toInt() == 404) {
                    Preferences.weatherProviderError = ""
                    Preferences.weatherProviderLocationError = context.getString(R.string.weather_provider_error_wrong_location)
                } else {
                    Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                    Preferences.weatherProviderLocationError = ""
                }

                WeatherHelper.removeWeather(
                    context
                )
                EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
            }
            else -> {
                Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_connection)
                Preferences.weatherProviderLocationError = ""
                EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
            }
        }
    }

    private suspend fun useHereProvider(context: Context) {
        if (Preferences.weatherProviderApiHere != "") {
            val repository = HereRepository()

            when (val response = repository.getWeather()) {
                is NetworkResponse.Success -> {
                    try {
                        Log.d("ciao - here", response.body.toString())
                        Preferences.weatherProviderError = ""
                        Preferences.weatherProviderLocationError = ""
                    } catch(ex: Exception) {
                        Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                        Preferences.weatherProviderLocationError = ""
                    } finally {
                        EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                    }
                }
                is NetworkResponse.ServerError -> {
                    Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                    Preferences.weatherProviderLocationError = ""
                    WeatherHelper.removeWeather(
                        context
                    )
                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                }
                else -> {
                    Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_connection)
                    Preferences.weatherProviderLocationError = ""
                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                }
            }
        } else {
            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_missing_key)
            Preferences.weatherProviderLocationError = ""

            WeatherHelper.removeWeather(
                context
            )
            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
        }
    }

    private suspend fun useWeatherBitProvider(context: Context) {
        if (Preferences.weatherProviderApiWeatherBit != "") {
            val repository = WeatherbitRepository()

            when (val response = repository.getWeather()) {
                is NetworkResponse.Success -> {
                    try {
                        val data = response.body["data"] as List<LinkedTreeMap<String, Any>>?
                        data?.first()?.let {
                            val temp = it["temp"] as Double
                            val weatherInfo = it["weather"] as LinkedTreeMap<String, Any>
                            val iconCode = weatherInfo["icon"] as String

                            Preferences.weatherTemp = temp.toFloat()
                            Preferences.weatherIcon = WeatherHelper.getWeatherBitIcon(iconCode)
                            Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                            MainWidget.updateWidget(context)

                            Preferences.weatherProviderError = ""
                            Preferences.weatherProviderLocationError = ""

                            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                        }
                    } catch(ex: Exception) {
                        Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                        Preferences.weatherProviderLocationError = ""
                    } finally {
                        EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                    }
                }
                is NetworkResponse.ServerError -> {
                    when (response.code) {
                        403 -> {
                            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_invalid_key)
                            Preferences.weatherProviderLocationError = ""
                        }
                        else -> {
                            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                            Preferences.weatherProviderLocationError = ""
                        }
                    }
                    WeatherHelper.removeWeather(
                        context
                    )
                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                }
                else -> {
                    Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_connection)
                    Preferences.weatherProviderLocationError = ""
                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                }
            }
        } else {
            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_missing_key)
            Preferences.weatherProviderLocationError = ""

            WeatherHelper.removeWeather(
                context
            )
            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
        }
    }

    private suspend fun useWeatherApiProvider(context: Context) {
        if (Preferences.weatherProviderApiWeatherApi != "") {
            val repository = WeatherApiRepository()

            when (val response = repository.getWeather()) {
                is NetworkResponse.Success -> {
                    try {
                        val current = response.body["current"] as LinkedTreeMap<String, Any>?
                        current?.let {
                            val tempC = current["temp_c"] as Double
                            val tempF = current["temp_f"] as Double
                            val isDay = current["is_day"] as Double
                            val condition = current["condition"] as LinkedTreeMap<String, Any>
                            val iconCode = condition["code"] as Double

                            Preferences.weatherTemp = if (Preferences.weatherTempUnit == "F") tempF.toFloat() else tempC.toFloat()
                            Preferences.weatherIcon = WeatherHelper.getWeatherApiIcon(iconCode.toInt(), isDay.toInt() == 1)
                            Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                            MainWidget.updateWidget(context)

                            Preferences.weatherProviderError = ""
                            Preferences.weatherProviderLocationError = ""

                            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                        }
                    } catch(ex: Exception) {
                        Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                        Preferences.weatherProviderLocationError = ""
                    } finally {
                        EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                    }
                }
                is NetworkResponse.ServerError -> {
                    when (response.code) {
                        401 -> {
                            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_invalid_key)
                            Preferences.weatherProviderLocationError = ""
                        }
                        403 -> {
                            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_expired_key)
                            Preferences.weatherProviderLocationError = ""
                        }
                        else -> {
                            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                            Preferences.weatherProviderLocationError = ""
                        }
                    }

                    WeatherHelper.removeWeather(
                        context
                    )
                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                }
                else -> {
                    Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_connection)
                    Preferences.weatherProviderLocationError = ""
                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                }
            }
        } else {
            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_missing_key)
            Preferences.weatherProviderLocationError = ""

            WeatherHelper.removeWeather(
                context
            )
            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
        }
    }

    private suspend fun useAccuweatherProvider(context: Context) {
        if (Preferences.weatherProviderApiAccuweather != "") {
//                val repository = AccuweatherRepository()

    //            when (val response = repository.getWeather()) {
    //                is NetworkResponse.Success -> {
    //                    try {
    //                        Log.d("ciao", response.body.toString())
    //                    } catch(ex: Exception) {
    //
//                Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_missing_key)
//                Preferences.weatherProviderLocationError = ""
    //                    }
    //                }
    //                is NetworkResponse.ServerError -> {
    //                    WeatherHelper.removeWeather(
    //                        context
    //                    )
    //                }
//                Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_missing_key)
//                Preferences.weatherProviderLocationError = ""
    //            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
    //            }

        } else {
            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_missing_key)
            Preferences.weatherProviderLocationError = ""

            WeatherHelper.removeWeather(
                context
            )
            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
        }
    }

    private suspend fun useYrProvider(context: Context) {
        val repository = YrRepository()

        when (val response = repository.getWeather()) {
            is NetworkResponse.Success -> {
                try {
                    val pp = response.body["properties"] as LinkedTreeMap<*, *>
                    val data = pp["timeseries"] as List<LinkedTreeMap<String, Any>>?
                    data?.let {
                        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        for (item in data) {
                            val time = Calendar.getInstance().apply { time = format.parse(item["time"] as String)!! }
                            val now = Calendar.getInstance()
                            if (time.timeInMillis >= now.timeInMillis) {
                                val dd = item["data"] as LinkedTreeMap<*, *>
                                val instant = dd["instant"] as LinkedTreeMap<*, *>
                                val next = dd["next_1_hours"] as LinkedTreeMap<*, *>

                                val details = instant["details"] as LinkedTreeMap<*, *>
                                val temp = details["air_temperature"] as Double

                                val summary = next["summary"] as LinkedTreeMap<*, *>
                                val iconCode = summary["symbol_code"] as String

                                Preferences.weatherTemp = temp.toFloat()
                                Preferences.weatherIcon = WeatherHelper.getYRIcon(iconCode, !(now.get(Calendar.HOUR_OF_DAY) >= 22 || now.get(Calendar.HOUR_OF_DAY) <= 8))
                                Preferences.weatherTempUnit = "C"
                                Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                                MainWidget.updateWidget(context)

                                Preferences.weatherProviderError = ""
                                Preferences.weatherProviderLocationError = ""
                                break
                            }
                        }
                    }



                } catch(ex: Exception) {
                    ex.printStackTrace()
                    Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                    Preferences.weatherProviderLocationError = ""
                } finally {
                    EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
                }
            }
            is NetworkResponse.ServerError -> {
                Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                Preferences.weatherProviderLocationError = ""
                WeatherHelper.removeWeather(
                    context
                )
                EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
            }
            else -> {
                Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_connection)
                Preferences.weatherProviderLocationError = ""
                EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
            }
        }
    }
}