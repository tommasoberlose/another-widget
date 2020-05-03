package com.tommasoberlose.anotherwidget.ui.viewmodels

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData

class ChooseApplicationViewModel(application: Application) : AndroidViewModel(application) {

    val appList: LiveData<List<ResolveInfo>> = liveData {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val app = application.packageManager.queryIntentActivities( mainIntent, 0)
        emit(app)
    }
    val searchInput: MutableLiveData<String> = MutableLiveData("")
}