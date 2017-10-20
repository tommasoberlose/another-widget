package com.tommasoberlose.anotherwidget.ui.widget

import android.Manifest
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.view.View
import android.widget.RemoteViews

import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.receiver.UpdatesReceiver
import com.tommasoberlose.anotherwidget.util.Util
import com.tommasoberlose.anotherwidget.receiver.WeatherReceiver

import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.app.PendingIntent
import android.provider.CalendarContract
import android.content.ContentUris
import android.util.Log
import com.tommasoberlose.anotherwidget.util.CalendarUtil
import com.tommasoberlose.anotherwidget.util.WeatherUtil
import android.graphics.Typeface
import android.net.Uri
import android.widget.TextClock
import android.widget.TextView
import android.content.ComponentName
import android.support.v4.content.ContextCompat.startActivity
import android.provider.CalendarContract.Events






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

            views = updateLocationView(context, views, appWidgetId)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateCalendarView(context: Context, views: RemoteViews, widgetID: Int): RemoteViews {
            val SP = PreferenceManager.getDefaultSharedPreferences(context)
            val now = Calendar.getInstance()
            val calendarLayout = SP.getBoolean(Constants.PREF_SHOW_EVENTS, true) && Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)

            views.setViewVisibility(R.id.empty_layout, View.VISIBLE)
            views.setViewVisibility(R.id.calendar_layout, View.GONE)
            var dateStringValue: String = Util.getCapWordString(Constants.engDateFormat.format(now.time))
            if (SP.getBoolean(Constants.PREF_ITA_FORMAT_DATE, false)) {
                dateStringValue = Util.getCapWordString(Constants.itDateFormat.format(now.time))
            }
            views.setTextViewText(R.id.empty_date, dateStringValue)
            //views.setImageViewBitmap(R.id.empty_date, Util.buildUpdate(context, Constants.dateFormat.format(now.time)[0].toUpperCase() + Constants.dateFormat.format(now.time).substring(1), "fonts/product_sans_regular.ttf"))


            val calPIntent = PendingIntent.getActivity(context, widgetID, Util.getCalendarIntent(context), 0)
            views.setOnClickPendingIntent(R.id.main_layout, calPIntent)


            if (calendarLayout) {
                val e = CalendarUtil.getNextEvent(context)

                if (e.id != 0) {
                    views.setTextViewText(R.id.next_event, Util.getDifferenceText(context, e.title, now.timeInMillis, e.startDate))

                    if (!e.allDay) {
                        val startHour: String = if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) Constants.badHourFormat.format(e.startDate) else Constants.goodHourFormat.format(e.startDate)
                        val endHour: String = if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) Constants.badHourFormat.format(e.endDate) else Constants.goodHourFormat.format(e.endDate)
                        var dayDiff = TimeUnit.MILLISECONDS.toDays(e.endDate - e.startDate)

                        val startCal = Calendar.getInstance()
                        startCal.timeInMillis = e.startDate

                        val endCal = Calendar.getInstance()
                        endCal.timeInMillis = e.endDate

                        if (startCal.get(Calendar.HOUR_OF_DAY) > endCal.get(Calendar.HOUR_OF_DAY)) {
                            dayDiff++
                        } else if (startCal.get(Calendar.HOUR_OF_DAY) == endCal.get(Calendar.HOUR_OF_DAY) && startCal.get(Calendar.MINUTE) >= endCal.get(Calendar.MINUTE)) {
                            dayDiff++
                        }

                        var multipleDay: String = ""
                        if (dayDiff > 0) {
                            multipleDay = String.format(" (+%s%s)", dayDiff, context.getString(R.string.day_char))
                        }

                        views.setTextViewText(R.id.next_event_date, String.format("%s - %s%s", startHour, endHour, multipleDay))
                    } else {
                        views.setTextViewText(R.id.next_event_date, dateStringValue)
                    }

                    views.setViewVisibility(R.id.empty_layout, View.GONE)
                    views.setViewVisibility(R.id.calendar_layout, View.VISIBLE)



                    val pIntent = PendingIntent.getActivity(context, widgetID, Util.getEventIntent(context, e), 0)
                    views.setOnClickPendingIntent(R.id.main_layout, pIntent)
                }
            }

            return views
        }

        fun updateLocationView(context: Context, views: RemoteViews, widgetID: Int): RemoteViews {
            val SP = PreferenceManager.getDefaultSharedPreferences(context)
            val locationLayout = SP.getBoolean(Constants.PREF_SHOW_WEATHER, true)

            if (locationLayout && SP.contains(Constants.PREF_WEATHER_TEMP) && SP.contains(Constants.PREF_WEATHER_ICON)) {
                views.setViewVisibility(R.id.weather, View.VISIBLE)
                views.setViewVisibility(R.id.calendar_weather, View.VISIBLE)
                val temp = String.format(Locale.getDefault(), "%.0f °%s", SP.getFloat(Constants.PREF_WEATHER_TEMP, 0f), SP.getString(Constants.PREF_WEATHER_REAL_TEMP_UNIT, "F"))


                views.setViewVisibility(R.id.weather_icon, View.VISIBLE)
                views.setViewVisibility(R.id.empty_weather_icon, View.VISIBLE)
                val icon: String = SP.getString(Constants.PREF_WEATHER_ICON, "")
                if (icon.equals("")) {
                    views.setViewVisibility(R.id.weather_icon, View.GONE)
                    views.setViewVisibility(R.id.empty_weather_icon, View.GONE)
                } else {
                    views.setImageViewResource(R.id.weather_icon, WeatherUtil.getWeatherIconResource(icon))
                    views.setImageViewResource(R.id.empty_weather_icon, WeatherUtil.getWeatherIconResource(icon))
                }

                views.setTextViewText(R.id.temp, temp)
                views.setTextViewText(R.id.calendar_temp, temp)


                val weatherPIntent = PendingIntent.getActivity(context, widgetID, Util.getWeatherIntent(context), 0)

                views.setOnClickPendingIntent(R.id.weather, weatherPIntent)
                views.setOnClickPendingIntent(R.id.calendar_weather, weatherPIntent)
            } else {
                views.setViewVisibility(R.id.weather, View.GONE)
                views.setViewVisibility(R.id.calendar_weather, View.GONE)
            }
            return views
        }
    }
}

