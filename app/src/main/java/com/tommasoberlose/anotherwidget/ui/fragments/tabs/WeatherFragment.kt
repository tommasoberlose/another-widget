package com.tommasoberlose.anotherwidget.ui.fragments.tabs

import android.Manifest
import android.app.Activity
import android.content.Intent
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
import com.google.android.material.transition.MaterialSharedAxis
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.components.IconPackSelector
import com.tommasoberlose.anotherwidget.components.MaterialBottomSheetDialog
import com.tommasoberlose.anotherwidget.databinding.FragmentTabWeatherBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.receivers.WeatherReceiver
import com.tommasoberlose.anotherwidget.ui.activities.tabs.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.activities.tabs.CustomLocationActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.activities.tabs.WeatherProviderActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.collapse
import com.tommasoberlose.anotherwidget.utils.expand
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WeatherFragment : Fragment() {

    companion object {
        fun newInstance() = WeatherFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentTabWeatherBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        binding = FragmentTabWeatherBinding.inflate(inflater)

        subscribeUi(viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupListener()

        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {
            viewModel.fragmentScrollY.value = binding.scrollView.scrollY
        }
    }

    private fun subscribeUi(
        viewModel: MainViewModel
    ) {
        binding.isWeatherVisible = Preferences.showWeather

        viewModel.weatherProvider.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.labelWeatherProvider.text = WeatherHelper.getProviderName(requireContext(), Constants.WeatherProvider.fromInt(it)!!)
            }
        }

        viewModel.weatherProviderError.observe(viewLifecycleOwner) {
            checkWeatherProviderConfig()
            checkLocationPermission()
        }

        viewModel.weatherProviderLocationError.observe(viewLifecycleOwner) {
            checkWeatherProviderConfig()
            checkLocationPermission()
        }

        viewModel.customLocationAdd.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.labelCustomLocation.text =
                    if (it == "") getString(R.string.custom_location_gps) else it
            }
            checkWeatherProviderConfig()
            checkLocationPermission()
        }

        viewModel.weatherTempUnit.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.tempUnit.text =
                    if (it == "F") getString(R.string.fahrenheit) else getString(R.string.celsius)
            }
        }

        viewModel.weatherRefreshPeriod.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.labelWeatherRefreshPeriod.text = getString(SettingsStringHelper.getRefreshPeriodString(it))
            }
        }

        viewModel.weatherIconPack.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.labelWeatherIconPack.text = getString(R.string.settings_weather_icon_pack_default).format((it + 1))
            }
        }
    }

    private fun checkLocationPermission() {
        if (requireActivity().checkGrantedPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            binding.locationPermissionAlert.isVisible = false
        } else if (Preferences.customLocationAdd == "") {
            binding.locationPermissionAlert.isVisible = true
            binding.locationPermissionAlert.setOnClickListener {
                requirePermission()
            }
            binding.weatherProviderLocationError.isVisible = false
        } else {
            binding.locationPermissionAlert.isVisible = false
        }
    }

    private fun checkWeatherProviderConfig() {
        binding.weatherProviderError.isVisible = Preferences.weatherProviderError != "" && Preferences.weatherProviderError != "-"
        binding.weatherProviderError.text = Preferences.weatherProviderError

        binding.weatherProviderLocationError.isVisible = Preferences.weatherProviderLocationError != ""
        binding.weatherProviderLocationError.text = Preferences.weatherProviderLocationError
    }

    private fun setupListener() {
        binding.actionWeatherProvider.setOnClickListener {
            startActivityForResult(
                Intent(requireContext(), WeatherProviderActivity::class.java),
                RequestCode.WEATHER_PROVIDER_REQUEST_CODE.code
            )
        }

        binding.actionCustomLocation.setOnClickListener {
            startActivityForResult(
                Intent(requireContext(), CustomLocationActivity::class.java),
                Constants.RESULT_CODE_CUSTOM_LOCATION
            )
        }

        binding.actionChangeUnit.setOnClickListener {
            BottomSheetMenu<String>(requireContext(), header = getString(R.string.settings_unit_title)).setSelectedValue(Preferences.weatherTempUnit)
                .addItem(getString(R.string.fahrenheit), "F")
                .addItem(getString(R.string.celsius), "C")
                .addOnSelectItemListener { value ->
                    if (value != Preferences.weatherTempUnit) {
                        Preferences.weatherTempUnit = value
                        WeatherHelper.updateWeather(requireContext())
                    }
                }.show()
        }

        binding.actionWeatherRefreshPeriod.setOnClickListener {
            val dialog =
                BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_weather_refresh_period_title)).setSelectedValue(Preferences.weatherRefreshPeriod)
            (5 downTo 0).forEach {
                dialog.addItem(getString(SettingsStringHelper.getRefreshPeriodString(it)), it)
            }
            dialog
                .addOnSelectItemListener { value ->
                    if (value != Preferences.weatherRefreshPeriod) {
                        Preferences.weatherRefreshPeriod = value
                        WeatherReceiver.setUpdates(requireContext())
                    }
                }.show()
        }

        binding.actionWeatherIconPack.setOnClickListener {
            IconPackSelector(requireContext(), header = getString(R.string.settings_weather_icon_pack_title)).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Constants.RESULT_CODE_CUSTOM_LOCATION -> {
                    WeatherHelper.updateWeather(requireContext())
                }
                //RequestCode.WEATHER_PROVIDER_REQUEST_CODE.code -> {
                //}
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun requirePermission() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object: MultiplePermissionsListener {
                private var shouldShowRationale = false
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.grantedPermissionResponses.isNotEmpty()) {
                            checkLocationPermission()
                            WeatherHelper.updateWeather(requireContext())
                        }
                        else if (!shouldShowRationale && report.isAnyPermissionPermanentlyDenied) {
                            MaterialBottomSheetDialog(
                                requireContext(),
                                getString(R.string.title_permission_location),
                                getString(R.string.description_permission_location)
                            ).setNegativeButton(getString(R.string.action_ignore))
                            .setPositiveButton(getString(R.string.action_grant_permission)) {
                                startActivity(
                                    Intent(
                                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    ).apply {
                                        data = android.net.Uri.fromParts(
                                            "package",
                                            requireContext().packageName,
                                            null
                                        )
                                    }
                                )
                            }.show()
                        }
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    shouldShowRationale = true
                    // Remember to invoke this method when the custom rationale is closed
                    // or just by default if you don't want to use any custom rationale.
                    token?.continuePermissionRequest()
                }
            })
            .check()
    }

    override fun onResume() {
        super.onResume()
        checkWeatherProviderConfig()
        checkLocationPermission()
    }

    private fun maintainScrollPosition(callback: () -> Unit) {
        binding.scrollView.isScrollable = false
        callback.invoke()
        lifecycleScope.launch {
            delay(200)
            binding.scrollView.isScrollable = true
        }
    }
}
