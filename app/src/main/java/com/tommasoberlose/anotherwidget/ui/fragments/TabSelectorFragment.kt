package com.tommasoberlose.anotherwidget.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.findNavController
import com.chibatching.kotpref.bulk
import com.google.android.material.transition.MaterialSharedAxis
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.components.MaterialBottomSheetDialog
import com.tommasoberlose.anotherwidget.databinding.FragmentTabSelectorBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.helpers.IntentHelper
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.models.CalendarSelector
import com.tommasoberlose.anotherwidget.receivers.WeatherReceiver
import com.tommasoberlose.anotherwidget.ui.activities.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.*
import kotlinx.android.synthetic.main.fragment_calendar_settings.*
import kotlinx.android.synthetic.main.fragment_tab_selector.*
import kotlinx.android.synthetic.main.fragment_tab_selector.scrollView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TabSelectorFragment : Fragment() {

    companion object {
        fun newInstance() = TabSelectorFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        viewModel = ViewModelProvider(activity as MainActivity).get(MainViewModel::class.java)
        val binding = DataBindingUtil.inflate<FragmentTabSelectorBinding>(inflater, R.layout.fragment_tab_selector, container, false)

        subscribeUi(binding, viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupListener()

        scrollView?.viewTreeObserver?.addOnScrollChangedListener {
            viewModel.fragmentScrollY.value = scrollView?.scrollY ?: 0
        }
    }

    private fun subscribeUi(
        binding: FragmentTabSelectorBinding,
        viewModel: MainViewModel
    ) {
        binding.isCalendarEnabled = Preferences.showEvents
        binding.isWeatherVisible = Preferences.showWeather
        binding.isClockVisible = Preferences.showClock
        binding.isGlanceVisible = Preferences.showGlance

        viewModel.showEvents.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                binding.isCalendarEnabled = it

                if (it) {
                    CalendarHelper.setEventUpdatesAndroidN(requireContext())
                } else {
                    CalendarHelper.removeEventUpdatesAndroidN(requireContext())
                }
            }
            checkReadEventsPermission()
            updateCalendar()
        })

        viewModel.showWeather.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                binding.isWeatherVisible = it
            }
            checkLocationPermission()
        })

        viewModel.showClock.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                binding.isClockVisible = it
            }
        })

        viewModel.showGlance.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                binding.isGlanceVisible = it
            }
        })

    }

    private fun setupListener() {

        action_typography.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_typographyTabFragment)
        }

        action_general_settings.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_generalTabFragment)
        }

        action_show_events.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_calendarTabFragment)
        }

        show_events_switch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showEvents = enabled
            if (Preferences.showEvents) {
                requirePermission()
            }
        }

        action_show_weather.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_weatherTabFragment)
        }

        show_weather_switch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showWeather = enabled
        }

        action_show_clock.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_clockTabFragment)
        }

        show_clock_switch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showClock = enabled
        }
        action_show_glance.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_glanceTabFragment)
        }

        show_glance_switch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showGlance = enabled
        }
    }

    private fun checkReadEventsPermission(showEvents: Boolean = Preferences.showEvents) {
        if (activity?.checkGrantedPermission(Manifest.permission.READ_CALENDAR) == true) {
        } else {
        }
    }

    private fun updateCalendar() {
        if (activity?.checkGrantedPermission(Manifest.permission.READ_CALENDAR) == true) {
            CalendarHelper.updateEventList(requireContext())
        }
    }

    private fun requirePermission() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.READ_CALENDAR
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.areAllPermissionsGranted()){
                            checkReadEventsPermission()
                        } else {
                            Preferences.showEvents = false
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

    private fun checkLocationPermission() {
        if (requireActivity().checkGrantedPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            WeatherReceiver.setUpdates(requireContext())
        } else if (Preferences.showWeather && Preferences.customLocationAdd == "") {
            MaterialBottomSheetDialog(requireContext(), message = getString(R.string.background_location_warning))
                .setPositiveButton(getString(android.R.string.ok)) {
                    requirePermission()
                }
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                RequestCode.CALENDAR_APP_REQUEST_CODE.code -> {
                    Preferences.bulk {
                        calendarAppName = data?.getStringExtra(Constants.RESULT_APP_NAME) ?: getString(
                            R.string.default_calendar_app)
                        calendarAppPackage = data?.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: ""
                    }
                }
                RequestCode.EVENT_APP_REQUEST_CODE.code -> {
                    Preferences.bulk {
                        eventAppName = data?.getStringExtra(Constants.RESULT_APP_NAME) ?: getString(
                            R.string.default_event_app)
                        eventAppPackage = data?.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: ""
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
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
