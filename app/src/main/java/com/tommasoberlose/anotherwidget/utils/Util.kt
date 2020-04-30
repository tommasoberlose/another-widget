package com.tommasoberlose.anotherwidget.utils

import android.app.*
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.DisplayMetrics
import com.tommasoberlose.anotherwidget.R

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import android.content.Intent
import android.content.ComponentName
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.tommasoberlose.anotherwidget.components.events.Event
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.ui.widgets.TheWidget
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Created by tommaso on 05/10/17.
 */

object Util {

    fun checkGrantedPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun updateWidget(context: Context) {
        val widgetManager = AppWidgetManager.getInstance(context)
        val widgetComponent = ComponentName(context, TheWidget::class.java)
        val widgetIds = widgetManager.getAppWidgetIds(widgetComponent)
        val update = Intent(context, TheWidget::class.java)
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        update.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        context.sendBroadcast(update)
    }

    fun showWeatherErrorNotification(context: Context) {
        TODO("weather notification")
//        val mNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
//        val SP = PreferenceManager.getDefaultSharedPreferences(context)
//
//        if (SP.getBoolean(Constants.PREF_SHOW_GPS_NOTIFICATION, true) && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) == Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) {
//            val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//            val pi: PendingIntent = PendingIntent.getActivity(context, 50, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT)
//
//            val providerIntent2 = Intent(context, MainActivity::class.java)
//            providerIntent2.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            providerIntent2.putExtra(Constants.ACTION_EXTRA_OPEN_WEATHER_PROVIDER, true)
//            val pi2: PendingIntent = PendingIntent.getActivity(context, 51, providerIntent2, PendingIntent.FLAG_UPDATE_CURRENT)
//
//            val providerIntentDisable = Intent(context, MainActivity::class.java)
//            providerIntentDisable.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            providerIntentDisable.putExtra(Constants.ACTION_EXTRA_DISABLE_GPS_NOTIFICATION, true)
//            val piDisable: PendingIntent = PendingIntent.getActivity(context, 52, providerIntentDisable, PendingIntent.FLAG_UPDATE_CURRENT)
//
//            val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, "Error")
//                    .setSmallIcon(R.drawable.ic_stat_name)
//                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
//                    .setContentTitle(context.getString(R.string.notification_gps_title))
//                    .setContentText(context.getString(R.string.notification_gps_subtitle))
//                    .addAction(R.drawable.ic_action_sync, context.getString(R.string.change_provider), pi2)
//                    .addAction(R.drawable.ic_action_settings, context.getString(R.string.disable_notification), piDisable)
//                    .setContentIntent(pi)
//
//            mNotificationManager.notify(10, mBuilder.build());
//        } else {
//            mNotificationManager.cancel(10)
//        }
    }

    fun getGoogleMapsIntentFromAddress(context: Context, address:String): Intent {
        val gmmIntentUri: Uri = Uri.parse("geo:0,0?q=$address")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.`package` = "com.google.android.apps.maps"

        return if (mapIntent.resolveActivity(context.packageManager) != null) {
            mapIntent
        } else {
            val map = "http://maps.google.co.in/maps?q=$address"
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(map));
            i
        }
    }

    fun getCurrentWallpaper(context: Context): Drawable? = try {
        WallpaperManager.getInstance(context).drawable
    } catch (e: Exception) {
//        BitmapDrawable(context.resources, getResizedBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.pixel_2_wallpaper), 800))
        null
    }

    fun getBitmapFromView(view: View): Bitmap {
        //Define a bitmap with the same size as the view
        val measuredWidth = View.MeasureSpec.makeMeasureSpec(view.width, View.MeasureSpec.UNSPECIFIED)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(view.height, View.MeasureSpec.UNSPECIFIED)
        view.measure(measuredWidth, measuredHeight)
        view.layout(0,0, measuredWidth, measuredHeight)
        val returnedBitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    fun getBitmapFromView(view: View, w: Int, h: Int): Bitmap {
        //Define a bitmap with the same size as the view
        val measuredWidth = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY)
        val measuredHeight = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        view.measure(measuredWidth, measuredHeight)
        view.layout(0,0, measuredWidth, measuredHeight)
        val returnedBitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        //Bind a canvas to it
        val canvas = Canvas(returnedBitmap)
        // draw the view on the canvas
        view.draw(canvas)
        //return the bitmap
        return returnedBitmap
    }

    fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources: Resources = context.resources
        val metrics: DisplayMetrics = resources.displayMetrics
        val px: Float = dp * (metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
        return px
    }

    fun convertSpToPixels(sp: Float, context: Context): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.resources.displayMetrics)
    }

    fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }

        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    fun getRefreshPeriodString(period: Int): Int {
        return when (period) {
            0 -> R.string.settings_weather_refresh_period_subtitle_0
            1 -> R.string.settings_weather_refresh_period_subtitle_1
            2 -> R.string.settings_weather_refresh_period_subtitle_2
            3 -> R.string.settings_weather_refresh_period_subtitle_3
            4 -> R.string.settings_weather_refresh_period_subtitle_4
            5 -> R.string.settings_weather_refresh_period_subtitle_5
            else -> R.string.settings_weather_refresh_period_subtitle_0
        }
    }

    fun getShowUntilString(period: Int): Int {
        return when (period) {
            0 -> R.string.settings_show_until_subtitle_0
            1 -> R.string.settings_show_until_subtitle_1
            2 -> R.string.settings_show_until_subtitle_2
            3 -> R.string.settings_show_until_subtitle_3
            4 -> R.string.settings_show_until_subtitle_4
            5 -> R.string.settings_show_until_subtitle_5
            6 -> R.string.settings_show_until_subtitle_6
            7 -> R.string.settings_show_until_subtitle_7
            else -> R.string.settings_show_until_subtitle_1
        }
    }

    fun getSecondRowInfoString(info: Int): Int {
        return when (info) {
            0 -> R.string.settings_second_row_info_subtitle_0
            1 -> R.string.settings_second_row_info_subtitle_1
            2 -> R.string.settings_second_row_info_subtitle_2
            else -> R.string.settings_second_row_info_subtitle_0
        }
    }

    fun getTextShadowString(shadow: Int): Int {
        return when (shadow) {
            0 -> R.string.settings_text_shadow_subtitle_none
            1 -> R.string.settings_text_shadow_subtitle_low
            2 -> R.string.settings_text_shadow_subtitle_high
            else -> R.string.settings_text_shadow_subtitle_low
        }
    }

    fun getCustomFontLabel(shadow: Int): Int {
        return when (shadow) {
            0 -> R.string.custom_font_subtitle_0
            1 -> R.string.custom_font_subtitle_1
            else -> R.string.custom_font_subtitle_1
        }
    }

    fun getCalendarIntent(context: Context): Intent {
        return when (Preferences.calendarAppPackage) {
            "" -> {
                Intent(Intent.ACTION_MAIN).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                }
            }
            "_" -> {
                Intent()
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.calendarAppPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Intent(Intent.ACTION_MAIN).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addCategory(Intent.CATEGORY_APP_CALENDAR)
                    }
                }
            }
        }
    }

    fun getWeatherIntent(context: Context): Intent {
        return when (Preferences.weatherAppPackage) {
            "" -> {
                Intent(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    data = Uri.parse("dynact://velour/weather/ProxyActivity")
                    component = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.velour.DynamicActivityTrampoline")
                }
            }
            "_" -> {
                Intent()
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.weatherAppPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                } catch (e: Exception) {
                    Intent(Intent.ACTION_VIEW).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        data = Uri.parse("dynact://velour/weather/ProxyActivity")
                        component = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.velour.DynamicActivityTrampoline")
                    }
                }
            }
        }
    }

    fun getEventIntent(context: Context, e: Event): Intent {
        return when (Preferences.eventAppPackage) {
            "" -> {
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, e.eventID)
                Intent(Intent.ACTION_VIEW).apply {
                    data = uri
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra("beginTime", e.startDate)
                    putExtra("endTime", e.endDate)
                }
            }
            "_" -> {
                Intent()
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.eventAppPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                } catch (ex: Exception) {
                    val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, e.id)
                    Intent(Intent.ACTION_VIEW).apply {
                        data = uri
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("beginTime", e.startDate)
                        putExtra("endTime", e.endDate)
                    }
                }
            }
        }
    }

    fun getClockIntent(context: Context): Intent {
        return when (Preferences.clockAppPackage) {
            "" -> {
                Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
            "_" -> {
                Intent()
            }
            else -> {
                val pm: PackageManager = context.packageManager
                try {
                    pm.getLaunchIntentForPackage(Preferences.clockAppPackage)!!.apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                } catch (e: Exception) {
                    Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                }
            }
        }
    }

    fun getCapWordString(text: String): String {
        return try {
            val ar = text.split(" ")
            var newText = ""
            for (t: String in ar) {
                newText += " "
                newText += t.substring(0, 1).toUpperCase(Locale.getDefault())
                newText += t.substring(1)
            }
            newText.substring(1)
        } catch (e: Exception) {
            text
        }
    }

    fun showLocationNotification(context: Context, show: Boolean) {
        TODO("Show location notification")
//        val mNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
//
//        if (show) {
//            val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, "Config")
//                    .setSmallIcon(R.drawable.ic_stat_name)
//                    .setPriority(Notification.PRIORITY_MIN)
//                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
//                    .setContentTitle(context.getString(R.string.notification_gps_title))
//                    .setContentText(context.getString(R.string.notification_gps_subtitle))
//                    .setAutoCancel(true);
//
//            val intent: Intent = Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
//            val pi: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//            mBuilder.setContentIntent(pi);
//            mNotificationManager.notify(1, mBuilder.build());
//        } else {
//            mNotificationManager.cancel(1);
//        }

    }

    fun showWeatherNotification(context: Context, show: Boolean) {
        TODO("Show location notification")
//        val mNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
//
//        if (show) {
//            val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, "Config")
//                    .setSmallIcon(R.drawable.ic_stat_name)
//                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
//                    .setContentTitle(context.getString(R.string.settings_weather_provider_api_key_title))
//                    .setContentText(context.getString(R.string.settings_weather_provider_api_key_subtitle_not_set))
//                    .setAutoCancel(true);
//
//            val intent: Intent = Intent(context, MainActivity::class.java);
//            intent.putExtra(Constants.ACTION_EXTRA_OPEN_WEATHER_PROVIDER, true)
//            val pi: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//            mBuilder.setContentIntent(pi);
//            mNotificationManager.notify(2, mBuilder.build());
//        } else {
//            mNotificationManager.cancel(2);
//        }
    }

    fun expand(v: View) {
        if (v.visibility != View.VISIBLE) {
            v.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val targetHeight = v.measuredHeight

            v.layoutParams.height = 0
            v.visibility = View.VISIBLE
            val a = object : Animation() {
                protected override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    v.layoutParams.height = if (interpolatedTime == 1f)
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    else
                        (targetHeight * interpolatedTime).toInt()
                    v.translationY = 0f
                    v.requestLayout()
                }

                override fun willChangeBounds(): Boolean {
                    return true
                }
            }

            a.duration = 500L
            v.startAnimation(a)
        }
    }

    fun collapse(v: View) {
        if (v.visibility != View.GONE) {
            val initialHeight = v.measuredHeight

            val a = object : Animation() {
                protected override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    if (interpolatedTime == 1f) {
                        v.visibility = View.GONE
                    } else {
                        v.layoutParams.height = initialHeight - (initialHeight * interpolatedTime).toInt()
                        v.requestLayout()
                    }
                }

                override fun willChangeBounds(): Boolean {
                    return true
                }
            }

            a.duration = 500L //(initialHeight / v.context.resources.displayMetrics.density).toLong()
            v.startAnimation(a)
        }
    }

    fun getEmojiByUnicode(unicode: Int): String {
        return String(Character.toChars(unicode))
    }

    fun getDifferenceText(context: Context, now: Long, start: Long): String {
        val nowDate = DateTime(now)
        val eventDate = DateTime(start)

        var difference = start - now
        difference += 60 * 1000 - (difference % (60 * 1000))

        when {
            difference <= 0 || TimeUnit.MILLISECONDS.toHours(difference) < 1 -> {
                return ""
            }
            TimeUnit.MILLISECONDS.toHours(difference) < 12 -> {
                return DateUtils.getRelativeTimeSpanString(start, now, DateUtils.HOUR_IN_MILLIS).toString()
            }
            eventDate.dayOfYear == nowDate.plusDays(1).dayOfYear -> {
                return String.format("%s", context.getString(R.string.tomorrow))
            }
            eventDate.dayOfYear == nowDate.dayOfYear -> {
                return String.format("%s", context.getString(R.string.today))
            }
            else -> {
                return DateUtils.getRelativeTimeSpanString(start, now, DateUtils.DAY_IN_MILLIS).toString()
            }
        }

    }

    fun getFontColor(): Int {
        return try {
            Color.parseColor(Preferences.textGlobalColor)
        } catch (e: Exception) {
            Color.parseColor("#FFFFFF")
        }
    }

    fun getTintedDrawable(context: Context, inputDrawable: Int, color: Int): Drawable?  = ContextCompat.getDrawable(context, inputDrawable)?.apply {
        DrawableCompat.setTint(this, color)
        DrawableCompat.setTintMode(this, PorterDuff.Mode.SRC_IN)
    }

    fun changeBitmapColor(sourceBitmap: Bitmap, color: Int): Bitmap {
        val resultBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0,
                sourceBitmap.width - 1, sourceBitmap.height - 1)
        val p = Paint()
        val filter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        p.colorFilter = filter

        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(resultBitmap, 0f, 0f, p)

        return resultBitmap
    }

    fun getNextAlarm(context: Context): String = with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
        return if (nextAlarmClock != null && nextAlarmClock.triggerTime - Calendar.getInstance().timeInMillis > 5 * 60 * 1000) {
            DateFormat.getTimeFormat(context).format(Date(nextAlarmClock.triggerTime))
        } else {
            ""
        }
    }
}
