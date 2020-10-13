package com.tommasoberlose.anotherwidget.helpers

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.service.notification.StatusBarNotification
import android.util.Log
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.blockingBulk
import com.google.gson.Gson
import com.tommasoberlose.anotherwidget.global.Preferences
import java.lang.Exception

object ActiveNotificationsHelper {
    fun showLastNotification(): Boolean {
        return Preferences.lastNotificationId != -1 && Preferences.lastNotificationIcon != 0 && Preferences.lastNotificationPackage.isNotBlank() && Preferences.lastNotificationTitle.isNotBlank()
    }

    fun clearLastNotification(context: Context) {
        Kotpref.init(context)
        Preferences.blockingBulk {
            remove(Preferences::lastNotificationId)
            remove(Preferences::lastNotificationTitle)
            remove(Preferences::lastNotificationPackage)
            remove(Preferences::lastNotificationIcon)
        }
    }
}