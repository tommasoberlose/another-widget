package com.tommasoberlose.anotherwidget.helpers

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.chibatching.kotpref.Kotpref
import com.chibatching.kotpref.blockingBulk
import com.chibatching.kotpref.bulk
import com.tommasoberlose.anotherwidget.global.Preferences
import com.tommasoberlose.anotherwidget.receivers.NotificationListener
import com.tommasoberlose.anotherwidget.ui.widgets.MainWidget
import java.lang.Exception

object MediaPlayerHelper {
    const val MEDIA_INFO_TITLE = "%TITLE"
    const val MEDIA_INFO_ARTIST = "%ARTIST"
    const val MEDIA_INFO_ALBUM = "%ALBUM"

    const val DEFAULT_MEDIA_INFO_FORMAT = "%TITLE, %ARTIST"

    fun isSomeonePlaying(context: Context) = Preferences.showMusic && ActiveNotificationsHelper.checkNotificationAccess(context) && Preferences.mediaPlayerTitle != ""

    fun getMediaInfo(format: String = Preferences.mediaInfoFormat, title: String = Preferences.mediaPlayerTitle, artist: String = Preferences.mediaPlayerArtist, album: String = Preferences.mediaPlayerAlbum): String {
        return when (format) {
            "",
            DEFAULT_MEDIA_INFO_FORMAT -> {
                if (Preferences.mediaPlayerArtist == "") {
                    Preferences.mediaPlayerTitle
                } else {
                    DEFAULT_MEDIA_INFO_FORMAT.replace(MEDIA_INFO_TITLE, title)
                        .replace(MEDIA_INFO_ARTIST, artist)
                        .replace(MEDIA_INFO_ALBUM, album)
                        .replace("\\n", System.getProperty("line.separator") ?: " ")
                }
            }
            else -> {
                format.replace(MEDIA_INFO_TITLE, title)
                    .replace(MEDIA_INFO_ARTIST, artist)
                    .replace(MEDIA_INFO_ALBUM, album)
                    .replace("\\n", System.getProperty("line.separator") ?: " ")
            }
        }
    }

    fun updatePlayingMediaInfo(context: Context) {
        Kotpref.init(context)
        if (ActiveNotificationsHelper.checkNotificationAccess(context)) {
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

    fun isMusicPlayerAccepted(appPkg: String): Boolean = Preferences.musicPlayersFilter == "" || Preferences.musicPlayersFilter.contains(appPkg)

    fun toggleMusicPlayerFilter(appPkg: String) {
        if (Preferences.musicPlayersFilter == "" || !Preferences.musicPlayersFilter.contains(appPkg)) {
            Preferences.musicPlayersFilter = Preferences.musicPlayersFilter.split(",").union(listOf(appPkg)).joinToString(separator = ",")
        } else {
            Preferences.musicPlayersFilter = Preferences.musicPlayersFilter.split(",").filter { it != appPkg }.joinToString(separator = ",")
        }
    }
}