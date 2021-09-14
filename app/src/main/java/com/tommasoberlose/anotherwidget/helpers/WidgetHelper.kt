package com.tommasoberlose.anotherwidget.helpers

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Typeface
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.db.EventRepository
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import com.tommasoberlose.anotherwidget.utils.toPixel
import kotlin.math.min

object WidgetHelper {
    class WidgetSizeProvider(
        private val context: Context,
        private val appWidgetManager: AppWidgetManager
    ) {

        fun getWidgetsSize(widgetId: Int): Pair<Int, Int> {
            val width = getWidgetWidth(widgetId)
            val height = getWidgetHeight(widgetId)
            val widthInPx = context.dip(width)
            val heightInPx = context.dip(height)
            FirebaseCrashlytics.getInstance().setCustomKey("widthInPx", widthInPx)
            FirebaseCrashlytics.getInstance().setCustomKey("heightInPx", heightInPx)
            return widthInPx to heightInPx
        }

        private fun getWidgetWidth(widgetId: Int): Int = getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)

        private fun getWidgetHeight(widgetId: Int): Int = getWidgetSizeInDp(widgetId, AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

        private fun getWidgetSizeInDp(widgetId: Int, key: String): Int =
            appWidgetManager.getAppWidgetOptions(widgetId).getInt(key, 0)

        private fun Context.dip(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    }

    fun Pair<Int, Int>.reduceDimensionWithMaxWidth(width: Int): Pair<Int, Int> {
        return if (first < width) {
            this
        } else {
            val factor = width / first
            width to second * factor
        }
    }

    fun runWithCustomTypeface(context: Context, function: (typeface: Typeface?) -> Unit) {
        if (Preferences.customFontFile != "") {
            val request = FontRequest(
                "com.google.android.gms.fonts",
                "com.google.android.gms",
                Preferences.customFontFile,
                R.array.com_google_android_gms_fonts_certs
            )

            val handlerThread = HandlerThread("generateView")
            val callback = object : FontsContractCompat.FontRequestCallback() {
                override fun onTypefaceRetrieved(typeface: Typeface) {
                    handlerThread.quit()
                    function.invoke(typeface)
                }

                override fun onTypefaceRequestFailed(reason: Int) {
                    handlerThread.quit()
                    function.invoke(null)
                }
            }

            handlerThread.start()
            //if (Looper.myLooper() == null) {
            //    Looper.prepare()
            //}

            Handler(handlerThread.looper).run {
                FontsContractCompat.requestFont(context, request, callback, this)
            }
        } else {
            function.invoke(null)
        }
    }
}