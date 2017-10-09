package com.tommasoberlose.anotherwidget.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.CalendarContract
import android.support.annotation.DrawableRes
import android.support.customtabs.CustomTabsIntent
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.survivingwithandroid.weather.lib.WeatherClient
import com.survivingwithandroid.weather.lib.WeatherConfig
import com.survivingwithandroid.weather.lib.exception.WeatherLibException
import com.survivingwithandroid.weather.lib.model.CurrentWeather
import com.survivingwithandroid.weather.lib.provider.openweathermap.OpenweathermapProviderType
import com.survivingwithandroid.weather.lib.request.WeatherRequest
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.`object`.Constants

import com.tommasoberlose.anotherwidget.`object`.Event
import com.tommasoberlose.anotherwidget.ui.activity.MainActivity
import com.tommasoberlose.anotherwidget.ui.activity.SplashActivity
import com.tommasoberlose.anotherwidget.ui.widget.TheWidget

import java.util.ArrayList
import java.util.Calendar

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

    fun showIntro(context: Context) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val firstTime: Boolean = SP.getBoolean(Constants.PREF_FIRST_STEP, true)
        // TODO SP.edit().putBoolean(Constants.PREF_FIRST_STEP, false).apply()
        if (firstTime) {
            context.startActivity(Intent(context, SplashActivity::class.java))
        }
    }

    fun getCurrentWallpaper(context: Context): Drawable {
        return WallpaperManager.getInstance(context).getDrawable();
    }
}
