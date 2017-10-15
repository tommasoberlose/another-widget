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
import com.tommasoberlose.anotherwidget.receiver.WeatherReceiver
import java.util.*
import java.util.concurrent.TimeUnit
import android.content.Intent
import android.content.BroadcastReceiver
import com.tommasoberlose.anotherwidget.util.CalendarUtil
import com.tommasoberlose.anotherwidget.util.WeatherUtil
import android.content.DialogInterface
import android.graphics.drawable.Drawable
import android.support.design.widget.BottomSheetDialog
import android.util.Log
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.tommasoberlose.anotherwidget.`object`.CalendarSelector
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_menu_layout.view.*
import kotlinx.android.synthetic.main.the_widget.*


class MainActivity : AppCompatActivity() {

    var mAppWidgetId: Int = -1

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Fabric.with(this, Crashlytics())

        if (intent.extras?.containsKey(Constants.ACTION_EXTRA_OPEN_WEATHER_PROVIDER) == true) {
            startActivityForResult(Intent(this, WeatherProviderActivity::class.java), Constants.WEATHER_PROVIDER_REQUEST_CODE)
        }

        val extras = intent.extras
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID)

            if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                action_add_widget.visibility = View.VISIBLE

                action_add_widget.setOnClickListener {
                    addNewWidget()
                }
            }
        }



        action_menu.setOnClickListener {
            val mBottomSheetDialog: BottomSheetDialog = BottomSheetDialog(this)
            val menuView: View = getLayoutInflater().inflate(R.layout.main_menu_layout, null)
            menuView.action_support.setOnClickListener(object: View.OnClickListener {
                override fun onClick(p0: View?) {
                    Util.openURI(this@MainActivity, "https://paypal.me/tommasoberlose")
                    mBottomSheetDialog.dismiss()
                }
            })

            menuView.action_share.setOnClickListener(object: View.OnClickListener {
                override fun onClick(p0: View?) {
                    Util.share(this@MainActivity)
                    mBottomSheetDialog.dismiss()
                }
            })

            menuView.action_rate.setOnClickListener(object: View.OnClickListener {
                override fun onClick(p0: View?) {
                    Util.rateApp(this@MainActivity, "https://play.google.com/store/apps/details?id=com.tommasoberlose.anotherwidget")
                    mBottomSheetDialog.dismiss()
                }
            })

            menuView.action_feedback.setOnClickListener(object: View.OnClickListener {
                override fun onClick(p0: View?) {
                    Util.sendEmail(this@MainActivity)
                    mBottomSheetDialog.dismiss()
                }
            })

            menuView.action_refresh.setOnClickListener {
                WeatherUtil.updateWeather(this)
                CalendarUtil.updateEventList(this)
                Util.updateWidget(this)
                mBottomSheetDialog.dismiss()
            }
            mBottomSheetDialog.setContentView(menuView)
            mBottomSheetDialog.show();
        }
    }

    override fun onBackPressed() {
        if (mAppWidgetId > 0) {
            addNewWidget()
        } else {
            super.onBackPressed()
        }
    }

    fun addNewWidget() {
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
        sendBroadcast(Intent(Constants.ACTION_CALENDAR_UPDATE))
        sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(Constants.ACTION_SOMETHING_APPENED);
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
                sendBroadcast(Intent(Constants.ACTION_CALENDAR_UPDATE))
                updateAppWidget()
                updateSettings()
            }
            Constants.LOCATION_REQUEST_CODE -> if (!(permissions.size != 1 || grantResults.size != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                val SP = PreferenceManager.getDefaultSharedPreferences(this)
                sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))
                updateAppWidget()
                updateSettings()
            }
        }
    }

    fun updateUI() {
        updateSettings()
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

    @SuppressLint("ApplySharedPref")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        if (requestCode == Constants.RESULT_CODE_CUSTOM_LOCATION && resultCode == Activity.RESULT_OK) {
            sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))
            updateSettings()
        } else if (requestCode == Constants.CALENDAR_APP_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            SP.edit()
                    .putString(Constants.PREF_CALENDAR_APP_NAME, data.getStringExtra(Constants.RESULT_APP_NAME))
                    .putString(Constants.PREF_CALENDAR_APP_PACKAGE, data.getStringExtra(Constants.RESULT_APP_PACKAGE))
                    .commit()
            Util.updateWidget(this)
            updateSettings()
        } else if (requestCode == Constants.WEATHER_APP_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            SP.edit()
                    .putString(Constants.PREF_WEATHER_APP_NAME, data.getStringExtra(Constants.RESULT_APP_NAME))
                    .putString(Constants.PREF_WEATHER_APP_PACKAGE, data.getStringExtra(Constants.RESULT_APP_PACKAGE))
                    .commit()
            Util.updateWidget(this)
            updateSettings()
        } else if (requestCode == Constants.WEATHER_PROVIDER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))
            updateSettings()
        }
    }

    fun updateCalendarView() {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        val now = Calendar.getInstance()
        val calendarLayout = SP.getBoolean(Constants.PREF_SHOW_EVENTS, true) && Util.checkGrantedPermission(this, Manifest.permission.READ_CALENDAR)

        empty_layout.visibility = View.VISIBLE
        calendar_layout.visibility = View.GONE
        var dateStringValue: String = Util.getCapWordString(Constants.engDateFormat.format(now.time))
        if (SP.getBoolean(Constants.PREF_ITA_FORMAT_DATE, false)) {
            dateStringValue = Util.getCapWordString(Constants.itDateFormat.format(now.time))
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
                    var dayDiff = TimeUnit.MILLISECONDS.toDays(e.endDate - e.startDate)

                    val startCal = Calendar.getInstance()
                    startCal.timeInMillis = e.startDate

                    val endCal = Calendar.getInstance()
                    endCal.timeInMillis = e.endDate

                    if (startCal.get(Calendar.HOUR_OF_DAY) >= endCal.get(Calendar.HOUR_OF_DAY)) {
                        dayDiff++
                    }
                    var multipleDay: String = ""
                    if (dayDiff > 0) {
                        multipleDay = String.format(" (+%s%s)", dayDiff, getString(R.string.day_char))
                    }
                    next_event_date.text = String.format("%s - %s%s", startHour, endHour, multipleDay)
                } else {
                    next_event_date.text = dateStringValue
                }

                empty_layout.visibility = View.GONE
                calendar_layout.visibility = View.VISIBLE
            }
        }
    }

    fun updateLocationView() {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        val locationLayout = SP.getBoolean(Constants.PREF_SHOW_WEATHER, true)

        if (locationLayout && SP.contains(Constants.PREF_WEATHER_TEMP) && SP.contains(Constants.PREF_WEATHER_ICON)) {
            weather.visibility = View.VISIBLE
            calendar_weather.visibility = View.VISIBLE
            val currentTemp = String.format(Locale.getDefault(), "%.0f °%s", SP.getFloat(Constants.PREF_WEATHER_TEMP, 0f), SP.getString(Constants.PREF_WEATHER_REAL_TEMP_UNIT, "F"))


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

        if (SP.getBoolean(Constants.PREF_SHOW_EVENTS, true) && Util.checkGrantedPermission(this, Manifest.permission.READ_CALENDAR)) {
            calendar_settings.visibility = View.VISIBLE
            action_show_events.setOnClickListener {
                SP.edit()
                        .putBoolean(Constants.PREF_SHOW_EVENTS, false)
                        .commit()
                sendBroadcast(Intent(Constants.ACTION_CALENDAR_UPDATE))
                updateSettings()
                updateAppWidget()
            }
            show_events_label.text = getString(R.string.show_events_visible)
        } else {
            calendar_settings.visibility= View.GONE
            action_show_events.setOnClickListener {
                if (Util.checkGrantedPermission(this, Manifest.permission.READ_CALENDAR)) {
                    SP.edit()
                            .putBoolean(Constants.PREF_SHOW_EVENTS, true)
                            .commit()
                    sendBroadcast(Intent(Constants.ACTION_CALENDAR_UPDATE))
                    updateSettings()
                    updateAppWidget()
                } else {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_CALENDAR), Constants.CALENDAR_REQUEST_CODE)
                }
            }
            show_events_label.text = if (Util.checkGrantedPermission(this, Manifest.permission.READ_CALENDAR)) getString(R.string.show_events_not_visible) else getString(R.string.description_permission_calendar)
        }

        if (SP.getBoolean(Constants.PREF_SHOW_WEATHER, true) && Util.checkGrantedPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            weather_settings.visibility = View.VISIBLE
            action_show_weather.setOnClickListener {
                SP.edit()
                        .putBoolean(Constants.PREF_SHOW_WEATHER, false)
                        .commit()
                sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))
                updateSettings()
                updateAppWidget()
            }
            show_weather_label.text = getString(R.string.show_weather_visible)
        } else {
            weather_settings.visibility= View.GONE
            action_show_weather.setOnClickListener {
                if (Util.checkGrantedPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    SP.edit()
                            .putBoolean(Constants.PREF_SHOW_WEATHER, true)
                            .commit()
                    sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))
                    updateSettings()
                    updateAppWidget()
                } else {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), Constants.LOCATION_REQUEST_CODE)
                }
            }
            show_weather_label.text = if (Util.checkGrantedPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) getString(R.string.show_weather_not_visible) else getString(R.string.description_permission_location)
        }

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
            Util.updateWidget(this)
            updateSettings()
            updateAppWidget()
        }

        val now = Calendar.getInstance()
        var dateStringValue: String = String.format("%s%s", Constants.engDateFormat.format(now.time)[0].toUpperCase(), Constants.engDateFormat.format(now.time).substring(1))
        if (SP.getBoolean(Constants.PREF_ITA_FORMAT_DATE, false)) {
            dateStringValue = String.format("%s%s", Constants.itDateFormat.format(now.time)[0].toUpperCase(), Constants.itDateFormat.format(now.time).substring(1))
        }
        date_format_label.text = dateStringValue
        action_date_format.setOnClickListener {
            SP.edit().putBoolean(Constants.PREF_ITA_FORMAT_DATE, !SP.getBoolean(Constants.PREF_ITA_FORMAT_DATE, false)).commit()
            Util.updateWidget(this)
            updateAppWidget()
            updateSettings()
        }

        label_weather_refresh_period.text = getString(Util.getRefreshPeriodString(SP.getInt(Constants.PREF_WEATHER_REFRESH_PERIOD, 1)))
        action_weather_refresh_period.setOnClickListener {
            SP.edit().putInt(Constants.PREF_WEATHER_REFRESH_PERIOD, when (SP.getInt(Constants.PREF_WEATHER_REFRESH_PERIOD, 1)) {
                0 -> 1
                1 -> 2
                2 -> 3
                3 -> 4
                4 -> 5
                5 -> 0
                else -> 1
            }).commit()
            updateSettings()
            WeatherReceiver().setUpdates(this@MainActivity)
        }

        show_until_label.text = getString(Util.getShowUntilString(SP.getInt(Constants.PREF_SHOW_UNTIL, 1)))
        action_show_until.setOnClickListener {
            SP.edit().putInt(Constants.PREF_SHOW_UNTIL, when (SP.getInt(Constants.PREF_SHOW_UNTIL, 1)) {
                0 -> 1
                1 -> 2
                2 -> 3
                3 -> 4
                4 -> 5
                5 -> 0
                else -> 1
            }).commit()
            updateSettings()
            sendBroadcast(Intent(Constants.ACTION_CALENDAR_UPDATE))
        }

        label_custom_location.text = SP.getString(Constants.PREF_CUSTOM_LOCATION_ADD, getString(R.string.custom_location_gps))
        action_custom_location.setOnClickListener {
            startActivityForResult(Intent(this, CustomLocationActivity::class.java), Constants.RESULT_CODE_CUSTOM_LOCATION)
        }

        calendar_app_label.text = SP.getString(Constants.PREF_CALENDAR_APP_NAME, getString(R.string.default_name))
        action_calendar_app.setOnClickListener {
            startActivityForResult(Intent(this, ChooseApplicationActivity::class.java), Constants.CALENDAR_APP_REQUEST_CODE)
        }

        weather_app_label.text = SP.getString(Constants.PREF_WEATHER_APP_NAME, getString(R.string.default_name))
        action_weather_app.setOnClickListener {
            startActivityForResult(Intent(this, ChooseApplicationActivity::class.java), Constants.WEATHER_APP_REQUEST_CODE)
        }

        if (!SP.getString(Constants.PREF_WEATHER_PROVIDER_API_KEY, "").equals("")) {
            label_weather_provider_api_key.text = getString(R.string.settings_weather_provider_api_key_subtitle_all_set)
            alert_icon.visibility = View.GONE
        } else {
            label_weather_provider_api_key.text = getString(R.string.settings_weather_provider_api_key_subtitle_not_set)
            alert_icon.visibility = View.VISIBLE
        }
        action_weather_provider_api_key.setOnClickListener {
            startActivityForResult(Intent(this, WeatherProviderActivity::class.java), Constants.WEATHER_PROVIDER_REQUEST_CODE)
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
