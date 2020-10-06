package com.tommasoberlose.anotherwidget.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.BuildCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.chibatching.kotpref.bulk
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.components.IconPackSelector
import com.tommasoberlose.anotherwidget.components.MaterialBottomSheetDialog
import com.tommasoberlose.anotherwidget.databinding.FragmentWeatherSettingsBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.receivers.WeatherReceiver
import com.tommasoberlose.anotherwidget.ui.activities.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.activities.CustomLocationActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.activities.WeatherProviderActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import kotlinx.android.synthetic.main.fragment_weather_settings.*
import kotlinx.android.synthetic.main.fragment_weather_settings.scrollView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WeatherTabFragment : Fragment() {

    companion object {
        fun newInstance() = WeatherTabFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<FragmentWeatherSettingsBinding>(inflater, R.layout.fragment_weather_settings, container, false)

        subscribeUi(binding, viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupListener()
    }

    private fun subscribeUi(
        binding: FragmentWeatherSettingsBinding,
        viewModel: MainViewModel
    ) {
        binding.isWeatherVisible = Preferences.showWeather

        viewModel.showWeatherWarning.observe(viewLifecycleOwner, Observer {
            weather_warning?.isVisible = it
            checkLocationPermission()
        })

        viewModel.showWeather.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_weather_label?.text =
                    if (it) getString(R.string.show_weather_visible) else getString(R.string.show_weather_not_visible)
                checkWeatherProviderConfig()
                binding.isWeatherVisible = it
            }
            checkLocationPermission()
        })

        viewModel.weatherProvider.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                label_weather_provider.text = WeatherHelper.getProviderName(requireContext(), Constants.WeatherProvider.fromInt(it)!!)
                checkWeatherProviderConfig()
            }
        })

        viewModel.weatherProviderError.observe(viewLifecycleOwner, Observer {
            checkWeatherProviderConfig()
        })

        viewModel.weatherProviderLocationError.observe(viewLifecycleOwner, Observer {
            checkWeatherProviderConfig()
        })

        viewModel.customLocationAdd.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                background_location_warning.isVisible = it == ""
                label_custom_location?.text =
                    if (it == "") getString(R.string.custom_location_gps) else it
            }
            checkLocationPermission()
        })

        viewModel.weatherTempUnit.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                temp_unit?.text =
                    if (it == "F") getString(R.string.fahrenheit) else getString(R.string.celsius)
            }
            checkLocationPermission()
        })

        viewModel.weatherRefreshPeriod.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                label_weather_refresh_period?.text = getString(SettingsStringHelper.getRefreshPeriodString(it))
            }
            checkLocationPermission()
        })

        viewModel.weatherIconPack.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                label_weather_icon_pack?.text = getString(R.string.settings_weather_icon_pack_default).format((it + 1))
//                weather_icon_pack.setImageDrawable(ContextCompat.getDrawable(requireContext(), WeatherHelper.getWeatherIconResource("02d")))
//                if (it == Constants.WeatherIconPack.MINIMAL.value) {
//                    weather_icon_pack.setColorFilter(ContextCompat.getColor(requireContext(), R.color.colorPrimaryText))
//                } else {
//                    weather_icon_pack.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.transparent))
//                }
            }
            checkLocationPermission()
        })

        viewModel.weatherAppName.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                weather_app_label?.text =
                    if (it != "") it else getString(R.string.default_weather_app)
            }
        })
    }

    private fun checkLocationPermission() {
        // Background permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && activity?.checkGrantedPermission(Manifest.permission.ACCESS_FINE_LOCATION) == true && activity?.checkGrantedPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != true) {
            requirePermission()
        }

        if (activity?.checkGrantedPermission(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else Manifest.permission.ACCESS_FINE_LOCATION) == true) {
            location_permission_alert?.isVisible = false
            background_location_warning.isVisible = Preferences.customLocationAdd == ""
            WeatherReceiver.setUpdates(requireContext())
        } else if (Preferences.showWeather && Preferences.customLocationAdd == "") {
            location_permission_alert?.isVisible = true
            background_location_warning.isVisible = false
            location_permission_alert?.setOnClickListener {
                MaterialBottomSheetDialog(requireContext(), message = getString(R.string.background_location_warning))
                    .setPositiveButton(getString(android.R.string.ok)) {
                        requirePermission()
                    }
                    .show()
            }
        } else {
            location_permission_alert?.isVisible = false
        }
    }

    private fun checkWeatherProviderConfig() {
        weather_provider_error.isVisible = Preferences.weatherProviderError != ""
        weather_provider_error?.text = Preferences.weatherProviderError

        weather_provider_location_error.isVisible = Preferences.weatherProviderLocationError != ""
        weather_provider_location_error?.text = Preferences.weatherProviderLocationError
    }

    private fun setupListener() {
        action_hide_weather_warning.setOnClickListener {
            Preferences.showWeatherWarning = false
        }

        action_show_weather.setOnClickListener {
            Preferences.showWeather = !Preferences.showWeather
        }

        show_weather_switch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showWeather = enabled
        }

        action_weather_provider.setOnClickListener {
            if (Preferences.showWeather) {
                startActivityForResult(
                    Intent(requireContext(), WeatherProviderActivity::class.java),
                    RequestCode.WEATHER_PROVIDER_REQUEST_CODE.code
                )
            }
        }

        action_custom_location.setOnClickListener {
            if (Preferences.showWeather) {
                startActivityForResult(
                    Intent(requireContext(), CustomLocationActivity::class.java),
                    Constants.RESULT_CODE_CUSTOM_LOCATION
                )
            }
        }

        action_change_unit.setOnClickListener {
            if (Preferences.showWeather) {
                BottomSheetMenu<String>(requireContext(), header = getString(R.string.settings_unit_title)).setSelectedValue(Preferences.weatherTempUnit)
                    .addItem(getString(R.string.fahrenheit), "F")
                    .addItem(getString(R.string.celsius), "C")
                    .addOnSelectItemListener { value ->
                        if (value != Preferences.weatherTempUnit) {
                            WeatherHelper.updateWeather(requireContext())
                        }
                        Preferences.weatherTempUnit = value
                    }.show()
            }
        }

        action_weather_refresh_period.setOnClickListener {
            if (Preferences.showWeather) {
                val dialog =
                    BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_weather_refresh_period_title)).setSelectedValue(Preferences.weatherRefreshPeriod)
                (5 downTo 0).forEach {
                    dialog.addItem(getString(SettingsStringHelper.getRefreshPeriodString(it)), it)
                }
                dialog
                    .addOnSelectItemListener { value ->
                        Preferences.weatherRefreshPeriod = value
                    }.show()
            }
        }

        action_weather_icon_pack.setOnClickListener {
            if (Preferences.showWeather) {
                IconPackSelector(requireContext(), header = getString(R.string.settings_weather_icon_pack_title)).show()
            }
        }

        action_weather_app.setOnClickListener {
            if (Preferences.showWeather) {
                startActivityForResult(
                    Intent(requireContext(), ChooseApplicationActivity::class.java),
                    RequestCode.WEATHER_APP_REQUEST_CODE.code
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constants.RESULT_CODE_CUSTOM_LOCATION -> {
                    WeatherReceiver.setUpdates(requireContext())
                    checkLocationPermission()
                }
                RequestCode.WEATHER_APP_REQUEST_CODE.code -> {
                    Preferences.bulk {
                        weatherAppName = data?.getStringExtra(Constants.RESULT_APP_NAME) ?: getString(R.string.default_weather_app)
                        weatherAppPackage = data?.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: ""
                    }
                    MainWidget.updateWidget(requireContext())
                }
                RequestCode.WEATHER_PROVIDER_REQUEST_CODE.code -> {
                    checkLocationPermission()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun requirePermission() {
        Dexter.withContext(requireContext())
            .withPermissions(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACCESS_BACKGROUND_LOCATION else Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.areAllPermissionsGranted()){
                            checkLocationPermission()
                        }
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    // Remember to invoke this method when the custom rationale is closed
                    // or just by default if you don't want to use any custom rationale.
                    token?.continuePermissionRequest()
                }
            })
            .check()
    }

    private fun maintainScrollPosition(callback: () -> Unit) {
        scrollView.isScrollable = false
        callback.invoke()
        lifecycleScope.launch {
            delay(200)
            scrollView.isScrollable = true
        }
    }
}
