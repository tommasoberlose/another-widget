package com.tommasoberlose.anotherwidget.helpers

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.blockingBulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.NotificationListener
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget

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
        MainWidget.updateWidget(context)
    }

    fun checkNotificationAccess(context: Context): Boolean {
        val contentResolver: ContentResolver = context.contentResolver
        val enabledNotificationListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val packageName: String = context.packageName
        return NotificationManagerCompat.getEnabledListenerPackages(context).contains(packageName) && (enabledNotificationListeners != null && enabledNotificationListeners.contains(NotificationListener::class.java.name))
    }

    fun isAppAccepted(appPkg: String): Boolean = Preferences.appNotificationsFilter == "" || Preferences.appNotificationsFilter.contains(appPkg)

    fun toggleAppFilter(appPkg: String) {
        if (Preferences.appNotificationsFilter == "" || !Preferences.appNotificationsFilter.contains(appPkg)) {
            Preferences.appNotificationsFilter = Preferences.appNotificationsFilter.split(",").union(listOf(appPkg)).joinToString(separator = ",")
        } else {
            Preferences.appNotificationsFilter = Preferences.appNotificationsFilter.split(",").filter { it != appPkg }.joinToString(separator = ",")
        }
    }
}