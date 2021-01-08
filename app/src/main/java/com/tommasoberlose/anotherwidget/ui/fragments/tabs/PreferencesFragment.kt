package com.tommasoberlose.anotherwidget.ui.fragments.tabs

import android.Manifest
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
import androidx.navigation.Navigation
import com.google.android.material.transition.MaterialSharedAxis
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.MaterialBottomSheetDialog
import com.tommasoberlose.anotherwidget.databinding.FragmentPreferencesBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.receivers.WeatherReceiver
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PreferencesFragment : Fragment() {

    companion object {
        fun newInstance() = PreferencesFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentPreferencesBinding

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
        binding = FragmentPreferencesBinding.inflate(inflater)

        subscribeUi(viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupListener()

        binding.showEventsSwitch.setCheckedImmediatelyNoEvent(Preferences.showEvents)
        binding.showWeatherSwitch.setCheckedImmediatelyNoEvent(Preferences.showWeather)
        binding.showClockSwitch.setCheckedImmediatelyNoEvent(Preferences.showClock)

        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {
            viewModel.fragmentScrollY.value = binding.scrollView.scrollY
        }
    }

    private fun subscribeUi(
        viewModel: MainViewModel
    ) {

        viewModel.showEvents.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.showEventsSwitch.setCheckedImmediatelyNoEvent(it)
                if (it) {
                    CalendarHelper.setEventUpdatesAndroidN(requireContext())
                } else {
                    CalendarHelper.removeEventUpdatesAndroidN(requireContext())
                }
            }
        }

        viewModel.showWeather.observe(viewLifecycleOwner) {
            checkWeatherProviderConfig()
        }

        viewModel.showClock.observe(viewLifecycleOwner) {
        }
    }

    private fun setupListener() {

        binding.actionTypography.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_typographyTabFragment)
        }

        binding.actionGeneralSettings.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_generalTabFragment)
        }

        binding.actionShowEvents.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_calendarTabFragment)
        }

        binding.showEventsSwitch.setOnCheckedChangeListener { _, enabled: Boolean ->
            if (enabled) {
                if (!requireActivity().checkGrantedPermission(Manifest.permission.READ_CALENDAR)) {
                    binding.showEventsSwitch.setCheckedImmediatelyNoEvent(false)
                }
                requireCalendarPermission()
            } else {
                Preferences.showEvents = enabled
            }
        }

        binding.actionShowWeather.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_weatherTabFragment)
        }

        binding.showWeatherSwitch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showWeather = enabled
            if (enabled) {
                WeatherReceiver.setUpdates(requireContext())
            } else {
                WeatherReceiver.removeUpdates(requireContext())
            }
        }

        binding.actionShowClock.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_clockTabFragment)
        }

        binding.showClockSwitch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showClock = enabled
        }

        binding.actionShowGlance.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_glanceTabFragment)
        }

        binding.actionTabDefaultApp.setOnClickListener {
            Navigation.findNavController(it).navigate(R.id.action_tabSelectorFragment_to_gesturesFragment)
        }
    }

    private fun requireCalendarPermission() {
        Dexter.withContext(requireContext())
            .withPermissions(
                Manifest.permission.READ_CALENDAR
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        val granted = report.areAllPermissionsGranted()
                        Preferences.showEvents = granted
                        if (granted) {
                            CalendarHelper.updateEventList(requireContext())
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

    private fun checkWeatherProviderConfig() {
        if (Preferences.showWeather && Preferences.weatherProviderError != "" && Preferences.weatherProviderError != "-" && !binding.weatherProviderError.isVisible) {
            binding.weatherProviderError.expand()
        } else {
            binding.weatherProviderError.collapse()
        }
        binding.weatherProviderError.text = Preferences.weatherProviderError

        if (Preferences.showWeather && Preferences.weatherProviderLocationError != "" && !binding.weatherProviderError.isVisible) {
            binding.weatherProviderLocationError.expand()
        } else {
            binding.weatherProviderLocationError.collapse()
        }
        binding.weatherProviderLocationError.text = Preferences.weatherProviderLocationError
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
