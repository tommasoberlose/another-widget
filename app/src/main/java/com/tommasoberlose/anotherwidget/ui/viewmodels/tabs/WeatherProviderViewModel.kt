package com.tommasoberlose.anotherwidget.ui.viewmodels.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.chibatching.kotpref.livedata.asLiveData
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences

class WeatherProviderViewModel(application: Application) : AndroidViewModel(application) {

    val weatherProvider = Preferences.asLiveData(Preferences::weatherProvider)
    val weatherProviderError = Preferences.asLiveData(Preferences::weatherProviderError)
    val weatherProviderLocationError = Preferences.asLiveData(Preferences::weatherProviderLocationError)


}