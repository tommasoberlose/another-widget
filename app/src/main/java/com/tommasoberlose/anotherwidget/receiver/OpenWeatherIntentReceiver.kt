package com.tommasoberlose.anotherwidget.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tommasoberlose.anotherwidget.`object`.Constants
import com.tommasoberlose.anotherwidget.util.Util
import android.support.v4.content.ContextCompat.startActivity



class OpenWeatherIntentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Constants.ACTION_OPEN_WEATHER_INTENT) {
            context.sendBroadcast(Intent(Constants.ACTION_WEATHER_UPDATE))
            try {
                context.startActivity(Util.getWeatherIntent(context))
            } catch (e: Exception) {
                try {
                    context.applicationContext.startActivity(Util.getWeatherIntent(context.applicationContext))
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
