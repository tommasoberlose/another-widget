package com.tommasoberlose.anotherwidget.ui.viewmodels.tabs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper

class MediaInfoFormatViewModel(application: Application) : AndroidViewModel(application) {
    val mediaInfoFormatInput: MutableLiveData<String> = MutableLiveData(if (Preferences.mediaInfoFormat == "") MediaPlayerHelper.DEFAULT_MEDIA_INFO_FORMAT else Preferences.mediaInfoFormat)
}