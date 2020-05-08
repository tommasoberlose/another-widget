package com.tommasoberlose.anotherwidget.receivers

import android.app.Notification
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.helpers.MediaPlayerHelper
import com.tommasoberlose.anotherwidget.helpers.WidgetHelper
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget


class MusicNotificationListener : NotificationListenerService() {
    override fun onListenerConnected() {
        MediaPlayerHelper.updatePlayingMediaInfo(this)
        super.onListenerConnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.notification?.extras?.let { bundle ->
            bundle.getParcelable<MediaSession.Token>(Notification.EXTRA_MEDIA_SESSION)?.let {
                MediaPlayerHelper.updatePlayingMediaInfo(this)
            }
        }
        super.onNotificationPosted(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        MediaPlayerHelper.updatePlayingMediaInfo(this)
        super.onNotificationRemoved(sbn)
    }
}