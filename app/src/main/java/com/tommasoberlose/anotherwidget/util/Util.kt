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

    fun getNextEvent(context: Context): List<Event> {
        val eventList = ArrayList<Event>()

        val now = Calendar.getInstance()
        val hourLimit = Calendar.getInstance()
        hourLimit.add(Calendar.HOUR, 6)

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, now.timeInMillis)
        ContentUris.appendId(builder, hourLimit.timeInMillis)

        if (!checkGrantedPermission(context, Manifest.permission.READ_CALENDAR)) {
            return eventList
        }

        val instanceCursor = context.contentResolver.query(builder.build(), arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END), null, null, null) ?: return eventList
        instanceCursor.moveToFirst()

        for (i in 0 until instanceCursor.count) {
            val ID = instanceCursor.getInt(0)

            val eventCursor = context.contentResolver.query(CalendarContract.Events.CONTENT_URI, arrayOf(CalendarContract.Events.TITLE),
                    CalendarContract.Events._ID + " is ?",
                    arrayOf(Integer.toString(ID)), null) ?: return eventList
            eventCursor.moveToFirst()

            for (j in 0 until eventCursor.count) {
                val e = Event(eventCursor, instanceCursor)
                if (e.endDate - now.timeInMillis > 1000 * 60 * 30) {
                    eventList.add(e)
                }
                eventCursor.moveToNext()
            }

            eventCursor.close()

            instanceCursor.moveToNext()
        }

        instanceCursor.close()
        return eventList
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

    fun getWeather(context: Context) {
        if (!checkGrantedPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            return
        }
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        getCurrentWeather(context, locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0f, object: LocationListener {
            override fun onLocationChanged(location: Location) {
                locationManager.removeUpdates(this)
                getCurrentWeather(context, location)
            }

            @SuppressLint("ApplySharedPref")
            override fun onProviderDisabled(p0: String?) {
                SP.edit()
                        .remove(Constants.PREF_WEATHER_TEMP)
                        .remove(Constants.PREF_WEATHER_ICON)
                        .commit()
            }

            @SuppressLint("ApplySharedPref")
            override fun onProviderEnabled(p0: String?) {
                SP.edit()
                        .remove(Constants.PREF_WEATHER_TEMP)
                        .remove(Constants.PREF_WEATHER_ICON)
                        .commit()
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {
            }
        })
    }

    @SuppressLint("ApplySharedPref")
    fun getCurrentWeather(context: Context, location: Location) {
        val SP: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        try {
            val config = WeatherConfig()
            config.unitSystem = if (SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F").equals("C")) WeatherConfig.UNIT_SYSTEM.M else WeatherConfig.UNIT_SYSTEM.I
            config.lang = "en" // If you want to use english
            config.maxResult = 1 // Max number of cities retrieved
            config.numDays = 1 // Max num of days in the forecast
            config.ApiKey = "43e744ad8ff91b09ea62dbc7d0e7c1dd";

            val client = WeatherClient.ClientBuilder().attach(context)
                    .httpClient(com.survivingwithandroid.weather.lib.client.volley.WeatherClientDefault::class.java)
                    .provider(OpenweathermapProviderType())
                    .config(config)
                    .build()

            client.getCurrentCondition(WeatherRequest(location.longitude, location.latitude), object : WeatherClient.WeatherEventListener {
                @SuppressLint("ApplySharedPref")
                override fun onWeatherRetrieved(currentWeather: CurrentWeather) {
                    SP.edit()
                            .putFloat(Constants.PREF_WEATHER_TEMP, currentWeather.weather.temperature.temp)
                            .putString(Constants.PREF_WEATHER_ICON, currentWeather.weather.currentCondition.icon)
                            .commit()
                    updateWidget(context)
                }

                @SuppressLint("ApplySharedPref")
                override fun onWeatherError(e: WeatherLibException?) {
                    SP.edit()
                            .remove(Constants.PREF_WEATHER_TEMP)
                            .remove(Constants.PREF_WEATHER_ICON)
                            .commit()
                }

                @SuppressLint("ApplySharedPref")
                override fun onConnectionError(throwable: Throwable?) {
                    SP.edit()
                            .remove(Constants.PREF_WEATHER_TEMP)
                            .remove(Constants.PREF_WEATHER_ICON)
                            .commit()
                }
            })
        } catch (t: Exception) {
            SP.edit()
                    .remove(Constants.PREF_WEATHER_TEMP)
                    .remove(Constants.PREF_WEATHER_ICON)
                    .commit()
        }
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

    fun getWeatherIconResource(icon: String): Int {
        when (icon) {
            "01d" -> {
                return R.drawable.clear_day
            }
            "02d" -> {
                return R.drawable.partly_cloudy
            }
            "03d" -> {
                return R.drawable.mostly_cloudy
            }
            "04d" -> {
                return R.drawable.cloudy_weather
            }
            "09d" -> {
                return R.drawable.storm_weather_day
            }
            "10d" -> {
                return R.drawable.rainy_day
            }
            "11d" -> {
                return R.drawable.thunder_day
            }
            "13d" -> {
                return R.drawable.snow_day
            }
            "50d" -> {
                return R.drawable.haze_day
            }
            "01n" -> {
                return R.drawable.clear_night
            }
            "02n" -> {
                return R.drawable.partly_cloudy_night
            }
            "03n" -> {
                return R.drawable.mostly_cloudy_night
            }
            "04n" -> {
                return R.drawable.cloudy_weather
            }
            "09n" -> {
                return R.drawable.storm_weather_night
            }
            "10n" -> {
                return R.drawable.rainy_night
            }
            "11n" -> {
                return R.drawable.thunder_night
            }
            "13n" -> {
                return R.drawable.snow_night
            }
            "50n" -> {
                return R.drawable.haze_night
            }
            else -> {
                return -1
            }
        }
    }

    fun getCurrentWallpaper(context: Context): Drawable {
        return WallpaperManager.getInstance(context).getDrawable();
    }
}
