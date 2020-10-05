package com.tommasoberlose.anotherwidget.components

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import kotlinx.android.synthetic.main.bottom_sheet_menu.view.*
import kotlinx.android.synthetic.main.bottom_sheet_menu.view.header
import kotlinx.android.synthetic.main.fragment_weather_settings.*
import kotlinx.android.synthetic.main.icon_pack_menu_item.view.*

class IconPackSelector(context: Context, private val header: String? = null) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    override fun show() {
        val view = View.inflate(context, R.layout.bottom_sheet_menu, null)

        // Header
        view.header.isVisible = header != null
        view.header_text.text = header ?: ""

        view.warning_text.isVisible = false

        // Menu
        for (item in Constants.WeatherIconPack.values()) {
            val itemView = View.inflate(context, R.layout.icon_pack_menu_item, null)
            itemView.label.text = context.getString(R.string.settings_weather_icon_pack_default).format(item.value + 1)
            itemView.isSelected = item.value == Preferences.weatherIconPack

            itemView.icon_1.setImageDrawable(ContextCompat.getDrawable(context, WeatherHelper.getWeatherIconResource(context, "01d", item.value)))
            itemView.icon_2.setImageDrawable(ContextCompat.getDrawable(context, WeatherHelper.getWeatherIconResource(context, "01n", item.value)))
            itemView.icon_3.setImageDrawable(ContextCompat.getDrawable(context, WeatherHelper.getWeatherIconResource(context, "10d", item.value)))
            itemView.icon_4.setImageDrawable(ContextCompat.getDrawable(context, WeatherHelper.getWeatherIconResource(context, "09n", item.value)))

            listOf<ImageView>(itemView.icon_1, itemView.icon_2, itemView.icon_3, itemView.icon_4).forEach {
                if (item == Constants.WeatherIconPack.MINIMAL) {
                    it.setColorFilter(ContextCompat.getColor(context, R.color.colorPrimaryText))
                } else {
                    it.setColorFilter(ContextCompat.getColor(context, android.R.color.transparent))
                }
            }

            itemView.setOnClickListener {
                Preferences.weatherIconPack = item.value
                this.dismiss()
            }
            view.menu.addView(itemView)
        }
        setContentView(view)
        super.show()
    }
}