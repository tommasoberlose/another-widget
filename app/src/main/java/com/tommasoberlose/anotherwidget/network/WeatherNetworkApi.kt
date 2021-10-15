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
import java.lang.Exception
import java.text.SimpleDateFormat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import org.greenrobot.eventbus.EventBus

class WeatherNetworkApi(val context: Context) {
    suspend fun updateWeather() {
        Kotpref.init(context)
        Preferences.weatherProviderError = "-"
        Preferences.weatherProviderLocationError = ""

        if (Preferences.customLocationLat != "" && Preferences.customLocationLon != "") {
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
            Preferences.weatherProviderLocationError = context.getString(R.string.weather_provider_error_missing_location)
            Preferences.weatherProviderError = ""

            WeatherHelper.removeWeather(
                context
            )
            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
        }
    }

    private suspend fun useOpenWeatherMap(context: Context) {
        if (Preferences.weatherProviderApiOpen != "") {
            val helper = OpenWeatherMapHelper(Preferences.weatherProviderApiOpen)
            helper.setUnits(if (Preferences.weatherTempUnit == "F") Units.IMPERIAL else Units.METRIC)
            when (val response = suspendCancellableCoroutine<Any?> { continuation ->
                helper.getCurrentWeatherByGeoCoordinates(Preferences.customLocationLat.toDouble(), Preferences.customLocationLon.toDouble(), object :
                    CurrentWeatherCallback {
                    override fun onSuccess(currentWeather: CurrentWeather?) {
                        continuation.resume(currentWeather)
                    }

                    override fun onFailure(throwable: Throwable?) {
                        continuation.resume(throwable)
                    }
                })
            }) {
                is CurrentWeather -> {
                    Preferences.weatherTemp = response.main.temp.toFloat()
                    Preferences.weatherIcon = response.weather[0].icon
                    Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                    MainWidget.updateWidget(context)

                    Preferences.weatherProviderError = ""
                    Preferences.weatherProviderLocationError = ""
                }
                is Throwable -> {
                    if (response.javaClass == Throwable::class.java) {
                        // server error, see [OpenWeatherMapHelper.handleCurrentWeatherResponse]
                        if (response.message?.startsWith("UnAuthorized") == true) {
                            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_invalid_key)
                            Preferences.weatherProviderLocationError = ""
                        }
                        else {
                            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                            Preferences.weatherProviderLocationError = ""
                        }
                        WeatherHelper.removeWeather(
                            context
                        )
                    }
                    else {
                        Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_connection)
                        Preferences.weatherProviderLocationError = ""
                    }
                }
                else -> {
                    Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                    Preferences.weatherProviderLocationError = ""
                }
            }
            EventBus.getDefault().post(MainFragment.UpdateUiMessageEvent())
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
                                @android.annotation.SuppressLint("SimpleDateFormat")
                                val format = SimpleDateFormat(
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N)
                                        "yyyy-MM-dd'T'HH:mm:ssXXX"
                                    else
                                        "yyyy-MM-dd'T'HH:mm:ssZ"
                                )
                                for (period in periods) {
                                    val now = period as LinkedTreeMap<*, *>
                                    val endTime = format.parse(now["endTime"] as String)!!
                                    if (endTime.time > System.currentTimeMillis()) {
                                        val temp = now["temperature"] as Double
                                        val fullIcon = now["icon"] as String
                                        val isDaytime = now["isDaytime"] as Boolean

                                        Preferences.weatherTemp = temp.toFloat()
                                        Preferences.weatherIcon = WeatherHelper.getWeatherGovIcon(fullIcon, isDaytime)
                                        Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                                        MainWidget.updateWidget(context)

                                        Preferences.weatherProviderError = ""
                                        Preferences.weatherProviderLocationError = ""
                                        break
                                    }
                                }
                            } catch (ex: Exception) {
                                Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                                Preferences.weatherProviderLocationError = ""
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
                when (pointsResponse.code) {
                    404 -> {
                        Preferences.weatherProviderError = ""
                        Preferences.weatherProviderLocationError = context.getString(R.string.weather_provider_error_wrong_location)
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
    }

    private suspend fun useHereProvider(context: Context) {
        if (Preferences.weatherProviderApiHere != "") {
            val repository = HereRepository()

            when (val response = repository.getWeather()) {
                is NetworkResponse.Success -> {
                    try {
                        val observations = response.body["observations"] as LinkedTreeMap<*, *>
                        val location = (observations["location"] as List<*>).first() as LinkedTreeMap<*, *>
                        val observation = (location["observation"] as List<*>).first() as LinkedTreeMap<*, *>
                        val iconName = observation["iconName"] as String
                        val daylight = observation["daylight"] as String
                        val temperature = observation["temperature"] as String

                        Preferences.weatherTemp = temperature.toFloat()
                        Preferences.weatherIcon = repository.getWeatherIcon(iconName, daylight != "N")
                        Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                        MainWidget.updateWidget(context)

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
                    when (response.code) {
                        401 -> {
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

    private suspend fun useWeatherBitProvider(context: Context) {
        if (Preferences.weatherProviderApiWeatherBit != "") {
            val repository = WeatherbitRepository()

            when (val response = repository.getWeather()) {
                is NetworkResponse.Success -> {
                    try {
                        val data = response.body["data"] as List<*>?
                        data?.first()?.let { it as LinkedTreeMap<*, *>
                            val temp = it["temp"] as Double
                            val weatherInfo = it["weather"] as LinkedTreeMap<*, *>
                            val iconCode = weatherInfo["icon"] as String

                            Preferences.weatherTemp = temp.toFloat()
                            Preferences.weatherIcon = WeatherHelper.getWeatherBitIcon(iconCode)
                            Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                            MainWidget.updateWidget(context)

                            Preferences.weatherProviderError = ""
                            Preferences.weatherProviderLocationError = ""
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
                        val current = response.body["current"] as LinkedTreeMap<*, *>?
                        current?.let {
                            val tempC = current["temp_c"] as Double
                            val tempF = current["temp_f"] as Double
                            val isDay = current["is_day"] as Double
                            val condition = current["condition"] as LinkedTreeMap<*, *>
                            val iconCode = condition["code"] as Double

                            Preferences.weatherTemp = if (Preferences.weatherTempUnit == "F") tempF.toFloat() else tempC.toFloat()
                            Preferences.weatherIcon = WeatherHelper.getWeatherApiIcon(iconCode.toInt(), isDay.toInt() == 1)
                            Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                            MainWidget.updateWidget(context)

                            Preferences.weatherProviderError = ""
                            Preferences.weatherProviderLocationError = ""
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
            val repository = AccuweatherRepository()

            when (val locationResponse = repository.getLocation()) {
                is NetworkResponse.Success -> {
                    try {
                        val key = locationResponse.body["Key"] as String

                        when (val weatherResponse = repository.getWeather(key)) {
                            is NetworkResponse.Success -> {
                                try {
                                    weatherResponse.body.first().let {
                                        val temp = it["Temperature"] as LinkedTreeMap<*, *>
                                        val tempC = (temp["Metric"] as LinkedTreeMap<*, *>)["Value"] as Double
                                        val tempF = (temp["Imperial"] as LinkedTreeMap<*, *>)["Value"] as Double
                                        val isDay = it["IsDayTime"] as Boolean
                                        val icon = it["WeatherIcon"] as Double

                                        Preferences.weatherTemp = if (Preferences.weatherTempUnit == "F") tempF.toFloat() else tempC.toFloat()
                                        Preferences.weatherIcon = repository.getWeatherIcon(icon.toInt(), isDay)
                                        Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                                        MainWidget.updateWidget(context)
                                    }

                                    Preferences.weatherProviderError = ""
                                    Preferences.weatherProviderLocationError = ""
                                } catch (ex: Exception) {
                                    Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_generic)
                                    Preferences.weatherProviderLocationError = ""
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
                    when (locationResponse.code) {
                        401 -> {
                            Preferences.weatherProviderError = context.getString(R.string.weather_provider_error_invalid_key)
                            Preferences.weatherProviderLocationError = ""
                        }
                        503 -> {
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

    private suspend fun useYrProvider(context: Context) {
        val repository = YrRepository()

        when (val response = repository.getWeather()) {
            is NetworkResponse.Success -> {
                try {
                    val pp = response.body["properties"] as LinkedTreeMap<*, *>
                    val data = pp["timeseries"] as List<*>?
                    data?.first()?.let { it as LinkedTreeMap<*, *>
                        val dd = it["data"] as LinkedTreeMap<*, *>
                        val instant = dd["instant"] as LinkedTreeMap<*, *>
                        val next = dd["next_1_hours"] as LinkedTreeMap<*, *>

                        val details = instant["details"] as LinkedTreeMap<*, *>
                        val temp = details["air_temperature"] as Double

                        val summary = next["summary"] as LinkedTreeMap<*, *>
                        val iconCode = summary["symbol_code"] as String

                        Preferences.weatherTemp = temp.toFloat()
                        Preferences.weatherIcon = WeatherHelper.getYRIcon(iconCode)
                        Preferences.weatherTempUnit = "C"
                        Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                        MainWidget.updateWidget(context)

                        Preferences.weatherProviderError = ""
                        Preferences.weatherProviderLocationError = ""
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
