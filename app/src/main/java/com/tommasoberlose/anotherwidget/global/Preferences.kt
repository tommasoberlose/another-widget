package com.tommasoberlose.anotherwidget.global

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate.*
import com.chibatching.kotpref.KotprefModel

object Preferences : KotprefModel() {
    override val commitAllPropertiesByDefault: Boolean = true

    var darkThemePreference by intPref(default = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) MODE_NIGHT_FOLLOW_SYSTEM else MODE_NIGHT_AUTO_BATTERY)

    var showEvents by booleanPref(key = "PREF_SHOW_EVENTS", default = false)
    var showWeather by booleanPref(key = "PREF_SHOW_WEATHER")
    var weatherIcon by stringPref(key = "PREF_WEATHER_ICON")
    var weatherTemp by floatPref(key = "PREF_WEATHER_TEMP")
    var weatherTempUnit by stringPref(key = "PREF_WEATHER_TEMP_UNIT", default = "F")
    var weatherRealTempUnit by stringPref(key = "PREF_WEATHER_REAL_TEMP_UNIT", default = "F")
    var calendarAllDay by booleanPref(key = "PREF_CALENDAR_ALL_DAY", default = false)
    var calendarFilter by stringPref(key = "PREF_CALENDAR_FILTER", default = "")

    var eventId by intPref(key = "PREF_EVENT_ID", default = -1)
    var nextEventId by longPref(key = "PREF_NEXT_EVENT_ID", default = -1)
    var nextEventName by stringPref(key = "PREF_NEXT_EVENT_NAME")
    var nextEventStartDate by longPref(key = "PREF_NEXT_EVENT_START_DATE")
    var nextEventAllDay by booleanPref(key = "PREF_NEXT_EVENT_ALL_DAY")
    var nextEventLocation by stringPref(key = "PREF_NEXT_EVENT_LOCATION")
    var nextEventEndDate by longPref(key = "PREF_NEXT_EVENT_END_DATE")
    var nextEventCalendarId by intPref(key = "PREF_NEXT_EVENT_CALENDAR_ID")
    var customLocationLat by stringPref(key = "PREF_CUSTOM_LOCATION_LAT", default = "")
    var customLocationLon by stringPref(key = "PREF_CUSTOM_LOCATION_LON", default = "")
    var customLocationAdd by stringPref(key = "PREF_CUSTOM_LOCATION_ADD", default = "")
    var hourFormat by stringPref(key = "PREF_HOUR_FORMAT", default = "12")
    var dateFormat by booleanPref(key = "PREF_ITA_FORMAT_DATE", default = false)
    var weatherRefreshPeriod by intPref(key = "PREF_WEATHER_REFRESH_PERIOD", default = 1)
    var showUntil by intPref(key = "PREF_SHOW_UNTIL", default = 1)
    var calendarAppName by stringPref(key = "PREF_CALENDAR_APP_NAME", default = "")
    var calendarAppPackage by stringPref(key = "PREF_CALENDAR_APP_PACKAGE", default = "")
    var weatherAppName by stringPref(key = "PREF_WEATHER_APP_NAME", default = "")
    var weatherAppPackage by stringPref(key = "PREF_WEATHER_APP_PACKAGE", default = "")
    var weatherProviderApi by stringPref(key = "PREF_WEATHER_PROVIDER_API_KEY", default = "")
    var eventAppName by stringPref(key = "PREF_EVENT_APP_NAME", default = "")
    var eventAppPackage by stringPref(key = "PREF_EVENT_APP_PACKAGE", default = "")
    var showEventLocation by stringPref(key = "PREF_SHOW_EVENT_LOCATION", default = "")
    var textGlobalColor by stringPref(key = "PREF_TEXT_COLOR", default = "#FFFFFF")
    var textMainSize by floatPref(key = "PREF_TEXT_MAIN_SIZE", default = 26f)
    var textSecondSize by floatPref(key = "PREF_TEXT_SECOND_SIZE", default = 18f)
    var clockTextSize by floatPref(key = "PREF_TEXT_CLOCK_SIZE", default = 90f)
    var showClock by booleanPref(key = "PREF_SHOW_CLOCK", default = false)
    var clockAppName by stringPref(key = "PREF_CLOCK_APP_NAME", default = "")
    var clockAppPackage by stringPref(key = "PREF_CLOCK_APP_PACKAGE", default = "")
    var showNextAlarm by booleanPref(default = true)
    var textShadow by intPref(key = "PREF_TEXT_SHADOW", default = 1)
    var showDiffTime by booleanPref(key = "PREF_SHOW_DIFF_TIME")
    var showDeclinedEvents by booleanPref(key = "PREF_SHOW_DECLINED_EVENTS", default = true)
    var openWeatherApiKey by stringPref(key = "PREF_OPEN_WEATHER_API_KEY", default = "")
    var darkSkyApiKey by stringPref(key = "PREF_DARK_SKY_API_KEY", default = "")
    var wuApiKey by stringPref(key = "PREF_WU_API_KEY", default = "")
    var secondRowInformation by intPref(key = "PREF_SECOND_ROW_INFORMATION", default = 1)
    var customFont by intPref(key = "PREF_CUSTOM_FONT", default = Constants.CUSTOM_FONT_PRODUCT_SANS)
    var customFontFile by stringPref(key = "PREF_CUSTOM_FONT_FILE")
    var showNextEvent by booleanPref(key = "PREF_SHOW_NEXT_EVENT", default = true)
    var showGpsInformation by booleanPref(key = "PREF_SHOW_GPS_NOTIFICATION", default = true)

    var showWallpaper by booleanPref(default = false)
}
