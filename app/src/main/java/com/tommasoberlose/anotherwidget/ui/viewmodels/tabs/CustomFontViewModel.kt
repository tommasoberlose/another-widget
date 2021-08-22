package com.tommasoberlose.anotherwidget.ui.viewmodels.tabs

import android.app.Application
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.koolio.library.DownloadableFontList
import com.koolio.library.Font
import com.koolio.library.FontList
import com.tommasoberlose.anotherwidget.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomFontViewModel(application: Application) : AndroidViewModel(application) {

    val fontList: MutableLiveData<ArrayList<Font>> = MutableLiveData()
    val searchInput: MutableLiveData<String> = MutableLiveData("")

    init {
        viewModelScope.launch(Dispatchers.IO) {


            val fontListCallback: DownloadableFontList.FontListCallback =
                object : DownloadableFontList.FontListCallback {
                    override fun onFontListRetrieved(downloadedList: FontList?) {
                        fontList.postValue(downloadedList?.fontArrayList)
                    }

                    override fun onTypefaceRequestFailed(reason: Int) {

                    }
                }

            //DownloadableFontList.requestDownloadableFontList(fontListCallback, BuildConfig.GOOGLE_API_KEY)
        }
    }
}