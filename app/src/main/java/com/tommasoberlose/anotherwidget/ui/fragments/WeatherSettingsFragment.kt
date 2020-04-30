package com.tommasoberlose.anotherwidget.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.tommasoberlose.anotherwidget.databinding.FragmentWeatherSettingsBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.receivers.WeatherReceiver
import com.tommasoberlose.anotherwidget.ui.activities.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.activities.CustomLocationActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.activities.WeatherProviderActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.Util
import kotlinx.android.synthetic.main.fragment_weather_settings.*
import kotlinx.android.synthetic.main.fragment_weather_settings.scrollView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WeatherSettingsFragment : Fragment() {

    companion object {
        fun newInstance() = WeatherSettingsFragment()
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
        viewModel.showWeather.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_weather_label.text =
                    if (it) getString(R.string.show_weather_visible) else getString(R.string.show_weather_not_visible)
                binding.isWeatherVisible = it
            }
            checkLocationPermission()
        })

        viewModel.weatherProviderApi.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                label_weather_provider_api_key.text =
                    if (it == "") getString(R.string.settings_weather_provider_api_key_subtitle_not_set) else getString(
                        R.string.settings_weather_provider_api_key_subtitle_all_set
                    )
                api_key_alert_icon.isVisible = it == ""
            }
            checkLocationPermission()
        })

        viewModel.customLocationAdd.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                label_custom_location.text =
                    if (it == "") getString(R.string.custom_location_gps) else it
            }
            checkLocationPermission()
        })

        viewModel.weatherTempUnit.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                temp_unit.text =
                    if (it == "F") getString(R.string.fahrenheit) else getString(R.string.celsius)
            }
        })

        viewModel.weatherRefreshPeriod.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                label_weather_refresh_period.text = getString(Util.getRefreshPeriodString(it))
            }
            checkLocationPermission()
        })

        viewModel.weatherAppName.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                weather_app_label.text =
                    if (it != "") it else getString(R.string.default_weather_app)
            }
        })
    }

    private fun checkLocationPermission() {
        if (requireActivity().checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            location_permission_alert_icon.isVisible = false
            WeatherReceiver.setUpdates(requireContext())
        } else if (Preferences.showWeather && Preferences.customLocationAdd == "") {
            location_permission_alert_icon.isVisible = true
            location_permission_alert_icon.setOnClickListener {
                requirePermission()
            }
        }
    }

    private fun setupListener() {

        action_show_weather.setOnClickListener {
            Preferences.showWeather = !Preferences.showWeather
        }

        action_weather_provider_api_key.setOnClickListener {
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
                BottomSheetMenu<String>(requireContext()).selectResource(Preferences.weatherTempUnit)
                    .addItem(getString(R.string.fahrenheit), "F")
                    .addItem(getString(R.string.celsius), "C")
                    .addOnSelectItemListener { value ->
                        Preferences.weatherTempUnit = value
                    }.show()
            }
        }

        action_weather_refresh_period.setOnClickListener {
            if (Preferences.showWeather) {
                val dialog =
                    BottomSheetMenu<Int>(requireContext()).selectResource(Preferences.weatherRefreshPeriod)
                (5 downTo 0).forEach {
                    dialog.addItem(getString(Util.getRefreshPeriodString(it)), it)
                }
                dialog
                    .addOnSelectItemListener { value ->
                        Preferences.weatherRefreshPeriod = value
                    }.show()
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
                }
                RequestCode.WEATHER_APP_REQUEST_CODE.code -> {
                    Preferences.bulk {
                        weatherAppName = data?.getStringExtra(Constants.RESULT_APP_NAME) ?: getString(R.string.default_weather_app)
                        weatherAppPackage = data?.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: ""
                    }
                    Util.updateWidget(requireContext())
                }
                RequestCode.WEATHER_PROVIDER_REQUEST_CODE.code -> {
                    WeatherReceiver.setOneTimeUpdate(requireContext())
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun requirePermission() {
        Dexter.withContext(requireActivity())
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION
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
        val scrollPosition = scrollView.scrollY
        callback.invoke()
        lifecycleScope.launch {
            delay(200)
            scrollView.smoothScrollTo(0, scrollPosition)
        }
    }
}
