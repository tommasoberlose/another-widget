package com.tommasoberlose.anotherwidget.ui.fragments

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import androidx.appcompat.app.AlertDialog
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
import com.tommasoberlose.anotherwidget.components.CalendarSelector
import com.tommasoberlose.anotherwidget.databinding.FragmentCalendarSettingsBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.ui.activities.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.CalendarUtil
import com.tommasoberlose.anotherwidget.utils.Util
import com.tommasoberlose.anotherwidget.utils.toast
import kotlinx.android.synthetic.main.fragment_calendar_settings.*
import kotlinx.android.synthetic.main.fragment_calendar_settings.scrollView
import kotlinx.android.synthetic.main.fragment_weather_settings.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarSettingsFragment : Fragment() {

    companion object {
        fun newInstance() = CalendarSettingsFragment()
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

        setupListener()
    }

    private fun subscribeUi(
        binding: FragmentCalendarSettingsBinding,
        viewModel: MainViewModel
    ) {
        viewModel.showEvents.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                binding.isCalendarEnabled = it
            }
            checkReadEventsPermission()
        })

        viewModel.calendarAllDay.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                all_day_label.text =
                    if (it) getString(R.string.settings_all_day_subtitle_visible) else getString(R.string.settings_all_day_subtitle_gone)
            }
            checkReadEventsPermission()
        })

        viewModel.showDeclinedEvents.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_declined_events_label.text = if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
            checkReadEventsPermission()
        })

        viewModel.secondRowInformation.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                second_row_info_label.text = getString(Util.getSecondRowInfoString(it))
            }
        })

        viewModel.showDiffTime.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_diff_time_label.text = if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        })

        viewModel.showUntil.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_until_label.text = getString(Util.getShowUntilString(it))
            }
            checkReadEventsPermission()
        })

        viewModel.showNextEvent.observe(viewLifecycleOwner, Observer {
            show_multiple_events_label.text = if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
        })

        viewModel.dateFormat.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                val now = Calendar.getInstance()
                var dateStringValue: String = String.format("%s%s", SimpleDateFormat(Constants.engDateFormat, Locale.getDefault()).format(now.time)[0].toUpperCase(), SimpleDateFormat(Constants.engDateFormat, Locale.getDefault()).format(now.time).substring(1))
                if (it) {
                    dateStringValue = String.format("%s%s", SimpleDateFormat(Constants.itDateFormat, Locale.getDefault()).format(now.time)[0].toUpperCase(), SimpleDateFormat(Constants.itDateFormat, Locale.getDefault()).format(now.time).substring(1))
                }
                date_format_label.text = dateStringValue
            }
        })

        viewModel.calendarAppName.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                calendar_app_label.text = if (it != "") it else getString(R.string.default_calendar_app)
            }
        })

        viewModel.eventAppName.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                event_app_label.text = if (it != "") it else getString(R.string.default_calendar_app)
            }
        })

    }

    private fun setupListener() {

        action_show_events.setOnClickListener {
            Preferences.showEvents = !Preferences.showEvents
        }

        action_filter_calendar.setOnClickListener {
            val calendarSelectorList: List<CalendarSelector> = CalendarUtil.getCalendarList(requireContext()).map { CalendarSelector(it.id.toInt(), it.displayName, it.accountName) }
            var calFiltered = Preferences.calendarFilter

            if (calendarSelectorList.isNotEmpty()) {
                val calNames = calendarSelectorList.map { if (it.name == it.account_name) String.format("%s: %s", getString(R.string.main_calendar), it.name) else it.name }.toTypedArray()
                val calSelected = calendarSelectorList.map { !calFiltered.contains(" " + it.id.toString() + ",") }.toBooleanArray()

                AlertDialog.Builder(requireContext()).setTitle(getString(R.string.settings_filter_calendar_subtitle))
                    .setMultiChoiceItems(calNames, calSelected) { _, item, isChecked ->
                        val dialogItem: String = String.format(" %s%s", calendarSelectorList.get(item).id, ",")
                        calFiltered = calFiltered.replace(dialogItem, "");
                        if (!isChecked) {
                            calFiltered += dialogItem
                        }
                    }
                    .setPositiveButton(android.R.string.ok) { _: DialogInterface, _: Int ->
                        Preferences.calendarFilter = calFiltered
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } else {
                requireActivity().toast(getString(R.string.calendar_settings_list_error))
            }
        }

        action_show_all_day.setOnClickListener {
            if (Preferences.showEvents) {
                BottomSheetMenu<Boolean>(requireContext()).selectResource(Preferences.calendarAllDay)
                    .addItem(getString(R.string.settings_all_day_subtitle_visible), true)
                    .addItem(getString(R.string.settings_all_day_subtitle_gone), false)
                    .addOnSelectItemListener { value ->
                        Preferences.calendarAllDay = value
                    }.show()
            }
        }

        action_show_declined_events.setOnClickListener {
            if (Preferences.showEvents) {
                BottomSheetMenu<Boolean>(requireContext()).selectResource(Preferences.showDeclinedEvents)
                    .addItem(getString(R.string.settings_visible), true)
                    .addItem(getString(R.string.settings_not_visible), false)
                    .addOnSelectItemListener { value ->
                        Preferences.showDeclinedEvents = value
                    }.show()
            }
        }

        action_show_multiple_events.setOnClickListener {
            if (Preferences.showEvents) {
                BottomSheetMenu<Boolean>(requireContext()).selectResource(Preferences.showNextEvent)
                    .addItem(getString(R.string.settings_visible), true)
                    .addItem(getString(R.string.settings_not_visible), false)
                    .addOnSelectItemListener { value ->
                        Preferences.showNextEvent = value
                    }.show()
            }
        }

        action_show_diff_time.setOnClickListener {
            if (Preferences.showEvents) {
                BottomSheetMenu<Boolean>(requireContext()).selectResource(Preferences.showDiffTime)
                    .addItem(getString(R.string.settings_visible), true)
                    .addItem(getString(R.string.settings_not_visible), false)
                    .addOnSelectItemListener { value ->
                        Preferences.showDiffTime = value
                    }.show()
            }
        }

        action_second_row_info.setOnClickListener {
            if (Preferences.showEvents) {
                val dialog = BottomSheetMenu<Int>(requireContext()).selectResource(Preferences.secondRowInformation)
                (0 .. 1).forEach {
                    dialog.addItem(getString(Util.getSecondRowInfoString(it)), it)
                }
                dialog.addOnSelectItemListener { value ->
                        Preferences.secondRowInformation = value
                    }.show()
            }
        }

        action_show_until.setOnClickListener {
            if (Preferences.showEvents) {
                val dialog = BottomSheetMenu<Int>(requireContext()).selectResource(Preferences.showUntil)
                intArrayOf(6,7,0,1,2,3,4,5).forEach {
                    dialog.addItem(getString(Util.getShowUntilString(it)), it)
                }
                dialog.addOnSelectItemListener { value ->
                        Preferences.showUntil = value
                    }.show()
            }
        }

        action_event_app.setOnClickListener {
            startActivityForResult(Intent(requireContext(), ChooseApplicationActivity::class.java), RequestCode.EVENT_APP_REQUEST_CODE.code)
        }

        action_calendar_app.setOnClickListener {
            startActivityForResult(Intent(requireContext(), ChooseApplicationActivity::class.java), RequestCode.CALENDAR_APP_REQUEST_CODE.code)
        }
    }

    private fun checkReadEventsPermission(showEvents: Boolean = Preferences.showEvents) {
        if (requireActivity().checkCallingOrSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            show_events_label.text = if (showEvents) getString(R.string.show_events_visible) else getString(R.string.show_events_not_visible)
            read_calendar_permission_alert_icon.isVisible = false
            CalendarUtil.updateEventList(requireContext())
        } else {
            show_events_label.text = if (showEvents) getString(R.string.description_permission_calendar) else getString(R.string.show_events_not_visible)
            read_calendar_permission_alert_icon.isVisible = showEvents
            read_calendar_permission_alert_icon.setOnClickListener {
                requirePermission()
            }
        }
    }

    private fun requirePermission() {
        Dexter.withContext(requireActivity())
            .withPermissions(
                Manifest.permission.READ_CALENDAR
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.areAllPermissionsGranted()){
                            checkReadEventsPermission()
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
        val scrollPosition = scrollView.scrollY
        callback.invoke()
        lifecycleScope.launch {
            delay(200)
            scrollView.smoothScrollTo(0, scrollPosition)
        }
    }
}
