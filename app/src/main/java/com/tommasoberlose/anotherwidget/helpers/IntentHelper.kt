package com.tommasoberlose.anotherwidget.helpers

import android.appwidget.AppWidgetManager
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
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import java.util.*


object IntentHelper {

    fun getWidgetUpdateIntent(context: Context): Intent {
        val widgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, MainWidget::class.java)
        val widgetIds = widgetManager.getAppWidgetIds(widgetComponent)
        return Intent(context, MainWidget::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
    }

    fun getGoogleMapsIntentFromAddress(context: Context, address: String): Intent {
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
        val calendarUri = CalendarContract.CONTENT_URI
            .buildUpon()
            .appendPath("time")
            .appendPath(Calendar.getInstance().timeInMillis.toString())
            .build()
        return when (Preferences.calendarAppPackage) {
            "" -> {
                Intent(Intent.ACTION_VIEW).apply {
                    data = calendarUri
                }
            }
            "_" -> {
                Intent()
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.calendarAppPackage)!!.apply {
                        action = Intent.ACTION_VIEW
                        data = calendarUri
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Intent(Intent.ACTION_VIEW).apply {
                        data = calendarUri
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
                        flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        if (!e.allDay) {
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.startDate)
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.endDate)
                        } else {
                            val start = Calendar.getInstance().apply {
                                timeInMillis = e.startDate
                            }
                            val end = Calendar.getInstance().apply {
                                timeInMillis = e.endDate
                            }

                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.startDate + start.timeZone.getOffset(start.timeInMillis))
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.endDate + end.timeZone.getOffset(end.timeInMillis))
//                            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, 1)
                        }

                        Log.d("ciao", "intent: ${this} - extras: ${this.extras}")
                    }
                } else {
                    getCalendarIntent(context).apply {
                        action = Intent.ACTION_VIEW
                        data = uri
                        if (!e.allDay) {
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, e.startDate)
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, e.endDate)
                        } else {
                            val start = Calendar.getInstance().apply {
                                timeInMillis = e.startDate
                            }
                            val end = Calendar.getInstance().apply {
                                timeInMillis = e.endDate
                            }
                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.timeInMillis + start.timeZone.getOffset(start.timeInMillis))
                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.timeInMillis + end.timeZone.getOffset(end.timeInMillis))
                            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, 1)
                        }
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

    fun getBatteryIntent(): Intent {
        return Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
    }

    fun getMusicIntent(context: Context): Intent {
        return when (Preferences.mediaPlayerPackage) {
            "" -> {
                Intent()
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.mediaPlayerPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                } catch (e: Exception) {
                    Intent()
                }
            }
        }
    }

    fun getFitIntent(context: Context): Intent {
        val pm: PackageManager = context.packageManager
        return try {
            pm.getLaunchIntentForPackage("com.google.android.apps.fitness")!!.apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
        } catch (e: Exception) {
            Intent()
        }
    }
}