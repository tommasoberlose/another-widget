package com.tommasoberlose.anotherwidget.ui.widgets

import android.app.PendingIntent
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ColorHelper
import com.tommasoberlose.anotherwidget.helpers.IntentHelper
import com.tommasoberlose.anotherwidget.utils.isDarkTheme
import com.tommasoberlose.anotherwidget.utils.toPixel

class ClockWidget(val context: Context) {
    fun updateClockView(views: RemoteViews, widgetID: Int): RemoteViews {
        try {
            if (!Preferences.showClock) {
                views.setViewVisibility(R.id.time, View.GONE)
                views.setViewVisibility(R.id.time_am_pm, View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_none, View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_small, View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_medium, View.GONE)
                views.setViewVisibility(R.id.clock_bottom_margin_large, View.GONE)
                views.setViewVisibility(R.id.timezones_container, View.GONE)
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
                    if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.NONE.rawValue) View.VISIBLE else View.GONE
                )
                views.setViewVisibility(
                    R.id.clock_bottom_margin_small,
                    if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.SMALL.rawValue) View.VISIBLE else View.GONE
                )
                views.setViewVisibility(
                    R.id.clock_bottom_margin_medium,
                    if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.MEDIUM.rawValue) View.VISIBLE else View.GONE
                )
                views.setViewVisibility(
                    R.id.clock_bottom_margin_large,
                    if (Preferences.clockBottomMargin == Constants.ClockBottomMargin.LARGE.rawValue) View.VISIBLE else View.GONE
                )


                // Timezones
                if (Preferences.altTimezoneId != "" && Preferences.altTimezoneLabel != "") {
                    views.setString(R.id.alt_timezone_time, "setTimeZone", Preferences.altTimezoneId)
                    views.setString(R.id.alt_timezone_time_am_pm, "setTimeZone", Preferences.altTimezoneId)
                    views.setTextViewText(R.id.alt_timezone_label, Preferences.altTimezoneLabel)

                    views.setTextColor(R.id.alt_timezone_time, ColorHelper.getClockFontColor(context.isDarkTheme()))
                    views.setTextColor(R.id.alt_timezone_time_am_pm, ColorHelper.getClockFontColor(context.isDarkTheme()))
                    views.setTextColor(R.id.alt_timezone_label, ColorHelper.getClockFontColor(context.isDarkTheme()))
                    views.setTextViewTextSize(
                        R.id.alt_timezone_time,
                        TypedValue.COMPLEX_UNIT_SP,
                        Preferences.clockTextSize.toPixel(context) / 3
                    )
                    views.setTextViewTextSize(
                        R.id.alt_timezone_time_am_pm,
                        TypedValue.COMPLEX_UNIT_SP,
                        (Preferences.clockTextSize.toPixel(context) / 3) / 5 * 2
                    )
                    views.setTextViewTextSize(
                        R.id.alt_timezone_label,
                        TypedValue.COMPLEX_UNIT_SP,
                        (Preferences.clockTextSize.toPixel(context) / 3) / 5 * 2
                    )

                    views.setOnClickPendingIntent(R.id.timezones_container, clockPIntent)
                    views.setViewVisibility(R.id.timezones_container, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.timezones_container, View.GONE)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        return views
    }
}