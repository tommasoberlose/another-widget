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
        val update = Intent()
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
        update.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        context.sendBroadcast(update)
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
            val builder: CustomTabsIntent.Builder = CustomTabsIntent.Builder();
            builder.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary));
            val customTabsIntent: CustomTabsIntent = builder.build();
            customTabsIntent.launchUrl(context, Uri.parse(url));
        } catch (e: Exception) {
            val legalIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(legalIntent);
        }
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
}
