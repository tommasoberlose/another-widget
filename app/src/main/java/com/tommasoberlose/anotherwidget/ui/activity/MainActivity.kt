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
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.support.design.widget.BottomSheetDialog
import android.support.v4.content.ContextCompat
import android.text.Html
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.util.TypedValue
import android.widget.Toast
import com.crashlytics.android.Crashlytics
import com.pes.androidmaterialcolorpickerdialog.ColorPicker
import com.tommasoberlose.anotherwidget.`object`.CalendarSelector
import com.tommasoberlose.anotherwidget.receiver.UpdatesReceiver
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.key_time_wait_layout.view.*
import kotlinx.android.synthetic.main.main_menu_layout.view.*
import kotlinx.android.synthetic.main.the_widget.*
import kotlinx.android.synthetic.main.the_widget.view.*


class MainActivity : AppCompatActivity() {

    private var mAppWidgetId: Int = -1

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateUI()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Fabric.with(this, Crashlytics())
        sendBroadcast(Intent(Constants.ACTION_CALENDAR_UPDATE))
        sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))

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

            menuView.action_support.setOnClickListener {
                startActivity(Intent(this, SupportDevActivity::class.java))
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
            setResult(Activity.RESULT_OK)
            finish()
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
        filter.addAction(Constants.ACTION_SOMETHING_HAPPENED);
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
        updateClockView()

        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        val displayMetrics = Resources.getSystem().displayMetrics
        var height = Util.convertDpToPixel(120f, this).toInt()
        if (SP.getBoolean(Constants.PREF_SHOW_CLOCK, false)) {
            height += Util.convertSpToPixels(SP.getFloat(Constants.PREF_TEXT_CLOCK_SIZE, 90f), this).toInt() + Util.convertDpToPixel(8f, this).toInt()
        }
        if (SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f) + SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) > 50) {
            height += Util.convertDpToPixel(24f, this).toInt()
        }
        widget_bitmap.setImageBitmap(Util.getBitmapFromView(main_layout, displayMetrics.widthPixels, height - Util.convertDpToPixel(32f, this).toInt()))
        widget.layoutParams.height = height + Util.convertDpToPixel(16f, this).toInt()
    }


    internal fun updateAppWidget() {
        val wallpaper: Drawable? = Util.getCurrentWallpaper(this)
        if (wallpaper != null) {
            widget_bg.setImageDrawable(wallpaper)
        }
        widget_bg.setBackgroundColor(Color.WHITE)
        updateCalendarView()
        updateLocationView()
        updateClockView()
    }

    @SuppressLint("ApplySharedPref")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        if (requestCode == Constants.RESULT_CODE_CUSTOM_LOCATION && resultCode == Activity.RESULT_OK) {
            sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))
            updateSettings()
        } else if (requestCode == Constants.CALENDAR_APP_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            SP.edit()
                    .putString(Constants.PREF_CALENDAR_APP_NAME, if (data.getStringExtra(Constants.RESULT_APP_NAME) != "") data.getStringExtra(Constants.RESULT_APP_NAME) else getString(R.string.default_calendar_app))
                    .putString(Constants.PREF_CALENDAR_APP_PACKAGE, data.getStringExtra(Constants.RESULT_APP_PACKAGE))
                    .commit()
            Util.updateWidget(this)
            updateSettings()
        } else if (requestCode == Constants.WEATHER_APP_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            SP.edit()
                    .putString(Constants.PREF_WEATHER_APP_NAME, if (data.getStringExtra(Constants.RESULT_APP_NAME) != "") data.getStringExtra(Constants.RESULT_APP_NAME) else getString(R.string.default_weather_app))
                    .putString(Constants.PREF_WEATHER_APP_PACKAGE, data.getStringExtra(Constants.RESULT_APP_PACKAGE))
                    .commit()
            Util.updateWidget(this)
            updateSettings()
        } else if (requestCode == Constants.EVENT_APP_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            SP.edit()
                    .putString(Constants.PREF_EVENT_APP_NAME, if (data.getStringExtra(Constants.RESULT_APP_NAME) != "") data.getStringExtra(Constants.RESULT_APP_NAME) else getString(R.string.default_event_app))
                    .putString(Constants.PREF_EVENT_APP_PACKAGE, data.getStringExtra(Constants.RESULT_APP_PACKAGE))
                    .commit()
            Util.updateWidget(this)
            updateSettings()
        } else if (requestCode == Constants.CLOCK_APP_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            SP.edit()
                    .putString(Constants.PREF_CLOCK_APP_NAME, if (data.getStringExtra(Constants.RESULT_APP_NAME) != "") data.getStringExtra(Constants.RESULT_APP_NAME) else getString(R.string.default_clock_app))
                    .putString(Constants.PREF_CLOCK_APP_PACKAGE, data.getStringExtra(Constants.RESULT_APP_PACKAGE))
                    .commit()
            Util.updateWidget(this)
            updateSettings()
        } else if (requestCode == Constants.WEATHER_PROVIDER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            WeatherReceiver().setOneTimeUpdate(this)
            sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))
            updateSettings()
        }
    }

    fun updateClockView() {
        val SP = PreferenceManager.getDefaultSharedPreferences(this)
        if (!SP.getBoolean(Constants.PREF_SHOW_CLOCK, false)) {
            time.visibility = View.GONE
        } else {
            time.visibility = View.VISIBLE
        }
        val now = Calendar.getInstance()
        if (SP.getString(Constants.PREF_HOUR_FORMAT, "12") == "12") {
            val textBadHour = SpannableString(Constants.badHourFormat.format(now.timeInMillis))
            textBadHour.setSpan(RelativeSizeSpan(0.4f), textBadHour.length - 2,
                    textBadHour.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);

            time.text = textBadHour
        } else {
            time.text = Constants.goodHourFormat.format(now.timeInMillis)
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

        if (calendarLayout) {
            val e = CalendarUtil.getNextEvent(this)

            if (e.id != 0) {
                next_event.text = e.title

                if (SP.getBoolean(Constants.PREF_SHOW_NEXT_EVENT, false) && CalendarUtil.getEventsCount(this) > 1) {
                    multiple_events.visibility = View.VISIBLE
                } else {
                    multiple_events.visibility = View.GONE
                }

                if (SP.getBoolean(Constants.PREF_SHOW_DIFF_TIME, true)) {
                    next_event_difference_time.text = Util.getDifferenceText(this, now.timeInMillis, e.startDate)
                    next_event_difference_time.visibility = View.VISIBLE
                } else {
                    next_event_difference_time.visibility = View.GONE
                }


                if (SP.getInt(Constants.PREF_SECOND_ROW_INFORMATION, 0) == 2 && Util.getNextAlarm(this) != null) {
                    second_row_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_action_alarm))
                    next_event_date.text = Util.getNextAlarm(this)
                } else if (!e.address.equals("") && SP.getInt(Constants.PREF_SECOND_ROW_INFORMATION, 0) == 1) {
                    second_row_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_action_location))
                    next_event_date.text = e.address
                } else {
                    second_row_icon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_action_calendar))
                    if (!e.allDay) {
                        val startHour: String = if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) Constants.badHourFormat.format(e.startDate) else Constants.goodHourFormat.format(e.startDate)
                        val endHour: String = if (SP.getString(Constants.PREF_HOUR_FORMAT, "12").equals("12")) Constants.badHourFormat.format(e.endDate) else Constants.goodHourFormat.format(e.endDate)
                        var dayDiff = TimeUnit.MILLISECONDS.toDays(e.endDate - e.startDate)

                        val startCal = Calendar.getInstance()
                        startCal.timeInMillis = e.startDate

                        val endCal = Calendar.getInstance()
                        endCal.timeInMillis = e.endDate

                        if (startCal.get(Calendar.HOUR_OF_DAY) > endCal.get(Calendar.HOUR_OF_DAY)) {
                            dayDiff++
                        } else if (startCal.get(Calendar.HOUR_OF_DAY) == endCal.get(Calendar.HOUR_OF_DAY) && startCal.get(Calendar.MINUTE) >= endCal.get(Calendar.MINUTE)) {
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
                }

                empty_layout.visibility = View.GONE
                calendar_layout.visibility = View.VISIBLE
            }
        }

        empty_date.setTextColor(Util.getFontColor(SP))
        divider1.setTextColor(Util.getFontColor(SP))
        temp.setTextColor(Util.getFontColor(SP))
        next_event.setTextColor(Util.getFontColor(SP))
        next_event_difference_time.setTextColor(Util.getFontColor(SP))
        next_event_date.setTextColor(Util.getFontColor(SP))
        divider2.setTextColor(Util.getFontColor(SP))
        calendar_temp.setTextColor(Util.getFontColor(SP))
        second_row_icon.setColorFilter(Util.getFontColor(SP))
        time.setTextColor(Util.getFontColor(SP))


        empty_date.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
        divider1.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
        temp.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
        next_event.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
        next_event_difference_time.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f))
        next_event_date.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
        divider2.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
        calendar_temp.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f))
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, SP.getFloat(Constants.PREF_TEXT_CLOCK_SIZE, 90f))

        second_row_icon.scaleX = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 18f
        second_row_icon.scaleY = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 18f

        weather_icon.scaleX = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 20f
        weather_icon.scaleY = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 20f

        empty_weather_icon.scaleX = SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f) / 24f
        empty_weather_icon.scaleY = SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f) / 24f

        multiple_events.scaleX = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 16f
        multiple_events.scaleY = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) / 16f

        val shadowRadius = when (SP.getInt(Constants.PREF_TEXT_SHADOW, 1)) {
            0 -> 0f
            1 -> 5f
            2 -> 4f
            else -> 5f
        }
        val shadowColor =  when (SP.getInt(Constants.PREF_TEXT_SHADOW, 1)) {
            0 -> Color.TRANSPARENT
            1 -> R.color.black_50
            2 -> Color.BLACK
            else -> R.color.black_50
        }
        val shadowDy =  when (SP.getInt(Constants.PREF_TEXT_SHADOW, 1)) {
            0 -> 0f
            1 -> 0f
            2 -> 1f
            else -> 0f
        }
        empty_date.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        divider1.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        temp.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        next_event.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        next_event_difference_time.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        next_event_date.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        divider2.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)
        calendar_temp.setShadowLayer(shadowRadius, 0f, 0f, shadowColor)
        time.setShadowLayer(shadowRadius, 0f, shadowDy, shadowColor)

        if (SP.getInt(Constants.PREF_CUSTOM_FONT, Constants.CUSTOM_FONT_PRODUCT_SANS) == Constants.CUSTOM_FONT_PRODUCT_SANS) {
            val product_sans: Typeface = Typeface.createFromAsset(assets, "fonts/product_sans_regular.ttf")
            empty_date.typeface = product_sans
            divider1.typeface = product_sans
            temp.typeface = product_sans
            next_event.typeface = product_sans
            next_event_difference_time.typeface = product_sans
            next_event_date.typeface = product_sans
            divider2.typeface = product_sans
            calendar_temp.typeface = product_sans
            time.typeface = product_sans
        } else {
            empty_date.typeface = Typeface.DEFAULT
            divider1.typeface = Typeface.DEFAULT
            temp.typeface = Typeface.DEFAULT
            next_event.typeface = Typeface.DEFAULT
            next_event_difference_time.typeface = Typeface.DEFAULT
            next_event_date.typeface = Typeface.DEFAULT
            divider2.typeface = Typeface.DEFAULT
            calendar_temp.typeface = Typeface.DEFAULT
            time.typeface = Typeface.DEFAULT
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

        if (SP.getBoolean(Constants.PREF_SHOW_CLOCK, false)) {
            clock_settings.visibility = View.VISIBLE
            action_show_clock.setOnClickListener {
                SP.edit()
                        .putBoolean(Constants.PREF_SHOW_CLOCK, false)
                        .commit()
                sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
            }
            show_clock_label.text = getString(R.string.show_clock_visible)
        } else {
            clock_settings.visibility= View.GONE
            action_show_clock.setOnClickListener {
                SP.edit()
                        .putBoolean(Constants.PREF_SHOW_CLOCK, true)
                        .commit()
                sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
            }
            show_clock_label.text = getString(R.string.show_clock_not_visible)
        }


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

        show_multiple_events_label.text = if (SP.getBoolean(Constants.PREF_SHOW_NEXT_EVENT, true)) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
        action_show_multiple_events.setOnClickListener {
            SP.edit().putBoolean(Constants.PREF_SHOW_NEXT_EVENT, !SP.getBoolean(Constants.PREF_SHOW_NEXT_EVENT, true)).commit()
            sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
            updateSettings()
        }

        show_diff_time_label.text = if (SP.getBoolean(Constants.PREF_SHOW_DIFF_TIME, true)) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
        action_show_diff_time.setOnClickListener {
            SP.edit().putBoolean(Constants.PREF_SHOW_DIFF_TIME, !SP.getBoolean(Constants.PREF_SHOW_DIFF_TIME, true)).commit()
            sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
            updateSettings()
        }

        show_declined_events_label.text = if (SP.getBoolean(Constants.PREF_SHOW_DECLINED_EVENTS, true)) getString(R.string.settings_visible) else getString(R.string.settings_not_visible)
        action_show_declined_events.setOnClickListener {
            SP.edit().putBoolean(Constants.PREF_SHOW_DECLINED_EVENTS, !SP.getBoolean(Constants.PREF_SHOW_DECLINED_EVENTS, true)).commit()
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

        second_row_info_label.text = getString(Util.getSecondRowInfoString(SP.getInt(Constants.PREF_SECOND_ROW_INFORMATION, 0)))
        action_second_row_info.setOnClickListener {
            SP.edit().putInt(Constants.PREF_SECOND_ROW_INFORMATION, when (SP.getInt(Constants.PREF_SECOND_ROW_INFORMATION, 0)) {
                0 -> 1
                1 -> 2
                2 -> 0
                else -> 0
            }).commit()
            updateSettings()
            sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
        }

        main_text_size_label.text = String.format("%.0f%s", SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f), "sp")
        action_main_text_size.setOnClickListener {
            var fontSize = SP.getFloat(Constants.PREF_TEXT_MAIN_SIZE, 24f) + 1
            if (fontSize > 36) {
                fontSize = 20f
            }
            SP.edit().putFloat(Constants.PREF_TEXT_MAIN_SIZE, fontSize).commit()
            Util.updateWidget(this)
            updateSettings()
            updateAppWidget()
        }

        second_text_size_label.text = String.format("%.0f%s", SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f), "sp")
        action_second_text_size.setOnClickListener {
            var fontSize = SP.getFloat(Constants.PREF_TEXT_SECOND_SIZE, 16f) + 1
            if (fontSize > 28) {
                fontSize = 12f
            }
            SP.edit().putFloat(Constants.PREF_TEXT_SECOND_SIZE, fontSize).commit()
            Util.updateWidget(this)
            updateSettings()
        }

        clock_text_size_label.text = String.format("%.0f%s", SP.getFloat(Constants.PREF_TEXT_CLOCK_SIZE, 90f), "sp")
        action_clock_text_size.setOnClickListener {
            var fontSize = SP.getFloat(Constants.PREF_TEXT_CLOCK_SIZE, 90f) + 5
            if (fontSize > 110) {
                fontSize = 70f
            }
            SP.edit().putFloat(Constants.PREF_TEXT_CLOCK_SIZE, fontSize).commit()
            Util.updateWidget(this)
            updateSettings()
        }

        val textColor = try {
            Color.parseColor(SP.getString(Constants.PREF_TEXT_COLOR, "#FFFFFF"))
        } catch (e: Exception) {
            SP.edit().remove(Constants.PREF_TEXT_COLOR).commit()
            Color.parseColor(SP.getString(Constants.PREF_TEXT_COLOR, "#FFFFFF"))
        }
        font_color_label.text = SP.getString(Constants.PREF_TEXT_COLOR, "#FFFFFF").toUpperCase()
        action_font_color.setOnClickListener {
            val cp: ColorPicker = ColorPicker(this@MainActivity, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
            cp.setOnColorSelected { color ->
                SP.edit().putString(Constants.PREF_TEXT_COLOR, "#" + Integer.toHexString(color)).commit()
                Util.updateWidget(this)
                updateSettings()
                updateAppWidget()
                cp.dismiss()
            }
            cp.show()
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


        if (SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) == Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) {
            action_weather_refresh_period.visibility = View.GONE
        } else {
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
            action_weather_refresh_period.visibility = View.VISIBLE
        }

        show_until_label.text = getString(Util.getShowUntilString(SP.getInt(Constants.PREF_SHOW_UNTIL, 1)))
        action_show_until.setOnClickListener {
            SP.edit().putInt(Constants.PREF_SHOW_UNTIL, when (SP.getInt(Constants.PREF_SHOW_UNTIL, 1)) {
                0 -> 1
                1 -> 2
                2 -> 3
                3 -> 4
                4 -> 5
                5 -> 6
                6 -> 7
                7 -> 0
                else -> 1
            }).commit()
            updateSettings()
            sendBroadcast(Intent(Constants.ACTION_CALENDAR_UPDATE))
        }

        text_shadow_label.text = getString(Util.getTextShadowString(SP.getInt(Constants.PREF_TEXT_SHADOW, 1)))
        action_text_shadow.setOnClickListener {
            SP.edit().putInt(Constants.PREF_TEXT_SHADOW, when (SP.getInt(Constants.PREF_TEXT_SHADOW, 1)) {
                0 -> 1
                1 -> 2
                2 -> 0
                else -> 1
            }).commit()
            sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
            updateSettings()
            updateAppWidget()
        }

        custom_font_label.text = getString(Util.getCustomFontLabel(SP.getInt(Constants.PREF_CUSTOM_FONT, Constants.CUSTOM_FONT_PRODUCT_SANS)))
        action_custom_font.setOnClickListener {
            SP.edit().putInt(Constants.PREF_CUSTOM_FONT, when (SP.getInt(Constants.PREF_CUSTOM_FONT, Constants.CUSTOM_FONT_PRODUCT_SANS)) {
                0 -> Constants.CUSTOM_FONT_PRODUCT_SANS
                Constants.CUSTOM_FONT_PRODUCT_SANS -> 0
                else -> Constants.CUSTOM_FONT_PRODUCT_SANS
            }).commit()
            sendBroadcast(Intent(Constants.ACTION_TIME_UPDATE))
            updateSettings()
            updateAppWidget()
        }

        if (SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) == Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) {
            action_custom_location.visibility = View.GONE
        } else {
            label_custom_location.text = SP.getString(Constants.PREF_CUSTOM_LOCATION_ADD, getString(R.string.custom_location_gps))
            action_custom_location.setOnClickListener {
                startActivityForResult(Intent(this, CustomLocationActivity::class.java), Constants.RESULT_CODE_CUSTOM_LOCATION)
            }
            action_custom_location.visibility = View.VISIBLE
        }

        if (SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) == Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS) {
            label_weather_provider_api_key.text = getString(R.string.provider_google_awareness)
            alert_icon.visibility = View.GONE
        } else {
            if (SP.getString(when (SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS)) {
                Constants.WEATHER_PROVIDER_OPEN_WEATHER -> Constants.PREF_OPEN_WEATHER_API_KEY
                else -> Constants.PREF_OPEN_WEATHER_API_KEY
            }, "") == ("")) {
                label_weather_provider_api_key.text = getString(R.string.settings_weather_provider_api_key_subtitle_not_set)
                alert_icon.visibility = View.VISIBLE
            } else {
                label_weather_provider_api_key.text = getString(when (SP.getInt(Constants.PREF_WEATHER_PROVIDER, Constants.WEATHER_PROVIDER_GOOGLE_AWARENESS)) {
                    Constants.WEATHER_PROVIDER_OPEN_WEATHER -> R.string.provider_open_weather
                    else -> R.string.settings_weather_provider_api_key_subtitle_all_set
                })
                alert_icon.visibility = View.GONE
            }
        }
        action_weather_provider_api_key.setOnClickListener {
            startActivityForResult(Intent(this, WeatherProviderActivity::class.java), Constants.WEATHER_PROVIDER_REQUEST_CODE)
        }

        calendar_app_label.text = SP.getString(Constants.PREF_CALENDAR_APP_NAME, getString(R.string.default_calendar_app))
        action_calendar_app.setOnClickListener {
            val i = Intent(this, ChooseApplicationActivity::class.java)
            i.putExtra("requestCode", Constants.CALENDAR_APP_REQUEST_CODE)
            startActivityForResult(i, Constants.CALENDAR_APP_REQUEST_CODE)
        }

        weather_app_label.text = SP.getString(Constants.PREF_WEATHER_APP_NAME, getString(R.string.default_weather_app))
        action_weather_app.setOnClickListener {
            startActivityForResult(Intent(this, ChooseApplicationActivity::class.java), Constants.WEATHER_APP_REQUEST_CODE)
        }

        clock_app_label.text = SP.getString(Constants.PREF_CLOCK_APP_NAME, getString(R.string.default_clock_app))
        action_clock_app.setOnClickListener {
            startActivityForResult(Intent(this, ChooseApplicationActivity::class.java), Constants.CLOCK_APP_REQUEST_CODE)
        }

        event_app_label.text = SP.getString(Constants.PREF_EVENT_APP_NAME, getString(R.string.default_event_app))
        action_event_app.setOnClickListener {
            startActivityForResult(Intent(this, ChooseApplicationActivity::class.java), Constants.EVENT_APP_REQUEST_CODE)
        }

        action_filter_calendar.setOnClickListener {
            val calendarSelectorList: List<CalendarSelector> = CalendarUtil.getCalendarList(this).map { CalendarSelector(it.id.toInt(), it.displayName, it.accountName) }
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
