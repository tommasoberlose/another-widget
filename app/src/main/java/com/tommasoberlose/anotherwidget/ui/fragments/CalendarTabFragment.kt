package com.tommasoberlose.anotherwidget.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.tommasoberlose.anotherwidget.models.CalendarSelector
import com.tommasoberlose.anotherwidget.databinding.FragmentCalendarSettingsBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.ui.activities.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.helpers.DateHelper
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.ui.activities.CustomDateActivity
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.toast
import kotlinx.android.synthetic.main.fragment_calendar_settings.*
import kotlinx.android.synthetic.main.fragment_calendar_settings.scrollView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.Comparator

class CalendarTabFragment : Fragment() {

    companion object {
        fun newInstance() = CalendarTabFragment()
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
        val binding = DataBindingUtil.inflate<FragmentCalendarSettingsBinding>(inflater, R.layout.fragment_calendar_settings, container, false)

        subscribeUi(binding, viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        show_all_day_toggle.isChecked = Preferences.calendarAllDay
        show_declined_events_toggle.isChecked = Preferences.showDeclinedEvents
        show_diff_time_toggle.isChecked = Preferences.showDiffTime
        show_multiple_events_toggle.isChecked = Preferences.showNextEvent

        setupListener()
    }

    private fun subscribeUi(
        binding: FragmentCalendarSettingsBinding,
        viewModel: MainViewModel
    ) {
        binding.isCalendarEnabled = Preferences.showEvents
        binding.isDiffEnabled = Preferences.showDiffTime || !Preferences.showEvents

        viewModel.showEvents.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                binding.isCalendarEnabled = it

                if (it) {
                    CalendarHelper.setEventUpdatesAndroidN(requireContext())
                } else {
                    CalendarHelper.removeEventUpdatesAndroidN(requireContext())
                }
                binding.isDiffEnabled = Preferences.showDiffTime || !it
            }
            checkReadEventsPermission()
            updateCalendar()
        })

        viewModel.calendarAllDay.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                all_day_label?.text =
                    if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
            updateCalendar()
        })

        viewModel.showDeclinedEvents.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_declined_events_label?.text = if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
            updateCalendar()
        })

        viewModel.secondRowInformation.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                second_row_info_label?.text = getString(SettingsStringHelper.getSecondRowInfoString(it))
            }
        })

        viewModel.showDiffTime.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_diff_time_label?.text = if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
                binding.isDiffEnabled = it || !Preferences.showEvents
            }
        })

        viewModel.widgetUpdateFrequency.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                widget_update_frequency_label?.text = when (it) {
                    Constants.WidgetUpdateFrequency.HIGH.value -> getString(R.string.settings_widget_update_frequency_high)
                    Constants.WidgetUpdateFrequency.DEFAULT.value -> getString(R.string.settings_widget_update_frequency_default)
                    Constants.WidgetUpdateFrequency.LOW.value -> getString(R.string.settings_widget_update_frequency_low)
                    else -> ""
                }
            }
        })

        viewModel.showUntil.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_until_label?.text = getString(SettingsStringHelper.getShowUntilString(it))
            }
            updateCalendar()
        })

        viewModel.showNextEvent.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_multiple_events_label?.text =
                    if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        })

        viewModel.calendarAppName.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                calendar_app_label?.text = if (it != "") it else getString(R.string.default_calendar_app)
            }
        })

        viewModel.openEventDetails.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                open_event_details_label?.text = if (it) getString(R.string.default_event_app) else getString(R.string.default_calendar_app)
            }
        })

    }

    private fun setupListener() {

        action_show_events.setOnClickListener {
            Preferences.showEvents = !Preferences.showEvents
            if (Preferences.showEvents) {
                requirePermission()
            }
        }

        show_events_switch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showEvents = enabled
            if (Preferences.showEvents) {
                requirePermission()
            }
        }

        action_filter_calendar.setOnClickListener {
            val calendarSelectorList: List<CalendarSelector> = CalendarHelper.getCalendarList(requireContext()).map {
                CalendarSelector(
                    it.id,
                    it.displayName,
                    it.accountName
                )
            }.sortedWith(Comparator { cal1, cal2 ->
                when {
                    cal1.accountName != cal2.accountName -> {
                        cal1.accountName.compareTo(cal2.accountName)
                    }
                    cal1.accountName == cal1.name -> {
                        -1
                    }
                    cal2.accountName == cal2.name -> {
                        1
                    }
                    else -> {
                        cal1.name.compareTo(cal2.name)
                    }
                }
            })

            if (calendarSelectorList.isNotEmpty()) {
                val filteredCalendarIds = CalendarHelper.getFilteredCalendarIdList()
                val visibleCalendarIds = calendarSelectorList.map { it.id }.filter { id: Long -> !filteredCalendarIds.contains(id) }

                val dialog = BottomSheetMenu<Long>(requireContext(), header = getString(R.string.settings_filter_calendar_subtitle), isMultiSelection = true)
                    .setSelectedValues(visibleCalendarIds)

                calendarSelectorList.indices.forEach { index ->
                    if (index == 0 || calendarSelectorList[index].accountName != calendarSelectorList[index - 1].accountName) {
                        dialog.addItem(calendarSelectorList[index].accountName)
                    }
                    
                    dialog.addItem(
                        if (calendarSelectorList[index].name == calendarSelectorList[index].accountName) getString(R.string.main_calendar) else calendarSelectorList[index].name,
                        calendarSelectorList[index].id
                    )
                }

                dialog.addOnMultipleSelectItemListener { values ->
                    CalendarHelper.filterCalendar(calendarSelectorList.map { it.id }.filter { !values.contains(it) })
                    updateCalendar()
                }.show()
            } else {
                activity?.toast(getString(R.string.calendar_settings_list_error))
            }
        }

        action_show_all_day.setOnClickListener {
            if (Preferences.showEvents) {
                show_all_day_toggle.isChecked = !show_all_day_toggle.isChecked
            }
        }

        show_all_day_toggle.setOnCheckedChangeListener { _, isChecked ->
            if (Preferences.showEvents) {
                Preferences.calendarAllDay = isChecked
            }
        }

        action_show_declined_events.setOnClickListener {
            if (Preferences.showEvents) {
                show_declined_events_toggle.isChecked = !show_declined_events_toggle.isChecked
            }
        }

        show_declined_events_toggle.setOnCheckedChangeListener { _, isChecked ->
            if (Preferences.showEvents) {
                Preferences.showDeclinedEvents = isChecked
            }
        }

        action_show_multiple_events.setOnClickListener {
            if (Preferences.showEvents) {
                show_multiple_events_toggle.isChecked = !show_multiple_events_toggle.isChecked
            }
        }

        show_multiple_events_toggle.setOnCheckedChangeListener { _, isChecked ->
            if (Preferences.showEvents) {
                Preferences.showNextEvent = isChecked
            }
        }

        action_show_diff_time.setOnClickListener {
            if (Preferences.showEvents) {
                show_diff_time_toggle.isChecked = !show_diff_time_toggle.isChecked
            }
        }

        show_diff_time_toggle.setOnCheckedChangeListener { _, isChecked ->
            if (Preferences.showEvents) {
                Preferences.showDiffTime = isChecked
            }
        }

        action_widget_update_frequency.setOnClickListener {
            if (Preferences.showEvents && Preferences.showDiffTime) {
                BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_widget_update_frequency_title), message = getString(R.string.settings_widget_update_frequency_subtitle)).setSelectedValue(Preferences.widgetUpdateFrequency)
                    .addItem(getString(R.string.settings_widget_update_frequency_high), Constants.WidgetUpdateFrequency.HIGH.value)
                    .addItem(getString(R.string.settings_widget_update_frequency_default), Constants.WidgetUpdateFrequency.DEFAULT.value)
                    .addItem(getString(R.string.settings_widget_update_frequency_low), Constants.WidgetUpdateFrequency.LOW.value)
                    .addOnSelectItemListener { value ->
                        Preferences.widgetUpdateFrequency = value
                    }.show()
            }
        }

        action_second_row_info.setOnClickListener {
            if (Preferences.showEvents) {
                val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_second_row_info_title)).setSelectedValue(Preferences.secondRowInformation)
                (0 .. 1).forEach {
                    dialog.addItem(getString(SettingsStringHelper.getSecondRowInfoString(it)), it)
                }
                dialog.addOnSelectItemListener { value ->
                        Preferences.secondRowInformation = value
                    }.show()
            }
        }

        action_show_until.setOnClickListener {
            if (Preferences.showEvents) {
                val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_show_until_title)).setSelectedValue(Preferences.showUntil)
                intArrayOf(6,7,0,1,2,3, 4, 5).forEach {
                    dialog.addItem(getString(SettingsStringHelper.getShowUntilString(it)), it)
                }
                dialog.addOnSelectItemListener { value ->
                        Preferences.showUntil = value
                    }.show()
            }
        }

        action_open_event_details.setOnClickListener {
            if (Preferences.showEvents) {
                BottomSheetMenu<Boolean>(requireContext(), header = getString(R.string.settings_event_app_title)).setSelectedValue(Preferences.openEventDetails)
                    .addItem(getString(R.string.default_event_app), true)
                    .addItem(getString(R.string.default_calendar_app), false)
                    .addOnSelectItemListener { value ->
                        Preferences.openEventDetails = value
                    }.show()
            }
        }

        action_calendar_app.setOnClickListener {
            startActivityForResult(Intent(requireContext(), ChooseApplicationActivity::class.java), RequestCode.CALENDAR_APP_REQUEST_CODE.code)
        }
    }

    private fun checkReadEventsPermission(showEvents: Boolean = Preferences.showEvents) {
        if (activity?.checkGrantedPermission(Manifest.permission.READ_CALENDAR) == true) {
            show_events_label?.text = if (showEvents) getString(R.string.show_events_visible) else getString(R.string.show_events_not_visible)
            read_calendar_permission_alert?.isVisible = false
        } else {
            show_events_label?.text = if (showEvents) getString(R.string.description_permission_calendar) else getString(R.string.show_events_not_visible)
            read_calendar_permission_alert?.isVisible = showEvents
            read_calendar_permission_alert?.setOnClickListener {
                requirePermission()
            }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                RequestCode.CALENDAR_APP_REQUEST_CODE.code -> {
                    Preferences.bulk {
                        calendarAppName = data?.getStringExtra(Constants.RESULT_APP_NAME) ?: getString(R.string.default_calendar_app)
                        calendarAppPackage = data?.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: ""
                    }
                }
                RequestCode.EVENT_APP_REQUEST_CODE.code -> {
                    Preferences.bulk {
                        eventAppName = data?.getStringExtra(Constants.RESULT_APP_NAME) ?: getString(R.string.default_event_app)
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
