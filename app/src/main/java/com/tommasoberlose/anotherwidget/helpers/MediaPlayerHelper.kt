package com.tommasoberlose.anotherwidget.helpers

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.core.app.NotificationManagerCompat
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.blockingBulk
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.NotificationListener
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
        Kotpref.init(context)
        if (NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) {
            val list = try {
                (context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager).getActiveSessions(
                    ComponentName(context.packageName, NotificationListener::class.java.name)
                )
            } catch (ex: Exception) {
                emptyList<MediaController>()
            }.filter {
                Preferences.musicPlayersFilter == "" || isMusicPlayerAccepted(it.packageName)
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
                    removeMediaInfo(context)
                }

            } else {
                removeMediaInfo(context)
            }
        } else {
            removeMediaInfo(context)
        }
        MainWidget.updateWidget(context)
    }

    private fun removeMediaInfo(context: Context) {
        Kotpref.init(context)
        Preferences.blockingBulk {
            remove(Preferences::mediaPlayerTitle)
            remove(Preferences::mediaPlayerArtist)
            remove(Preferences::mediaPlayerAlbum)
            remove(Preferences::mediaPlayerPackage)
        }
    }

    fun isMusicPlayerAccepted(appPkg: String): Boolean = Preferences.musicPlayersFilter.contains(appPkg)

    fun toggleMusicPlayerFilter(appPkg: String) {
        if (Preferences.musicPlayersFilter == "" || !Preferences.musicPlayersFilter.contains(appPkg)) {
            Preferences.musicPlayersFilter = Preferences.musicPlayersFilter.split(",").union(listOf(appPkg)).joinToString(separator = ",")
        } else {
            Preferences.musicPlayersFilter = Preferences.musicPlayersFilter.split(",").filter { it != appPkg }.joinToString(separator = ",")
        }
    }
}