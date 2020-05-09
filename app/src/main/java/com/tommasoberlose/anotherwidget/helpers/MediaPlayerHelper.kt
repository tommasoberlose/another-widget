package com.tommasoberlose.anotherwidget.helpers

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.MusicNotificationListener
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import java.lang.Exception

object MediaPlayerHelper {
    fun isSomeonePlaying(context: Context) = Preferences.showMusic && NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName) && Preferences.mediaPlayerTitle != ""

    fun getMediaInfo(): String {
      return if (Preferences.mediaPlayerArtist == "") {
          Preferences.mediaPlayerTitle
      } else {
          "%s, %s".format(Preferences.mediaPlayerTitle, Preferences.mediaPlayerArtist)
      }
    }

    fun updatePlayingMediaInfo(context: Context) {
        if (NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) {
            val list = try {
                (context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager).getActiveSessions(
                    ComponentName(context.packageName, MusicNotificationListener::class.java.name)
                )
            } catch (ex: Exception) {
                emptyList<MediaController>()
            }

            if (list.isNotEmpty()) {
                var isSomeonePlaying = false
                list.forEach { mc ->
                    val metadata = mc.metadata
                    val isPlaying =
                        mc.playbackState?.state == PlaybackState.STATE_PLAYING || mc.playbackState?.state == PlaybackState.STATE_CONNECTING

                    if (isPlaying) {
                        isSomeonePlaying = true
                        if (metadata != null) {
                            Preferences.bulk {
                                mediaPlayerTitle =
                                    metadata.getText(MediaMetadata.METADATA_KEY_TITLE)?.toString()
                                        ?: ""
                                mediaPlayerArtist =
                                    metadata.getText(MediaMetadata.METADATA_KEY_ARTIST)?.toString()
                                        ?: ""
                                mediaPlayerAlbum =
                                    metadata.getText(MediaMetadata.METADATA_KEY_ALBUM)?.toString()
                                        ?: ""
                            }
                        }

                        Preferences.mediaPlayerPackage = mc.packageName
                    }
                }

                if (!isSomeonePlaying) {
                    removeMediaInfo()
                }

            } else {
                removeMediaInfo()
            }
        } else {
            removeMediaInfo()
        }
        MainWidget.updateWidget(context)
    }

    private fun removeMediaInfo() {
        Preferences.bulk {
            remove(Preferences::mediaPlayerTitle)
            remove(Preferences::mediaPlayerArtist)
            remove(Preferences::mediaPlayerAlbum)
            remove(Preferences::mediaPlayerPackage)
        }
    }
}