package com.tommasoberlose.anotherwidget.util

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.support.customtabs.CustomTabsIntent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import com.tommasoberlose.anotherwidget.R

import com.tommasoberlose.anotherwidget.ui.activity.MainActivity
import com.tommasoberlose.anotherwidget.ui.widget.TheWidget
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.support.annotation.StringRes
import android.util.TypedValue
import android.content.Intent
import android.content.ComponentName
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import android.widget.Toast
import com.tommasoberlose.anotherwidget.`object`.Constants


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
        context.sendBroadcast(Intent(Constants.ACTION_SOMETHING_APPENED))
    }

    fun showNotification(context: Context) {
        val mNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;

        if (!Util.checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
            val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, "Settings")
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setContentTitle(context.getString(R.string.notification_title))
                    .setContentText(context.getString(R.string.notification_subtitle))
                    .setAutoCancel(true);

            val intent: Intent = Intent(context, MainActivity::class.java);
            val pi: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(pi);
            mNotificationManager.notify(0, mBuilder.build());
        } else {
            mNotificationManager.cancel(0);
        }

    }

    fun openURI(context: Context, url: String) {
        try {
            val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder()
            builder.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary))
            val customTabsIntent: CustomTabsIntent = builder.build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (e: Exception) {
            try {
                val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(openIntent);
            } catch (ignored: Exception) {
                val clipboard:ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(context.getString(R.string.app_name), url);
                clipboard.primaryClip = clip;
                Toast.makeText(context, R.string.error_opening_uri, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun rateApp(context: Context, url: String) {
        val openIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(openIntent)
    }

    fun share(context: Context) {
        val sendIntent = Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                "Yep, just another cool widget: https://play.google.com/store/apps/details?id=com.tommasoberlose.anotherwidget");
        sendIntent.setType("text/plain");
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.action_share)));
    }

    fun getCurrentWallpaper(context: Context): Drawable? {
        var wallpaper: Drawable? = null
        try {
            wallpaper = WallpaperManager.getInstance(context).drawable
        } catch (e: Exception) {
            wallpaper = BitmapDrawable(context.resources, getResizedBitmap(BitmapFactory.decodeResource(context.resources, R.drawable.pixel_2_wallpaper), 800))
        } finally {
            return wallpaper
        }
    }

    fun buildUpdate(context: Context, time: String, fontPath: String): Bitmap {
        val myBitmap:Bitmap = Bitmap.createBitmap(convertDpToPixel(300f, context).toInt(), convertDpToPixel(40f, context).toInt(), Bitmap.Config.ARGB_8888)
        val myCanvas: Canvas = Canvas(myBitmap)
        val paint: Paint = Paint()
        val clock: Typeface = Typeface.createFromAsset(context.assets, fontPath)
        paint.isAntiAlias = true
        paint.isSubpixelText = true
        paint.typeface = clock
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = convertSpToPixels(14f, context)
        paint.textAlign = Paint.Align.CENTER
        paint.setShadowLayer(5f,0f, 0f, R.color.black_50)
        myCanvas.drawText("Ultra Pixel Meeting Ciao Ciao", convertDpToPixel(150f, context), convertDpToPixel(20f, context), paint)
        return myBitmap;
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
            else -> R.string.settings_show_until_subtitle_1
        }
    }

    fun getCalendarIntent(context: Context): Intent {
        val SP = PreferenceManager.getDefaultSharedPreferences(context)
        if (SP.getString(Constants.PREF_CALENDAR_APP_PACKAGE, "").equals("")) {
            val calIntent = Intent(Intent.ACTION_MAIN)
            calIntent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            return calIntent
        } else {
            val pm: PackageManager = context.packageManager
            return try {
                val intent: Intent = pm.getLaunchIntentForPackage(SP.getString(Constants.PREF_CALENDAR_APP_PACKAGE, ""))
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent
            } catch (e: Exception) {
                e.printStackTrace()
                val calIntent = Intent(Intent.ACTION_MAIN)
                calIntent.addCategory(Intent.CATEGORY_APP_CALENDAR)
                calIntent
            }
        }
    }

    fun getWeatherIntent(context: Context): Intent {
        val SP = PreferenceManager.getDefaultSharedPreferences(context)
        if (SP.getString(Constants.PREF_WEATHER_APP_PACKAGE, "").equals("")) {
            val weatherIntent: Intent = Intent(Intent.ACTION_VIEW)
            weatherIntent.addCategory(Intent.CATEGORY_DEFAULT)
            weatherIntent.data = Uri.parse("dynact://velour/weather/ProxyActivity")
            weatherIntent.component = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.velour.DynamicActivityTrampoline")
            return weatherIntent
        } else {
            val pm: PackageManager = context.packageManager
            return try {
                val intent: Intent = pm.getLaunchIntentForPackage(SP.getString(Constants.PREF_WEATHER_APP_PACKAGE, ""))
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
                intent
            } catch (e: Exception) {
                val weatherIntent: Intent = Intent(Intent.ACTION_VIEW)
                weatherIntent.addCategory(Intent.CATEGORY_DEFAULT)
                weatherIntent.data = Uri.parse("dynact://velour/weather/ProxyActivity")
                weatherIntent.component = ComponentName("com.google.android.googlequicksearchbox", "com.google.android.apps.gsa.velour.DynamicActivityTrampoline")
                weatherIntent
            }
        }
    }

    fun getCapWordString(text: String): String {
        return try {
            val ar = text.split(" ")
            var newText = ""
            for (t: String in ar) {
                newText += " "
                newText += t.substring(0, 1).toUpperCase()
                newText += t.substring(1)
            }
            newText.substring(1)
        } catch (e: Exception) {
            text
        }
    }

    fun showLocationNotification(context: Context, show: Boolean) {
        val mNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;

        if (show) {
            val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, "Settings")
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setContentTitle(context.getString(R.string.notification_gps_title))
                    .setContentText(context.getString(R.string.notification_gps_subtitle))
                    .setAutoCancel(true);

            val intent: Intent = Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            val pi: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(pi);
            mNotificationManager.notify(1, mBuilder.build());
        } else {
            mNotificationManager.cancel(1);
        }

    }

    fun showWeatherNotification(context: Context, show: Boolean) {
        val mNotificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;

        if (show) {
            val mBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, "Settings")
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setContentTitle(context.getString(R.string.settings_weather_provider_api_key_title))
                    .setContentText(context.getString(R.string.settings_weather_provider_api_key_subtitle_not_set))
                    .setAutoCancel(true);

            val intent: Intent = Intent(context, MainActivity::class.java);
            intent.putExtra(Constants.ACTION_EXTRA_OPEN_WEATHER_PROVIDER, true)
            val pi: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(pi);
            mNotificationManager.notify(2, mBuilder.build());
        } else {
            mNotificationManager.cancel(2);
        }

    }

    fun sendEmail(context: Context) {
        val i:Intent = Intent(Intent.ACTION_SEND)
        i.type = "message/rfc822"
        i.putExtra(Intent.EXTRA_EMAIL, arrayOf("tommaso.berlose@gmail.com"))
        i.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_title))
        try {
            context.startActivity(Intent.createChooser(i, context.getString(R.string.feedback_chooser_title)))
        } catch (ex: Exception) {
            Toast.makeText(context, R.string.feedback_error, Toast.LENGTH_SHORT).show();
        }
    }

    fun expand(v: View) {
        if (v.visibility != View.VISIBLE) {
            v.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val targetHeight = v.getMeasuredHeight()

            v.layoutParams.height = 0
            v.visibility = View.VISIBLE
            val a = object : Animation() {
                protected override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    v.layoutParams.height = if (interpolatedTime == 1f)
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    else
                        (targetHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }

                override fun willChangeBounds(): Boolean {
                    return true
                }
            }

            a.duration = (targetHeight / v.context.resources.displayMetrics.density).toLong()
            v.startAnimation(a)
        }
    }

    fun collapse(v: View) {
        if (v.visibility != View.GONE) {
            val initialHeight = v.getMeasuredHeight()

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

            a.duration = (initialHeight / v.context.resources.displayMetrics.density).toLong()
            v.startAnimation(a)
        }
    }
}
