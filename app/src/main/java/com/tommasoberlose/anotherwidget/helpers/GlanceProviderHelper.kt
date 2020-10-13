package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import android.util.Log
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.models.GlanceProvider
import com.tommasoberlose.anotherwidget.utils.checkIfFitInstalled
import java.util.ArrayList

object GlanceProviderHelper {
    fun getGlanceProviders(context: Context): ArrayList<Constants.GlanceProviderId> {
        val enabledProviders = Preferences.enabledGlanceProviderOrder.split(",").filter { it != "" }

        val providers = Constants.GlanceProviderId.values()
            .filter {
                context.checkIfFitInstalled() || it != Constants.GlanceProviderId.GOOGLE_FIT_STEPS
            }.toTypedArray()

        providers.sortWith(Comparator { p1, p2 ->
            when {
                enabledProviders.contains(p1.id) && enabledProviders.contains(p2.id) -> {
                    enabledProviders.indexOf(p1.id).compareTo(enabledProviders.indexOf(p2.id))
                }
                enabledProviders.contains(p1.id) -> {
                    -1
                }
                enabledProviders.contains(p2.id) -> {
                    1
                }
                else -> {
                    p1.id.compareTo(p2.id)
                }
            }
        })

        return ArrayList(providers.toList())
    }

    fun getGlanceProviderById(context: Context, providerId: Constants.GlanceProviderId): GlanceProvider? {
        return when(providerId) {
            Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_show_next_alarm_title),
                   R.drawable.round_alarm
               )
            }
            Constants.GlanceProviderId.PLAYING_SONG -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_show_music_title),
                   R.drawable.round_music_note
               )
            }
            Constants.GlanceProviderId.CUSTOM_INFO -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_custom_notes_title),
                   R.drawable.round_notes
               )
            }
            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_low_battery_level_title),
                   R.drawable.round_battery_charging_full
               )
            }
            Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_daily_steps_title),
                   R.drawable.round_directions_walk
               )
            }
        }
    }

    fun saveGlanceProviderOrder(list: ArrayList<Constants.GlanceProviderId>) {
        Preferences.enabledGlanceProviderOrder = list.joinToString(separator = ",")
    }

    fun showGlanceProviders(context: Context): Boolean {
        val eventRepository = EventRepository(context)
        BatteryHelper.updateBatteryInfo(context)

        val showGlance = Preferences.showGlance && (eventRepository.getEventsCount() == 0 || !Preferences.showEvents)
            && (
                (Preferences.showNextAlarm && AlarmHelper.getNextAlarm(context) != "") ||
                (MediaPlayerHelper.isSomeonePlaying(context)) ||
                (Preferences.showBatteryCharging && Preferences.isCharging || Preferences.isBatteryLevelLow) ||
                (Preferences.customNotes.isNotEmpty()) ||
                (Preferences.showDailySteps && Preferences.googleFitSteps > 0) ||
                (Preferences.showNotifications && ActiveNotificationsHelper.getLastNotification(context) != null)
            )
        eventRepository.close()
        return showGlance
    }
}