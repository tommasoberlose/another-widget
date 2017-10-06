package com.tommasoberlose.anotherwidget.`object`

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by tommaso on 05/10/17.
 */

object Constants {
    val CALENDAR_REQUEST_CODE = 1
    val LOCATION_REQUEST_CODE = 2

    val PREF_WEATHER_ICON = "PREF_WEATHER_ICON"
    val PREF_WEATHER_TEMP = "PREF_WEATHER_TEMP"

    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val ACTION_TIME_UPDATE = "com.tommasoberlose.anotherwidget.action.ACTION_TIME_UPDATE"
    val ACTION_WEATHER_UPDATE = "com.tommasoberlose.anotherwidget.action.ACTION_WEATHER_UPDATE"
}
