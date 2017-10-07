package com.tommasoberlose.anotherwidget.ui.widget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews

import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.`object`.Event
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.ui.activity.MainActivity
import com.tommasoberlose.anotherwidget.util.UpdatesReceiver
import com.tommasoberlose.anotherwidget.util.Util
import com.tommasoberlose.anotherwidget.util.WeatherReceiver

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.app.PendingIntent
import android.net.Uri
import android.provider.CalendarContract
import android.content.ContentUris




/**
 * Implementation of App Widget functionality.
 */
class TheWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        UpdatesReceiver().setUpdates(context)
        WeatherReceiver().setUpdates(context)
        Util.showNotification(context)
    }

    override fun onDisabled(context: Context) {
        UpdatesReceiver().removeUpdates(context)
        WeatherReceiver().removeUpdates(context)
    }

    companion object {

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int) {

            var views = RemoteViews(context.packageName, R.layout.the_widget)

            views = updateCalendarView(context, views, appWidgetId)

            views = updateLocationView(context, views)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateCalendarView(context: Context, views: RemoteViews, widgetID: Int): RemoteViews {
                val now = Calendar.getInstance()
                val calendarLayout = Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)

                views.setViewVisibility(R.id.empty_layout, View.VISIBLE)
                views.setViewVisibility(R.id.calendar_layout, View.GONE)
                views.setTextViewText(R.id.empty_date, Constants.dateFormat.format(now.time))

                val calIntent = Intent(Intent.ACTION_MAIN)
                calIntent.addCategory(Intent.CATEGORY_APP_CALENDAR)
                val calPIntent = PendingIntent.getActivity(context, widgetID, calIntent, 0)
                views.setOnClickPendingIntent(R.id.main_layout, calPIntent)


                if (calendarLayout) {
                    val eventList = Util.getNextEvent(context)

                    if (eventList.isNotEmpty()) {
                        val difference = eventList[0].startDate - now.timeInMillis

                        if (difference > 1000 * 60) {
                            var time = ""
                            val hour = TimeUnit.MILLISECONDS.toHours(difference)
                            if (hour > 0) {
                                time = hour.toString() + context.getString(R.string.h_code)
                            }
                            val minutes = TimeUnit.MILLISECONDS.toMinutes(difference - hour * 3600 * 1000)
                            if (minutes > 0) {
                                time += " " + minutes + context.getString(R.string.min_code)
                            }

                            views.setTextViewText(R.id.next_event, String.format("%s %s %s", eventList[0].title, context.getString(R.string.in_code), time))
                        } else {
                            views.setTextViewText(R.id.next_event, String.format("%s", eventList[0].title))
                        }
                        views.setTextViewText(R.id.next_event_date, String.format("%s - %s", Constants.hourFormat.format(eventList[0].startDate), Constants.hourFormat.format(eventList[0].endDate)))

                        views.setViewVisibility(R.id.empty_layout, View.GONE)
                        views.setViewVisibility(R.id.calendar_layout, View.VISIBLE)

                        val builder = CalendarContract.CONTENT_URI.buildUpon()
                        builder.appendPath("time")
                        ContentUris.appendId(builder, eventList[0].startDate)
                        val intent = Intent(Intent.ACTION_VIEW)
                                .setData(builder.build())
                        val pIntent = PendingIntent.getActivity(context, widgetID, intent, 0)
                        views.setOnClickPendingIntent(R.id.main_layout, pIntent)
                    }
                }

                return views
            }

            fun updateLocationView(context: Context, views: RemoteViews): RemoteViews {
                val locationLayout = Util.checkGrantedPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)

                val SP = PreferenceManager.getDefaultSharedPreferences(context)
                if (locationLayout && SP.contains(Constants.PREF_WEATHER_TEMP) && SP.contains(Constants.PREF_WEATHER_ICON)) {
                    views.setViewVisibility(R.id.weather, View.VISIBLE)
                    views.setViewVisibility(R.id.calendar_weather, View.VISIBLE)
                    val temp = String.format(Locale.getDefault(), "%.0f °%s", SP.getFloat(Constants.PREF_WEATHER_TEMP, 0f), SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F"))


                    views.setViewVisibility(R.id.weather_icon, View.VISIBLE)
                    views.setViewVisibility(R.id.empty_weather_icon, View.VISIBLE)
                    val icon: String = SP.getString(Constants.PREF_WEATHER_ICON, "")
                    if (icon.equals("")) {
                        views.setViewVisibility(R.id.weather_icon, View.GONE)
                        views.setViewVisibility(R.id.empty_weather_icon, View.GONE)
                    } else {
                        views.setImageViewResource(R.id.weather_icon, Util.getWeatherIconResource(icon))
                        views.setImageViewResource(R.id.empty_weather_icon, Util.getWeatherIconResource(icon))
                    }

                    views.setTextViewText(R.id.temp, temp)
                    views.setTextViewText(R.id.calendar_temp, temp)
                } else {
                    views.setViewVisibility(R.id.weather, View.GONE)
                    views.setViewVisibility(R.id.calendar_weather, View.GONE)
                }
                return views
        }
    }
}

