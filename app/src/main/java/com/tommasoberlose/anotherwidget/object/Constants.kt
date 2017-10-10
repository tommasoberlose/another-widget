package com.tommasoberlose.anotherwidget.`object`

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by tommaso on 05/10/17.
 */

object Constants {
    val CALENDAR_REQUEST_CODE = 1
    val LOCATION_REQUEST_CODE = 2

    val PREF_FIRST_STEP = "PREF_FIRST_STEP"
    val PREF_WEATHER_ICON = "PREF_WEATHER_ICON"
    val PREF_WEATHER_TEMP = "PREF_WEATHER_TEMP"
    val PREF_WEATHER_TEMP_UNIT = "PREF_WEATHER_TEMP_UNIT"
    val PREF_CALENDAR_ALL_DAY = "PREF_CALENDAR_ALL_DAY"
    val PREF_CALENDAR_FILTER = "PREF_CALENDAR_FILTER"

    val PREF_NEXT_EVENT_ID = "PREF_NEXT_EVENT_ID"
    val PREF_NEXT_EVENT_NAME = "PREF_NEXT_EVENT_NAME"
    val PREF_NEXT_EVENT_START_DATE = "PREF_NEXT_EVENT_START_DATE"
    val PREF_NEXT_EVENT_END_DATE = "PREF_NEXT_EVENT_END_DATE"
    val PREF_NEXT_EVENT_CALENDAR_ID = "PREF_NEXT_EVENT_CALENDAR_ID"

    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val ACTION_TIME_UPDATE = "com.tommasoberlose.anotherwidget.action.ACTION_TIME_UPDATE"
    val ACTION_WEATHER_UPDATE = "com.tommasoberlose.anotherwidget.action.ACTION_WEATHER_UPDATE"
}
