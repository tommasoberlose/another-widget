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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.databinding.LeftAlignedWidgetBinding
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
                val views = when (Preferences.widgetAlign) {
                    Constants.WidgetAlign.LEFT.rawValue -> LeftAlignedWidget(context).generateWidget(appWidgetId, min(dimensions.first - 8.toPixel(context), min(width, height) - 16.toPixel(context)), it)
                    else -> StandardWidget(context).generateWidget(appWidgetId, min(dimensions.first - 8.toPixel(context), min(width, height) - 16.toPixel(context)), it)
                }
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        fun getWidgetView(context: Context, typeface: Typeface?): ViewBinding {
            return when (Preferences.widgetAlign) {
                Constants.WidgetAlign.LEFT.rawValue -> LeftAlignedWidget(context).generateWidgetView(typeface)
                else -> StandardWidget(context).generateWidgetView(typeface)
            }
        }
    }
}

