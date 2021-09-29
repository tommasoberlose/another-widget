package com.tommasoberlose.anotherwidget.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.os.ConfigurationCompat
import androidx.lifecycle.*
import com.chibatching.kotpref.livedata.asLiveData
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.ColorHelper.isColor
import com.tommasoberlose.anotherwidget.utils.isMetric

class MainViewModel(context: Application) : AndroidViewModel(context) {

    // General Settings
    val textGlobalColor = MediatorLiveData<Boolean>().apply {
        addSource(Preferences.asLiveData(Preferences::textGlobalColor)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textGlobalAlpha)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textGlobalColorDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textGlobalAlphaDark)) { value = true }
    }
    val textSecondaryColor = MediatorLiveData<Boolean>().apply {
        addSource(Preferences.asLiveData(Preferences::textSecondaryColor)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textSecondaryAlpha)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textSecondaryColorDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textSecondaryAlphaDark)) { value = true }
    }
    val backgroundCardColor = MediatorLiveData<Boolean>().apply {
        addSource(Preferences.asLiveData(Preferences::backgroundCardColor)) { value = true }
        addSource(Preferences.asLiveData(Preferences::backgroundCardAlpha)) { value = true }
        addSource(Preferences.asLiveData(Preferences::backgroundCardColorDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::backgroundCardAlphaDark)) { value = true }
    }

    val textMainSize = Preferences.asLiveData(Preferences::textMainSize)
    val textSecondSize = Preferences.asLiveData(Preferences::textSecondSize)
    val textShadow = Preferences.asLiveData(Preferences::textShadow)
    val textShadowDark = Preferences.asLiveData(Preferences::textShadowDark)
    val font = MediatorLiveData<Boolean>().apply {
        addSource(Preferences.asLiveData(Preferences::customFont)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customFontFile)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customFontName)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customFontVariant)) { value = true }
    }

    val secondRowInformation = Preferences.asLiveData(Preferences::secondRowInformation)
    val showDividers = Preferences.asLiveData(Preferences::showDividers)
    val secondRowTopMargin = Preferences.asLiveData(Preferences::secondRowTopMargin)
    val widgetAlign = Preferences.asLiveData(Preferences::widgetAlign)
    val widgetMargin = Preferences.asLiveData(Preferences::widgetMargin)
    val widgetPadding = Preferences.asLiveData(Preferences::widgetPadding)

    // Calendar Settings
    val showEvents = Preferences.asLiveData(Preferences::showEvents)
    val calendarAllDay = Preferences.asLiveData(Preferences::calendarAllDay)
    val showUntil = Preferences.asLiveData(Preferences::showUntil)
    val showDiffTime = Preferences.asLiveData(Preferences::showDiffTime)
    val showNextEvent = Preferences.asLiveData(Preferences::showNextEvent)
    val showNextEventOnMultipleLines = Preferences.asLiveData(Preferences::showNextEventOnMultipleLines)
    val openEventDetails = Preferences.asLiveData(Preferences::openEventDetails)
    val calendarAppName = Preferences.asLiveData(Preferences::calendarAppName)
    val widgetUpdateFrequency = Preferences.asLiveData(Preferences::widgetUpdateFrequency)
    val dateFormat = Preferences.asLiveData(Preferences::dateFormat)

    // Clock Settings
    val showClock = Preferences.asLiveData(Preferences::showClock)
    val clockTextSize = Preferences.asLiveData(Preferences::clockTextSize)
    val altTimezoneLabel = Preferences.asLiveData(Preferences::altTimezoneLabel)
    val clockTextColor = MediatorLiveData<Boolean>().apply {
        addSource(Preferences.asLiveData(Preferences::clockTextColor)) { value = true }
        addSource(Preferences.asLiveData(Preferences::clockTextAlpha)) { value = true }
    }
    val clockTextColorDark = MediatorLiveData<Boolean>().apply {
        addSource(Preferences.asLiveData(Preferences::clockTextColorDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::clockTextAlphaDark)) { value = true }
    }
    val showAMPMIndicator = Preferences.asLiveData(Preferences::showAMPMIndicator)

    val clockAppName = Preferences.asLiveData(Preferences::clockAppName)
    val clockBottomMargin = Preferences.asLiveData(Preferences::clockBottomMargin)

    // Weather Settings
    val showWeather = Preferences.asLiveData(Preferences::showWeather)
    val weatherTempUnit = Preferences.asLiveData(Preferences::weatherTempUnit)
    val weatherRefreshPeriod = Preferences.asLiveData(Preferences::weatherRefreshPeriod)

    val weatherAppName = Preferences.asLiveData(Preferences::weatherAppName)

    val customLocationAdd = Preferences.asLiveData(Preferences::customLocationAdd)

    val weatherIconPack = Preferences.asLiveData(Preferences::weatherIconPack)
    val weatherProvider = Preferences.asLiveData(Preferences::weatherProvider)
    val weatherProviderError = Preferences.asLiveData(Preferences::weatherProviderError)
    val weatherProviderLocationError = Preferences.asLiveData(Preferences::weatherProviderLocationError)

    // Advanced Settings
    val darkThemePreference = Preferences.asLiveData(Preferences::darkThemePreference)
    val showWallpaper = Preferences.asLiveData(Preferences::showWallpaper)
    val showPreview = Preferences.asLiveData(Preferences::showPreview)
    val installedIntegrations = Preferences.asLiveData(Preferences::installedIntegrations)

    // UI
    val fragmentScrollY = MutableLiveData<Int>()
    val widgetPreferencesUpdate = MediatorLiveData<Boolean>().apply {
        addSource(Preferences.asLiveData(Preferences::showClock)) { value = true }
        addSource(Preferences.asLiveData(Preferences::clockTextSize)) { value = true }
        addSource(Preferences.asLiveData(Preferences::clockTextColor)) { value = true }
        addSource(Preferences.asLiveData(Preferences::clockTextAlpha)) { value = true }
        addSource(Preferences.asLiveData(Preferences::clockTextColorDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::clockTextAlphaDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showAMPMIndicator)) { value = true }
        addSource(Preferences.asLiveData(Preferences::clockBottomMargin)) { value = true }
        addSource(Preferences.asLiveData(Preferences::altTimezoneLabel)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textGlobalColor)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textGlobalAlpha)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textSecondaryColor)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textSecondaryAlpha)) { value = true }
        addSource(Preferences.asLiveData(Preferences::backgroundCardColor)) { value = true }
        addSource(Preferences.asLiveData(Preferences::backgroundCardAlpha)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textGlobalColorDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textGlobalAlphaDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::dateFormat)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textSecondaryColorDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textSecondaryAlphaDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::backgroundCardColorDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::backgroundCardAlphaDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textMainSize)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textSecondSize)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textShadow)) { value = true }
        addSource(Preferences.asLiveData(Preferences::textShadowDark)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customFont)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customFontFile)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customFontName)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customFontVariant)) { value = true }
        addSource(Preferences.asLiveData(Preferences::widgetAlign)) { value = true }
        addSource(Preferences.asLiveData(Preferences::widgetMargin)) { value = true }
        addSource(Preferences.asLiveData(Preferences::widgetPadding)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showDividers)) { value = true }
        addSource(Preferences.asLiveData(Preferences::secondRowTopMargin)) { value = true }
        addSource(Preferences.asLiveData(Preferences::isDateCapitalize)) { value = true }
        addSource(Preferences.asLiveData(Preferences::isDateUppercase)) { value = true }

        addSource(Preferences.asLiveData(Preferences::showEvents)) { value = true }
        addSource(Preferences.asLiveData(Preferences::calendarAllDay)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showDiffTime)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showNextEvent)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showNextEventOnMultipleLines)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showDeclinedEvents)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showInvitedEvents)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showAcceptedEvents)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showOnlyBusyEvents)) { value = true }
        addSource(Preferences.asLiveData(Preferences::secondRowInformation)) { value = true }

        addSource(Preferences.asLiveData(Preferences::showWeather)) { value = true }
        addSource(Preferences.asLiveData(Preferences::weatherTempUnit)) { value = true }
        addSource(Preferences.asLiveData(Preferences::weatherIconPack)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customLocationLat)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customLocationLon)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customLocationAdd)) { value = true }

        addSource(Preferences.asLiveData(Preferences::enabledGlanceProviderOrder)) { value = true }
        addSource(Preferences.asLiveData(Preferences::customNotes)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showNextAlarm)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showBatteryCharging)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showDailySteps)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showGreetings)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showNotifications)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showMusic)) { value = true }
        addSource(Preferences.asLiveData(Preferences::mediaInfoFormat)) { value = true }
        addSource(Preferences.asLiveData(Preferences::musicPlayersFilter)) { value = true }
        addSource(Preferences.asLiveData(Preferences::appNotificationsFilter)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showEventsAsGlanceProvider)) { value = true }
        addSource(Preferences.asLiveData(Preferences::showWeatherAsGlanceProvider)) { value = true }

        addSource(Preferences.asLiveData(Preferences::installedIntegrations)) { value = true }
    }
}
