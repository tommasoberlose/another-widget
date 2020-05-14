package com.tommasoberlose.anotherwidget.ui.viewmodels

import androidx.lifecycle.*
import com.chibatching.kotpref.livedata.asLiveData
import com.tommasoberlose.anotherwidget.global.Preferences

class MainViewModel : ViewModel() {

    // General Settings
    val textGlobalColor = Preferences.asLiveData(Preferences::textGlobalColor)
    val textGlobalAlpha = Preferences.asLiveData(Preferences::textGlobalAlpha)
    val textSecondaryColor = Preferences.asLiveData(Preferences::textSecondaryColor)
    val textSecondaryAlpha = Preferences.asLiveData(Preferences::textSecondaryAlpha)
    val backgroundCardColor = Preferences.asLiveData(Preferences::backgroundCardColor)
    val backgroundCardAlpha = Preferences.asLiveData(Preferences::backgroundCardAlpha)
    val textMainSize = Preferences.asLiveData(Preferences::textMainSize)
    val textSecondSize = Preferences.asLiveData(Preferences::textSecondSize)
    val textShadow = Preferences.asLiveData(Preferences::textShadow)
    val customFont = Preferences.asLiveData(Preferences::customFont)
    val secondRowInformation = Preferences.asLiveData(Preferences::secondRowInformation)
    val showDividers = Preferences.asLiveData(Preferences::showDividers)

    // Calendar Settings
    val showEvents = Preferences.asLiveData(Preferences::showEvents)
    val calendarAllDay = Preferences.asLiveData(Preferences::calendarAllDay)
    val showUntil = Preferences.asLiveData(Preferences::showUntil)
    val showDiffTime = Preferences.asLiveData(Preferences::showDiffTime)
    val showDeclinedEvents = Preferences.asLiveData(Preferences::showDeclinedEvents)
    val showNextEvent = Preferences.asLiveData(Preferences::showNextEvent)
    val openEventDetails = Preferences.asLiveData(Preferences::openEventDetails)
    val calendarAppName = Preferences.asLiveData(Preferences::calendarAppName)
    val widgetUpdateFrequency = Preferences.asLiveData(Preferences::widgetUpdateFrequency)

    // Clock Settings
    val showClock = Preferences.asLiveData(Preferences::showClock)
    val clockTextSize = Preferences.asLiveData(Preferences::clockTextSize)
    val clockTextColor = Preferences.asLiveData(Preferences::clockTextColor)
    val clockTextAlpha = Preferences.asLiveData(Preferences::clockTextAlpha)
    val showAMPMIndicator = Preferences.asLiveData(Preferences::showAMPMIndicator)

    val clockAppName = Preferences.asLiveData(Preferences::clockAppName)
    val dateFormat = Preferences.asLiveData(Preferences::dateFormat)
    val clockBottomMargin = Preferences.asLiveData(Preferences::clockBottomMargin)

    val showBigClockWarning = Preferences.asLiveData(Preferences::showBigClockWarning)

    // Weather Settings
    val showWeather = Preferences.asLiveData(Preferences::showWeather)
    val weatherTempUnit = Preferences.asLiveData(Preferences::weatherTempUnit)
    val weatherRefreshPeriod = Preferences.asLiveData(Preferences::weatherRefreshPeriod)

    val weatherAppName = Preferences.asLiveData(Preferences::weatherAppName)
    val weatherProviderApi = Preferences.asLiveData(Preferences::weatherProviderApi)

    val customLocationAdd = Preferences.asLiveData(Preferences::customLocationAdd)

    val showWeatherWarning = Preferences.asLiveData(Preferences::showWeatherWarning)
    val weatherIconPack = Preferences.asLiveData(Preferences::weatherIconPack)

    // Glance
    val showGlance = Preferences.asLiveData(Preferences::showGlance)
    val showMusic = Preferences.asLiveData(Preferences::showMusic)
    val showNextAlarm = Preferences.asLiveData(Preferences::showNextAlarm)
    val showBatteryCharging = Preferences.asLiveData(Preferences::showBatteryCharging)
    val showDailySteps = Preferences.asLiveData(Preferences::showDailySteps)
    val customInfo = Preferences.asLiveData(Preferences::customNotes)

    // Advanced Settings
    val darkThemePreference = Preferences.asLiveData(Preferences::darkThemePreference)
    val showWallpaper = Preferences.asLiveData(Preferences::showWallpaper)
    val showPreview = Preferences.asLiveData(Preferences::showPreview)
    val installedIntegrations = Preferences.asLiveData(Preferences::installedIntegrations)
}
