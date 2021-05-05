package com.tommasoberlose.anotherwidget.ui.viewmodels.tabs

import android.app.Application
import android.location.Address
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.tommasoberlose.anotherwidget.global.Preferences

class TimeZoneSelectorViewModel(application: Application) : AndroidViewModel(application) {

    val addresses: MutableLiveData<List<Address>> =  MutableLiveData(emptyList())
    val locationInput: MutableLiveData<String> = MutableLiveData(Preferences.altTimezoneLabel)
}