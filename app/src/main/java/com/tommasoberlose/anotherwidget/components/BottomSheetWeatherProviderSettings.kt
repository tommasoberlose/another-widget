package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.utils.openURI
import kotlinx.android.synthetic.main.weather_provider_settings_layout.view.*

class BottomSheetWeatherProviderSettings(context: Context, callback: () -> Unit) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    init {
        val view = View.inflate(context, R.layout.weather_provider_settings_layout, null)
        view.api_key_container.isVisible = WeatherHelper.isKeyRequired()
        view.action_save_key.isVisible = WeatherHelper.isKeyRequired()

        WeatherHelper.getProviderInfoTitle(context).let { title ->
            view.info_title.text = title
            view.info_title.isVisible = title != ""
        }

        WeatherHelper.getProviderInfoSubtitle(context).let { subtitle ->
            view.info_subtitle.text = subtitle
            view.info_subtitle.isVisible = subtitle != ""
        }

        view.info_provider.text = WeatherHelper.getProviderName(context)

        view.api_key.editText?.setText(when (Constants.WeatherProvider.fromInt(Preferences.weatherProvider)) {
            Constants.WeatherProvider.OPEN_WEATHER -> Preferences.weatherProviderApiOpen
            Constants.WeatherProvider.WEATHER_BIT -> Preferences.weatherProviderApiWeatherBit
            Constants.WeatherProvider.WEATHER_API -> Preferences.weatherProviderApiWeatherApi
            Constants.WeatherProvider.HERE -> Preferences.weatherProviderApiHere
            Constants.WeatherProvider.ACCUWEATHER -> Preferences.weatherProviderApiAccuweather
            Constants.WeatherProvider.WEATHER_GOV,
            Constants.WeatherProvider.YR,
            null -> ""
        })

        view.action_open_provider.setOnClickListener {
            context.openURI(WeatherHelper.getProviderLink())
        }

        view.action_save_key.setOnClickListener {
            val key = view.api_key.editText?.text.toString()
            when (Constants.WeatherProvider.fromInt(Preferences.weatherProvider)) {
                Constants.WeatherProvider.OPEN_WEATHER -> Preferences.weatherProviderApiOpen = key
                Constants.WeatherProvider.WEATHER_BIT -> Preferences.weatherProviderApiWeatherBit = key
                Constants.WeatherProvider.WEATHER_API -> Preferences.weatherProviderApiWeatherApi = key
                Constants.WeatherProvider.HERE -> Preferences.weatherProviderApiHere = key
                Constants.WeatherProvider.ACCUWEATHER -> Preferences.weatherProviderApiAccuweather = key
                else -> {}
            }
            callback.invoke()
            dismiss()
        }

        setContentView(view)
    }
}