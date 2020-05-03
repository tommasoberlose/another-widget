package com.tommasoberlose.anotherwidget.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.tommasoberlose.anotherwidget.global.Actions
import com.tommasoberlose.anotherwidget.helpers.IntentHelper


class WidgetClickListenerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Actions.ACTION_OPEN_WEATHER_INTENT) {
            context.sendBroadcast(Intent(Actions.ACTION_WEATHER_UPDATE))
            try {
                context.startActivity(IntentHelper.getWeatherIntent(context))
            } catch (e: Exception) {
                try {
                    context.applicationContext.startActivity(IntentHelper.getWeatherIntent(context.applicationContext))
                } catch (e: Exception) {
                    val uri = Uri.parse("http://www.google.com/#q=weather")
                    val i = Intent(Intent.ACTION_VIEW, uri)
                    i.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    try {
                        context.startActivity(i)
                    } catch (e: Exception) {
                        context.applicationContext.startActivity(i)
                    }
                }
            }
        }
    }
}
