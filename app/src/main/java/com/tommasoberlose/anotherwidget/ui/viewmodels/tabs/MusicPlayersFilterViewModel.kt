package com.tommasoberlose.anotherwidget.ui.viewmodels.tabs

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.lifecycle.*
import com.chibatching.kotpref.livedata.asLiveData
import com.tommasoberlose.anotherwidget.global.Preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicPlayersFilterViewModel(application: Application) : AndroidViewModel(application) {

    val pm: PackageManager by lazy { application.packageManager }
    val appList: MutableLiveData<List<ResolveInfo>> = MutableLiveData()
    val searchInput: MutableLiveData<String> = MutableLiveData("")
    var musicPlayersFilter = Preferences.asLiveData(Preferences::musicPlayersFilter)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val app = application.packageManager.queryIntentActivities(mainIntent, 0)
            val sortedApp = app.sortedWith { app1: ResolveInfo, app2: ResolveInfo ->
                app1.loadLabel(pm).toString().compareTo(app2.loadLabel(pm).toString())
            }
            withContext(Dispatchers.Main) {
                appList.postValue(sortedApp)
            }
        }
    }
}