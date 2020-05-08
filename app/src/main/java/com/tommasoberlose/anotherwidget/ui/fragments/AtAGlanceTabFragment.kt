package com.tommasoberlose.anotherwidget.ui.fragments

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentAtAGlanceSettingsBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.AlarmHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.fragment_at_a_glance_settings.*
import kotlinx.android.synthetic.main.fragment_at_a_glance_settings.scrollView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class AtAGlanceTabFragment : Fragment() {

    companion object {
        fun newInstance() = AtAGlanceTabFragment()
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
        val binding = DataBindingUtil.inflate<FragmentAtAGlanceSettingsBinding>(inflater, R.layout.fragment_at_a_glance_settings, container, false)

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
        binding: FragmentAtAGlanceSettingsBinding,
        viewModel: MainViewModel
    ) {

        viewModel.showMusic.observe(viewLifecycleOwner, Observer {
            checkNotificationPermission()
        })

        viewModel.showNextAlarm.observe(viewLifecycleOwner, Observer {
            updateNextAlarmWarningUi()
        })
    }

    private fun setupListener() {
        action_show_music.setOnClickListener {
            Preferences.showMusic = !Preferences.showMusic
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
        with(requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val alarm = nextAlarmClock
            if (AlarmHelper.isAlarmProbablyWrong(requireContext()) && alarm != null && alarm.showIntent != null) {
                val pm = requireContext().packageManager as PackageManager
                val appNameOrPackage = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(alarm.showIntent?.creatorPackage ?: "", 0))
                } catch (e: Exception) {
                    alarm.showIntent?.creatorPackage ?: ""
                }
                show_next_alarm_warning.text = getString(R.string.next_alarm_warning).format(appNameOrPackage)
            } else {
                maintainScrollPosition {
                    show_next_alarm_label?.text = if (Preferences.showNextAlarm) getString(R.string.settings_visible) else getString(
                        R.string.settings_not_visible)
                }
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

    private fun checkNotificationPermission() {
        if (NotificationManagerCompat.getEnabledListenerPackages(requireContext()).contains(requireContext().packageName)) {
            notification_permission_alert?.isVisible = false
            MediaPlayerHelper.updatePlayingMediaInfo(requireContext())
            show_music_label?.text = if (Preferences.showMusic) getString(R.string.settings_show_music_enabled_subtitle) else getString(R.string.settings_show_music_disabled_subtitle)
        } else if (Preferences.showMusic) {
            notification_permission_alert?.isVisible = true
            show_music_label?.text = getString(R.string.settings_request_notification_access)
            notification_permission_alert?.setOnClickListener {
                activity?.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
        } else {
            show_music_label?.text = getString(R.string.settings_show_music_disabled_subtitle)
            notification_permission_alert?.isVisible = false
        }
    }

    private fun maintainScrollPosition(callback: () -> Unit) {
        val scrollPosition = scrollView.scrollY
        callback.invoke()
        lifecycleScope.launch {
            delay(200)
            scrollView.smoothScrollTo(0, scrollPosition)
        }
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
    }
}
