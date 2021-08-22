package com.tommasoberlose.anotherwidget.components

import android.content.Context
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.WeatherProviderSettingsLayoutBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.utils.openURI

class BottomSheetWeatherProviderSettings(context: Context, callback: () -> Unit) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private var binding: WeatherProviderSettingsLayoutBinding = WeatherProviderSettingsLayoutBinding.inflate(android.view.LayoutInflater.from(context))

    init {
        binding.apiKeyContainer.isVisible = WeatherHelper.isKeyRequired()
        binding.actionSaveKey.isVisible = WeatherHelper.isKeyRequired()

        WeatherHelper.getProviderInfoTitle(context).let { title ->
            binding.infoTitle.text = title
            binding.infoTitle.isVisible = title != ""
        }

        WeatherHelper.getProviderInfoSubtitle(context).let { subtitle ->
            binding.infoSubtitle.text = subtitle
            binding.infoSubtitle.isVisible = subtitle != ""
        }

        binding.infoProvider.text = WeatherHelper.getProviderName(context)

        binding.apiKey.editText?.setText(when (Constants.WeatherProvider.fromInt(Preferences.weatherProvider)) {
            Constants.WeatherProvider.OPEN_WEATHER -> Preferences.weatherProviderApiOpen
            Constants.WeatherProvider.WEATHER_BIT -> Preferences.weatherProviderApiWeatherBit
            Constants.WeatherProvider.WEATHER_API -> Preferences.weatherProviderApiWeatherApi
            Constants.WeatherProvider.HERE -> Preferences.weatherProviderApiHere
            Constants.WeatherProvider.ACCUWEATHER -> Preferences.weatherProviderApiAccuweather
            Constants.WeatherProvider.WEATHER_GOV,
            Constants.WeatherProvider.YR,
            null -> ""
        })

        binding.actionOpenProvider.setOnClickListener {
            context.openURI(WeatherHelper.getProviderLink())
        }

        binding.actionSaveKey.setOnClickListener {
            val key = binding.apiKey.editText?.text.toString()
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

        setContentView(binding.root)
    }
}