package com.tommasoberlose.anotherwidget.ui.activities.tabs

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetWeatherProviderSettings
import com.tommasoberlose.anotherwidget.databinding.ActivityWeatherProviderBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.ui.viewmodels.tabs.WeatherProviderViewModel
import kotlinx.coroutines.launch
import net.idik.lib.slimadapter.SlimAdapter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class WeatherProviderActivity : AppCompatActivity() {

    private lateinit var adapter: SlimAdapter
    private lateinit var viewModel: WeatherProviderViewModel
    private lateinit var binding: ActivityWeatherProviderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this).get(WeatherProviderViewModel::class.java)
        binding = ActivityWeatherProviderBinding.inflate(layoutInflater)

        binding.listView.setHasFixedSize(true)
        val mLayoutManager = LinearLayoutManager(this)
        binding.listView.layoutManager = mLayoutManager

        adapter = SlimAdapter.create()
        adapter
            .register<Constants.WeatherProvider>(R.layout.weather_provider_list_item) { provider, injector ->
                injector
                    .text(R.id.text, WeatherHelper.getProviderName(this, provider))
                    .clicked(R.id.item) {
                        if (Preferences.weatherProvider != provider.rawValue) {
                            Preferences.weatherProviderError = "-"
                            Preferences.weatherProviderLocationError = ""
                        }
                        val oldValue = Preferences.weatherProvider
                        Preferences.weatherProvider = provider.rawValue
                        updateListItem(oldValue)
                        updateListItem()
                        binding.loader.isVisible = true

                        WeatherHelper.updateWeather(this@WeatherProviderActivity, true)
                    }
                    .clicked(R.id.radioButton) {
                        if (Preferences.weatherProvider != provider.rawValue) {
                            Preferences.weatherProviderError = "-"
                            Preferences.weatherProviderLocationError = ""
                        }
                        val oldValue = Preferences.weatherProvider
                        Preferences.weatherProvider = provider.rawValue
                        updateListItem(oldValue)
                        updateListItem()
                        binding.loader.isVisible = true

                        WeatherHelper.updateWeather(this@WeatherProviderActivity, true)
                    }
                    .checked(R.id.radioButton, provider.rawValue == Preferences.weatherProvider)
                    .with<TextView>(R.id.text2) {
                        if (WeatherHelper.isKeyRequired(provider)) {
                            it.text = getString(R.string.api_key_required_message)
                        }

                        if (provider == Constants.WeatherProvider.WEATHER_GOV) {
                            it.text = getString(R.string.us_only_message)
                        }

                        if (provider == Constants.WeatherProvider.YR) {
                            it.text = getString(R.string.celsius_only_message)
                        }
                    }
                    .clicked(R.id.action_configure) {
                        BottomSheetWeatherProviderSettings(this) {
                            binding.loader.isVisible = true
                            WeatherHelper.updateWeather(this@WeatherProviderActivity, true)
                        }.show()
                    }
                    .visibility(R.id.action_configure, if (/*WeatherHelper.isKeyRequired(provider) && */provider.rawValue == Preferences.weatherProvider) View.VISIBLE else View.GONE)
                    .with<TextView>(R.id.provider_error) {
                        if (Preferences.weatherProviderError != "" && Preferences.weatherProviderError != "-") {
                            it.text = Preferences.weatherProviderError
                            it.isVisible = provider.rawValue == Preferences.weatherProvider
                        } else if (Preferences.weatherProviderLocationError != "") {
                            it.text = Preferences.weatherProviderLocationError
                            it.isVisible = provider.rawValue == Preferences.weatherProvider
                        } else {
                            it.isVisible = false
                        }
                    }
                    .image(R.id.action_configure, ContextCompat.getDrawable(this, if (WeatherHelper.isKeyRequired(provider)) R.drawable.round_settings_24 else R.drawable.outline_info_24))
            }.attachTo(binding.listView)

        adapter.updateData(
            Constants.WeatherProvider.values().asList()
        )

        setupListener()
        subscribeUi(viewModel)

        setContentView(binding.root)
    }

    private fun subscribeUi(viewModel: WeatherProviderViewModel) {
        viewModel.weatherProviderError.observe(this) {
            binding.listView.postDelayed({ updateListItem() }, 300)
        }

        viewModel.weatherProviderLocationError.observe(this) {
            binding.listView.postDelayed({ updateListItem() }, 300)
        }
    }

    private fun updateListItem(provider: Int = Preferences.weatherProvider) {
        (adapter.data).forEachIndexed { index, item ->
            if (item is Constants.WeatherProvider && item.rawValue == provider) {
                adapter.notifyItemChanged(index)
            }
        }
    }

    private fun setupListener() {
        binding.actionBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        EventBus.getDefault().unregister(this)
        super.onPause()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(@Suppress("UNUSED_PARAMETER") ignore: MainFragment.UpdateUiMessageEvent?) {
        binding.loader.isVisible = Preferences.weatherProviderError == "-"
        if (Preferences.weatherProviderError == "" && Preferences.weatherProviderLocationError == "") {
            Snackbar.make(binding.listView, getString(R.string.settings_weather_provider_api_key_subtitle_all_set), Snackbar.LENGTH_LONG).show()
        }
    }
}
