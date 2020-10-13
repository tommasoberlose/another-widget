package com.tommasoberlose.anotherwidget.components

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.AlarmHelper
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.receivers.ActivityDetectionReceiver
import com.tommasoberlose.anotherwidget.ui.activities.MusicPlayersFilterActivity
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import kotlinx.android.synthetic.main.glance_provider_settings_layout.view.*

class GlanceSettingsDialog(val context: Activity, val provider: Constants.GlanceProviderId, private val statusCallback: (() -> Unit)?) : BottomSheetDialog(context, R.style.BottomSheetDialogTheme) {

    override fun show() {
        val view = View.inflate(context, R.layout.glance_provider_settings_layout, null)

        /* TITLE */
        view.title.text = when (provider) {
            Constants.GlanceProviderId.PLAYING_SONG -> context.getString(R.string.settings_show_music_title)
            Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> context.getString(R.string.settings_show_next_alarm_title)
            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> context.getString(R.string.settings_low_battery_level_title)
            Constants.GlanceProviderId.CUSTOM_INFO -> context.getString(R.string.settings_custom_notes_title)
            Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> context.getString(R.string.settings_daily_steps_title)
            Constants.GlanceProviderId.NOTIFICATIONS -> context.getString(R.string.settings_show_notifications_title)
            Constants.GlanceProviderId.GREETINGS -> context.getString(R.string.settings_show_greetings_title)
        }

        /* SUBTITLE*/
        view.subtitle.text = when (provider) {
            Constants.GlanceProviderId.PLAYING_SONG -> context.getString(R.string.settings_show_music_subtitle)
            Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> context.getString(R.string.settings_show_next_alarm_subtitle)
            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> context.getString(R.string.settings_low_battery_level_subtitle)
            Constants.GlanceProviderId.CUSTOM_INFO -> ""
            Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> context.getString(R.string.settings_daily_steps_subtitle)
            Constants.GlanceProviderId.NOTIFICATIONS -> context.getString(R.string.settings_show_notifications_subtitle)
            Constants.GlanceProviderId.GREETINGS -> context.getString(R.string.settings_show_greetings_subtitle)
        }

        /* SONG */
        view.action_filter_music_players.isVisible = provider == Constants.GlanceProviderId.PLAYING_SONG
        if (provider == Constants.GlanceProviderId.PLAYING_SONG) {
            view.action_filter_music_players.setOnClickListener {
                context.startActivity(Intent(context, MusicPlayersFilterActivity::class.java))
            }
            checkNotificationPermission(view)
        }

        /* ALARM */
        view.alarm_set_by_container.isVisible = provider == Constants.GlanceProviderId.NEXT_CLOCK_ALARM
        if (provider == Constants.GlanceProviderId.NEXT_CLOCK_ALARM) {
            view.header.text = context.getString(R.string.information_header)
            view.warning_container.isVisible = false
            checkNextAlarm(view)
        }

        /* GOOGLE STEPS */
        view.action_toggle_google_fit.isVisible = provider == Constants.GlanceProviderId.GOOGLE_FIT_STEPS
        if (provider == Constants.GlanceProviderId.GOOGLE_FIT_STEPS) {
            checkFitnessPermission(view)
        }

        /* BATTERY INFO */
        if (provider == Constants.GlanceProviderId.BATTERY_LEVEL_LOW) {
            view.warning_container.isVisible = false
            view.header.isVisible = false
            view.divider.isVisible = false
        }

        /* NOTIFICATIONS */
        view.action_filter_notifications_app.isVisible = provider == Constants.GlanceProviderId.NOTIFICATIONS
        if (provider == Constants.GlanceProviderId.NOTIFICATIONS) {
            checkLastNotificationsPermission(view)
        }

        /* GREETINGS */
        if (provider == Constants.GlanceProviderId.GREETINGS) {
            view.warning_container.isVisible = false
            view.header.isVisible = false
            view.divider.isVisible = false
        }

        /* TOGGLE */
        view.provider_switch.isChecked = when (provider) {
            Constants.GlanceProviderId.PLAYING_SONG -> Preferences.showMusic
            Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> Preferences.showNextAlarm
            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> Preferences.showBatteryCharging
            Constants.GlanceProviderId.CUSTOM_INFO -> true
            Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> Preferences.showDailySteps
            Constants.GlanceProviderId.NOTIFICATIONS -> Preferences.showNotifications
            Constants.GlanceProviderId.GREETINGS -> Preferences.showGreetings
        }

        view.provider_switch.setOnCheckedChangeListener { _, isChecked ->
            when (provider) {
                Constants.GlanceProviderId.PLAYING_SONG -> {
                    Preferences.showMusic = isChecked
                    checkNotificationPermission(view)
                }
                Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> {
                    Preferences.showNextAlarm = isChecked
                    checkNextAlarm(view)
                }
                Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
                    Preferences.showBatteryCharging = isChecked
                }
                Constants.GlanceProviderId.NOTIFICATIONS -> {
                    Preferences.showNotifications = isChecked
                    checkLastNotificationsPermission(view)
                }
                Constants.GlanceProviderId.GREETINGS -> {
                    Preferences.showGreetings = isChecked
                }
                Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> {
                    if (isChecked) {
                        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(context)
                        if (!GoogleSignIn.hasPermissions(account,
                                ActivityDetectionReceiver.FITNESS_OPTIONS
                            )) {
                            val mGoogleSignInClient = GoogleSignIn.getClient(context, GoogleSignInOptions.Builder(
                                GoogleSignInOptions.DEFAULT_SIGN_IN).addExtension(
                                ActivityDetectionReceiver.FITNESS_OPTIONS
                            ).build())
                            context.startActivityForResult(mGoogleSignInClient.signInIntent, 2)
                        } else {
                            Preferences.showDailySteps = true
                        }
                    } else {
                        Preferences.showDailySteps = false
                    }
                    checkFitnessPermission(view)
                }
                else -> {}
            }
            statusCallback?.invoke()
        }

        setContentView(view)
        super.show()
    }
    
    private fun checkNextAlarm(view: View) {
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            val alarm = nextAlarmClock
            if (alarm != null && alarm.showIntent != null) {
                val pm = context.packageManager as PackageManager
                val appNameOrPackage = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(alarm.showIntent?.creatorPackage ?: "", 0))
                } catch (e: Exception) {
                    alarm.showIntent?.creatorPackage ?: ""
                }
                view.alarm_set_by_title.text = context.getString(R.string.settings_show_next_alarm_app_title).format(appNameOrPackage)
                view.alarm_set_by_subtitle.text = if (AlarmHelper.isAlarmProbablyWrong(context)) context.getString(R.string.settings_show_next_alarm_app_subtitle_wrong) else context.getString(R.string.settings_show_next_alarm_app_subtitle_correct)
                view.alarm_set_by_title.isVisible = true
            } else {
                view.alarm_set_by_title.isVisible = false
            }
        }
        statusCallback?.invoke()
    }

    private fun checkNotificationPermission(view: View) {
        Log.d("ciao", NotificationManagerCompat.getEnabledListenerPackages(context).toString())
        when {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) -> {
                view.warning_container.isVisible = false
                MediaPlayerHelper.updatePlayingMediaInfo(context)
            }
            Preferences.showMusic -> {
                view.warning_container.isVisible = true
                view.warning_title.text = context.getString(R.string.settings_request_notification_access)
                view.warning_container.setOnClickListener {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
            }
            else -> {
                view.warning_container.isVisible = false
            }
        }
        statusCallback?.invoke()
    }

    private fun checkLastNotificationsPermission(view: View) {
        when {
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) -> {
                view.warning_container.isVisible = false
            }
            Preferences.showNotifications -> {
                view.warning_container.isVisible = true
                view.warning_title.text = context.getString(R.string.settings_request_last_notification_access)
                view.warning_container.setOnClickListener {
                    context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                }
            }
            else -> {
                view.warning_container.isVisible = false
            }
        }
        statusCallback?.invoke()
    }

    private fun checkFitnessPermission(view: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || context.checkGrantedPermission(
                Manifest.permission.ACTIVITY_RECOGNITION)
        ) {
            view.warning_container.isVisible = false
            if (Preferences.showDailySteps) {
                ActivityDetectionReceiver.registerFence(context)
            } else {
                ActivityDetectionReceiver.unregisterFence(context)
            }
        } else if (Preferences.showDailySteps) {
            ActivityDetectionReceiver.unregisterFence(context)
            view.warning_container.isVisible = true
            view.warning_title.text = context.getString(R.string.settings_request_fitness_access)
            view.warning_container.setOnClickListener {
                requireFitnessPermission(view)
            }
        } else {
            ActivityDetectionReceiver.unregisterFence(context)
            view.warning_container.isVisible = false
        }
        statusCallback?.invoke()
    }

    private fun requireFitnessPermission(view: View) {
        Dexter.withContext(context)
            .withPermissions(
                "com.google.android.gms.permission.ACTIVITY_RECOGNITION",
                "android.gms.permission.ACTIVITY_RECOGNITION",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Manifest.permission.ACTIVITY_RECOGNITION else "com.google.android.gms.permission.ACTIVITY_RECOGNITION"
            ).withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    checkFitnessPermission(view)
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
}