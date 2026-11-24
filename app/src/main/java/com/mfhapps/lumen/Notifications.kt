package com.mfhapps.lumen

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object Badges {
    private val counts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val state: StateFlow<Map<String, Int>> = counts
    @Volatile var connected = false
        internal set

    internal fun publish(list: Array<StatusBarNotification>?) {
        counts.value = list.orEmpty()
            .filter { it.isClearable && !it.isOngoing && it.notification.group?.let { _ -> !it.notification.flags.hasGroupSummary() } ?: true }
            .groupingBy { it.packageName }
            .eachCount()
    }

    private fun Int.hasGroupSummary() = this and android.app.Notification.FLAG_GROUP_SUMMARY != 0

    fun granted(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        return enabled.contains(ComponentName(context, LumenNotifications::class.java).flattenToString())
    }

    fun requestAccess(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                    .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, ComponentName(context, LumenNotifications::class.java).flattenToString())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

class LumenNotifications : NotificationListenerService() {
    override fun onListenerConnected() {
        instance = this
        Badges.connected = true
        refresh()
    }

    override fun onListenerDisconnected() {
        Badges.connected = false
        if (instance === this) instance = null
    }

    companion object {
        @Volatile
        private var instance: LumenNotifications? = null

        fun dismiss(key: String) {
            runCatching { instance?.cancelNotification(key) }
        }

        fun snooze(key: String, millis: Long) {
            runCatching { instance?.snoozeNotification(key, millis) }
        }

        fun clearAll() {
            runCatching { instance?.cancelAllNotifications() }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val items = runCatching { activeNotifications }.getOrNull()
        Badges.publish(items)
        NoticeStore.publish(this, items)
    }
}
