package com.tommasoberlose.anotherwidget.ui.viewmodels

import android.app.Application
import android.content.pm.ApplicationInfo
import android.location.Address
import android.location.Geocoder
import androidx.lifecycle.*
import com.tommasoberlose.anotherwidget.global.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

class CustomLocationViewModel(application: Application) : AndroidViewModel(application) {

    val addresses: MutableLiveData<List<Address>> =  MutableLiveData(emptyList())
    val locationInput: MutableLiveData<String> = MutableLiveData(Preferences.customLocationAdd)
}