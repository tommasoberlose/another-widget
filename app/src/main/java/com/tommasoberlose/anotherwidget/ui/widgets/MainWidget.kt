package com.tommasoberlose.anotherwidget.ui.widgets

import android.Manifest
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.gson.Gson
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.TheWidgetBinding
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.*
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toIntValue
import com.tommasoberlose.anotherwidget.helpers.ImageHelper.applyShadow
import com.tommasoberlose.anotherwidget.receivers.*
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.convertDpToPixel
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import com.tommasoberlose.anotherwidget.utils.toPixel
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt


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
        MediaPlayerHelper.updatePlayingMediaInfo(context)

        if (Preferences.showEvents) {
            CalendarHelper.setEventUpdatesAndroidN(context)
        } else {
            CalendarHelper.removeEventUpdatesAndroidN(context)
        }
    }

    override fun onDisabled(context: Context) {
        if (getWidgetCount(context) == 0) {
            UpdatesReceiver.removeUpdates(context)
            WeatherReceiver.removeUpdates(context)
        }
    }

    companion object {

        fun updateWidget(context: Context) {
            context.sendBroadcast(IntentHelper.getWidgetUpdateIntent(context))
        }

        fun getWidgetCount(context: Context): Int {
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, MainWidget::class.java)
            return widgetManager.getAppWidgetIds(widgetComponent).size
        }

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int) {
            val displayMetrics = Resources.getSystem().displayMetrics
            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels

            val dimensions = WidgetHelper.WidgetSizeProvider(context, appWidgetManager).getWidgetsSize(appWidgetId)

            WidgetHelper.runWithCustomTypeface(context) {
                generateWidgetView(context, appWidgetId, appWidgetManager, min(dimensions.first - 8.toPixel(context), min(width, height) - 16.toPixel(context)), it)
            }
        }

        private fun generateWidgetView(context: Context, appWidgetId: Int, appWidgetManager: AppWidgetManager, w: Int, typeface: Typeface? = null) {

            var views = RemoteViews(context.packageName, R.layout.the_widget_sans)

            try {
                // Background
                views.setInt(
                    R.id.widget_shape_background,
                    "setColorFilter",
                    ColorHelper.getBackgroundColorRgb(context.isDarkTheme())
                )
                views.setInt(
                    R.id.widget_shape_background,
                    "setImageAlpha",
                    ColorHelper.getBackgroundAlpha(context.isDarkTheme())
                )
                val refreshIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    IntentHelper.getWidgetUpdateIntent(context),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widget_shape_background, refreshIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
                CrashlyticsReceiver.sendCrash(context, ex)
            }

            // Clock
            views = updateClockView(context, views, appWidgetId)

            // Setup listener
            try {

                val generatedBinding = generateWidgetView(context, typeface)
                views.setImageViewBitmap(
                    R.id.bitmap_container,
                    BitmapHelper.getBitmapFromView(generatedBinding.root, width = w)
                )
                views = updateCalendarView(context, generatedBinding, views, appWidgetId)
                views = updateWeatherView(context, generatedBinding, views, appWidgetId)
            } catch (ex: Exception) {
                ex.printStackTrace()
                CrashlyticsReceiver.sendCrash(context, ex)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun updateCalendarView(context: Context, bindingView: TheWidgetBinding, views: RemoteViews, widgetID: Int): RemoteViews {
            val eventRepository = EventRepository(context)
            try {
                views.setImageViewBitmap(
                    R.id.empty_date_rect,
                    BitmapHelper.getBitmapFromView(bindingView.emptyDate, draw = false)
                )

                views.setViewVisibility(R.id.empty_layout_rect, View.VISIBLE)
                views.setViewVisibility(R.id.calendar_layout_rect, View.GONE)
                views.setViewVisibility(R.id.second_row_rect, View.GONE)
                views.setViewVisibility(R.id.next_event_difference_time_rect, View.GONE)

                val calPIntent = PendingIntent.getActivity(
                    context,
                    widgetID,
                    IntentHelper.getCalendarIntent(context),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.empty_date_rect, calPIntent)

                val nextEvent = eventRepository.getNextEvent()
                val nextAlarm = AlarmHelper.getNextAlarm(context)

                if (Preferences.showEvents && context.checkGrantedPermission(Manifest.permission.READ_CALENDAR) && nextEvent != null && !Preferences.showEventsAsGlanceProvider) {
                    if (Preferences.showNextEvent && eventRepository.getEventsCount() > 1) {
                        views.setImageViewBitmap(
                            R.id.action_next_rect,
                            BitmapHelper.getBitmapFromView(bindingView.actionNext, draw = false)
                        )
                        views.setViewVisibility(R.id.action_next_rect, View.VISIBLE)
                        views.setOnClickPendingIntent(
                            R.id.action_next_rect,
                            PendingIntent.getBroadcast(
                                context,
                                widgetID,
                                Intent(
                                    context,
                                    NewCalendarEventReceiver::class.java
                                ).apply { action = Actions.ACTION_GO_TO_NEXT_EVENT },
                                PendingIntent.FLAG_UPDATE_CURRENT
                            )
                        )

                        views.setImageViewBitmap(
                            R.id.action_previous_rect,
                            BitmapHelper.getBitmapFromView(bindingView.actionPrevious, draw = false)
                        )
                        views.setViewVisibility(R.id.action_previous_rect, View.VISIBLE)
                        views.setOnClickPendingIntent(
                            R.id.action_previous_rect,
                            PendingIntent.getBroadcast(
                                context,
                                widgetID,
                                Intent(
                                    context,
                                    NewCalendarEventReceiver::class.java
                                ).apply { action = Actions.ACTION_GO_TO_PREVIOUS_EVENT },
                                PendingIntent.FLAG_UPDATE_CURRENT
                            )
                        )
                    } else {
                        views.setViewVisibility(R.id.action_next_rect, View.GONE)
                        views.setViewVisibility(R.id.action_previous_rect, View.GONE)
                    }

                    val pIntent = PendingIntent.getActivity(
                        context,
                        widgetID,
                        IntentHelper.getEventIntent(context, nextEvent),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    views.setOnClickPendingIntent(R.id.next_event_rect, pIntent)
                    views.setOnClickPendingIntent(R.id.next_event_difference_time_rect, pIntent)

                    if (Preferences.showDiffTime && Calendar.getInstance().timeInMillis < (nextEvent.startDate - 1000 * 60 * 60)) {
                        views.setImageViewBitmap(
                            R.id.next_event_difference_time_rect,
                            BitmapHelper.getBitmapFromView(
                                bindingView.nextEventDifferenceTime,
                                draw = false
                            )
                        )
                        views.setViewVisibility(R.id.next_event_difference_time_rect, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.next_event_difference_time_rect, View.GONE)
                    }

                    if (nextEvent.address != "" && Preferences.secondRowInformation == 1) {
                        val mapIntent = PendingIntent.getActivity(
                            context,
                            widgetID,
                            IntentHelper.getGoogleMapsIntentFromAddress(context, nextEvent.address),
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        views.setOnClickPendingIntent(R.id.second_row_rect, mapIntent)
                    } else {
                        val pIntentDetail = PendingIntent.getActivity(
                            context,
                            widgetID,
                            IntentHelper.getEventIntent(
                                context,
                                nextEvent,
                                forceEventDetails = true
                            ),
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        views.setOnClickPendingIntent(R.id.second_row_rect, pIntentDetail)
                    }

                    views.setImageViewBitmap(
                        R.id.next_event_rect,
                        BitmapHelper.getBitmapFromView(bindingView.nextEvent, draw = false)
                    )

                    views.setImageViewBitmap(
                        R.id.second_row_rect,
                        BitmapHelper.getBitmapFromView(bindingView.secondRow, draw = false)
                    )
                    views.setViewVisibility(R.id.second_row_rect, View.VISIBLE)

                    views.setViewVisibility(R.id.empty_layout_rect, View.GONE)
                    views.setViewVisibility(R.id.calendar_layout_rect, View.VISIBLE)

                    views.setViewVisibility(
                        R.id.second_row_top_margin_small_sans,
                        if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.SMALL.value) View.VISIBLE else View.GONE
                    )
                    views.setViewVisibility(
                        R.id.second_row_top_margin_medium_sans,
                        if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.MEDIUM.value) View.VISIBLE else View.GONE
                    )
                    views.setViewVisibility(
                        R.id.second_row_top_margin_large_sans,
                        if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.LARGE.value) View.VISIBLE else View.GONE
                    )
                } else if (GlanceProviderHelper.showGlanceProviders(context) && bindingView.calendarLayout.isVisible) {
                    var showSomething = false
                    loop@ for (provider:Constants.GlanceProviderId in GlanceProviderHelper.getGlanceProviders(context)) {
                        when (provider) {
                            Constants.GlanceProviderId.PLAYING_SONG -> {
                                if (MediaPlayerHelper.isSomeonePlaying(context)) {
                                    val musicIntent = PendingIntent.getActivity(
                                        context,
                                        widgetID,
                                        IntentHelper.getMusicIntent(context),
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    views.setOnClickPendingIntent(R.id.second_row_rect, musicIntent)
                                    showSomething = true
                                    break@loop
                                }
                            }
                            Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> {
                                if (Preferences.showNextAlarm && nextAlarm != "") {
                                    val alarmIntent = PendingIntent.getActivity(
                                        context,
                                        widgetID,
                                        IntentHelper.getClockIntent(context),
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    views.setOnClickPendingIntent(R.id.second_row_rect, alarmIntent)
                                    showSomething = true
                                    break@loop
                                }
                            }
                            Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
                                if (Preferences.showBatteryCharging) {
                                    BatteryHelper.updateBatteryInfo(context)
                                    if (Preferences.isCharging || Preferences.isBatteryLevelLow) {
                                        val batteryIntent = PendingIntent.getActivity(
                                            context,
                                            widgetID,
                                            IntentHelper.getBatteryIntent(),
                                            PendingIntent.FLAG_UPDATE_CURRENT
                                        )
                                        views.setOnClickPendingIntent(R.id.second_row_rect, batteryIntent)
                                        showSomething = true
                                        break@loop
                                    }
                                }
                            }
                            Constants.GlanceProviderId.CUSTOM_INFO -> {
                                if (Preferences.customNotes.isNotEmpty()) {
                                    break@loop
                                }
                            }
                            Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> {
                                if (Preferences.showDailySteps && Preferences.googleFitSteps > 0) {
                                    val fitIntent = PendingIntent.getActivity(
                                        context,
                                        widgetID,
                                        IntentHelper.getFitIntent(context),
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    views.setOnClickPendingIntent(R.id.second_row_rect, fitIntent)
                                    showSomething = true
                                    break@loop
                                }
                            }
                            Constants.GlanceProviderId.NOTIFICATIONS -> {
                                if (Preferences.showNotifications && ActiveNotificationsHelper.showLastNotification()) {
                                    try {
                                        if (Preferences.lastNotificationIcon != 0) {
                                            val remotePackageContext = context.createPackageContext(Preferences.lastNotificationPackage, 0)
                                            ContextCompat.getDrawable(
                                                remotePackageContext,
                                                Preferences.lastNotificationIcon)
                                        }
                                        val notificationIntent = PendingIntent.getActivity(
                                            context,
                                            widgetID,
                                            IntentHelper.getNotificationIntent(context),
                                            PendingIntent.FLAG_UPDATE_CURRENT
                                        )
                                        views.setOnClickPendingIntent(
                                            R.id.second_row_rect,
                                            notificationIntent
                                        )
                                        showSomething = true
                                        break@loop
                                    } catch (ex: Exception) {}
                                }
                            }
                            Constants.GlanceProviderId.GREETINGS -> {
                                if (Preferences.showGreetings && GreetingsHelper.showGreetings() && GreetingsHelper.getRandomString(context).isNotBlank()) {
                                    showSomething = true
                                    break@loop
                                }
                            }
                            Constants.GlanceProviderId.EVENTS -> {
                                if (Preferences.showEventsAsGlanceProvider&& Preferences.showEvents && context.checkGrantedPermission(Manifest.permission.READ_CALENDAR) && nextEvent != null) {
                                    val pIntentDetail = PendingIntent.getActivity(
                                        context,
                                        widgetID,
                                        IntentHelper.getEventIntent(
                                            context,
                                            nextEvent,
                                            forceEventDetails = true
                                        ),
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    views.setOnClickPendingIntent(
                                        R.id.second_row_rect,
                                        pIntentDetail
                                    )
                                    showSomething = true
                                    break@loop
                                }
                            }
                        }
                    }


                    if (showSomething) {
                        views.setImageViewBitmap(
                            R.id.next_event_rect,
                            BitmapHelper.getBitmapFromView(bindingView.nextEvent, draw = false)
                        )

                        views.setImageViewBitmap(
                            R.id.second_row_rect,
                            BitmapHelper.getBitmapFromView(bindingView.secondRow, draw = false)
                        )

                        views.setViewVisibility(R.id.second_row_rect, View.VISIBLE)
                        views.setViewVisibility(R.id.empty_layout_rect, View.GONE)
                        views.setViewVisibility(R.id.calendar_layout_rect, View.VISIBLE)
                        views.setOnClickPendingIntent(R.id.next_event_rect, calPIntent)


                        views.setViewVisibility(
                            R.id.second_row_top_margin_small_sans,
                            if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.SMALL.value) View.VISIBLE else View.GONE
                        )
                        views.setViewVisibility(
                            R.id.second_row_top_margin_medium_sans,
                            if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.MEDIUM.value) View.VISIBLE else View.GONE
                        )
                        views.setViewVisibility(
                            R.id.second_row_top_margin_large_sans,
                            if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.LARGE.value) View.VISIBLE else View.GONE
                        )
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                CrashlyticsReceiver.sendCrash(context, ex)
            } finally {
                eventRepository.close()
            }

            return views
        }

        private fun updateWeatherView(context: Context, bindingView: TheWidgetBinding, views: RemoteViews, widgetID: Int): RemoteViews {
            try {
                if (Preferences.showWeather && Preferences.weatherIcon != "") {
                    views.setViewVisibility(R.id.weather_rect, View.VISIBLE)
                    views.setViewVisibility(R.id.calendar_weather_rect, View.VISIBLE)
                    views.setViewVisibility(R.id.special_weather_rect, View.VISIBLE)

                    val i = Intent(context, WidgetClickListenerReceiver::class.java)
                    i.action = Actions.ACTION_OPEN_WEATHER_INTENT
                    val weatherPIntent = PendingIntent.getBroadcast(context, widgetID, i, 0)

                    views.setOnClickPendingIntent(R.id.weather_rect, weatherPIntent)
                    views.setOnClickPendingIntent(R.id.calendar_weather_rect, weatherPIntent)
                    views.setOnClickPendingIntent(R.id.special_weather_rect, weatherPIntent)

                    views.setImageViewBitmap(
                        R.id.weather_rect,
                        BitmapHelper.getBitmapFromView(bindingView.weather, draw = false)
                    )

                    views.setImageViewBitmap(
                        R.id.calendar_weather_rect,
                        BitmapHelper.getBitmapFromView(bindingView.calendarWeather, draw = false)
                    )

                    views.setImageViewBitmap(
                        R.id.special_weather_rect,
                        BitmapHelper.getBitmapFromView(bindingView.calendarWeather, draw = false)
                    )

                    if (GlanceProviderHelper.showGlanceProviders(context)) {
                        views.setViewVisibility(R.id.calendar_weather_rect, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.special_weather_rect, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.weather_rect, View.GONE)
                    views.setViewVisibility(R.id.calendar_weather_rect, View.GONE)
                    views.setViewVisibility(R.id.special_weather_rect, View.GONE)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                CrashlyticsReceiver.sendCrash(context, ex)
            }
            return views
        }

        private fun updateClockView(context: Context, views: RemoteViews, widgetID: Int): RemoteViews {
            try {
                if (!Preferences.showClock) {
                    views.setViewVisibility(R.id.time, View.GONE)
                    views.setViewVisibility(R.id.time_am_pm, View.GONE)
                    views.setViewVisibility(R.id.clock_bottom_margin_none, View.GONE)
                    views.setViewVisibility(R.id.clock_bottom_margin_small, View.GONE)
                    views.setViewVisibility(R.id.clock_bottom_margin_medium, View.GONE)
                    views.setViewVisibility(R.id.clock_bottom_margin_large, View.GONE)
                } else {
                    views.setTextColor(R.id.time, ColorHelper.getClockFontColor(context.isDarkTheme()))
                    views.setTextColor(R.id.time_am_pm, ColorHelper.getClockFontColor(context.isDarkTheme()))
                    views.setTextViewTextSize(
                        R.id.time,
                        TypedValue.COMPLEX_UNIT_SP,
                        Preferences.clockTextSize.toPixel(context)
                    )
                    views.setTextViewTextSize(
                        R.id.time_am_pm,
                        TypedValue.COMPLEX_UNIT_SP,
                        Preferences.clockTextSize.toPixel(context) / 5 * 2
                    )
                    val clockPIntent = PendingIntent.getActivity(
                        context,
                        widgetID,
                        IntentHelper.getClockIntent(context),
                        0
                    )
                    views.setOnClickPendingIntent(R.id.time, clockPIntent)
                    views.setOnClickPendingIntent(R.id.time_am_pm, clockPIntent)
                    views.setViewVisibility(R.id.time, View.VISIBLE)
                    views.setViewVisibility(R.id.time_am_pm, if (Preferences.showAMPMIndicator) View.VISIBLE else View.GONE)

                    views.setViewVisibility(
                        R.id.clock_bottom_margin_none,
                        if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.NONE.value) View.VISIBLE else View.GONE
                    )
                    views.setViewVisibility(
                        R.id.clock_bottom_margin_small,
                        if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.SMALL.value) View.VISIBLE else View.GONE
                    )
                    views.setViewVisibility(
                        R.id.clock_bottom_margin_medium,
                        if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.MEDIUM.value) View.VISIBLE else View.GONE
                    )
                    views.setViewVisibility(
                        R.id.clock_bottom_margin_large,
                        if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.LARGE.value) View.VISIBLE else View.GONE
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                CrashlyticsReceiver.sendCrash(context, ex)
            }

            return views
        }


        // Generates the widget bitmap from the view
        fun generateWidgetView(context: Context, typeface: Typeface? = null): TheWidgetBinding {
            val eventRepository = EventRepository(context)
            val bindingView = TheWidgetBinding.inflate(LayoutInflater.from(context))

            bindingView.loader.isVisible = false

            val now = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            bindingView.emptyLayout.visibility = View.VISIBLE
            bindingView.calendarLayout.visibility = View.GONE
            bindingView.nextEventDifferenceTime.visibility = View.GONE
            bindingView.actionNext.isVisible = false
            bindingView.actionPrevious.isVisible = false

            bindingView.emptyDate.text = DateHelper.getDateText(context, now)

            val nextEvent = eventRepository.getNextEvent()
            val nextAlarm = AlarmHelper.getNextAlarm(context)

            if (Preferences.showEvents && context.checkGrantedPermission(Manifest.permission.READ_CALENDAR) && nextEvent != null && !Preferences.showEventsAsGlanceProvider) {
                // Multiple counter
                bindingView.actionNext.isVisible =
                    Preferences.showNextEvent && eventRepository.getEventsCount() > 1
                bindingView.actionPrevious.isVisible =
                    Preferences.showNextEvent && eventRepository.getEventsCount() > 1

                bindingView.nextEvent.text = nextEvent.title

                if (Preferences.showDiffTime && now.timeInMillis < nextEvent.startDate) {
                    bindingView.nextEventDifferenceTime.text = if (!nextEvent.allDay) {
                        SettingsStringHelper.getDifferenceText(
                            context,
                            now.timeInMillis,
                            nextEvent.startDate
                        )
                            .toLowerCase(Locale.getDefault())
                    } else {
                        SettingsStringHelper.getAllDayEventDifferenceText(
                            context,
                            now.timeInMillis,
                            nextEvent.startDate
                        ).toLowerCase(Locale.getDefault())
                    }
                    bindingView.nextEventDifferenceTime.visibility = View.VISIBLE
                } else {
                    bindingView.nextEventDifferenceTime.visibility = View.GONE
                }

                if (nextEvent.address != "" && Preferences.secondRowInformation == 1) {
                    bindingView.secondRowIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.round_place_24
                        )
                    )
                    bindingView.nextEventDate.text = nextEvent.address
                } else {
                    bindingView.secondRowIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.round_today_24
                        )
                    )
                    if (!nextEvent.allDay) {
                        val startHour =
                            DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
                                .format(nextEvent.startDate)
                        val endHour =
                            DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
                                .format(nextEvent.endDate)

                        var dayDiff =
                            TimeUnit.MILLISECONDS.toDays(nextEvent.endDate - nextEvent.startDate)

                        val startCal = Calendar.getInstance()
                        startCal.timeInMillis = nextEvent.startDate

                        val endCal = Calendar.getInstance()
                        endCal.timeInMillis = nextEvent.endDate

                        if (startCal.get(Calendar.HOUR_OF_DAY) > endCal.get(Calendar.HOUR_OF_DAY)) {
                            dayDiff++
                        } else if (startCal.get(Calendar.HOUR_OF_DAY) == endCal.get(Calendar.HOUR_OF_DAY) && startCal.get(
                                Calendar.MINUTE
                            ) > endCal.get(Calendar.MINUTE)
                        ) {
                            dayDiff++
                        }
                        var multipleDay = ""
                        if (dayDiff > 0) {
                            multipleDay = String.format(
                                " (+%s%s)",
                                dayDiff,
                                context.getString(R.string.day_char)
                            )
                        }

                        if (nextEvent.startDate != nextEvent.endDate) {
                            bindingView.nextEventDate.text =
                                String.format("%s - %s%s", startHour, endHour, multipleDay)
                        } else {
                            bindingView.nextEventDate.text =
                                String.format("%s", startHour)
                        }

                    } else {
                        val flags: Int =
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_ABBREV_MONTH
                        val start = Calendar.getInstance().apply { timeInMillis = nextEvent.startDate }

                        bindingView.nextEventDate.text = if (now.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)) {
                            DateUtils.formatDateTime(context, nextEvent.startDate, flags)
                        } else if (now.get(Calendar.DAY_OF_YEAR) > start.get(Calendar.DAY_OF_YEAR) || now.get(Calendar.YEAR) > start.get(Calendar.YEAR)) {
                            DateUtils.formatDateTime(context, now.timeInMillis, flags)
                        } else {
                            DateUtils.formatDateTime(context, nextEvent.startDate, flags)
                        }
                    }
                }

                bindingView.emptyLayout.visibility = View.GONE
                bindingView.calendarLayout.visibility = View.VISIBLE

                bindingView.secondRowTopMarginSmall.visibility =
                    if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.SMALL.value) View.VISIBLE else View.GONE
                bindingView.secondRowTopMarginMedium.visibility =
                    if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.MEDIUM.value) View.VISIBLE else View.GONE
                bindingView.secondRowTopMarginLarge.visibility =
                    if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.LARGE.value) View.VISIBLE else View.GONE
            } else if (GlanceProviderHelper.showGlanceProviders(context)) {
                bindingView.secondRowIcon.isVisible = true
                var showSomething = false
                loop@ for (provider: Constants.GlanceProviderId in GlanceProviderHelper.getGlanceProviders(
                    context
                )) {
                    when (provider) {
                        Constants.GlanceProviderId.PLAYING_SONG -> {
                            if (MediaPlayerHelper.isSomeonePlaying(context)) {
                                bindingView.secondRowIcon.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context,
                                        R.drawable.round_music_note_24
                                    )
                                )
                                bindingView.nextEventDate.text = MediaPlayerHelper.getMediaInfo()
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> {
                            if (Preferences.showNextAlarm && nextAlarm != "") {
                                bindingView.secondRowIcon.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context,
                                        R.drawable.round_alarm_24
                                    )
                                )
                                bindingView.nextEventDate.text = AlarmHelper.getNextAlarm(context)
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
                            if (Preferences.showBatteryCharging) {
                                BatteryHelper.updateBatteryInfo(context)
                                if (Preferences.isCharging) {
                                    bindingView.secondRowIcon.isVisible = false
                                    val batteryLevel = BatteryHelper.getBatteryLevel(context)
                                    if (batteryLevel != 100) {
                                        bindingView.nextEventDate.text = context.getString(R.string.charging)
                                    } else {
                                        bindingView.nextEventDate.text =
                                            context.getString(R.string.charged)
                                    }
                                    showSomething = true
                                    break@loop
                                } else if (Preferences.isBatteryLevelLow) {
                                    bindingView.secondRowIcon.isVisible = false
                                    bindingView.nextEventDate.text =
                                        context.getString(R.string.battery_low_warning)
                                    showSomething = true
                                    break@loop
                                }
                            }
                        }
                        Constants.GlanceProviderId.CUSTOM_INFO -> {
                            if (Preferences.customNotes.isNotEmpty()) {
                                bindingView.secondRowIcon.isVisible = false
                                bindingView.nextEventDate.text = Preferences.customNotes
                                bindingView.nextEventDate.maxLines = 2
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> {
                            if (Preferences.showDailySteps && Preferences.googleFitSteps > 0) {
                                bindingView.secondRowIcon.isVisible = false
                                bindingView.nextEventDate.text =
                                    context.getString(R.string.daily_steps_counter)
                                        .format(Preferences.googleFitSteps)
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.NOTIFICATIONS -> {
                            if (Preferences.showNotifications && ActiveNotificationsHelper.showLastNotification()) {
                                try {
                                    if (Preferences.lastNotificationIcon != 0) {
                                        val remotePackageContext = context.createPackageContext(Preferences.lastNotificationPackage, 0)
                                        val icon = ContextCompat.getDrawable(remotePackageContext,
                                            Preferences.lastNotificationIcon)
                                        bindingView.secondRowIcon.isVisible = true
                                        bindingView.secondRowIcon.setImageDrawable(icon)
                                    } else {
                                        bindingView.secondRowIcon.isVisible = false
                                    }
                                    bindingView.nextEventDate.text = Preferences.lastNotificationTitle
                                    showSomething = true
                                    break@loop
                                } catch (ex: Exception) {}
                            }
                        }
                        Constants.GlanceProviderId.GREETINGS -> {
                            val greetingsText = GreetingsHelper.getRandomString(context)
                            if (Preferences.showGreetings && GreetingsHelper.showGreetings() && greetingsText.isNotBlank()) {
                                bindingView.nextEventDate.text = greetingsText
                                bindingView.nextEventDate.maxLines = 2
                                bindingView.secondRowIcon.isVisible = false
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.EVENTS -> {
                            if (Preferences.showEventsAsGlanceProvider && Preferences.showEvents && context.checkGrantedPermission(Manifest.permission.READ_CALENDAR) && nextEvent != null) {
                                bindingView.nextEventDate.text = context.getString(R.string.events_glance_provider_format).format(nextEvent.title, if (Preferences.showDiffTime && now.timeInMillis < nextEvent.startDate) {
                                     if (!nextEvent.allDay) {
                                        SettingsStringHelper.getDifferenceText(
                                            context,
                                            now.timeInMillis,
                                            nextEvent.startDate
                                        )
                                            .toLowerCase(Locale.getDefault())
                                    } else {
                                        SettingsStringHelper.getAllDayEventDifferenceText(
                                            context,
                                            now.timeInMillis,
                                            nextEvent.startDate
                                        ).toLowerCase(Locale.getDefault())
                                    }
                                } else "").trimEnd()
                                bindingView.secondRowIcon.isVisible = true
                                bindingView.secondRowIcon.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context,
                                        R.drawable.round_today_24
                                    )
                                )
                                showSomething = true
                                break@loop
                            }
                        }
                    }
                }

                if (showSomething) {
                    bindingView.nextEvent.text = DateHelper.getDateText(context, now)
                    bindingView.emptyLayout.visibility = View.GONE
                    bindingView.calendarLayout.visibility = View.VISIBLE

                    bindingView.secondRowTopMarginSmall.visibility =
                        if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.SMALL.value) View.VISIBLE else View.GONE
                    bindingView.secondRowTopMarginMedium.visibility =
                        if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.MEDIUM.value) View.VISIBLE else View.GONE
                    bindingView.secondRowTopMarginLarge.visibility =
                        if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.LARGE.value) View.VISIBLE else View.GONE
                } else {
                    bindingView.secondRowIcon.isVisible = false
                }
            }


            // Color
            listOf<TextView>(
                bindingView.emptyDate,
                bindingView.divider1,
                bindingView.temp,
                bindingView.nextEvent,
                bindingView.nextEventDifferenceTime,
                bindingView.divider3,
                bindingView.specialTemp
            ).forEach {
                it.setTextColor(ColorHelper.getFontColor(context.applicationContext.isDarkTheme()))
            }

            if (Preferences.weatherIconPack != Constants.WeatherIconPack.MINIMAL.value) {
                listOf<ImageView>(bindingView.actionNext, bindingView.actionPrevious)
            } else {
                listOf<ImageView>(
                    bindingView.actionNext,
                    bindingView.actionPrevious,
                    bindingView.emptyWeatherIcon,
                    bindingView.specialWeatherIcon
                )
            }.forEach {
                it.setColorFilter(ColorHelper.getFontColorRgb(context.applicationContext.isDarkTheme()))
                it.alpha =
                    (if (context.isDarkTheme()) Preferences.textGlobalAlphaDark.toIntValue()
                        .toFloat() else Preferences.textGlobalAlpha.toIntValue()
                        .toFloat()) / 100
            }

            listOf<TextView>(bindingView.nextEventDate, bindingView.divider2, bindingView.calendarTemp).forEach {
                it.setTextColor(ColorHelper.getSecondaryFontColor(context.applicationContext.isDarkTheme()))
            }

            if (Preferences.weatherIconPack != Constants.WeatherIconPack.MINIMAL.value) {
                listOf<ImageView>(bindingView.secondRowIcon, bindingView.secondRowIconShadow)
            } else {
                listOf<ImageView>(bindingView.secondRowIcon, bindingView.weatherIcon, bindingView.secondRowIconShadow)
            }.forEach {
                it.setColorFilter(ColorHelper.getSecondaryFontColorRgb(context.applicationContext.isDarkTheme()))
                it.alpha =
                    (if (context.isDarkTheme()) Preferences.textSecondaryAlphaDark.toIntValue()
                        .toFloat() else Preferences.textSecondaryAlpha.toIntValue()
                        .toFloat()) / 100
            }

            // Text Size
            listOf<Pair<TextView, Float>>(
                bindingView.emptyDate to Preferences.textMainSize,
                bindingView.divider1 to (Preferences.textMainSize - 2),
                bindingView.temp to Preferences.textMainSize,
                bindingView.nextEvent to Preferences.textMainSize,
                bindingView.nextEventDifferenceTime to Preferences.textMainSize,
                bindingView.nextEventDate to Preferences.textSecondSize,
                bindingView.divider2 to (Preferences.textSecondSize - 2),
                bindingView.calendarTemp to Preferences.textSecondSize,
                bindingView.divider3 to (Preferences.textMainSize - 2),
                bindingView.specialTemp to Preferences.textMainSize
            ).forEach {
                it.first.setTextSize(TypedValue.COMPLEX_UNIT_SP, it.second)
            }

            // Icons scale
            bindingView.secondRowIcon.scaleX = Preferences.textSecondSize / 18f
            bindingView.secondRowIcon.scaleY = Preferences.textSecondSize / 18f

            bindingView.weatherIcon.scaleX = Preferences.textSecondSize / 14f
            bindingView.weatherIcon.scaleY = Preferences.textSecondSize / 14f

            bindingView.emptyWeatherIcon.scaleX = Preferences.textMainSize / 18f
            bindingView.emptyWeatherIcon.scaleY = Preferences.textMainSize / 18f

            bindingView.actionNext.scaleX = Preferences.textMainSize / 28f
            bindingView.actionNext.scaleY = Preferences.textMainSize / 28f

            bindingView.actionPrevious.scaleX = Preferences.textMainSize / 28f
            bindingView.actionPrevious.scaleY = Preferences.textMainSize / 28f

            bindingView.specialWeatherIcon.scaleX = Preferences.textMainSize / 18f
            bindingView.specialWeatherIcon.scaleY = Preferences.textMainSize / 18f


            // Shadows
            val shadowRadius =
                when (if (context.isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow) {
                    0 -> 0f
                    1 -> 5f
                    2 -> 5f
                    else -> 5f
                }
            val shadowColor =
                when (if (context.isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow) {
                    0 -> Color.TRANSPARENT
                    1 -> R.color.black_50
                    2 -> Color.BLACK
                    else -> R.color.black_50
                }
            val shadowDy =
                when (if (context.isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow) {
                    0 -> 0f
                    1 -> 0f
                    2 -> 1f
                    else -> 0f
                }

            listOf<TextView>(
                bindingView.emptyDate,
                bindingView.divider1,
                bindingView.temp,
                bindingView.nextEvent,
                bindingView.nextEventDifferenceTime,
                bindingView.nextEventDate,
                bindingView.divider2,
                bindingView.calendarTemp,
                bindingView.divider3,
                bindingView.specialTemp
            ).forEach {
                it.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
            }

            // Icons shadow

            listOf(
                Pair(bindingView.secondRowIcon, bindingView.secondRowIconShadow),
            ).forEach {
                if ((if (context.isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow) == 0) {
                    it.second.isVisible = false
                } else {
                    it.second.isVisible = it.first.isVisible
                    it.second.scaleX = it.first.scaleX
                    it.second.scaleY = it.first.scaleY
                    it.second.applyShadow(it.first)
                }
            }

            listOf(
                Pair(bindingView.actionNext, bindingView.actionNextShadow),
                Pair(bindingView.actionPrevious, bindingView.actionPreviousShadow),
            ).forEach {
                if ((if (context.isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow) == 0) {
                    it.second.isVisible = false
                } else {
                    it.second.isVisible = it.first.isVisible
                    it.second.scaleX = it.first.scaleX
                    it.second.scaleY = it.first.scaleY
                    it.second.applyShadow(it.first, 0.6f)
                }
            }

            bindingView.actionPrevious.scaleX = bindingView.actionPrevious.scaleX * -1
            bindingView.actionPreviousShadow.scaleX = bindingView.actionPreviousShadow.scaleX * -1

            // Custom Font
            if (Preferences.customFont == Constants.CUSTOM_FONT_GOOGLE_SANS) {
                val googleSans: Typeface = when (Preferences.customFontVariant) {
                    "100" -> Typeface.createFromAsset(context.assets, "fonts/google_sans_thin.ttf")
                    "200" -> Typeface.createFromAsset(context.assets, "fonts/google_sans_light.ttf")
                    "500" -> Typeface.createFromAsset(context.assets, "fonts/google_sans_medium.ttf")
                    "700" -> Typeface.createFromAsset(context.assets, "fonts/google_sans_bold.ttf")
                    "800" -> Typeface.createFromAsset(context.assets, "fonts/google_sans_black.ttf")
                    else -> Typeface.createFromAsset(context.assets, "fonts/google_sans_regular.ttf")
                }

                listOf<TextView>(
                    bindingView.emptyDate,
                    bindingView.divider1,
                    bindingView.temp,
                    bindingView.nextEvent,
                    bindingView.nextEventDifferenceTime,
                    bindingView.nextEventDate,
                    bindingView.divider2,
                    bindingView.calendarTemp,
                    bindingView.divider3,
                    bindingView.specialTemp
                ).forEach {
                    it.typeface = googleSans
                }
            } else if (Preferences.customFont == Constants.CUSTOM_FONT_DOWNLOADED && typeface != null) {
                listOf<TextView>(
                    bindingView.emptyDate,
                    bindingView.divider1,
                    bindingView.temp,
                    bindingView.nextEvent,
                    bindingView.nextEventDifferenceTime,
                    bindingView.nextEventDate,
                    bindingView.divider2,
                    bindingView.calendarTemp,
                    bindingView.divider3,
                    bindingView.specialTemp
                ).forEach {
                    it.typeface = typeface
                }
            }

            // Weather
            if (Preferences.showWeather && Preferences.weatherIcon != "") {
                bindingView.weather.visibility = View.VISIBLE
                bindingView.calendarWeather.visibility = View.VISIBLE
                bindingView.specialWeather.visibility = View.VISIBLE
                val currentTemp = String.format(
                    Locale.getDefault(),
                    "%d °%s",
                    Preferences.weatherTemp.roundToInt(),
                    Preferences.weatherRealTempUnit
                )

                val icon: String = Preferences.weatherIcon
                if (icon == "") {
                    bindingView.weatherIcon.visibility = View.GONE
                    bindingView.emptyWeatherIcon.visibility = View.GONE
                    bindingView.specialWeatherIcon.visibility = View.GONE
                } else {
                    bindingView.weatherIcon.setImageResource(WeatherHelper.getWeatherIconResource(context, icon))
                    bindingView.emptyWeatherIcon.setImageResource(WeatherHelper.getWeatherIconResource(context, icon))
                    bindingView.specialWeatherIcon.setImageResource(WeatherHelper.getWeatherIconResource(context, icon))
                    bindingView.weatherIcon.visibility = View.VISIBLE
                    bindingView.emptyWeatherIcon.visibility = View.VISIBLE
                    bindingView.specialWeatherIcon.visibility = View.VISIBLE
                }

                bindingView.temp.text = currentTemp
                bindingView.calendarTemp.text = currentTemp
                bindingView.specialTemp.text = currentTemp

                if (GlanceProviderHelper.showGlanceProviders(context)) {
                    bindingView.calendarWeather.visibility = View.GONE
                } else {
                    bindingView.specialWeather.visibility = View.GONE
                }
            } else {
                bindingView.weather.visibility = View.GONE
                bindingView.calendarWeather.visibility = View.GONE
                bindingView.specialWeather.visibility = View.GONE
            }

            // Dividers
            arrayOf(bindingView.divider1, bindingView.divider2, bindingView.divider3).forEach {
                it.visibility = if (Preferences.showDividers) View.VISIBLE else View.INVISIBLE
                it.layoutParams = (it.layoutParams as ViewGroup.MarginLayoutParams).apply {
                    this.marginEnd = if (Preferences.showDividers) 8f.convertDpToPixel(context).toInt() else 0
                }
            }

            eventRepository.close()

            return bindingView
        }
    }
}

