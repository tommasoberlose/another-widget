package com.tommasoberlose.anotherwidget.ui.activities

import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.collapse
import com.tommasoberlose.anotherwidget.utils.expand
import com.tommasoberlose.anotherwidget.utils.openURI
import kotlinx.android.synthetic.main.activity_weather_provider.*
import kotlinx.android.synthetic.main.the_widget_sans.*

class WeatherProviderActivity : AppCompatActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_provider)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        api_key.editText?.setText(Preferences.weatherProviderApi)
    }

    override fun onResume() {
        super.onResume()

        setListener()
        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: MainViewModel) {
        viewModel.weatherProvider.observe(this, Observer {
            weather_provider.editText?.setText(WeatherHelper.getProviderName(this, Constants.WeatherProvider.fromInt(Preferences.weatherProvider)!!).split("\n").first())

            api_key_container.isVisible = WeatherHelper.isKeyRequired()

            WeatherHelper.getProviderInfoTitle(this).let {
                info_title.text = it
                info_title.isVisible = it != ""
            }

            WeatherHelper.getProviderInfoSubtitle(this).let {
                info_subtitle.text = it
                info_subtitle.isVisible = it != ""
            }

            action_open_provider.text = WeatherHelper.getProviderLinkName(this)
        })

        viewModel.weatherProviderApi
    }

    private fun setListener() {
        action_back.setOnClickListener {
            onBackPressed()
        }

        action_open_provider.setOnClickListener {
            openURI(WeatherHelper.getProviderLink())
        }

        weather_provider_inner.setOnClickListener {
            if (Preferences.showWeather) {
                val dialog = BottomSheetMenu<Int>(this, header = getString(R.string.settings_weather_provider_api)).setSelectedValue(Preferences.weatherProvider)
                (0 until 11).forEach {
                    val item = Constants.WeatherProvider.fromInt(it)
                    dialog.addItem(WeatherHelper.getProviderName(this, item!!), it)
                }

                dialog.addOnSelectItemListener { value ->
                    Preferences.weatherProvider = value
                }.show()
            }
        }

        api_key.editText?.addTextChangedListener {
            val key = it?.toString() ?: ""
            Preferences.weatherProviderApi = key
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}
