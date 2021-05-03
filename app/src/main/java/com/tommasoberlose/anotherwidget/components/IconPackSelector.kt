package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.BottomSheetMenuBinding
import com.tommasoberlose.anotherwidget.databinding.IconPackMenuItemBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper

class IconPackSelector(context: Context, private val header: String? = null) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    private var binding = BottomSheetMenuBinding.inflate(LayoutInflater.from(context))

    override fun show() {
        // Header
        binding.header.isVisible = header != null
        binding.headerText.text = header ?: ""

        binding.warningText.isVisible = false

        // Menu
        for (item in Constants.WeatherIconPack.values()) {
            val itemBinding = IconPackMenuItemBinding.inflate(LayoutInflater.from(context))
            itemBinding.label.text = context.getString(R.string.settings_weather_icon_pack_default).format(item.rawValue + 1)
            itemBinding.root.isSelected = item.rawValue == Preferences.weatherIconPack

            itemBinding.icon1.setImageDrawable(ContextCompat.getDrawable(context, WeatherHelper.getWeatherIconResource(context, "01d", item.rawValue)))
            itemBinding.icon2.setImageDrawable(ContextCompat.getDrawable(context, WeatherHelper.getWeatherIconResource(context, "01n", item.rawValue)))
            itemBinding.icon3.setImageDrawable(ContextCompat.getDrawable(context, WeatherHelper.getWeatherIconResource(context, "10d", item.rawValue)))
            itemBinding.icon4.setImageDrawable(ContextCompat.getDrawable(context, WeatherHelper.getWeatherIconResource(context, "09n", item.rawValue)))

            listOf<ImageView>(itemBinding.icon1, itemBinding.icon2, itemBinding.icon3, itemBinding.icon4).forEach {
                if (item == Constants.WeatherIconPack.MINIMAL) {
                    it.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimaryText))
                } else {
                    it.setColorFilter(ContextCompat.getColor(context, android.R.color.transparent))
                }
            }

            itemBinding.root.setOnClickListener {
                Preferences.weatherIconPack = item.rawValue
                this.dismiss()
            }
            binding.menu.addView(itemBinding.root)
        }
        setContentView(binding.root)
        super.show()
    }
}