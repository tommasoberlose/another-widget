package com.tommasoberlose.anotherwidget.ui.viewmodels.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.koolio.library.DownloadableFontList
import com.koolio.library.Font
import com.koolio.library.FontList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CustomFontViewModel(application: Application) : AndroidViewModel(application) {

    val fontList: MutableLiveData<ArrayList<Font>> = MutableLiveData()
    val searchInput: MutableLiveData<String> = MutableLiveData("")

    init {
        viewModelScope.launch(Dispatchers.IO) {


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