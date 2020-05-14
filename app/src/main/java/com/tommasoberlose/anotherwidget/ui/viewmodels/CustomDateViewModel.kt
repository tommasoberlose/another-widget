package com.tommasoberlose.anotherwidget.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.chibatching.kotpref.livedata.asLiveData
import com.tommasoberlose.anotherwidget.global.Preferences

class CustomDateViewModel(application: Application) : AndroidViewModel(application) {
    val dateInput: MutableLiveData<String> = MutableLiveData(if (Preferences.dateFormat == "") "EEEE, MMM dd" else Preferences.dateFormat)
    val isDateCapitalize = Preferences.asLiveData(Preferences::isDateCapitalize)
}