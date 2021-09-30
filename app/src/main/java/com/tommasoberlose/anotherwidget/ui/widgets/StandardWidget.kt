package com.tommasoberlose.anotherwidget.ui.widgets

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.TheWidgetBinding
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.*
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.toIntValue
import com.tommasoberlose.anotherwidget.helpers.ImageHelper.applyShadow
import com.tommasoberlose.anotherwidget.receivers.NewCalendarEventReceiver
import com.tommasoberlose.anotherwidget.receivers.WidgetClickListenerReceiver
import com.tommasoberlose.anotherwidget.utils.checkGrantedPermission
import com.tommasoberlose.anotherwidget.utils.convertDpToPixel
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class StandardWidget(val context: Context) {
    fun generateWidget(appWidgetId: Int, w: Int, typeface: Typeface? = null): RemoteViews? {

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
            val margin = Preferences.widgetMargin.convertDpToPixel(context).toInt()
            views.setViewPadding(R.id.widget_shape_background, margin, margin, margin, margin)
            val refreshIntent = IntentHelper.getPendingIntent(
                context,
                appWidgetId,
                IntentHelper.getWidgetUpdateIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.widget_shape_background, refreshIntent)

            // Padding
            val padding = (Preferences.widgetPadding.convertDpToPixel(context) + Preferences.widgetMargin.convertDpToPixel(context)).toInt()
            views.setViewPadding(R.id.main_layout, padding, padding, padding, padding)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        // Clock
        views = ClockWidget(context).updateClockView(views, appWidgetId)

        // Setup listener
        try {
            val generatedBinding = generateWidgetView(typeface) ?: return null

            val width = w - (Preferences.widgetPadding.convertDpToPixel(context) + Preferences.widgetMargin.convertDpToPixel(context)).toInt() * 2
            views.setImageViewBitmap(
                R.id.bitmap_container,
                BitmapHelper.getBitmapFromView(generatedBinding.root, width)
            )
            views = updateGridView(generatedBinding, views, appWidgetId)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return views
    }

    private fun updateGridView(bindingView: TheWidgetBinding, views: RemoteViews, widgetID: Int): RemoteViews {
        try {
            val eventRepository = EventRepository(context)
            val nextEvent = eventRepository.getNextEvent()
            val eventsCount = eventRepository.getEventsCount()
            eventRepository.close()

            // Weather
            if (Preferences.showWeather && Preferences.weatherIcon != "") {
                views.setViewVisibility(R.id.weather_rect, View.VISIBLE)
                views.setViewVisibility(R.id.weather_sub_line_rect, View.GONE)

                val i = Intent(context, WidgetClickListenerReceiver::class.java)
                i.action = Actions.ACTION_OPEN_WEATHER_INTENT
                val weatherPIntent = PendingIntent.getBroadcast(context, widgetID, i, 0)

                views.setOnClickPendingIntent(R.id.weather_rect, weatherPIntent)
                views.setOnClickPendingIntent(R.id.weather_sub_line_rect, weatherPIntent)

                views.setImageViewBitmap(
                    R.id.weather_rect,
                    BitmapHelper.getBitmapFromView(bindingView.weatherDateLine, draw = false, width = bindingView.weatherDateLine.width, height = bindingView.weatherDateLine.height)
                )

                views.setImageViewBitmap(
                    R.id.weather_sub_line_rect,
                    BitmapHelper.getBitmapFromView(bindingView.weatherSubLine, draw = false, width = bindingView.weatherSubLine.width, height = bindingView.weatherSubLine.height)
                )
            } else {
                views.setViewVisibility(R.id.weather_rect, View.GONE)
                views.setViewVisibility(R.id.weather_sub_line_rect, View.GONE)
            }


            // Calendar
            views.setImageViewBitmap(
                R.id.date_rect,
                BitmapHelper.getBitmapFromView(bindingView.date, draw = false, width = bindingView.date.width, height = bindingView.date.height)
            )

            val calPIntent = IntentHelper.getPendingIntent(
                context,
                widgetID,
                IntentHelper.getCalendarIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setOnClickPendingIntent(R.id.date_rect, calPIntent)
            views.setViewVisibility(R.id.first_line_rect, View.VISIBLE)

            // Second row
            views.setImageViewBitmap(
                R.id.sub_line_rect,
                BitmapHelper.getBitmapFromView(bindingView.subLine, draw = false, width = bindingView.subLine.width, height = bindingView.subLine.height)
            )

            // Spacing
            views.setViewVisibility(
                R.id.sub_line_top_margin_small_sans,
                if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.SMALL.rawValue) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.sub_line_top_margin_medium_sans,
                if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.MEDIUM.rawValue) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.sub_line_top_margin_large_sans,
                if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.LARGE.rawValue) View.VISIBLE else View.GONE
            )

            if (Preferences.showEvents && context.checkGrantedPermission(Manifest.permission.READ_CALENDAR) && nextEvent != null && !Preferences.showEventsAsGlanceProvider) {
                if (Preferences.showNextEvent && eventsCount > 1) {

                    // Action next event
                    views.setImageViewBitmap(
                        R.id.action_next_rect,
                        BitmapHelper.getBitmapFromView(bindingView.actionNext, draw = false, width = bindingView.actionNext.width, height = bindingView.actionNext.height)
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

                    // Action previous event
                    views.setImageViewBitmap(
                        R.id.action_previous_rect,
                        BitmapHelper.getBitmapFromView(bindingView.actionPrevious, draw = false, width = bindingView.actionPrevious.width, height = bindingView.actionPrevious.height)
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

                    views.setViewVisibility(R.id.action_next_rect, View.VISIBLE)
                    views.setViewVisibility(R.id.action_previous_rect, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.action_next_rect, View.GONE)
                    views.setViewVisibility(R.id.action_previous_rect, View.GONE)
                }

                // Event intent
                val eventIntent = IntentHelper.getPendingIntent(
                    context,
                    widgetID,
                    IntentHelper.getEventIntent(context, nextEvent),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.next_event_rect, eventIntent)
                views.setViewVisibility(R.id.next_event_rect, View.VISIBLE)

                // Event time difference
                if (Preferences.showDiffTime && Calendar.getInstance().timeInMillis < nextEvent.startDate) {
                    views.setImageViewBitmap(
                        R.id.next_event_difference_time_rect,
                        BitmapHelper.getBitmapFromView(
                            bindingView.nextEventDifferenceTime,
                            draw = false,
                            width = bindingView.nextEventDifferenceTime.width,
                            height = bindingView.nextEventDifferenceTime.height
                        )
                    )

                    views.setOnClickPendingIntent(R.id.next_event_difference_time_rect, eventIntent)
                    if (!Preferences.showNextEventOnMultipleLines) {
                        views.setViewVisibility(R.id.next_event_difference_time_rect, View.VISIBLE)
                    } else {
                        views.setViewVisibility(R.id.next_event_difference_time_rect, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.next_event_difference_time_rect, View.GONE)
                }

                // Event information
                if (nextEvent.address != "" && Preferences.secondRowInformation == 1) {
                    val mapIntent = IntentHelper.getPendingIntent(
                        context,
                        widgetID,
                        IntentHelper.getGoogleMapsIntentFromAddress(context, nextEvent.address),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    views.setOnClickPendingIntent(R.id.sub_line_rect, mapIntent)
                } else {
                    val pIntentDetail = IntentHelper.getPendingIntent(
                        context,
                        widgetID,
                        IntentHelper.getCalendarIntent(context, nextEvent.startDate),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    views.setOnClickPendingIntent(R.id.sub_line_rect, pIntentDetail)
                }

                views.setImageViewBitmap(
                    R.id.next_event_rect,
                    BitmapHelper.getBitmapFromView(bindingView.nextEvent, draw = false, width = bindingView.nextEvent.width, height = bindingView.nextEvent.height)
                )
                views.setViewVisibility(R.id.calendar_layout_rect, View.VISIBLE)
                views.setViewVisibility(R.id.sub_line_rect, View.VISIBLE)
                views.setViewVisibility(R.id.weather_sub_line_rect, if (Preferences.showWeather && Preferences.weatherIcon != "") View.VISIBLE else View.GONE)

                views.setViewVisibility(R.id.first_line_rect, View.GONE)

            } else if (GlanceProviderHelper.showGlanceProviders(context)) {
                var showSomething = false
                var isWeatherShown = false
                loop@ for (provider: Constants.GlanceProviderId in GlanceProviderHelper.getGlanceProviders(context)) {
                    when (provider) {
                        Constants.GlanceProviderId.PLAYING_SONG -> {
                            if (MediaPlayerHelper.isSomeonePlaying(context)) {
                                val musicIntent = IntentHelper.getPendingIntent(
                                    context,
                                    widgetID,
                                    IntentHelper.getMusicIntent(context),
                                    PendingIntent.FLAG_UPDATE_CURRENT
                                )
                                views.setOnClickPendingIntent(R.id.sub_line_rect, musicIntent)
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> {
                            if (Preferences.showNextAlarm) {
                                val nextAlarm = AlarmHelper.getNextAlarm(context)
                                if (nextAlarm != "") {
                                    val alarmIntent = IntentHelper.getPendingIntent(
                                        context,
                                        widgetID,
                                        IntentHelper.getClockIntent(context),
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    views.setOnClickPendingIntent(R.id.sub_line_rect, alarmIntent)
                                    showSomething = true
                                    break@loop
                                }
                            }
                        }
                        Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
                            if (Preferences.showBatteryCharging) {
                                BatteryHelper.updateBatteryInfo(context)
                                if (Preferences.isCharging || Preferences.isBatteryLevelLow) {
                                    val batteryIntent = IntentHelper.getPendingIntent(
                                        context,
                                        widgetID,
                                        IntentHelper.getBatteryIntent(),
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    views.setOnClickPendingIntent(R.id.sub_line_rect, batteryIntent)
                                    showSomething = true
                                    break@loop
                                }
                            }
                        }
                        Constants.GlanceProviderId.CUSTOM_INFO -> {
                            if (Preferences.customNotes.isNotEmpty()) {
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> {
                            if (Preferences.showDailySteps && Preferences.googleFitSteps > 0) {
                                val fitIntent = IntentHelper.getPendingIntent(
                                    context,
                                    widgetID,
                                    IntentHelper.getFitIntent(context),
                                    PendingIntent.FLAG_UPDATE_CURRENT
                                )
                                views.setOnClickPendingIntent(R.id.sub_line_rect, fitIntent)
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.NOTIFICATIONS -> {
                            if (Preferences.showNotifications && ActiveNotificationsHelper.showLastNotification()) {
                                try {
                                    if (Preferences.lastNotificationIcon != 0) {
                                        val remotePackageContext = context.createPackageContext(
                                            Preferences.lastNotificationPackage, 0)
                                        ContextCompat.getDrawable(
                                            remotePackageContext,
                                            Preferences.lastNotificationIcon)
                                    }
                                    val notificationIntent = IntentHelper.getPendingIntent(
                                        context,
                                        widgetID,
                                        IntentHelper.getNotificationIntent(context),
                                        PendingIntent.FLAG_UPDATE_CURRENT
                                    )
                                    views.setOnClickPendingIntent(
                                        R.id.sub_line_rect,
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
                            if (Preferences.showEventsAsGlanceProvider&& Preferences.showEvents && context.checkGrantedPermission(
                                    Manifest.permission.READ_CALENDAR) && nextEvent != null) {
                                val pIntentDetail = IntentHelper.getPendingIntent(
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
                                    R.id.sub_line_rect,
                                    pIntentDetail
                                )
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.WEATHER -> {
                            if (Preferences.showWeatherAsGlanceProvider && Preferences.showWeather && Preferences.weatherIcon != "") {
                                val i = Intent(context, WidgetClickListenerReceiver::class.java)
                                i.action = Actions.ACTION_OPEN_WEATHER_INTENT
                                val weatherPIntent = PendingIntent.getBroadcast(context, widgetID, i, 0)

                                views.setOnClickPendingIntent(
                                    R.id.sub_line_rect,
                                    weatherPIntent
                                )
                                showSomething = true
                                isWeatherShown = true
                                break@loop
                            }
                        }
                    }
                }


                if (showSomething) {
                    views.setImageViewBitmap(
                        R.id.sub_line_rect,
                        BitmapHelper.getBitmapFromView(bindingView.subLine, draw = false, width = bindingView.subLine.width, height = bindingView.subLine.height)
                    )

                    views.setViewVisibility(R.id.weather_rect, if (isWeatherShown) View.GONE else View.VISIBLE)
                    views.setViewVisibility(R.id.first_line_rect, View.VISIBLE)
                    views.setViewVisibility(R.id.sub_line_rect, View.VISIBLE)

                    views.setViewVisibility(R.id.calendar_layout_rect, View.GONE)
                    views.setViewVisibility(R.id.weather_sub_line_rect, View.GONE)
                } else {
                    // Spacing
                    views.setViewVisibility(R.id.sub_line_top_margin_small_sans, View.GONE)
                    views.setViewVisibility(R.id.sub_line_top_margin_medium_sans, View.GONE)
                    views.setViewVisibility(R.id.sub_line_top_margin_large_sans, View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.first_line_rect, View.VISIBLE)
                views.setViewVisibility(R.id.weather_rect, View.VISIBLE)

                views.setViewVisibility(R.id.calendar_layout_rect, View.GONE)
                views.setViewVisibility(R.id.sub_line_rect, View.GONE)
                views.setViewVisibility(R.id.weather_sub_line_rect, View.GONE)
                // Spacing
                views.setViewVisibility(R.id.sub_line_top_margin_small_sans, View.GONE)
                views.setViewVisibility(R.id.sub_line_top_margin_medium_sans, View.GONE)
                views.setViewVisibility(R.id.sub_line_top_margin_large_sans, View.GONE)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return views
    }


    // Generates the widget bitmap from the view
    private fun generateWidgetView(typeface: Typeface? = null): TheWidgetBinding? {
        try {
            var isWeatherShownAsGlanceProvider = false
            val eventRepository = EventRepository(context)
            val nextEvent = eventRepository.getNextEvent()
            val eventsCount = eventRepository.getEventsCount()
            eventRepository.close()

            val bindingView = TheWidgetBinding.inflate(LayoutInflater.from(context))

            bindingView.loader.isVisible = false

            // Weather
            if (Preferences.showWeather && Preferences.weatherIcon != "") {
                bindingView.weatherDateLine.isVisible = true
                val currentTemp = String.format(
                    Locale.getDefault(),
                    "%d°%s",
                    Preferences.weatherTemp.roundToInt(),
                    Preferences.weatherRealTempUnit
                )

                val icon: String = Preferences.weatherIcon
                if (icon == "") {
                    bindingView.weatherSubLineWeatherIcon.isVisible = false
                    bindingView.weatherDateLineWeatherIcon.isVisible = false
                } else {
                    bindingView.weatherSubLineWeatherIcon.setImageResource(WeatherHelper.getWeatherIconResource(context, icon))
                    bindingView.weatherDateLineWeatherIcon.setImageResource(WeatherHelper.getWeatherIconResource(context, icon))
                    bindingView.weatherSubLineWeatherIcon.isVisible = true
                    bindingView.weatherDateLineWeatherIcon.isVisible = true
                }

                bindingView.weatherDateLineTemperature.text = currentTemp
                bindingView.weatherSubLineTemperature.text = currentTemp

                if (GlanceProviderHelper.showGlanceProviders(context)) {
                    bindingView.weatherSubLine.isVisible = false
                }
            } else {
                bindingView.weatherDateLine.isVisible = false
                bindingView.weatherSubLine.isVisible = false
            }

            val now = Calendar.getInstance().apply {
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            bindingView.dateLayout.isVisible = true
            bindingView.calendarLayout.isVisible = false
            bindingView.nextEventDifferenceTime.isVisible = false
            bindingView.actionNext.isVisible = false
            bindingView.actionPrevious.isVisible = false

            bindingView.date.text = DateHelper.getDateText(context, now)

            if (Preferences.showEvents && context.checkGrantedPermission(Manifest.permission.READ_CALENDAR) && nextEvent != null && !Preferences.showEventsAsGlanceProvider) {
                // Multiple counter
                bindingView.actionNext.isVisible =
                    Preferences.showNextEvent && eventsCount > 1
                bindingView.actionPrevious.isVisible =
                    Preferences.showNextEvent && eventsCount > 1

                bindingView.nextEvent.text = nextEvent.title

                if (Preferences.showNextEventOnMultipleLines) {
                    bindingView.nextEvent.apply {
                        isSingleLine = false
                        maxLines = 3
                        gravity = Gravity.CENTER
                    }
                }

                if (Preferences.showDiffTime && now.timeInMillis < nextEvent.startDate) {
                    val diffTime = if (!nextEvent.allDay) {
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
                    bindingView.nextEventDifferenceTime.text = diffTime

                    if (!Preferences.showNextEventOnMultipleLines) {
                        bindingView.nextEventDifferenceTime.isVisible = true
                    } else {
                        bindingView.nextEvent.text = context.getString(R.string.events_glance_provider_format).format(nextEvent.title, diffTime)
                        bindingView.nextEventDifferenceTime.isVisible = false
                    }
                } else {
                    bindingView.nextEventDifferenceTime.isVisible = false
                }

                if (nextEvent.address != "" && Preferences.secondRowInformation == 1) {
                    bindingView.subLineIcon.setImageDrawable(
                        ContextCompat.getDrawable(
                            context,
                            R.drawable.round_place_24
                        )
                    )
                    bindingView.subLineText.text = nextEvent.address
                } else {
                    bindingView.subLineIcon.setImageDrawable(
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
                            bindingView.subLineText.text =
                                String.format("%s - %s%s", startHour, endHour, multipleDay)
                        } else {
                            bindingView.subLineText.text =
                                String.format("%s", startHour)
                        }

                    } else {
                        val start = Calendar.getInstance().apply { timeInMillis = nextEvent.startDate }

                        bindingView.subLineText.text = if (now.get(Calendar.DAY_OF_YEAR) == start.get(
                                Calendar.DAY_OF_YEAR)) {
                            DateHelper.getDateText(context, start)
                        } else if (now.get(Calendar.DAY_OF_YEAR) > start.get(Calendar.DAY_OF_YEAR) || now.get(
                                Calendar.YEAR) > start.get(Calendar.YEAR)) {
                            DateHelper.getDateText(context, now)
                        } else {
                            DateHelper.getDateText(context, start)
                        }
                    }
                }

                bindingView.dateLayout.isVisible = false
                bindingView.calendarLayout.isVisible = true
                bindingView.subLine.isVisible = true
                bindingView.weatherSubLine.isVisible = Preferences.showWeather && Preferences.weatherIcon != ""

                bindingView.subLineTopMarginSmall.visibility =
                    if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.SMALL.rawValue) View.VISIBLE else View.GONE
                bindingView.subLineTopMarginMedium.visibility =
                    if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.MEDIUM.rawValue) View.VISIBLE else View.GONE
                bindingView.subLineTopMarginLarge.visibility =
                    if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.LARGE.rawValue) View.VISIBLE else View.GONE
            } else if (GlanceProviderHelper.showGlanceProviders(context)) {
                bindingView.subLineIcon.isVisible = true
                var showSomething = false
                loop@ for (provider: Constants.GlanceProviderId in GlanceProviderHelper.getGlanceProviders(
                    context
                )) {
                    when (provider) {
                        Constants.GlanceProviderId.PLAYING_SONG -> {
                            if (MediaPlayerHelper.isSomeonePlaying(context)) {
                                bindingView.subLineIcon.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context,
                                        R.drawable.round_music_note_24
                                    )
                                )
                                bindingView.subLineText.text = MediaPlayerHelper.getMediaInfo()
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.NEXT_CLOCK_ALARM -> {
                            if (Preferences.showNextAlarm) {
                                val nextAlarm = AlarmHelper.getNextAlarm(context)
                                if (nextAlarm != "") {
                                    bindingView.subLineIcon.setImageDrawable(
                                        ContextCompat.getDrawable(
                                            context,
                                            R.drawable.round_alarm_24
                                        )
                                    )
                                    bindingView.subLineText.text = nextAlarm
                                    showSomething = true
                                    break@loop
                                }
                            }
                        }
                        Constants.GlanceProviderId.BATTERY_LEVEL_LOW -> {
                            if (Preferences.showBatteryCharging) {
                                BatteryHelper.updateBatteryInfo(context)
                                if (Preferences.isCharging) {
                                    bindingView.subLineIcon.isVisible = false
                                    val batteryLevel = BatteryHelper.getBatteryLevel(context)
                                    if (batteryLevel != 100) {
                                        bindingView.subLineText.text = context.getString(R.string.charging)
                                    } else {
                                        bindingView.subLineText.text =
                                            context.getString(R.string.charged)
                                    }
                                    showSomething = true
                                    break@loop
                                } else if (Preferences.isBatteryLevelLow) {
                                    bindingView.subLineIcon.isVisible = false
                                    bindingView.subLineText.text =
                                        context.getString(R.string.battery_low_warning)
                                    showSomething = true
                                    break@loop
                                }
                            }
                        }
                        Constants.GlanceProviderId.CUSTOM_INFO -> {
                            if (Preferences.customNotes.isNotEmpty()) {
                                bindingView.subLineIcon.isVisible = false
                                bindingView.subLineText.text = Preferences.customNotes
                                bindingView.subLineText.maxLines = 2
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.GOOGLE_FIT_STEPS -> {
                            if (Preferences.showDailySteps && Preferences.googleFitSteps > 0) {
                                bindingView.subLineIcon.isVisible = false
                                bindingView.subLineText.text =
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
                                        val remotePackageContext = context.createPackageContext(
                                            Preferences.lastNotificationPackage, 0)
                                        val icon = ContextCompat.getDrawable(remotePackageContext,
                                            Preferences.lastNotificationIcon)
                                        bindingView.subLineIcon.isVisible = true
                                        bindingView.subLineIcon.setImageDrawable(icon)
                                    } else {
                                        bindingView.subLineIcon.isVisible = false
                                    }
                                    bindingView.subLineText.text = Preferences.lastNotificationTitle
                                    showSomething = true
                                    break@loop
                                } catch (ex: Exception) {}
                            }
                        }
                        Constants.GlanceProviderId.GREETINGS -> {
                            val greetingsText = GreetingsHelper.getRandomString(context)
                            if (Preferences.showGreetings && GreetingsHelper.showGreetings() && greetingsText.isNotBlank()) {
                                bindingView.subLineText.text = greetingsText
                                bindingView.subLineText.maxLines = 2
                                bindingView.subLineIcon.isVisible = false
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.EVENTS -> {
                            if (Preferences.showEventsAsGlanceProvider && Preferences.showEvents && context.checkGrantedPermission(
                                    Manifest.permission.READ_CALENDAR) && nextEvent != null) {
                                bindingView.subLineText.text = context.getString(R.string.events_glance_provider_format).format(nextEvent.title, if (Preferences.showDiffTime && now.timeInMillis < nextEvent.startDate) {
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
                                bindingView.subLineIcon.isVisible = true
                                bindingView.subLineIcon.setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context,
                                        R.drawable.round_today_24
                                    )
                                )
                                showSomething = true
                                break@loop
                            }
                        }
                        Constants.GlanceProviderId.WEATHER -> {
                            if (Preferences.showWeatherAsGlanceProvider && Preferences.showWeather && Preferences.weatherIcon != "") {
                                bindingView.subLineText.text = String.format(
                                    Locale.getDefault(),
                                    "%d°%s  %s",
                                    Preferences.weatherTemp.roundToInt(),
                                    Preferences.weatherRealTempUnit,
                                    WeatherHelper.getWeatherLabel(context, Preferences.weatherIcon)
                                )
                                bindingView.subLineIcon.isVisible = true

                                val icon: String = Preferences.weatherIcon
                                if (icon == "") {
                                    bindingView.subLineIcon.isVisible = false
                                } else {
                                    bindingView.subLineIcon.setImageResource(WeatherHelper.getWeatherIconResource(context, icon))
                                    bindingView.subLineIcon.isVisible = true
                                }

                                bindingView.weatherDateLine.isVisible = false
                                bindingView.weatherSubLine.isVisible = false

                                isWeatherShownAsGlanceProvider = true
                                showSomething = true
                                break@loop
                            }
                        }
                    }
                }

                if (showSomething) {
                    bindingView.dateLayout.isVisible = true
                    bindingView.calendarLayout.isVisible = false
                    bindingView.subLine.isVisible = true
                    bindingView.weatherSubLine.isVisible = false

                    bindingView.subLineTopMarginSmall.visibility =
                        if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.SMALL.rawValue) View.VISIBLE else View.GONE
                    bindingView.subLineTopMarginMedium.visibility =
                        if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.MEDIUM.rawValue) View.VISIBLE else View.GONE
                    bindingView.subLineTopMarginLarge.visibility =
                        if (Preferences.secondRowTopMargin == Constants.SecondRowTopMargin.LARGE.rawValue) View.VISIBLE else View.GONE
                } else {
                    bindingView.subLineIcon.isVisible = false
                }
            }


            // Color
            listOf<TextView>(
                bindingView.date,
                bindingView.weatherDateLineDivider,
                bindingView.weatherDateLineTemperature,
                bindingView.nextEvent,
                bindingView.nextEventDifferenceTime,
            ).forEach {
                it.setTextColor(ColorHelper.getFontColor(context.applicationContext.isDarkTheme()))
            }

            if (Preferences.weatherIconPack != Constants.WeatherIconPack.MINIMAL.rawValue) {
                listOf<ImageView>(bindingView.actionNext, bindingView.actionPrevious)
            } else {
                listOf<ImageView>(
                    bindingView.actionNext,
                    bindingView.actionPrevious,
                    bindingView.weatherDateLineWeatherIcon,
                    bindingView.weatherSubLineWeatherIcon
                )
            }.forEach {
                it.setColorFilter(ColorHelper.getFontColorRgb(context.applicationContext.isDarkTheme()))
                it.alpha =
                    (if (context.isDarkTheme()) Preferences.textGlobalAlphaDark.toIntValue()
                        .toFloat() else Preferences.textGlobalAlpha.toIntValue()
                        .toFloat()) / 100
            }

            listOf<TextView>(bindingView.subLineText, bindingView.weatherSubLineDivider, bindingView.weatherSubLineTemperature).forEach {
                it.setTextColor(ColorHelper.getSecondaryFontColor(context.applicationContext.isDarkTheme()))
            }

            if (!isWeatherShownAsGlanceProvider) {
                if (Preferences.weatherIconPack != Constants.WeatherIconPack.MINIMAL.rawValue) {
                    listOf<ImageView>(bindingView.subLineIcon, bindingView.subLineIconShadow)
                } else {
                    listOf<ImageView>(
                        bindingView.subLineIcon,
                        bindingView.weatherSubLineWeatherIcon,
                        bindingView.subLineIconShadow
                    )
                }.forEach {
                    it.setColorFilter(ColorHelper.getSecondaryFontColorRgb(context.applicationContext.isDarkTheme()))
                    it.alpha =
                        (if (context.isDarkTheme()) Preferences.textSecondaryAlphaDark.toIntValue()
                            .toFloat() else Preferences.textSecondaryAlpha.toIntValue()
                            .toFloat()) / 100
                }
            }

            // Text Size
            listOf<Pair<TextView, Float>>(
                bindingView.date to Preferences.textMainSize,
                bindingView.weatherDateLineDivider to (Preferences.textMainSize * 0.9f),
                bindingView.weatherDateLineTemperature to Preferences.textMainSize,
                bindingView.nextEvent to Preferences.textMainSize,
                bindingView.nextEventDifferenceTime to Preferences.textMainSize,
                bindingView.subLineText to Preferences.textSecondSize,
                bindingView.weatherSubLineDivider to (Preferences.textSecondSize * 0.9f),
                bindingView.weatherSubLineTemperature to Preferences.textSecondSize,
            ).forEach {
                it.first.setTextSize(TypedValue.COMPLEX_UNIT_SP, it.second)
                if (!it.first.includeFontPadding && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
                    it.first.isFallbackLineSpacing = false
            }

            // Icons scale
            listOf(
                bindingView.subLineIcon to Preferences.textSecondSize / 16f,
                bindingView.subLineIconShadow to Preferences.textSecondSize / 16f,
                bindingView.weatherSubLineWeatherIcon to Preferences.textSecondSize / 16f,
                bindingView.weatherDateLineWeatherIcon to Preferences.textMainSize / 24f,
                bindingView.actionNext to Preferences.textMainSize / 24f,
                bindingView.actionNextShadow to Preferences.textMainSize / 24f,
                bindingView.actionPrevious to Preferences.textMainSize / 24f,
                bindingView.actionPreviousShadow to Preferences.textMainSize / 24f
            ).forEach {
                if (it.first.tag == null)
                    it.first.tag = it.first.layoutParams.height
                it.first.layoutParams = it.first.layoutParams.apply {
                    height = ((it.first.tag as Int) * it.second).roundToInt()
                    width = height
                }
            }

            // Shadows
            val shadowRadius =
                when (if (context.isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow) {
                    0 -> 0f
                    1 -> 2f
                    2 -> 3f
                    else -> 2f
                }.toPixel(context)
            val shadowColor =
                when (if (context.isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow) {
                    0 -> Color.TRANSPARENT
                    1 -> Color.DKGRAY
                    2 -> Color.BLACK
                    else -> Color.DKGRAY
                }
            val shadowOffset =
                when (if (context.isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow) {
                    0 -> 0f
                    1 -> 0f
                    2 -> 0.5f
                    else -> 0f
                }.toPixel(context)

            listOf<TextView>(
                bindingView.date,
                bindingView.weatherDateLineDivider,
                bindingView.weatherDateLineTemperature,
                bindingView.nextEvent,
                bindingView.nextEventDifferenceTime,
                bindingView.subLineText,
                bindingView.weatherSubLineDivider,
                bindingView.weatherSubLineTemperature,
            ).forEach {
                it.setShadowLayer(shadowRadius, shadowOffset, shadowOffset, shadowColor)
            }

            // Icons shadow

            listOf(
                Pair(bindingView.subLineIcon, bindingView.subLineIconShadow),
            ).forEach {
                if ((if (context.isDarkTheme()) Preferences.textShadowDark else Preferences.textShadow) == 0) {
                    it.second.isVisible = false
                } else {
                    it.second.isVisible = it.first.isVisible
                    it.second.scaleX = it.first.scaleX
                    it.second.scaleY = it.first.scaleY
                    it.second.applyShadow(it.first, 0.8f)
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
                val googleSans: Typeface? = androidx.core.content.res.ResourcesCompat.getFont(
                    context,
                    when (Preferences.customFontVariant) {
                        "100" -> R.font.google_sans_thin
                        "200" -> R.font.google_sans_light
                        "500" -> R.font.google_sans_medium
                        "700" -> R.font.google_sans_bold
                        "800" -> R.font.google_sans_black
                        else -> R.font.google_sans_regular
                    }
                )

                listOf<TextView>(
                    bindingView.date,
                    bindingView.weatherDateLineDivider,
                    bindingView.weatherDateLineTemperature,
                    bindingView.nextEvent,
                    bindingView.nextEventDifferenceTime,
                    bindingView.subLineText,
                    bindingView.weatherSubLineDivider,
                    bindingView.weatherSubLineTemperature,
                ).forEach {
                    it.typeface = googleSans
                }
            } else if (Preferences.customFont == Constants.CUSTOM_FONT_DOWNLOADED && typeface != null) {
                listOf<TextView>(
                    bindingView.date,
                    bindingView.weatherDateLineDivider,
                    bindingView.weatherDateLineTemperature,
                    bindingView.nextEvent,
                    bindingView.nextEventDifferenceTime,
                    bindingView.subLineText,
                    bindingView.weatherSubLineDivider,
                    bindingView.weatherSubLineTemperature,
                ).forEach {
                    it.typeface = typeface
                }
            }

            // Dividers
            arrayOf(bindingView.weatherDateLineDivider, bindingView.weatherSubLineDivider).forEach {
                it.visibility = if (Preferences.showDividers) View.VISIBLE else View.GONE
            }


            return bindingView

        } catch (ex: Exception) {
            ex.printStackTrace()
            return null
        }
    }
}