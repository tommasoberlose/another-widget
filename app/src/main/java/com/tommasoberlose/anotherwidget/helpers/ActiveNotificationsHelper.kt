package com.tommasoberlose.anotherwidget.helpers

import android.app.NotificationManager
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.google.gson.Gson

object ActiveNotificationsHelper {
    fun getLastNotification(context: Context): StatusBarNotification? {
        with(context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager) {
            activeNotifications.forEach {
                Log.d("ciao", Gson().toJson(it).toString())
            }

            return activeNotifications.lastOrNull()
        }
    }
}