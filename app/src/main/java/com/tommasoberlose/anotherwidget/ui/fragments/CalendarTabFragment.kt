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
import com.chibatching.kotpref.bulk
import com.google.android.material.transition.MaterialSharedAxis
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
import com.tommasoberlose.anotherwidget.helpers.IntentHelper
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.isDefaultSet
import com.tommasoberlose.anotherwidget.utils.toast
import kotlinx.android.synthetic.main.fragment_calendar_settings.*
import kotlinx.android.synthetic.main.fragment_calendar_settings.scrollView
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_tab_selector.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.Comparator

class CalendarTabFragment : Fragment() {

    companion object {
        fun newInstance() = CalendarTabFragment()
    }

    private lateinit var viewModel: MainViewModel

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
        val binding = DataBindingUtil.inflate<FragmentCalendarSettingsBinding>(inflater, R.layout.fragment_calendar_settings, container, false)

        subscribeUi(binding, viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        show_all_day_toggle.isChecked = Preferences.calendarAllDay
        show_only_busy_events_toggle.isChecked = Preferences.showOnlyBusyEvents
        show_diff_time_toggle.isChecked = Preferences.showDiffTime
        show_multiple_events_toggle.isChecked = Preferences.showNextEvent

        setupListener()

        scrollView?.viewTreeObserver?.addOnScrollChangedListener {
            viewModel.fragmentScrollY.value = scrollView?.scrollY ?: 0
        }
    }

    private fun subscribeUi(
        binding: FragmentCalendarSettingsBinding,
        viewModel: MainViewModel
    ) {
        binding.isCalendarEnabled = Preferences.showEvents
        binding.isDiffEnabled = Preferences.showDiffTime || !Preferences.showEvents

        viewModel.calendarAllDay.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                all_day_label?.text =
                    if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
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
                calendar_app_label?.text = when {
                    Preferences.calendarAppName != "" -> Preferences.calendarAppName
                    else -> {
                        if (IntentHelper.getCalendarIntent(requireContext()).isDefaultSet(requireContext())) {
                            getString(
                                R.string.default_calendar_app
                            )
                        } else {
                            getString(R.string.nothing)
                        }
                    }
                }
            }
        })

        viewModel.openEventDetails.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                open_event_details_label?.text = if (it) getString(R.string.default_event_app) else getString(R.string.default_calendar_app)
            }
        })

    }

    private fun setupListener() {

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
            show_all_day_toggle.isChecked = !show_all_day_toggle.isChecked
        }

        show_all_day_toggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.calendarAllDay = isChecked
            updateCalendar()
        }

        action_change_attendee_filter.setOnClickListener {
            val selectedValues = emptyList<Int>().toMutableList()
            if (Preferences.showDeclinedEvents) {
                selectedValues.add(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED)
            }
            if (Preferences.showInvitedEvents) {
                selectedValues.add(CalendarContract.Attendees.ATTENDEE_STATUS_INVITED)
            }
            if (Preferences.showAcceptedEvents) {
                selectedValues.add(CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED)
            }

            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_attendee_status_title), isMultiSelection = true)
                .setSelectedValues(selectedValues)

            dialog.addItem(
                getString(R.string.attendee_status_invited),
                CalendarContract.Attendees.ATTENDEE_STATUS_INVITED
            )
            dialog.addItem(
                getString(R.string.attendee_status_accepted),
                CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED
            )
            dialog.addItem(
                getString(R.string.attendee_status_declined),
                CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED
            )

            dialog.addOnMultipleSelectItemListener { values ->
                Preferences.showDeclinedEvents = values.contains(CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED)
                Preferences.showAcceptedEvents = values.contains(CalendarContract.Attendees.ATTENDEE_STATUS_ACCEPTED)
                Preferences.showInvitedEvents = values.contains(CalendarContract.Attendees.ATTENDEE_STATUS_INVITED)
                updateCalendar()
            }.show()
        }

        action_show_only_busy_events.setOnClickListener {
            show_only_busy_events_toggle.isChecked = !show_only_busy_events_toggle.isChecked
        }

        show_only_busy_events_toggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.showOnlyBusyEvents = isChecked
            updateCalendar()
        }

        action_show_multiple_events.setOnClickListener {
            show_multiple_events_toggle.isChecked = !show_multiple_events_toggle.isChecked
        }

        show_multiple_events_toggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.showNextEvent = isChecked
        }

        action_show_diff_time.setOnClickListener {
            show_diff_time_toggle.isChecked = !show_diff_time_toggle.isChecked
        }

        show_diff_time_toggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.showDiffTime = isChecked
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
            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_second_row_info_title)).setSelectedValue(Preferences.secondRowInformation)
            (0 .. 1).forEach {
                dialog.addItem(getString(SettingsStringHelper.getSecondRowInfoString(it)), it)
            }
            dialog.addOnSelectItemListener { value ->
                Preferences.secondRowInformation = value
            }.show()
        }

        action_show_until.setOnClickListener {
            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_show_until_title)).setSelectedValue(Preferences.showUntil)
            intArrayOf(6,7,0,1,2,3, 4, 5).forEach {
                dialog.addItem(getString(SettingsStringHelper.getShowUntilString(it)), it)
            }
            dialog.addOnSelectItemListener { value ->
                Preferences.showUntil = value
            }.show()
        }

        action_open_event_details.setOnClickListener {
            BottomSheetMenu<Boolean>(requireContext(), header = getString(R.string.settings_event_app_title)).setSelectedValue(Preferences.openEventDetails)
                .addItem(getString(R.string.default_event_app), true)
                .addItem(getString(R.string.default_calendar_app), false)
                .addOnSelectItemListener { value ->
                    Preferences.openEventDetails = value
                }
                .show()
        }

        action_calendar_app.setOnClickListener {
            startActivityForResult(Intent(requireContext(), ChooseApplicationActivity::class.java), RequestCode.CALENDAR_APP_REQUEST_CODE.code)
        }
    }

    private fun updateCalendar() {
        if (activity?.checkGrantedPermission(Manifest.permission.READ_CALENDAR) == true) {
            CalendarHelper.updateEventList(requireContext())
        }
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
