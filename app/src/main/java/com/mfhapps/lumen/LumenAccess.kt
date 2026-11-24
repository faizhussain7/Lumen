package com.mfhapps.lumen

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

class LumenAccess : AccessibilityService() {
    private val screen = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_SCREEN_ON) return
            val locked = getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked
            if (locked && LumenPrefs(this@LumenAccess).lockCover) LockCover.show(this@LumenAccess)
        }
    }

    override fun onServiceConnected() {
        instance = this
        registerReceiver(screen, android.content.IntentFilter(Intent.ACTION_SCREEN_ON))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        runCatching { unregisterReceiver(screen) }
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: LumenAccess? = null

        val ready: Boolean get() = instance != null

        fun lock(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) == true

        fun notifications(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true

        fun screenshot(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT) == true

        fun openSettings(context: Context) {
            runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}
