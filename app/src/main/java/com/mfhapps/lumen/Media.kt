package com.mfhapps.lumen

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

class NowPlaying(val controller: MediaController, val title: String, val artist: String, val art: ImageBitmap?, val playing: Boolean, val app: String)

@Composable
fun rememberNowPlaying(): NowPlaying? {
    val context = LocalContext.current
    var state by remember { mutableStateOf<NowPlaying?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            state = runCatching { read(context) }.getOrNull()
            delay(1000)
        }
    }
    return state
}

private fun read(context: Context): NowPlaying? {
    if (!Badges.granted(context)) return null
    val manager = context.getSystemService(MediaSessionManager::class.java)
    val sessions = manager.getActiveSessions(ComponentName(context, LumenNotifications::class.java))
    val controller = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull() ?: return null
    val meta = controller.metadata ?: return null
    val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
    if (title.isBlank()) return null
    val art = (meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART))?.asImageBitmap()
    val label = runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(controller.packageName, 0)).toString() }.getOrDefault("")
    return NowPlaying(controller, title, meta.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(), art, controller.playbackState?.state == PlaybackState.STATE_PLAYING, label)
}

class NoticeAction(val title: String, val intent: android.app.PendingIntent, val input: android.app.RemoteInput?)

class Notice(val key: String, val pkg: String, val app: String, val title: String, val time: Long, val text: String = "", val intent: android.app.PendingIntent? = null, val clearable: Boolean = true, val actions: List<NoticeAction> = emptyList())

object NoticeStore {
    val list = kotlinx.coroutines.flow.MutableStateFlow<List<Notice>>(emptyList())

    fun publish(context: Context, items: Array<StatusBarNotification>?) {
        val pm = context.packageManager
        list.value = items.orEmpty()
            .filter { it.isClearable && !it.isOngoing && (it.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) == 0 }
            .sortedByDescending { it.postTime }
            .take(40)
            .map { sbn ->
                val extras = sbn.notification.extras
                Notice(
                    sbn.key,
                    sbn.packageName,
                    runCatching { pm.getApplicationLabel(pm.getApplicationInfo(sbn.packageName, 0)).toString() }.getOrDefault(sbn.packageName),
                    (extras.getCharSequence(android.app.Notification.EXTRA_TITLE) ?: "").toString(),
                    sbn.postTime,
                    (extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(android.app.Notification.EXTRA_TEXT) ?: "").toString(),
                    sbn.notification.contentIntent,
                    sbn.isClearable,
                    sbn.notification.actions.orEmpty().mapNotNull { action ->
                        val pending = action.actionIntent ?: return@mapNotNull null
                        NoticeAction(action.title?.toString().orEmpty(), pending, action.remoteInputs?.firstOrNull { it.allowFreeFormInput })
                    }.take(3),
                )
            }
    }
}
