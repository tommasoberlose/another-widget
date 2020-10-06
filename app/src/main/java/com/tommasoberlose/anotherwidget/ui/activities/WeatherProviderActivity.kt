package com.tommasoberlose.anotherwidget.ui.activities

import android.app.Activity
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.ActivityChooseApplicationBinding
import com.tommasoberlose.anotherwidget.databinding.ActivityWeatherProviderBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.ui.viewmodels.ChooseApplicationViewModel
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.openURI
import kotlinx.android.synthetic.main.activity_choose_application.*
import kotlinx.android.synthetic.main.activity_weather_provider.*
import kotlinx.android.synthetic.main.activity_weather_provider.action_back
import kotlinx.android.synthetic.main.activity_weather_provider.list_view
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.idik.lib.slimadapter.SlimAdapter

class WeatherProviderActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_provider)

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        api_key.editText?.setText(Preferences.weatherProviderApiOpen)

        list_view.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        list_view.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<Int>(R.layout.list_item) { item, injector ->
                val provider = Constants.WeatherProvider.fromInt(item)!!
                injector
                    .text(R.id.text, WeatherHelper.getProviderName(this, provider))
                    .clicked(R.id.text) {
                        Preferences.weatherProvider = item
                    }
            }
            .attachTo(list_view)

        setupListener()
        subscribeUi(viewModel)
    }

    private fun subscribeUi(viewModel: MainViewModel) {
        adapter.updateData(Constants.WeatherProvider.values().map { it.value })

//        viewModel.weatherProvider.observe(this, Observer {
//            weather_provider.editText?.setText(WeatherHelper.getProviderName(this, Constants.WeatherProvider.fromInt(Preferences.weatherProvider)!!).split("\n").first())
//
//            api_key_container.isVisible = WeatherHelper.isKeyRequired()
//
//            WeatherHelper.getProviderInfoTitle(this).let {
//                info_title.text = it
//                info_title.isVisible = it != ""
//            }
//
//            WeatherHelper.getProviderInfoSubtitle(this).let {
//                info_subtitle.text = it
//                info_subtitle.isVisible = it != ""
//            }
//
//            action_open_provider.text = WeatherHelper.getProviderLinkName(this)
//
//            api_key.editText?.setText(when (Constants.WeatherProvider.fromInt(it)) {
//                Constants.WeatherProvider.OPEN_WEATHER -> Preferences.weatherProviderApiOpen
//                Constants.WeatherProvider.WEATHER_BIT -> Preferences.weatherProviderApiWeatherBit
//                Constants.WeatherProvider.WEATHER_API -> Preferences.weatherProviderApiWeatherApi
//                Constants.WeatherProvider.HERE -> Preferences.weatherProviderApiHere
//                Constants.WeatherProvider.ACCUWEATHER -> Preferences.weatherProviderApiAccuweather
//                Constants.WeatherProvider.WEATHER_GOV,
//                Constants.WeatherProvider.YR,
//                null -> ""
//            })
//        })
    }

    private fun setupListener() {
        action_back.setOnClickListener {
            onBackPressed()
        }

        action_open_provider.setOnClickListener {
            openURI(WeatherHelper.getProviderLink())
        }

        weather_provider_inner.setOnClickListener {
            if (Preferences.showWeather) {
                val dialog = BottomSheetMenu<Int>(this, header = getString(R.string.settings_weather_provider_api)).setSelectedValue(Preferences.weatherProvider)
                (0 until 7).forEach {
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
            when (Constants.WeatherProvider.fromInt(Preferences.weatherProvider)) {
                Constants.WeatherProvider.OPEN_WEATHER -> Preferences.weatherProviderApiOpen = key
                Constants.WeatherProvider.WEATHER_BIT -> Preferences.weatherProviderApiWeatherBit = key
                Constants.WeatherProvider.WEATHER_API -> Preferences.weatherProviderApiWeatherApi = key
                Constants.WeatherProvider.HERE -> Preferences.weatherProviderApiHere = key
                Constants.WeatherProvider.ACCUWEATHER -> Preferences.weatherProviderApiAccuweather = key
                else -> {}
            }
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}
