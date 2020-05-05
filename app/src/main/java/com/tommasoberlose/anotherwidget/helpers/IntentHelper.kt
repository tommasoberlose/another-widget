package com.tommasoberlose.anotherwidget.helpers

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.util.Log
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.models.Event


object IntentHelper {

    fun getGoogleMapsIntentFromAddress(context: Context, address:String): Intent {
        val gmmIntentUri: Uri = Uri.parse("geo:0,0?q=$address")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.`package` = "com.google.android.apps.maps"

        return if (mapIntent.resolveActivity(context.packageManager) != null) {
            mapIntent
        } else {
            val map = "http://maps.google.co.in/maps?q=$address"
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(map));
            i
        }
    }

    fun getWeatherIntent(context: Context): Intent {
        return when (Preferences.weatherAppPackage) {
            "" -> {
                Intent(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = Uri.parse("dynact://velour/weather/ProxyActivity")
                    component = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.velour.DynamicActivityTrampoline")
                }
            }
            "_" -> {
                Intent()
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.weatherAppPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } catch (e: Exception) {
                    Intent(Intent.ACTION_VIEW).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        data = Uri.parse("dynact://velour/weather/ProxyActivity")
                        component = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.velour.DynamicActivityTrampoline")
                    }
                }
            }
        }
    }

    fun getCalendarIntent(context: Context): Intent {
        return when (Preferences.calendarAppPackage) {
            "" -> {
                Intent(Intent.ACTION_MAIN).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                }
            }
            "_" -> {
                Intent()
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.calendarAppPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Intent(Intent.ACTION_MAIN).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addCategory(Intent.CATEGORY_APP_CALENDAR)
                    }
                }
            }
        }
    }

    fun getEventIntent(context: Context, e: Event, forceEventDetails: Boolean = false): Intent {
        return when (Preferences.openEventDetails || forceEventDetails) {
            true -> {
                val uri = ContentUris.withAppendedId(Events.CONTENT_URI, e.eventID)

                if (Preferences.calendarAppPackage == "") {
                    Intent(Intent.ACTION_VIEW).apply {
                        data = uri
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.startDate)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.endDate)
                    }
                } else {
                    getCalendarIntent(context).apply {
                        action = Intent.ACTION_VIEW
                        data = uri
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.startDate)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.endDate)
                    }
                }
            }
            false -> {
                getCalendarIntent(context)
            }
        }
    }

    fun getClockIntent(context: Context): Intent {
        return when (Preferences.clockAppPackage) {
            "" -> {
                Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            "_" -> {
                Intent()
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.clockAppPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                } catch (e: Exception) {
                    Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
            }
        }
    }
}