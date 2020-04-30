package com.tommasoberlose.anotherwidget.ui.viewmodels

import androidx.lifecycle.*
import com.chibatching.kotpref.livedata.asLiveData
import com.tommasoberlose.anotherwidget.global.Preferences

class MainViewModel : ViewModel() {

    // General Settings
    val textGlobalColor = Preferences.asLiveData(Preferences::textGlobalColor)
    val textMainSize = Preferences.asLiveData(Preferences::textMainSize)
    val textSecondSize = Preferences.asLiveData(Preferences::textSecondSize)
    val textShadow = Preferences.asLiveData(Preferences::textShadow)
    val customFont = Preferences.asLiveData(Preferences::customFont)
    val secondRowInformation = Preferences.asLiveData(Preferences::secondRowInformation)

    // Calendar Settings
    val showEvents = Preferences.asLiveData(Preferences::showEvents)
    val calendarAllDay = Preferences.asLiveData(Preferences::calendarAllDay)
    val showUntil = Preferences.asLiveData(Preferences::showUntil)
    val showDiffTime = Preferences.asLiveData(Preferences::showDiffTime)
    val showDeclinedEvents = Preferences.asLiveData(Preferences::showDeclinedEvents)
    val showNextEvent = Preferences.asLiveData(Preferences::showNextEvent)

    val calendarAppName = Preferences.asLiveData(Preferences::calendarAppName)

    val eventAppName = Preferences.asLiveData(Preferences::eventAppName)


    // Clock Settings
    val showClock = Preferences.asLiveData(Preferences::showClock)
    val clockTextSize = Preferences.asLiveData(Preferences::clockTextSize)

    val clockAppName = Preferences.asLiveData(Preferences::clockAppName)
    val showNextAlarm = Preferences.asLiveData(Preferences::showNextAlarm)
    val dateFormat = Preferences.asLiveData(Preferences::dateFormat)

    // Weather Settings
    val showWeather = Preferences.asLiveData(Preferences::showWeather)
    val weatherTempUnit = Preferences.asLiveData(Preferences::weatherTempUnit)
    val weatherRefreshPeriod = Preferences.asLiveData(Preferences::weatherRefreshPeriod)

    val weatherAppName = Preferences.asLiveData(Preferences::weatherAppName)
    val weatherProviderApi = Preferences.asLiveData(Preferences::weatherProviderApi)

    val customLocationAdd = Preferences.asLiveData(Preferences::customLocationAdd)

    // Advanced Settings
    val darkThemePreference = Preferences.asLiveData(Preferences::darkThemePreference)
    val showWallpaper = Preferences.asLiveData(Preferences::showWallpaper)
}
