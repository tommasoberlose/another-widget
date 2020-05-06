package com.tommasoberlose.anotherwidget.ui.fragments

import android.app.Activity
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
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
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentClockSettingsBinding
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.global.RequestCode
import com.tommasoberlose.anotherwidget.helpers.AlarmHelper
import com.tommasoberlose.anotherwidget.ui.activities.ChooseApplicationActivity
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.toast
import kotlinx.android.synthetic.main.fragment_clock_settings.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception


class ClockSettingsFragment : Fragment() {

    companion object {
        fun newInstance() = ClockSettingsFragment()
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
        val binding = DataBindingUtil.inflate<FragmentClockSettingsBinding>(inflater, R.layout.fragment_clock_settings, container, false)

        subscribeUi(binding, viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupListener()
        updateNextAlarmWarningUi()
    }

    private fun subscribeUi(
        binding: FragmentClockSettingsBinding,
        viewModel: MainViewModel
    ) {
        viewModel.showBigClockWarning.observe(viewLifecycleOwner, Observer {
            large_clock_warning?.isVisible = it
            small_clock_warning?.isVisible = !it
        })

        viewModel.showClock.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_clock_label?.text =
                    if (it) getString(R.string.show_clock_visible) else getString(R.string.show_clock_not_visible)
                binding.isClockVisible = it
            }
        })

        viewModel.clockTextSize.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                clock_text_size_label?.text = String.format("%.0fsp", it)
            }
        })

        viewModel.clockBottomMargin.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                clock_bottom_margin_label?.text = when (it) {
                    Constants.ClockBottomMargin.NONE.value -> getString(R.string.settings_clock_bottom_margin_subtitle_none)
                    Constants.ClockBottomMargin.SMALL.value -> getString(R.string.settings_clock_bottom_margin_subtitle_small)
                    Constants.ClockBottomMargin.LARGE.value -> getString(R.string.settings_clock_bottom_margin_subtitle_large)
                    else -> getString(R.string.settings_clock_bottom_margin_subtitle_medium)
                }
            }
        })

        viewModel.showNextAlarm.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_next_alarm_label?.text = if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        })

        viewModel.clockAppName.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                clock_app_label?.text =
                    if (Preferences.clockAppName != "") Preferences.clockAppName else getString(R.string.default_clock_app)
            }
        })
    }

    private fun setupListener() {
        action_hide_large_clock_warning.setOnClickListener {
            Preferences.showBigClockWarning = false
        }

        action_show_clock.setOnClickListener {
            Preferences.showClock = !Preferences.showClock
        }

        action_clock_text_size.setOnClickListener {
            val dialog = BottomSheetMenu<Float>(requireContext(), header = getString(R.string.settings_clock_text_size_title)).setSelectedValue(Preferences.clockTextSize)
            (46 downTo 12).filter { it % 2 == 0 }.forEach {
                dialog.addItem("${it}sp", it.toFloat())
            }
            dialog.addOnSelectItemListener { value ->
                Preferences.clockTextSize = value
            }.show()
        }

        action_clock_bottom_margin_size.setOnClickListener {
            BottomSheetMenu<Int>(requireContext(), header = getString(R.string.settings_show_next_alarm_title)).setSelectedValue(Preferences.clockBottomMargin)
                .addItem(getString(R.string.settings_clock_bottom_margin_subtitle_none), Constants.ClockBottomMargin.NONE.value)
                .addItem(getString(R.string.settings_clock_bottom_margin_subtitle_small), Constants.ClockBottomMargin.SMALL.value)
                .addItem(getString(R.string.settings_clock_bottom_margin_subtitle_medium), Constants.ClockBottomMargin.MEDIUM.value)
                .addItem(getString(R.string.settings_clock_bottom_margin_subtitle_large), Constants.ClockBottomMargin.LARGE.value)
                .addOnSelectItemListener { value ->
                    Preferences.clockBottomMargin = value
                }.show()
        }

        action_clock_app.setOnClickListener {
            if (Preferences.showClock) {
                startActivityForResult(Intent(requireContext(), ChooseApplicationActivity::class.java),
                    RequestCode.CLOCK_APP_REQUEST_CODE.code
                )
            }
        }

        action_show_next_alarm.setOnClickListener {
            BottomSheetMenu<Boolean>(requireContext(), header = getString(R.string.settings_show_next_alarm_title)).setSelectedValue(Preferences.showNextAlarm)
                .addItem(getString(R.string.settings_visible), true)
                .addItem(getString(R.string.settings_not_visible), false)
                .addOnSelectItemListener { value ->
                    Preferences.showNextAlarm = value
                }.show()
        }
    }

    private fun updateNextAlarmWarningUi() {
        show_next_alarm_warning.isVisible = AlarmHelper.isAlarmProbablyWrong(requireContext())
        with(requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val alarm = nextAlarmClock
            if (alarm != null) {
                val pm = requireContext().packageManager as PackageManager
                val appNameOrPackage = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(nextAlarmClock.showIntent.creatorPackage ?: "", 0))
                } catch (e: Exception) {
                    nextAlarmClock.showIntent.creatorPackage
                }
                show_next_alarm_warning.text = getString(R.string.next_alarm_warning).format(appNameOrPackage)
            }
        }
    }

    private val nextAlarmChangeBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateNextAlarmWarningUi()
        }
    }

    override fun onStart() {
        super.onStart()
        activity?.registerReceiver(nextAlarmChangeBroadcastReceiver, IntentFilter(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED))
    }

    override fun onStop() {
        activity?.unregisterReceiver(nextAlarmChangeBroadcastReceiver)
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode ==  RequestCode.CLOCK_APP_REQUEST_CODE.code) {
            Preferences.bulk {
                clockAppName = data?.getStringExtra(Constants.RESULT_APP_NAME) ?: getString(R.string.default_clock_app)
                clockAppPackage = data?.getStringExtra(Constants.RESULT_APP_PACKAGE) ?: ""
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
