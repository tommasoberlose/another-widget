package com.tommasoberlose.anotherwidget.ui.fragments.tabs

import android.Manifest
import android.os.Bundle
import android.provider.CalendarContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.transition.MaterialSharedAxis
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.models.CalendarSelector
import com.tommasoberlose.anotherwidget.databinding.FragmentTabCalendarBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.helpers.CalendarHelper
import com.tommasoberlose.anotherwidget.helpers.SettingsStringHelper
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CalendarFragment : Fragment() {

    companion object {
        fun newInstance() = CalendarFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentTabCalendarBinding

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
        binding = FragmentTabCalendarBinding.inflate(inflater)

        subscribeUi(viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        binding.showAllDayToggle.setCheckedImmediatelyNoEvent(Preferences.calendarAllDay)
        binding.showOnlyBusyEventsToggle.setCheckedImmediatelyNoEvent(Preferences.showOnlyBusyEvents)
        binding.showDiffTimeToggle.setCheckedImmediatelyNoEvent(Preferences.showDiffTime)
        binding.showNextEventOnMultipleLinesToggle.setCheckedImmediatelyNoEvent(Preferences.showNextEventOnMultipleLines)

        setupListener()

        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {
            viewModel.fragmentScrollY.value = binding.scrollView.scrollY
        }
    }

    private fun subscribeUi(
        viewModel: MainViewModel
    ) {
        binding.isCalendarEnabled = Preferences.showEvents
        binding.isDiffEnabled = Preferences.showDiffTime

        viewModel.calendarAllDay.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.allDayLabel.text =
                    if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        }

        viewModel.secondRowInformation.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.secondRowInfoLabel.text = getString(SettingsStringHelper.getSecondRowInfoString(it))
            }
        }

        viewModel.showNextEventOnMultipleLines.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.showNextEventOnMultipleLinesLabel.text = if (it) getString(R.string.settings_enabled) else getString(R.string.settings_disabled)
            }
        }

        viewModel.showDiffTime.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.showDiffTimeLabel.text = if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
                binding.isDiffEnabled = it || !Preferences.showEvents
            }
        }

        viewModel.widgetUpdateFrequency.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.widgetUpdateFrequencyLabel.text = when (it) {
                    Constants.WidgetUpdateFrequency.HIGH.rawValue -> getString(R.string.settings_widget_update_frequency_high)
                    Constants.WidgetUpdateFrequency.DEFAULT.rawValue -> getString(R.string.settings_widget_update_frequency_default)
                    Constants.WidgetUpdateFrequency.LOW.rawValue -> getString(R.string.settings_widget_update_frequency_low)
                    else -> ""
                }
            }
        }

        viewModel.showUntil.observe(viewLifecycleOwner) {
            maintainScrollPosition {
                binding.showUntilLabel.text = getString(SettingsStringHelper.getShowUntilString(it))
            }
        }

    }

    private fun setupListener() {

        binding.actionFilterCalendar.setOnClickListener {
            val calendarSelectorList: List<CalendarSelector> = CalendarHelper.getCalendarList(requireContext()).map {
                CalendarSelector(
                    it.id,
                    it.displayName,
                    it.accountName
                )
            }.sortedWith { cal1, cal2 ->
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
            }

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
                requireActivity().toast(getString(R.string.calendar_settings_list_error))
            }
        }

        binding.actionShowAllDay.setOnClickListener {
            binding.showAllDayToggle.isChecked = !binding.showAllDayToggle.isChecked
        }

        binding.showAllDayToggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.calendarAllDay = isChecked
            MainWidget.updateWidget(requireContext())
        }

        binding.actionChangeAttendeeFilter.setOnClickListener {
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

        binding.actionShowOnlyBusyEvents.setOnClickListener {
            binding.showOnlyBusyEventsToggle.isChecked = !binding.showOnlyBusyEventsToggle.isChecked
        }

        binding.showOnlyBusyEventsToggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.showOnlyBusyEvents = isChecked
            MainWidget.updateWidget(requireContext())
        }

        binding.actionShowDiffTime.setOnClickListener {
            binding.showDiffTimeToggle.isChecked = !binding.showDiffTimeToggle.isChecked
        }

        binding.showDiffTimeToggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.showDiffTime = isChecked
        }

        binding.actionShowNextEventOnMultipleLines.setOnClickListener {
            binding.showNextEventOnMultipleLinesToggle.isChecked = !binding.showNextEventOnMultipleLinesToggle.isChecked
        }

        binding.showNextEventOnMultipleLinesToggle.setOnCheckedChangeListener { _, isChecked ->
            Preferences.showNextEventOnMultipleLines = isChecked
        }

        binding.actionWidgetUpdateFrequency.setOnClickListener {
            if (Preferences.showEvents && Preferences.showDiffTime) {
                BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_widget_update_frequency_title), message = getString(R.string.settings_widget_update_frequency_subtitle)).setSelectedValue(Preferences.widgetUpdateFrequency)
                    .addItem(getString(R.string.settings_widget_update_frequency_high), Constants.WidgetUpdateFrequency.HIGH.rawValue)
                    .addItem(getString(R.string.settings_widget_update_frequency_default), Constants.WidgetUpdateFrequency.DEFAULT.rawValue)
                    .addItem(getString(R.string.settings_widget_update_frequency_low), Constants.WidgetUpdateFrequency.LOW.rawValue)
                    .addOnSelectItemListener { value ->
                        Preferences.widgetUpdateFrequency = value
                    }.show()
            }
        }

        binding.actionSecondRowInfo.setOnClickListener {
            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_second_row_info_title)).setSelectedValue(Preferences.secondRowInformation)
            (0 .. 1).forEach {
                dialog.addItem(getString(SettingsStringHelper.getSecondRowInfoString(it)), it)
            }
            dialog.addOnSelectItemListener { value ->
                Preferences.secondRowInformation = value
            }.show()
        }

        binding.actionShowUntil.setOnClickListener {
            val dialog = BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_show_until_title)).setSelectedValue(Preferences.showUntil)
            intArrayOf(6,7,0,1,2,3, 4, 5).forEach {
                dialog.addItem(getString(SettingsStringHelper.getShowUntilString(it)), it)
            }
            dialog.addOnSelectItemListener { value ->
                Preferences.showUntil = value
                updateCalendar()
            }.show()
        }
    }

    private fun updateCalendar() {
        if (requireActivity().checkGrantedPermission(Manifest.permission.READ_CALENDAR)) {
            CalendarHelper.updateEventList(requireContext())
        }
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
