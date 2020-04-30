package com.tommasoberlose.anotherwidget.ui.viewmodels

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.databinding.ObservableField
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChooseApplicationViewModel(application: Application) : AndroidViewModel(application) {

    val appList: LiveData<List<ApplicationInfo>> = liveData {
        val app = application.packageManager.getInstalledApplications(0)
        emit(app)
    }
    val searchInput: MutableLiveData<String> = MutableLiveData("")
}