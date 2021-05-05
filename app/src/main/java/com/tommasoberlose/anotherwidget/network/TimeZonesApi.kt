package com.tommasoberlose.anotherwidget.network

import android.content.Context
import android.util.Log
import com.chibatching.kotpref.Kotpref
import com.google.gson.internal.LinkedTreeMap
import com.haroldadmin.cnradapter.NetworkResponse
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Constants
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.WeatherHelper
import com.tommasoberlose.anotherwidget.network.repository.TimeZonesRepository
import com.tommasoberlose.anotherwidget.ui.fragments.MainFragment
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import org.greenrobot.eventbus.EventBus
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class TimeZonesApi(val context: Context) {
    suspend fun getTimeZone(lat: String, long: String): String? {
        Kotpref.init(context)
        val repository = TimeZonesRepository()
        var id: String? = null

        when (val response = repository.getTimeZone(lat, long)) {
            is NetworkResponse.Success -> {
                try {
                    Log.d("ciao", response.body.toString())
                    id = response.body["timezoneId"] as String

                } catch(ex: Exception) {
                    ex.printStackTrace()
                }
            }
        }

        return id
    }
}