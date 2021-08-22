package com.tommasoberlose.anotherwidget.ui.viewmodels.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.tommasoberlose.anotherwidget.global.Preferences

class CustomDateViewModel(application: Application) : AndroidViewModel(application) {
    val dateInput: MutableLiveData<String> = MutableLiveData(if (Preferences.dateFormat == "") "EEEE, MMM dd" else Preferences.dateFormat)
    val isDateCapitalize = MutableLiveData(Preferences.isDateCapitalize)
    val isDateUppercase = MutableLiveData(Preferences.isDateUppercase)
}