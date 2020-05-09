package com.tommasoberlose.anotherwidget.helpers

import android.content.Context
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.models.GlanceProvider
import java.util.ArrayList

object GlanceProviderHelper {
    fun getGlanceProviders(): ArrayList<Constants.GlanceProviderId> {
        val enabledProviders = Preferences.enabledGlanceProviderOrder.split(",").filter { it != "" }
        val providers = Constants.GlanceProviderId.values()

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
                   R.drawable.round_alarm,
                   context.getString(R.string.settings_show_next_alarm_subtitle)
               )
            }
            Constants.GlanceProviderId.PLAYING_SONG -> {
               GlanceProvider(providerId.id,
                   context.getString(R.string.settings_show_music_title),
                   R.drawable.round_music_note,
                   context.getString(R.string.settings_show_music_enabled_subtitle)
               )
            }
//            Constants.GlanceProviderId.CUSTOM_INFO -> {
//               GlanceProvider(providerId.id,
//                   context.getString(R.string.settings_show_next_alarm_title),
//                   R.drawable.round_event_note
//               )
//            }
//            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
//               GlanceProvider(providerId.id,
//                   context.getString(R.string.settings_show_next_alarm_title),
//                   R.drawable.round_battery_charging_full
//               )
//            }
//            Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> {
//               GlanceProvider(providerId.id,
//                   context.getString(R.string.settings_show_next_alarm_title),
//                   R.drawable.round_directions_walk
//               )
//            }
            else -> null
        }
    }

    fun saveGlanceProviderOrder(list: ArrayList<Constants.GlanceProviderId>) {
        Preferences.enabledGlanceProviderOrder = list.joinToString(separator = ",")
    }
}