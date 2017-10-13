package com.tommasoberlose.anotherwidget.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.appwidget.AppWidgetManager
import android.content.*
import android.preference.PreferenceManager
import android.view.View
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.util.Util
import com.tommasoberlose.anotherwidget.receiver.UpdatesReceiver
import com.tommasoberlose.anotherwidget.receiver.WeatherReceiver
import java.util.*
import java.util.concurrent.TimeUnit
import android.content.Intent
import android.content.BroadcastReceiver
import com.tommasoberlose.anotherwidget.util.CalendarUtil
import com.tommasoberlose.anotherwidget.util.WeatherUtil
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.github.rubensousa.bottomsheetbuilder.BottomSheetBuilder
import com.github.rubensousa.bottomsheetbuilder.BottomSheetMenuDialog
import com.tommasoberlose.anotherwidget.`object`.CalendarSelector
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.the_widget.*


class MainActivity : AppCompatActivity() {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateUI()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Fabric.with(this, Crashlytics())

        action_support.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                Util.openURI(this@MainActivity, "https://paypal.me/tommasoberlose")
            }
        })

        action_share.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                Util.share(this@MainActivity)
            }
        })

        action_rate.setOnClickListener(object: View.OnClickListener {
            override fun onClick(p0: View?) {
                Util.openURI(this@MainActivity, "https://play.google.com/store/apps/details?id=com.tommasoberlose.anotherwidget")
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        registerReceiver(receiver, filter);
        updateUI()
    }

    override fun onPause() {
        unregisterReceiver(receiver);
        super.onPause();
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                   grantResults: IntArray) {
        when (requestCode) {
            Constants.CALENDAR_REQUEST_CODE -> if (!(permissions.size != 1 || grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                CalendarUtil.updateEventList(this)
            }
            Constants.LOCATION_REQUEST_CODE -> if (!(permissions.size != 1 || grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                WeatherUtil.updateWeather(this)
            }
        }
    }

    fun updateUI() {
        no_calendar_permission_container.visibility= View.GONE
        no_location_permission_container.visibility= View.GONE
        all_set_container.visibility = View.GONE
        updateSettings()

        if (!Util.checkGrantedPermission(this, Manifest.permission.READ_CALENDAR)) {
            no_calendar_permission_container.visibility = View.VISIBLE
            request_calendar.setOnClickListener(object: View.OnClickListener {
                override fun onClick(view: View?) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_CALENDAR), Constants.CALENDAR_REQUEST_CODE)
                }
            })
        } else {
            if (!Util.checkGrantedPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                no_location_permission_container.visibility = View.VISIBLE
                request_location.setOnClickListener(object: View.OnClickListener {
                    override fun onClick(view: View?) {
                        ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Constants.LOCATION_REQUEST_CODE)
                    }
                })
            } else {
                all_set_container.visibility = View.VISIBLE
            }
        }
        updateAppWidget()
    }


    internal fun updateAppWidget() {
        val wallpaper: Drawable? = Util.getCurrentWallpaper(this)
        if (wallpaper != null) {
            widget_bg.setImageDrawable(wallpaper)
            updateCalendarView()
            updateLocationView()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.RESULT_CODE_CUSTOM_LOCATION) {
            if (resultCode == Activity.RESULT_OK) {
                updateSettings()
                WeatherUtil.updateWeather(this)
            }
        }
    }

    fun updateCalendarView() {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        val now = Calendar.getInstance()
        val calendarLayout = Util.checkGrantedPermission(this, Manifest.permission.READ_CALENDAR)

        empty_layout.visibility = View.VISIBLE
        calendar_layout.visibility = View.GONE
        var dateStringValue: String = String.format("%s%s", Constants.engDateFormat.format(now.time)[0].toUpperCase(), Constants.engDateFormat.format(now.time).substring(1))
        if (SP.getBoolean(Constants.PREF_ITA_FORMAT_DATE, false)) {
            dateStringValue = String.format("%s%s", Constants.itDateFormat.format(now.time)[0].toUpperCase(), Constants.itDateFormat.format(now.time).substring(1))
        }
        empty_date.text = dateStringValue
        //empty_date.setImageBitmap(Util.buildUpdate(this,  String.format("%s%s", Constants.dateFormat.format(now.time)[0].toUpperCase(), Constants.dateFormat.format(now.time).substring(1)), "fonts/product_sans_regular.ttf"))

        if (calendarLayout) {
            val e = CalendarUtil.getNextEvent(this)

            if (e.id != 0) {
                val difference = e.startDate - now.timeInMillis



                if (difference > 1000 * 60) {
                    var time = ""
                    val hour = TimeUnit.MILLISECONDS.toHours(difference)
                    if (hour > 0) {
                        time = hour.toString() + getString(R.string.h_code) + " "
                    }
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(difference - hour * 3600 * 1000)
                    if (minutes > 0) {
                        time += "" + minutes + getString(R.string.min_code)
                    }

                    next_event.text = String.format("%s %s %s", e.title, getString(R.string.in_code), time)
                } else {
                    next_event.text = String.format("%s", e.title)
                }

                if (!e.allDay) {
                    val startHour: String = if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) Constants.badHourFormat.format(e.startDate) else Constants.goodHourFormat.format(e.startDate)
                    val endHour: String = if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) Constants.badHourFormat.format(e.endDate) else Constants.goodHourFormat.format(e.endDate)
                    next_event_date.text = String.format("%s - %s", startHour, endHour)
                } else {
                    next_event_date.text = dateStringValue
                }

                empty_layout.visibility = View.GONE
                calendar_layout.visibility = View.VISIBLE
            }
        }
    }

    fun updateLocationView() {
        val locationLayout = Util.checkGrantedPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        if (locationLayout && SP.contains(Constants.PREF_WEATHER_TEMP) && SP.contains(Constants.PREF_WEATHER_ICON)) {
            weather.visibility = View.VISIBLE
            calendar_weather.visibility = View.VISIBLE
            val currentTemp = String.format(Locale.getDefault(), "%.0f °%s", SP.getFloat(Constants.PREF_WEATHER_TEMP, 0f), SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F"))


            weather_icon.visibility = View.VISIBLE
            empty_weather_icon.visibility = View.VISIBLE
            val icon: String = SP.getString(Constants.PREF_WEATHER_ICON, "")
            if (icon.equals("")) {
                weather_icon.visibility = View.GONE
                empty_weather_icon.visibility = View.GONE
            } else {
                weather_icon.setImageResource(WeatherUtil.getWeatherIconResource(icon))
                empty_weather_icon.setImageResource(WeatherUtil.getWeatherIconResource(icon))
            }

            temp.text = currentTemp
            calendar_temp.text = currentTemp
        } else {
            weather.visibility = View.GONE
            calendar_weather.visibility = View.GONE
        }
    }

    @SuppressLint("ApplySharedPref")
    fun updateSettings() {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        temp_unit.text = if (SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F").equals("F")) getString(R.string.fahrenheit) else getString(R.string.celsius)
        action_change_unit.setOnClickListener {
            SP.edit().putString(Constants.PREF_WEATHER_TEMP_UNIT, if (SP.getString(Constants.PREF_WEATHER_TEMP_UNIT, "F").equals("F")) "C" else "F").commit()
            sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))
            updateSettings()
        }

        all_day_label.text = if (SP.getBoolean(Constants.PREF_CALENDAR_ALL_DAY, false)) getString(R.string.settings_all_day_subtitle_visible) else getString(R.string.settings_all_day_subtitle_gone)
        action_show_all_day.setOnClickListener {
            SP.edit().putBoolean(Constants.PREF_CALENDAR_ALL_DAY, !SP.getBoolean(Constants.PREF_CALENDAR_ALL_DAY, false)).commit()
            sendBroadcast(Intent(Constants.ACTION_CALENDAR_UPDATE))
            updateSettings()
        }

        hour_format_label.text = if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) getString(R.string.settings_hour_format_subtitle_12) else getString(R.string.settings_hour_format_subtitle_24)
        action_hour_format.setOnClickListener {
            SP.edit().putString(Constants.PREF_HOUR_FORMAT, if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) "24" else "12").commit()
            sendBroadcast(Intent(Constants.ACTION_CALENDAR_UPDATE))
            updateSettings()
        }

        val now = Calendar.getInstance()
        var dateStringValue: String = String.format("%s%s", Constants.engDateFormat.format(now.time)[0].toUpperCase(), Constants.engDateFormat.format(now.time).substring(1))
        if (SP.getBoolean(Constants.PREF_ITA_FORMAT_DATE, false)) {
            dateStringValue = String.format("%s%s", Constants.itDateFormat.format(now.time)[0].toUpperCase(), Constants.itDateFormat.format(now.time).substring(1))
        }
        date_format_label.text = dateStringValue
        action_date_format.setOnClickListener {
            SP.edit().putBoolean(Constants.PREF_ITA_FORMAT_DATE, !SP.getBoolean(Constants.PREF_ITA_FORMAT_DATE, false)).commit()
            updateSettings()
            Util.updateWidget(this)
        }

        label_weather_refresh_period.text = getString(Util.getRefreshPeriodString(SP.getInt(Constants.PREF_WEATHER_REFRESH_PERIOD, 1)))
        action_weather_refresh_period.setOnClickListener {
            val dialog: BottomSheetMenuDialog = BottomSheetBuilder(this, R.style.Theme_Design_Light_BottomSheetDialog)
                    .setMode(BottomSheetBuilder.MODE_LIST)
                    .setMenu(R.menu.weather_refresh_period_menu)
                    .setIconTintColor(ContextCompat.getColor(this, R.color.black_50))
                    .delayDismissOnItemClick(false)
                    .setItemClickListener({ item: MenuItem ->
                        SP.edit().putInt(Constants.PREF_WEATHER_REFRESH_PERIOD, when (item.itemId) {
                            R.id.refresh_1 -> 1
                            R.id.refresh_2 -> 2
                            R.id.refresh_3 -> 3
                            R.id.refresh_4 -> 4
                            R.id.refresh_5 -> 5
                            else -> 1
                        }).commit()
                        updateSettings()
                        WeatherReceiver().setUpdates(this@MainActivity)
                    })
                    .createDialog()

            dialog.show()
        }

        label_custom_location.text = SP.getString(Constants.PREF_CUSTOM_LOCATION_ADD, getString(R.string.custom_location_gps))
        action_custom_location.setOnClickListener {
            startActivityForResult(Intent(this, CustomLocationActivity::class.java), Constants.RESULT_CODE_CUSTOM_LOCATION)
        }

        action_filter_calendar.setOnClickListener {
            val calendarSelectorList: List<CalendarSelector> = CalendarUtil.getCalendarList(this)
            var calFiltered = SP.getString(Constants.PREF_CALENDAR_FILTER, "")

            if (!calendarSelectorList.isEmpty()) {
                val calNames = calendarSelectorList.map { if (it.name.equals(it.account_name)) String.format("%s: %s", getString(R.string.main_calendar), it.name) else it.name }.toTypedArray()
                val calSelected = calendarSelectorList.map { !calFiltered.contains(" " + Integer.toString(it.id) + ",") }.toBooleanArray()

                AlertDialog.Builder(this).setTitle(getString(R.string.settings_filter_calendar_subtitle))
                        .setMultiChoiceItems(calNames, calSelected,
                                DialogInterface.OnMultiChoiceClickListener { dialog, item, isChecked ->
                                    val dialogItem: String = String.format(" %s%s", calendarSelectorList.get(item).id, ",")
                                    calFiltered = calFiltered.replace(dialogItem, "");
                                    if (!isChecked) {
                                        calFiltered += dialogItem
                                    }
                                })
                        .setPositiveButton(android.R.string.ok, { dialog: DialogInterface, _: Int ->
                            SP.edit().putString(Constants.PREF_CALENDAR_FILTER, calFiltered).commit()
                            sendBroadcast(Intent(Constants.ACTION_CALENDAR_UPDATE))
                            updateSettings()
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            } else {
                Toast.makeText(this, R.string.calendar_settings_list_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

}
