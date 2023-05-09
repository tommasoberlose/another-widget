package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tommasoberlose.anotherwidget.R
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.IntentHelper
import com.tommasoberlose.anotherwidget.utils.toast


class WidgetClickListenerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Actions.ACTION_OPEN_WEATHER_INTENT) {
            try {
                IntentHelper.getWeatherIntent(context).run {
                    if (flags and Intent.FLAG_ACTIVITY_NEW_TASK == Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(this)
                    else
                        context.sendBroadcast(this)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val uri = Uri.parse("https://www.google.com/search?q=weather")
                val i = Intent(Intent.ACTION_VIEW, uri)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                try {
                    context.startActivity(i)
                } catch (ignored: Exception) {
                    context.toast(context.getString(R.string.error_opening_app))
                }
            }
        }
    }
}
