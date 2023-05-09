package com.tommasoberlose.anotherwidget.ui.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.res.Resources
import android.graphics.Typeface
import android.os.Bundle
import android.widget.RemoteViews
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.*
import com.tommasoberlose.anotherwidget.receivers.*
import com.tommasoberlose.anotherwidget.utils.toPixel
import java.lang.Exception
import kotlin.math.min


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
        WeatherHelper.updateWeather(context)
        MediaPlayerHelper.updatePlayingMediaInfo(context)
    }

    override fun onDisabled(context: Context) {
        if (getWidgetCount(context) == 0) {
            CalendarHelper.removeEventUpdatesAndroidN(context)
            UpdatesReceiver.removeUpdates(context)
            WeatherReceiver.removeUpdates(context)
        }
    }

    companion object {
        private val handler by lazy { android.os.Handler(android.os.Looper.getMainLooper()) }

        fun updateWidget(context: Context) {
            handler.run {
                removeCallbacksAndMessages(null)
                postDelayed ({
                    context.sendBroadcast(IntentHelper.getWidgetUpdateIntent(context))
                }, 100)
            }
        }

        fun getWidgetCount(context: Context): Int {
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, MainWidget::class.java)
            return widgetManager.getAppWidgetIds(widgetComponent).size
        }

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                     appWidgetId: Int) {
            val dimensions = WidgetHelper.WidgetSizeProvider(context, appWidgetManager).getWidgetsSize(appWidgetId)

            WidgetHelper.runWithCustomTypeface(context) {
                val views = when (Preferences.widgetAlign) {
                    Constants.WidgetAlign.LEFT.rawValue -> AlignedWidget(context).generateWidget(appWidgetId, dimensions.first, it)
                    Constants.WidgetAlign.RIGHT.rawValue -> AlignedWidget(context, rightAligned = true).generateWidget(appWidgetId, dimensions.first, it)
                    else -> StandardWidget(context).generateWidget(appWidgetId, dimensions.first, it)
                }
                try {
                    if (views != null) appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        fun getWidgetView(context: Context, width: Int, typeface: Typeface?): RemoteViews? {
            return when (Preferences.widgetAlign) {
                Constants.WidgetAlign.LEFT.rawValue -> AlignedWidget(context).generateWidget(
                    0,
                    width,
                    typeface
                )
                Constants.WidgetAlign.RIGHT.rawValue -> AlignedWidget(
                    context,
                    rightAligned = true
                ).generateWidget(0, width, typeface)
                else -> StandardWidget(context).generateWidget(0, width, typeface)
            }
        }
    }
}

