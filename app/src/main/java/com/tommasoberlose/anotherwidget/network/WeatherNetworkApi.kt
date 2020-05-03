package com.tommasoberlose.anotherwidget.network

import android.content.Context
import com.kwabenaberko.openweathermaplib.constants.Units
import com.kwabenaberko.openweathermaplib.implementation.OpenWeatherMapHelper
import com.kwabenaberko.openweathermaplib.implementation.callbacks.CurrentWeatherCallback
import com.kwabenaberko.openweathermaplib.models.currentweather.CurrentWeather
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import org.greenrobot.eventbus.EventBus

class WeatherNetworkApi(val context: Context) {
    fun updateWeather() {
        if (Preferences.showWeather && Preferences.weatherProviderApi != "" && Preferences.customLocationLat != "" && Preferences.customLocationLon != "") {
            val helper = OpenWeatherMapHelper(Preferences.weatherProviderApi)
            helper.setUnits(if (Preferences.weatherTempUnit == "F") Units.IMPERIAL else Units.METRIC)
            helper.getCurrentWeatherByGeoCoordinates(Preferences.customLocationLat.toDouble(), Preferences.customLocationLon.toDouble(), object :
                CurrentWeatherCallback {
                override fun onSuccess(currentWeather: CurrentWeather?) {
                    currentWeather?.let {
                        Preferences.weatherTemp = currentWeather.main.temp.toFloat()
                        Preferences.weatherIcon = currentWeather.weather[0].icon
                        Preferences.weatherRealTempUnit = Preferences.weatherTempUnit
                        MainWidget.updateWidget(
                            context
                        )

                        EventBus.getDefault().post(MainActivity.UpdateUiMessageEvent())
                    }
                }

                override fun onFailure(throwable: Throwable?) {
                }

            })
        } else {
            WeatherHelper.removeWeather(
                context
            )
        }
    }
}