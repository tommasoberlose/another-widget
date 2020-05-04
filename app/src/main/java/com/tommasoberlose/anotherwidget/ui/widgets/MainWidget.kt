package com.tommasoberlose.anotherwidget.ui.widgets

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.*
import com.tommasoberlose.anotherwidget.receivers.NewCalendarEventReceiver
import com.tommasoberlose.anotherwidget.receivers.UpdatesReceiver
import com.tommasoberlose.anotherwidget.receivers.WeatherReceiver
import com.tommasoberlose.anotherwidget.receivers.WidgetClickListenerReceiver
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.convertSpToPixels
import com.tommasoberlose.anotherwidget.utils.getCapWordString
import com.tommasoberlose.anotherwidget.utils.toPixel
import kotlinx.android.synthetic.main.the_widget.view.*
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class MainWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle?) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onEnabled(context: Context) {
        CalendarHelper.updateEventList(context)
        WeatherReceiver.setUpdates(context)

        if (Preferences.showEvents) {
            CalendarHelper.setEventUpdatesAndroidN(context)
        } else {
            CalendarHelper.removeEventUpdatesAndroidN(context)
        }
    }

    override fun onDisabled(context: Context) {
        UpdatesReceiver.removeUpdates(context)
        WeatherReceiver.removeUpdates(context)
    }

    companion object {

        fun updateWidget(context: Context) {
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, MainWidget::class.java)
            val widgetIds = widgetManager.getAppWidgetIds(widgetComponent)
            val update = Intent(context, MainWidget::class.java)
            update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            update.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            context.sendBroadcast(update)
        }

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int) {
            val displayMetrics = Resources.getSystem().displayMetrics
            var height = 110.toPixel(context)
            val width = displayMetrics.widthPixels
            if (Preferences.showClock) {
                height += Preferences.clockTextSize.convertSpToPixels(context).toInt() + 16.toPixel(context)
            }
            if (Preferences.textMainSize > 30 && Preferences.textSecondSize > 22) {
                height += 24.toPixel(context)
            }

            generateWidgetView(context, appWidgetId, appWidgetManager, width - 16.toPixel(context))
        }

        private fun generateWidgetView(context: Context, appWidgetId: Int, appWidgetManager: AppWidgetManager, w: Int) {
            var views = RemoteViews(context.packageName, R.layout.the_widget_sans)

            val generatedView = generateWidgetView(context)
            views.setImageViewBitmap(R.id.bitmap_container, BitmapHelper.getBitmapFromView(generatedView, width = w - 32.toPixel(context)))

            // Clock
            views = updateClockView(context, views, appWidgetId)

            // Setup listener
            views = updateCalendarView(context, generatedView, views, appWidgetId)
            views = updateWeatherView(context, generatedView, views, appWidgetId)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun updateCalendarView(context: Context, v: View, views: RemoteViews, widgetID: Int): RemoteViews {
            val eventRepository = EventRepository(context)

            views.setImageViewBitmap(R.id.empty_date_rect, BitmapHelper.getBitmapFromView(v.empty_date, draw = false))

            views.setViewVisibility(R.id.empty_layout_rect, View.VISIBLE)
            views.setViewVisibility(R.id.calendar_layout_rect, View.GONE)
            views.setViewVisibility(R.id.second_row_rect, View.GONE)

            val calPIntent = PendingIntent.getActivity(context, widgetID, IntentHelper.getCalendarIntent(context), 0)
            views.setOnClickPendingIntent(R.id.empty_date_rect, calPIntent)

            val nextEvent = eventRepository.getNextEvent()
            val nextAlarm = AlarmHelper.getNextAlarm(context)

            if (Preferences.showEvents && context.checkGrantedPermission(Manifest.permission.READ_CALENDAR) && nextEvent != null) {
                if (Preferences.showNextEvent && eventRepository.getEventsCount() > 1) {
                    views.setImageViewBitmap(R.id.action_next_rect, BitmapHelper.getBitmapFromView(v.action_next, draw = false))
                    views.setViewVisibility(R.id.action_next_rect, View.VISIBLE)
                    views.setOnClickPendingIntent(R.id.action_next_rect, PendingIntent.getBroadcast(context, widgetID, Intent(context, NewCalendarEventReceiver::class.java).apply { action = Actions.ACTION_GO_TO_NEXT_EVENT }, 0))

                    views.setImageViewBitmap(R.id.action_previous_rect, BitmapHelper.getBitmapFromView(v.action_previous, draw = false))
                    views.setViewVisibility(R.id.action_previous_rect, View.VISIBLE)
                    views.setOnClickPendingIntent(R.id.action_previous_rect, PendingIntent.getBroadcast(context, widgetID, Intent(context, NewCalendarEventReceiver::class.java).apply { action = Actions.ACTION_GO_TO_PREVIOUS_EVENT }, 0))
                } else {
                    views.setViewVisibility(R.id.action_next_rect, View.GONE)
                    views.setViewVisibility(R.id.action_previous_rect, View.GONE)
                }

                val pIntent = PendingIntent.getActivity(context, widgetID, IntentHelper.getEventIntent(context, nextEvent), 0)
                views.setOnClickPendingIntent(R.id.next_event_rect, pIntent)
                views.setOnClickPendingIntent(R.id.next_event_difference_time_rect, pIntent)

                if (Preferences.showDiffTime && Calendar.getInstance().timeInMillis < (nextEvent.startDate - 1000 * 60 * 60)) {
                    views.setImageViewBitmap(R.id.next_event_difference_time_rect, BitmapHelper.getBitmapFromView(v.next_event_difference_time, draw = false))
                    views.setViewVisibility(R.id.next_event_difference_time_rect, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.next_event_difference_time_rect, View.GONE)
                }

                if (nextEvent.address != "" && Preferences.secondRowInformation == 1) {
                    val mapIntent = PendingIntent.getActivity(context, widgetID, IntentHelper.getGoogleMapsIntentFromAddress(context, nextEvent.address), 0)
                    views.setOnClickPendingIntent(R.id.second_row_rect, mapIntent)
                } else {
                    val pIntentDetail = PendingIntent.getActivity(context, widgetID, IntentHelper.getEventIntent(context, nextEvent, forceEventDetails = true), 0)
                    views.setOnClickPendingIntent(R.id.second_row_rect, pIntentDetail)
                }

                views.setImageViewBitmap(R.id.next_event_rect, BitmapHelper.getBitmapFromView(v.next_event, draw = false))

                views.setImageViewBitmap(R.id.second_row_rect, BitmapHelper.getBitmapFromView(v.second_row, draw = false))
                views.setViewVisibility(R.id.second_row_rect, View.VISIBLE)

                views.setViewVisibility(R.id.empty_layout_rect, View.GONE)
                views.setViewVisibility(R.id.calendar_layout_rect, View.VISIBLE)
            } else if (Preferences.showNextAlarm && nextAlarm != "") {
                val clockIntent = PendingIntent.getActivity(context, widgetID, IntentHelper.getClockIntent(context), 0)
                views.setOnClickPendingIntent(R.id.second_row_rect, clockIntent)

                views.setImageViewBitmap(R.id.next_event_rect, BitmapHelper.getBitmapFromView(v.next_event, draw = false))

                views.setImageViewBitmap(R.id.second_row_rect, BitmapHelper.getBitmapFromView(v.second_row, draw = false))
                views.setViewVisibility(R.id.second_row_rect, View.VISIBLE)

                views.setViewVisibility(R.id.empty_layout_rect, View.GONE)
                views.setViewVisibility(R.id.calendar_layout_rect, View.VISIBLE)
            }

            return views
        }

        private fun updateWeatherView(context: Context, v: View, views: RemoteViews, widgetID: Int): RemoteViews {

            if (Preferences.showWeather && Preferences.weatherIcon != "") {
                views.setViewVisibility(R.id.weather_rect, View.VISIBLE)
                views.setViewVisibility(R.id.calendar_weather_rect, View.VISIBLE)

                val i = Intent(context, WidgetClickListenerReceiver::class.java)
                i.action = Actions.ACTION_OPEN_WEATHER_INTENT
                val weatherPIntent = PendingIntent.getBroadcast(context, widgetID, i, 0)

                views.setOnClickPendingIntent(R.id.weather_rect, weatherPIntent)
                views.setOnClickPendingIntent(R.id.calendar_weather_rect, weatherPIntent)

                views.setImageViewBitmap(R.id.weather_rect, BitmapHelper.getBitmapFromView(v.weather, draw = false))

                views.setImageViewBitmap(R.id.calendar_weather_rect, BitmapHelper.getBitmapFromView(v.calendar_weather, draw = false))
            } else {
                views.setViewVisibility(R.id.weather_rect, View.GONE)
                views.setViewVisibility(R.id.calendar_weather_rect, View.GONE)
            }
            return views
        }

        private fun updateClockView(context: Context, views: RemoteViews, widgetID: Int): RemoteViews {
            if (!Preferences.showClock) {
                views.setViewVisibility(R.id.time, View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_none, View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_small, View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_medium, View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_large, View.GONE)
            } else {
                views.setTextColor(R.id.time, ColorHelper.getFontColor())
                views.setTextViewTextSize(R.id.time, TypedValue.COMPLEX_UNIT_SP, Preferences.clockTextSize.toPixel(context))
                val clockPIntent = PendingIntent.getActivity(context, widgetID, IntentHelper.getClockIntent(context), 0)
                views.setOnClickPendingIntent(R.id.time, clockPIntent)
                views.setViewVisibility(R.id.time, View.VISIBLE)

                views.setViewVisibility(R.id.clock_bottom_margin_none, if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.NONE.value)  View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_small, if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.SMALL.value)  View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_medium, if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.MEDIUM.value)  View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_large,  if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.LARGE.value)  View.VISIBLE else View.GONE)
            }

            return views
        }


        // Generates the widget bitmap from the view
        fun generateWidgetView(context: Context): View {
            val eventRepository = EventRepository(context)
            val v = View.inflate(context, R.layout.the_widget, null)

            val now = Calendar.getInstance()

            v.empty_layout.visibility = View.VISIBLE
            v.calendar_layout.visibility = View.GONE
            v.action_next.isVisible = false
            v.action_previous.isVisible = false

            v.empty_date.text = DateHelper.getDateText(context, now)

            val nextEvent = eventRepository.getNextEvent()
            val nextAlarm = AlarmHelper.getNextAlarm(context)

            if (Preferences.showEvents && context.checkGrantedPermission(Manifest.permission.READ_CALENDAR) && nextEvent != null) {
                // Multiple counter
                v.action_next.isVisible = Preferences.showNextEvent && eventRepository.getEventsCount() > 1
                v.action_previous.isVisible = Preferences.showNextEvent && eventRepository.getEventsCount() > 1

                v.next_event.text = nextEvent.title

                if (Preferences.showDiffTime && now.timeInMillis < (nextEvent.startDate - 1000 * 60 * 60)) {
                    v.next_event_difference_time.text = if (!nextEvent.allDay) {
                        SettingsStringHelper.getDifferenceText(
                            context,
                            now.timeInMillis,
                            nextEvent.startDate
                        )
                            .toLowerCase(Locale.getDefault())
                    } else {
                        SettingsStringHelper.getAllDayEventDifferenceText(context, now.timeInMillis, nextEvent.startDate).toLowerCase(Locale.getDefault())
                    }
                    v.next_event_difference_time.visibility = View.VISIBLE
                } else {
                    v.next_event_difference_time.visibility = View.GONE
                }

                if (nextEvent.address != "" && Preferences.secondRowInformation == 1) {
                    v.second_row_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.round_place))
                    v.next_event_date.text = nextEvent.address
                } else {
                    v.second_row_icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.round_today))
                    if (!nextEvent.allDay) {
                        val startHour = DateFormat.getTimeInstance(DateFormat.SHORT).format(nextEvent.startDate)
                        val endHour = DateFormat.getTimeInstance(DateFormat.SHORT).format(nextEvent.endDate)

                        var dayDiff = TimeUnit.MILLISECONDS.toDays(nextEvent.endDate - nextEvent.startDate)

                        val startCal = Calendar.getInstance()
                        startCal.timeInMillis = nextEvent.startDate

                        val endCal = Calendar.getInstance()
                        endCal.timeInMillis = nextEvent.endDate

                        if (startCal.get(Calendar.HOUR_OF_DAY) > endCal.get(Calendar.HOUR_OF_DAY)) {
                            dayDiff++
                        } else if (startCal.get(Calendar.HOUR_OF_DAY) == endCal.get(Calendar.HOUR_OF_DAY) && startCal.get(Calendar.MINUTE) >= endCal.get(Calendar.MINUTE)) {
                            dayDiff++
                        }
                        var multipleDay = ""
                        if (dayDiff > 0) {
                            multipleDay = String.format(" (+%s%s)", dayDiff, context.getString(R.string.day_char))
                        }
                        v.next_event_date.text = String.format("%s - %s%s", startHour, endHour, multipleDay)

                    } else {
                        val flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
                        v.next_event_date.text = DateUtils.formatDateTime(context, now.timeInMillis, flags).getCapWordString()
                    }
                }

                v.empty_layout.visibility = View.GONE
                v.calendar_layout.visibility = View.VISIBLE
            } else if (Preferences.showNextAlarm && nextAlarm != "") {
                v.second_row_icon.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.round_alarm
                    )
                )
                v.next_event.text = DateHelper.getDateText(context, now)
                v.next_event_date.text = AlarmHelper.getNextAlarm(context)
                v.empty_layout.visibility = View.GONE
                v.calendar_layout.visibility = View.VISIBLE
            }


            // Color
            listOf<TextView>(v.empty_date, v.divider1, v.temp, v.next_event, v.next_event_difference_time, v.next_event_date, v.divider2, v.calendar_temp).forEach {
                it.setTextColor(ColorHelper.getFontColor())
            }

            listOf<ImageView>(v.second_row_icon, v.action_next, v.action_previous).forEach {
                it.setColorFilter(ColorHelper.getFontColor())
            }

            // Text Size
            listOf<Pair<TextView, Float>>(
                v.empty_date to Preferences.textMainSize,
                v.divider1 to Preferences.textMainSize,
                v.temp to Preferences.textMainSize,
                v.next_event to Preferences.textMainSize,
                v.next_event_difference_time to Preferences.textMainSize,
                v.next_event_date to Preferences.textSecondSize,
                v.divider2 to Preferences.textSecondSize,
                v.calendar_temp to Preferences.textSecondSize
            ).forEach {
                it.first.setTextSize(TypedValue.COMPLEX_UNIT_SP, it.second)
            }

            // Icons scale
            v.second_row_icon.scaleX = Preferences.textSecondSize / 18f
            v.second_row_icon.scaleY = Preferences.textSecondSize / 18f

            v.weather_icon.scaleX = Preferences.textSecondSize / 16f
            v.weather_icon.scaleY = Preferences.textSecondSize / 16f

            v.empty_weather_icon.scaleX = Preferences.textMainSize / 20f
            v.empty_weather_icon.scaleY = Preferences.textMainSize / 20f

            v.action_next.scaleX = Preferences.textMainSize / 28f
            v.action_next.scaleY = Preferences.textMainSize / 28f

            v.action_previous.scaleX = Preferences.textMainSize / 28f
            v.action_previous.scaleY = Preferences.textMainSize / 28f


            // Shadows
            val shadowRadius = when (Preferences.textShadow) {
                0 -> 0f
                1 -> 5f
                2 -> 5f
                else -> 5f
            }
            val shadowColor =  when (Preferences.textShadow) {
                0 -> Color.TRANSPARENT
                1 -> R.color.black_50
                2 -> Color.BLACK
                else -> R.color.black_50
            }
            val shadowDy =  when (Preferences.textShadow) {
                0 -> 0f
                1 -> 0f
                2 -> 1f
                else -> 0f
            }

            listOf<TextView>(v.empty_date, v.divider1, v.temp, v.next_event, v.next_event_difference_time, v.next_event_date, v.divider2, v.calendar_temp).forEach {
                it.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
            }

            // Custom Font
            if (Preferences.customFont == Constants.CUSTOM_FONT_PRODUCT_SANS) {
                val productSans: Typeface = Typeface.createFromAsset(context.assets, "fonts/product_sans_regular.ttf")
                listOf<TextView>(v.empty_date, v.divider1, v.temp, v.next_event, v.next_event_difference_time, v.next_event_date, v.divider2, v.calendar_temp).forEach {
                    it.typeface = productSans
                }
            }

            // Weather
            if (Preferences.showWeather && Preferences.weatherIcon != "") {
                v.weather.visibility = View.VISIBLE
                v.calendar_weather.visibility = View.VISIBLE
                val currentTemp = String.format(Locale.getDefault(), "%.0f °%s", Preferences.weatherTemp, Preferences.weatherRealTempUnit)

                val icon: String = Preferences.weatherIcon
                if (icon == "") {
                    v.weather_icon.visibility = View.GONE
                    v.empty_weather_icon.visibility = View.GONE
                } else {
                    v.weather_icon.setImageResource(WeatherHelper.getWeatherIconResource(icon))
                    v.empty_weather_icon.setImageResource(WeatherHelper.getWeatherIconResource(icon))
                    v.weather_icon.visibility = View.VISIBLE
                    v.empty_weather_icon.visibility = View.VISIBLE
                }

                v.temp.text = currentTemp
                v.calendar_temp.text = currentTemp
            } else {
                v.weather.visibility = View.GONE
                v.calendar_weather.visibility = View.GONE
            }

            return v
        }
    }
}

