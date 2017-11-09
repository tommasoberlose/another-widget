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
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.v4.content.ContextCompat.startActivity
import android.provider.CalendarContract.Events
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.LinearLayout
import com.tommasoberlose.anotherwidget.receiver.OpenWeatherIntentReceiver
import com.tommasoberlose.anotherwidget.ui.view.CustomTypefaceSpan
import kotlinx.android.synthetic.main.the_widget.*
import kotlinx.android.synthetic.main.the_widget.view.*
import kotlinx.android.synthetic.main.the_widget_sans.view.*


/**
 * Implementation of App Widget functionality.
 */
class TheWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Util.updateSettingsByDefault(context)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        UpdatesReceiver().setUpdates(context)
        WeatherReceiver().setUpdates(context)
    }

    override fun onDisabled(context: Context) {
        UpdatesReceiver().removeUpdates(context)
        WeatherReceiver().removeUpdates(context)
    }

    companion object {

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int) {
            val SP = PreferenceManager.getDefaultSharedPreferences(context)
            val displayMetrics = Resources.getSystem().displayMetrics
            val widgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
            var height = Util.convertDpToPixel(80f, context).toInt()
            val width = displayMetrics.widthPixels
            if (widgetInfo != null) {
                height = widgetInfo.minHeight
            }
            if (SP.getBoolean(Constants.PREF_SHOW_CLOCK, false)) {
                height += Util.convertSpToPixels(SP.getFloat(Constants.PREF_TEXT_CLOCK_SIZE, 90f), context).toInt() + Util.convertDpToPixel(16f, context).toInt()
            }
            if (SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f) > 30 && SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) > 22) {
                height += Util.convertDpToPixel(24f, context).toInt()
            }

            generateWidgetView(context, appWidgetId, appWidgetManager, width - Util.convertDpToPixel(16f, context).toInt(), height)
        }

        fun generateWidgetView(context: Context, appWidgetId: Int, appWidgetManager: AppWidgetManager, w: Int, h: Int) {
            var views = RemoteViews(context.packageName, R.layout.the_widget_sans)
            var v = View.inflate(context, R.layout.the_widget, null)

            val SP = PreferenceManager.getDefaultSharedPreferences(context)
            views.setTextColor(R.id.empty_date, Util.getFontColor(SP))
            views.setTextColor(R.id.divider1, Util.getFontColor(PreferenceManager.getDefaultSharedPreferences(context)))
            views.setTextColor(R.id.temp, Util.getFontColor(PreferenceManager.getDefaultSharedPreferences(context)))
            views.setTextColor(R.id.next_event, Util.getFontColor(PreferenceManager.getDefaultSharedPreferences(context)))
            views.setTextColor(R.id.next_event_difference_time, Util.getFontColor(PreferenceManager.getDefaultSharedPreferences(context)))
            views.setTextColor(R.id.next_event_date, Util.getFontColor(PreferenceManager.getDefaultSharedPreferences(context)))
            views.setTextColor(R.id.divider2, Util.getFontColor(PreferenceManager.getDefaultSharedPreferences(context)))
            views.setTextColor(R.id.calendar_temp, Util.getFontColor(PreferenceManager.getDefaultSharedPreferences(context)))
            views.setTextColor(R.id.time, Util.getFontColor(PreferenceManager.getDefaultSharedPreferences(context)))

            views.setTextViewTextSize(R.id.empty_date, TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
            views.setTextViewTextSize(R.id.divider1, TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
            views.setTextViewTextSize(R.id.temp, TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
            views.setTextViewTextSize(R.id.next_event, TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
            views.setTextViewTextSize(R.id.next_event_difference_time, TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
            views.setTextViewTextSize(R.id.next_event_date, TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
            views.setTextViewTextSize(R.id.divider2, TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
            views.setTextViewTextSize(R.id.calendar_temp, TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
            views.setTextViewTextSize(R.id.time, TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_CLOCK_SIZE, 90f))

            v = updateCalendarViewByLayout(context, v)
            v = updateLocationViewByLayout(context, v)
            v = updateClockViewByLayout(context, v)
            views.setImageViewBitmap(R.id.bitmap_container, Util.getBitmapFromView(v, w, h))

            views = updateCalendarView(context, views, appWidgetId)
            views = updateLocationView(context, views, appWidgetId)
            views = updateClockView(context, views, appWidgetId)

            views = fixViewsMargin(context, views)

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


            val calPIntent = PendingIntent.getActivity(context, widgetID, Util.getCalendarIntent(context), 0)
            views.setOnClickPendingIntent(R.id.empty_date, calPIntent)


            if (calendarLayout) {
                val e = CalendarUtil.getNextEvent(context)

                if (e.id != 0.toLong()) {
                    views.setTextViewText(R.id.next_event, e.title)

                    if (SP.getBoolean(Constants.PREF_SHOW_NEXT_EVENT, false) && CalendarUtil.getEventsCount(context) > 1) {
                        val multipleIntent = PendingIntent.getBroadcast(context, widgetID, Intent(Constants.ACTION_GO_TO_NEXT_EVENT), 0)
                        views.setViewVisibility(R.id.multiple_events, View.VISIBLE)
                        views.setOnClickPendingIntent(R.id.multiple_events, multipleIntent)
                    } else {
                        views.setViewVisibility(R.id.multiple_events, View.GONE)
                    }

                    val pIntent = PendingIntent.getActivity(context, widgetID, Util.getEventIntent(context, e), 0)
                    views.setOnClickPendingIntent(R.id.next_event, pIntent)
                    views.setOnClickPendingIntent(R.id.next_event_difference_time, pIntent)

                    if (SP.getBoolean(Constants.PREF_SHOW_DIFF_TIME, true)) {
                        views.setTextViewText(R.id.next_event_difference_time, Util.getDifferenceText(context, now.timeInMillis, e.startDate))
                        views.setViewVisibility(R.id.next_event_difference_time, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.next_event_difference_time, View.GONE)
                    }

                    if (SP.getInt(Constants.PREF_SECOND_ROW_INFORMATION, 0) == 2 && Util.getNextAlarm(context) != null) {
                        val source = BitmapFactory.decodeResource(context.resources, R.drawable.ic_action_alarm);
                        val result = Util.changeBitmapColor(source, Util.getFontColor(SP))
                        views.setImageViewBitmap(R.id.second_row_icon, result)

                        views.setTextViewText(R.id.next_event_date, Util.getNextAlarm(context))

                        val clockIntent = PendingIntent.getActivity(context, widgetID, Util.getClockIntent(context), 0)
                        views.setOnClickPendingIntent(R.id.next_event_date, clockIntent)
                    } else if (e.address != "" && SP.getInt(Constants.PREF_SECOND_ROW_INFORMATION, 0) == 1) {

                        val source = BitmapFactory.decodeResource(context.resources, R.drawable.ic_action_location);
                        val result = Util.changeBitmapColor(source, Util.getFontColor(SP))
                        views.setImageViewBitmap(R.id.second_row_icon, result)

                        views.setTextViewText(R.id.next_event_date, e.address)

                        val mapIntent = PendingIntent.getActivity(context, widgetID, Util.getGoogleMapsIntentFromAddress(context, e.address), 0)
                        views.setOnClickPendingIntent(R.id.next_event_date, mapIntent)
                    } else {
                        val source = BitmapFactory.decodeResource(context.resources, R.drawable.ic_action_calendar);
                        val result = Util.changeBitmapColor(source, Util.getFontColor(SP))
                        views.setImageViewBitmap(R.id.second_row_icon, result)

                        if (!e.allDay) {
                            var startHour = Constants.goodHourFormat.format(e.startDate)
                            var endHour = Constants.goodHourFormat.format(e.endDate)
                            if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) {
                                startHour = Constants.badHourFormat.format(e.startDate)
                                endHour = Constants.badHourFormat.format(e.endDate)
                            }
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

                            var multipleDay = ""
                            if (dayDiff > 0) {
                                multipleDay = String.format(" (+%s%s)", dayDiff, context.getString(R.string.day_char))
                            }

                            views.setTextViewText(R.id.next_event_date, String.format("%s - %s%s", startHour, endHour, multipleDay))
                        } else {
                            views.setTextViewText(R.id.next_event_date, dateStringValue)
                        }

                        if (SP.getBoolean(Constants.PREF_SHOW_NEXT_EVENT, false) && CalendarUtil.getEventsCount(context) > 1) {
                            val multipleIntent = PendingIntent.getBroadcast(context, widgetID, Intent(Constants.ACTION_GO_TO_NEXT_EVENT), 0)
                            views.setOnClickPendingIntent(R.id.next_event_date, multipleIntent)
                        } else {
                            views.setOnClickPendingIntent(R.id.next_event_date, pIntent)
                        }
                    }

                    views.setViewVisibility(R.id.empty_layout, View.GONE)
                    views.setViewVisibility(R.id.calendar_layout, View.VISIBLE)
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

                val i = Intent(context, OpenWeatherIntentReceiver::class.java)
                i.action = Constants.ACTION_OPEN_WEATHER_INTENT
                val weatherPIntent = PendingIntent.getBroadcast(context, widgetID, i, 0)

                views.setOnClickPendingIntent(R.id.weather, weatherPIntent)
                views.setOnClickPendingIntent(R.id.calendar_weather, weatherPIntent)
            } else {
                views.setViewVisibility(R.id.weather, View.GONE)
                views.setViewVisibility(R.id.calendar_weather, View.GONE)
            }
            return views
        }

        fun updateClockView(context: Context, views: RemoteViews, widgetID: Int): RemoteViews {
            val SP = PreferenceManager.getDefaultSharedPreferences(context)
            if (!SP.getBoolean(Constants.PREF_SHOW_CLOCK, false)) {
                views.setViewVisibility(R.id.time, View.GONE)
            } else {
                val now = Calendar.getInstance()
                if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) {
                    val textBadHour = SpannableString(Constants.badHourFormat.format(now.timeInMillis).replace(" ", ""))
                    textBadHour.setSpan(RelativeSizeSpan(0.4f), textBadHour.length - 2,
                            textBadHour.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)

                    views.setTextViewText(R.id.time, textBadHour)
                } else {
                    views.setTextViewText(R.id.time,  Constants.goodHourFormat.format(now.timeInMillis))
                }

                val clockPIntent = PendingIntent.getActivity(context, widgetID, Util.getClockIntent(context), 0)
                views.setOnClickPendingIntent(R.id.time, clockPIntent)
                views.setViewVisibility(R.id.time, View.VISIBLE)
            }

            return views
        }

        fun fixViewsMargin(context: Context, views: RemoteViews): RemoteViews {
            val SP = PreferenceManager.getDefaultSharedPreferences(context)
            views.setViewVisibility(R.id.bottom_divider_24, View.GONE)
            views.setViewVisibility(R.id.bottom_divider_16, View.GONE)
            views.setViewVisibility(R.id.bottom_divider_8, View.GONE)
            val eVisible = SP.getBoolean(Constants.PREF_SHOW_EVENTS, true) && Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)
            if (SP.getBoolean(Constants.PREF_SHOW_CLOCK, false)) {
                if (eVisible) {
                    views.setViewVisibility(R.id.bottom_divider_8, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.bottom_divider_24, View.VISIBLE)
                }
            } else {
                if (eVisible) {
                    views.setViewVisibility(R.id.bottom_divider_8, View.VISIBLE)
                }
            }
            return views
        }

        fun updateCalendarViewByLayout(context: Context, v: View): View {
            val SP = PreferenceManager.getDefaultSharedPreferences(context)
            val now = Calendar.getInstance()
            val calendarLayout = SP.getBoolean(Constants.PREF_SHOW_EVENTS, true) && Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)

            v.empty_layout.visibility = View.VISIBLE
            v.calendar_layout.visibility = View.GONE
            var dateStringValue: String = Util.getCapWordString(Constants.engDateFormat.format(now.time))
            if (SP.getBoolean(Constants.PREF_ITA_FORMAT_DATE, false)) {
                dateStringValue = Util.getCapWordString(Constants.itDateFormat.format(now.time))
            }

            v.empty_date.text = dateStringValue

            if (calendarLayout) {
                val e = CalendarUtil.getNextEvent(context)

                if (e.id != 0.toLong()) {
                    v.next_event.text = e.title

                    if (SP.getBoolean(Constants.PREF_SHOW_NEXT_EVENT, false) && CalendarUtil.getEventsCount(context) > 1) {
                        v.multiple_events.visibility = View.VISIBLE
                    } else {
                        v.multiple_events.visibility = View.GONE
                    }

                    if (SP.getBoolean(Constants.PREF_SHOW_DIFF_TIME, true)) {
                        v.next_event_difference_time.text = Util.getDifferenceText(context, now.timeInMillis, e.startDate)
                        v.next_event_difference_time.visibility = View.VISIBLE
                    } else {
                        v.next_event_difference_time.visibility = View.GONE
                    }

                    if (SP.getInt(Constants.PREF_SECOND_ROW_INFORMATION, 0) == 2 && Util.getNextAlarm(context) != null) {
                        v.second_row_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_alarm))
                        v.next_event_date.text = Util.getNextAlarm(context)
                    } else if (!e.address.equals("") && SP.getInt(Constants.PREF_SECOND_ROW_INFORMATION, 0) == 1) {
                        v.second_row_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_location))
                        v.next_event_date.text = e.address
                    } else {
                        v.second_row_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_calendar))
                        if (!e.allDay) {
                            var startHour = Constants.goodHourFormat.format(e.startDate)
                            var endHour = Constants.goodHourFormat.format(e.endDate)
                            if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) {
                                startHour = Constants.badHourFormat.format(e.startDate)
                                endHour = Constants.badHourFormat.format(e.endDate)
                            }
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
                            v.next_event_date.text = String.format("%s - %s%s", startHour, endHour, multipleDay)

                        } else {
                            v.next_event_date.text = dateStringValue
                        }
                    }

                    v.empty_layout.visibility = View.GONE
                    v.calendar_layout.visibility = View.VISIBLE
                }
            }

            v.empty_date.setTextColor(Util.getFontColor(SP))
            v.divider1.setTextColor(Util.getFontColor(SP))
            v.temp.setTextColor(Util.getFontColor(SP))
            v.next_event.setTextColor(Util.getFontColor(SP))
            v.next_event_difference_time.setTextColor(Util.getFontColor(SP))
            v.next_event_date.setTextColor(Util.getFontColor(SP))
            v.divider2.setTextColor(Util.getFontColor(SP))
            v.calendar_temp.setTextColor(Util.getFontColor(SP))
            v.second_row_icon.setColorFilter(Util.getFontColor(SP))
            v.time.setTextColor(Util.getFontColor(SP))
            v.multiple_events.setColorFilter(Util.getFontColor(SP))


            v.empty_date.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
            v.divider1.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
            v.temp.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
            v.next_event.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
            v.next_event_difference_time.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
            v.next_event_date.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
            v.divider2.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
            v.calendar_temp.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
            v.time.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_CLOCK_SIZE, 90f))

            v.second_row_icon.scaleX = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 18f
            v.second_row_icon.scaleY = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 18f

            v.weather_icon.scaleX = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 16f
            v.weather_icon.scaleY = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 16f

            v.empty_weather_icon.scaleX = SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f) / 24f
            v.empty_weather_icon.scaleY = SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f) / 24f

            v.multiple_events.scaleX = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 16f
            v.multiple_events.scaleY = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 16f

            val shadowRadius = when (SP.getInt(Constants.PREF_TEXT_SHADOW, 1)) {
                0 -> 0f
                1 -> 5f
                2 -> 5f
                else -> 5f
            }
            val shadowColor =  when (SP.getInt(Constants.PREF_TEXT_SHADOW, 1)) {
                0 -> Color.TRANSPARENT
                1 -> R.color.black_50
                2 -> Color.BLACK
                else -> R.color.black_50
            }
            val shadowDy =  when (SP.getInt(Constants.PREF_TEXT_SHADOW, 1)) {
                0 -> 0f
                1 -> 0f
                2 -> 1f
                else -> 0f
            }
            v.empty_date.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
            v.divider1.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
            v.temp.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
            v.next_event.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
            v.next_event_difference_time.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
            v.next_event_date.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
            v.divider2.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
            v.calendar_temp.setShadowLayer(shadowRadius, 0f, 0f, shadowColor)
            v.time.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)

            if (SP.getInt(Constants.PREF_CUSTOM_FONT, Constants.CUSTOM_FONT_PRODUCT_SANS) == Constants.CUSTOM_FONT_PRODUCT_SANS) {
                val product_sans: Typeface = Typeface.createFromAsset(context.assets, "fonts/product_sans_regular.ttf")
                v.empty_date.typeface = product_sans
                v.divider1.typeface = product_sans
                v.temp.typeface = product_sans
                v.next_event.typeface = product_sans
                v.next_event_difference_time.typeface = product_sans
                v.next_event_date.typeface = product_sans
                v.divider2.typeface = product_sans
                v.calendar_temp.typeface = product_sans
                v.time.typeface = product_sans
            }

            return v
        }

        fun updateLocationViewByLayout(context: Context, v: View): View {
            val SP = PreferenceManager.getDefaultSharedPreferences(context)
            val locationLayout = SP.getBoolean(Constants.PREF_SHOW_WEATHER, true)

            if (locationLayout && SP.contains(Constants.PREF_WEATHER_TEMP) && SP.contains(Constants.PREF_WEATHER_ICON)) {
                v.weather.visibility = View.VISIBLE
                v.calendar_weather.visibility = View.VISIBLE
                val currentTemp = String.format(Locale.getDefault(), "%.0f °%s", SP.getFloat(Constants.PREF_WEATHER_TEMP, 0f), SP.getString(Constants.PREF_WEATHER_REAL_TEMP_UNIT, "F"))


                v.weather_icon.visibility = View.VISIBLE
                v.empty_weather_icon.visibility = View.VISIBLE
                val icon: String = SP.getString(Constants.PREF_WEATHER_ICON, "")
                if (icon.equals("")) {
                    v.weather_icon.visibility = View.GONE
                    v.empty_weather_icon.visibility = View.GONE
                } else {
                    v.weather_icon.setImageResource(WeatherUtil.getWeatherIconResource(icon))
                    v.empty_weather_icon.setImageResource(WeatherUtil.getWeatherIconResource(icon))
                }

                v.temp.text = currentTemp
                v.calendar_temp.text = currentTemp
            } else {
                v.weather.visibility = View.GONE
                v.calendar_weather.visibility = View.GONE
            }
            return v
        }

        fun updateClockViewByLayout(context: Context, v: View): View {
            val SP = PreferenceManager.getDefaultSharedPreferences(context)
            if (!SP.getBoolean(Constants.PREF_SHOW_CLOCK, false)) {
                v.time.visibility = View.GONE
            } else {
                val now = Calendar.getInstance()
                if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) {
                    val textBadHour = SpannableString(Constants.badHourFormat.format(now.timeInMillis).replace(" ", ""))
                    textBadHour.setSpan(RelativeSizeSpan(0.4f), textBadHour.length - 2,
                            textBadHour.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

                    v.time.text = textBadHour
                } else {
                    v.time.text = Constants.goodHourFormat.format(now.timeInMillis)
                }
                v.time.visibility = View.VISIBLE
            }
            return v
        }
    }
}

