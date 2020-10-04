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
    val textGlobalColorDark = Preferences.asLiveData(Preferences::textGlobalColorDark)
    val textGlobalAlphaDark = Preferences.asLiveData(Preferences::textGlobalAlphaDark)
    val textSecondaryColorDark = Preferences.asLiveData(Preferences::textSecondaryColorDark)
    val textSecondaryAlphaDark = Preferences.asLiveData(Preferences::textSecondaryAlphaDark)
    val backgroundCardColorDark = Preferences.asLiveData(Preferences::backgroundCardColorDark)
    val backgroundCardAlphaDark = Preferences.asLiveData(Preferences::backgroundCardAlphaDark)
    val textMainSize = Preferences.asLiveData(Preferences::textMainSize)
    val textSecondSize = Preferences.asLiveData(Preferences::textSecondSize)
    val textShadow = Preferences.asLiveData(Preferences::textShadow)
    val textShadowDark = Preferences.asLiveData(Preferences::textShadowDark)
    val customFont = Preferences.asLiveData(Preferences::customFont)
    val customFontFile = Preferences.asLiveData(Preferences::customFontFile)
    val customFontName = Preferences.asLiveData(Preferences::customFontName)
    val customFontVariant = Preferences.asLiveData(Preferences::customFontVariant)
    val secondRowInformation = Preferences.asLiveData(Preferences::secondRowInformation)
    val showDividers = Preferences.asLiveData(Preferences::showDividers)
    val secondRowTopMargin = Preferences.asLiveData(Preferences::secondRowTopMargin)

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
    val showOnlyBusyEvents = Preferences.asLiveData(Preferences::showOnlyBusyEvents)

    // Clock Settings
    val showClock = Preferences.asLiveData(Preferences::showClock)
    val clockTextSize = Preferences.asLiveData(Preferences::clockTextSize)
    val clockTextColor = Preferences.asLiveData(Preferences::clockTextColor)
    val clockTextAlpha = Preferences.asLiveData(Preferences::clockTextAlpha)
    val clockTextColorDark = Preferences.asLiveData(Preferences::clockTextColorDark)
    val clockTextAlphaDark = Preferences.asLiveData(Preferences::clockTextAlphaDark)
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
