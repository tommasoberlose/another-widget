package com.tommasoberlose.anotherwidget.ui.fragments

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.components.BottomSheetMenu
import com.tommasoberlose.anotherwidget.components.CustomNotesDialog
import com.tommasoberlose.anotherwidget.components.GlanceProviderSortMenu
import com.tommasoberlose.anotherwidget.databinding.FragmentGlanceSettingsBinding
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.AlarmHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.receivers.ActivityDetectionReceiver
import com.tommasoberlose.anotherwidget.receivers.ActivityDetectionReceiver.Companion.FITNESS_OPTIONS
import com.tommasoberlose.anotherwidget.ui.activities.MainActivity
import com.tommasoberlose.anotherwidget.ui.viewmodels.MainViewModel
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.checkIfFitInstalled
import kotlinx.android.synthetic.main.fragment_calendar_settings.*
import kotlinx.android.synthetic.main.fragment_glance_settings.*
import kotlinx.android.synthetic.main.fragment_glance_settings.scrollView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class GlanceTabFragment : Fragment() {

    companion object {
        fun newInstance() = GlanceTabFragment()
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
        val binding = DataBindingUtil.inflate<FragmentGlanceSettingsBinding>(inflater, R.layout.fragment_glance_settings, container, false)

        subscribeUi(binding, viewModel)

        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        action_show_steps.isVisible = requireContext().checkIfFitInstalled()

        setupListener()
        updateNextAlarmWarningUi()
    }

    private fun subscribeUi(
        binding: FragmentGlanceSettingsBinding,
        viewModel: MainViewModel
    ) {
        binding.isGlanceVisible = Preferences.showGlance

        viewModel.showGlance.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                binding.isGlanceVisible = it
                show_glance_label.text = if (it) getString(R.string.description_show_glance_visible) else getString(R.string.description_show_glance_not_visible)
            }
        })

        viewModel.showMusic.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                checkNotificationPermission()
            }
        })

        viewModel.showNextAlarm.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                updateNextAlarmWarningUi()
            }
        })

        viewModel.showBatteryCharging.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_low_battery_level_warning_label?.text = if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
        })

        viewModel.showDailySteps.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_steps_label?.text = if (it) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
            }
            checkFitnessPermission()
        })

        viewModel.customInfo.observe(viewLifecycleOwner, Observer {
            maintainScrollPosition {
                show_custom_notes_label?.text = if (it == "") getString(R.string.settings_not_visible) else it
            }
        })

    }

    private fun setupListener() {

        action_show_glance.setOnClickListener {
            Preferences.showGlance = !Preferences.showGlance
        }

        show_glance_switch.setOnCheckedChangeListener { _, enabled: Boolean ->
            Preferences.showGlance = enabled
        }

        action_sort_glance_providers.setOnClickListener {
            GlanceProviderSortMenu(requireContext())
                .show()
        }

        action_show_music.setOnClickListener {
            if (Preferences.showGlance) {
                BottomSheetMenu<Boolean>(
                    requireContext(),
                    header = getString(R.string.settings_show_music_title)
                ).setSelectedValue(Preferences.showMusic)
                    .addItem(getString(R.string.settings_visible), true)
                    .addItem(getString(R.string.settings_not_visible), false)
                    .addOnSelectItemListener { value ->
                        Preferences.showMusic = value
                    }.show()
            }
        }

        action_show_next_alarm.setOnClickListener {
            if (Preferences.showGlance) {
                BottomSheetMenu<Boolean>(
                    requireContext(),
                    header = getString(R.string.settings_show_next_alarm_title)
                ).setSelectedValue(Preferences.showNextAlarm)
                    .addItem(getString(R.string.settings_visible), true)
                    .addItem(getString(R.string.settings_not_visible), false)
                    .addOnSelectItemListener { value ->
                        Preferences.showNextAlarm = value
                    }.show()
            }
        }

        action_show_low_battery_level_warning.setOnClickListener {
            if (Preferences.showGlance) {
                BottomSheetMenu<Boolean>(
                    requireContext(),
                    header = getString(R.string.settings_low_battery_level_title)
                ).setSelectedValue(Preferences.showBatteryCharging)
                    .addItem(getString(R.string.settings_visible), true)
                    .addItem(getString(R.string.settings_not_visible), false)
                    .addOnSelectItemListener { value ->
                        Preferences.showBatteryCharging = value
                    }.show()
            }
        }

        action_show_steps.setOnClickListener {
            if (Preferences.showGlance) {
                BottomSheetMenu<Boolean>(
                    requireContext(),
                    header = getString(R.string.settings_daily_steps_title)
                ).setSelectedValue(Preferences.showDailySteps)
                    .addItem(getString(R.string.settings_visible), true)
                    .addItem(getString(R.string.settings_not_visible), false)
                    .addOnSelectItemListener { value ->
                        if (value) {
                            val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(requireContext())
                            if (!GoogleSignIn.hasPermissions(account, FITNESS_OPTIONS)) {
                                val mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).addExtension(FITNESS_OPTIONS).build())
                                startActivityForResult(mGoogleSignInClient.signInIntent, 2)
                            } else {
                                Preferences.showDailySteps = true
                            }
                        } else {
                            Preferences.showDailySteps = false
                        }
                    }.show()
            }
        }

        action_show_custom_notes.setOnClickListener {
            if (Preferences.showGlance) {
                CustomNotesDialog(requireContext()).show()
            }
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
                show_next_alarm_warning.text =
                    getString(R.string.next_alarm_warning).format(appNameOrPackage)
            } else {
                show_next_alarm_label?.text = if (Preferences.showNextAlarm) getString(R.string.settings_visible) else getString(
                        R.string.settings_not_visible)
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
            show_music_label?.text = if (Preferences.showMusic) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
        } else if (Preferences.showMusic) {
            notification_permission_alert?.isVisible = true
            show_music_label?.text = getString(R.string.settings_request_notification_access)
            notification_permission_alert?.setOnClickListener {
                activity?.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
        } else {
            show_music_label?.text = getString(R.string.settings_not_visible)
            notification_permission_alert?.isVisible = false
        }
    }

    private fun checkFitnessPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || activity?.checkGrantedPermission(Manifest.permission.ACTIVITY_RECOGNITION) == true) {
            fitness_permission_alert?.isVisible = false
            if (Preferences.showDailySteps) {
                ActivityDetectionReceiver.registerFence(requireContext())
            } else {
                ActivityDetectionReceiver.unregisterFence(requireContext())
            }
            show_steps_label?.text = if (Preferences.showDailySteps) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
        } else if (Preferences.showDailySteps) {
            ActivityDetectionReceiver.unregisterFence(requireContext())
            fitness_permission_alert?.isVisible = true
            show_steps_label?.text = getString(R.string.settings_request_fitness_access)
            fitness_permission_alert?.setOnClickListener {
                requireFitnessPermission()
            }
        } else {
            ActivityDetectionReceiver.unregisterFence(requireContext())
            show_steps_label?.text = getString(R.string.settings_not_visible)
            fitness_permission_alert?.isVisible = false
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        when (requestCode) {
            1 -> {
                if (resultCode == Activity.RESULT_OK) {
                    checkFitnessPermission()
                } else {
                    Preferences.showDailySteps = false
                }
            }
            2-> {
                try {
                    val account: GoogleSignInAccount? = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException::class.java)
                    if (!GoogleSignIn.hasPermissions(account, FITNESS_OPTIONS)) {
                        GoogleSignIn.requestPermissions(
                            requireActivity(),
                            1,
                            account,
                            FITNESS_OPTIONS)
                    } else {
                        checkFitnessPermission()
                    }
                } catch (e: ApiException) {
                    e.printStackTrace()
                    Preferences.showDailySteps = false
                }
            }
        }
    }

    private fun requireFitnessPermission() {
        Dexter.withContext(requireContext())
            .withPermissions(
                "com.google.android.gms.permission.ACTIVITY_RECOGNITION",
                "android.gms.permission.ACTIVITY_RECOGNITION",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else "com.google.android.gms.permission.ACTIVITY_RECOGNITION"
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.let {
                        if (report.areAllPermissionsGranted()){
                            checkFitnessPermission()
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

    override fun onResume() {
        super.onResume()
        checkNotificationPermission()
    }
}
