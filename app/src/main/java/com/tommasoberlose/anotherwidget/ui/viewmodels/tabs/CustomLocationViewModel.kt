package com.tommasoberlose.anotherwidget.ui.viewmodels.tabs

import android.app.Application
import android.location.Address
import androidx.lifecycle.*
import com.tommasoberlose.anotherwidget.global.Preferences

class CustomLocationViewModel(application: Application) : AndroidViewModel(application) {

    val addresses: MutableLiveData<List<Address>> =  MutableLiveData(emptyList())
    val locationInput: MutableLiveData<String> = MutableLiveData(Preferences.customLocationAdd)
}