package com.tommasoberlose.anotherwidget.helpers

import android.Manifest
import android.content.Context
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.models.GlanceProvider
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.checkIfFitInstalled
import java.util.ArrayList

object GlanceProviderHelper {
    fun getGlanceProviders(context: Context): ArrayList<Constants.GlanceProviderId> {
        val enabledProviders = Preferences.enabledGlanceProviderOrder.split(",").filter { it != "" }

        val providers = Constants.GlanceProviderId.values()
            .filter {
                context.checkIfFitInstalled() || it != Constants.GlanceProviderId.GOOGLE_FIT_STEPS
            }
            .toTypedArray()

        return ArrayList(providers.filter { enabledProviders.contains(it.id) }.sortedWith { p1, p2 ->
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
        } + providers.filter { !enabledProviders.contains(it.id) })
    }

    fun getGlanceProviderById(context: Context, providerId: Constants.GlanceProviderId): GlanceProvider {
        return when(providerId) {
            Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_show_next_alarm_title),
                   R.drawable.round_access_alarm_24
               )
            }
            Constants.GlanceProviderId.PLAYING_SONG -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_show_music_title),
                   R.drawable.round_music_note_24
               )
            }
            Constants.GlanceProviderId.CUSTOM_INFO -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_custom_notes_title),
                   R.drawable.round_sticky_note_2_24
               )
            }
            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_low_battery_level_title),
                   R.drawable.round_battery_charging_full_24
               )
            }
            Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_daily_steps_title),
                   R.drawable.round_favorite_border_24
               )
            }
            Constants.GlanceProviderId.NOTIFICATIONS -> {
                GlanceProvider(providerId.id,
                    context.getString(R.string.settings_show_notifications_title),
                    R.drawable.round_notifications_24
                )
            }
            Constants.GlanceProviderId.GREETINGS -> {
                GlanceProvider(providerId.id,
                    context.getString(R.string.settings_show_greetings_title),
                    R.drawable.round_history_edu_24
                )
            }
            Constants.GlanceProviderId.EVENTS -> {
                GlanceProvider(providerId.id,
                    context.getString(R.string.settings_show_events_as_glance_provider_title),
                    R.drawable.round_event_note_24
                )
            }
        }
    }

    fun saveGlanceProviderOrder(list: List<Constants.GlanceProviderId>) {
        Preferences.enabledGlanceProviderOrder = list.joinToString(separator = ",")
    }

    fun showGlanceProviders(context: Context): Boolean {
        val eventRepository = EventRepository(context)
        BatteryHelper.updateBatteryInfo(context)

        val showGlance = (eventRepository.getEventsCount() == 0 || !Preferences.showEvents || Preferences.showEventsAsGlanceProvider)
            && (
                (Preferences.showNotifications && ActiveNotificationsHelper.showLastNotification()) ||
                (Preferences.showNextAlarm && AlarmHelper.getNextAlarm(context) != "") ||
                (MediaPlayerHelper.isSomeonePlaying(context)) ||
                (Preferences.showBatteryCharging && Preferences.isCharging || Preferences.isBatteryLevelLow) ||
                (Preferences.customNotes.isNotEmpty()) ||
                (Preferences.showDailySteps && Preferences.googleFitSteps > 0) ||
                (Preferences.showGreetings && GreetingsHelper.showGreetings()) ||
                        (Preferences.showEventsAsGlanceProvider && Preferences.showEvents && context.checkGrantedPermission(
                            Manifest.permission.READ_CALENDAR) && eventRepository.getNextEvent() != null)
            )
        eventRepository.close()
        return showGlance
    }
}