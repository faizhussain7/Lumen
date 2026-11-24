package com.mfhapps.lumen

import android.provider.Settings

class SettingEntry(val title: String, val keywords: String, val action: String)

object SettingsIndex {
    val entries: List<SettingEntry> = listOf(
        SettingEntry("Settings", "settings preferences options system", Settings.ACTION_SETTINGS),
        SettingEntry("Wi-Fi", "wifi wireless internet network wlan", Settings.ACTION_WIFI_SETTINGS),
        SettingEntry("Saved networks", "wifi saved networks forget password", "android.settings.WIFI_SAVED_NETWORK_SETTINGS"),
        SettingEntry("Wi-Fi preferences", "wifi advanced scanning", Settings.ACTION_WIFI_IP_SETTINGS),
        SettingEntry("Internet panel", "internet connectivity mobile data wifi quick", Settings.Panel.ACTION_INTERNET_CONNECTIVITY),
        SettingEntry("Mobile network", "mobile data cellular sim network operator roaming apn 4g 5g lte", Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
        SettingEntry("Data roaming", "roaming data abroad travel", Settings.ACTION_DATA_ROAMING_SETTINGS),
        SettingEntry("Access point names (APN)", "apn access point names mms internet carrier", Settings.ACTION_APN_SETTINGS),
        SettingEntry("Data usage", "data usage limit warning consumption", Settings.ACTION_DATA_USAGE_SETTINGS),
        SettingEntry("Data saver", "data saver restrict background", "android.settings.DATA_SAVER_SETTINGS"),
        SettingEntry("SIMs", "sim cards dual sim esim manage", "android.settings.MANAGE_ALL_SIM_PROFILES_SETTINGS"),
        SettingEntry("Hotspot and tethering", "hotspot tethering share internet portable usb tether", "android.settings.TETHER_SETTINGS"),
        SettingEntry("Airplane mode", "airplane flight mode offline", Settings.ACTION_AIRPLANE_MODE_SETTINGS),
        SettingEntry("Bluetooth", "bluetooth pair devices headphones earbuds", Settings.ACTION_BLUETOOTH_SETTINGS),
        SettingEntry("Connected devices", "connected devices cast usb nearby share", "android.settings.CONNECTED_DEVICES_SETTINGS"),
        SettingEntry("NFC and payments", "nfc contactless tap pay payment", Settings.ACTION_NFC_SETTINGS),
        SettingEntry("Tap and pay", "tap pay wallet contactless default payment", Settings.ACTION_NFC_PAYMENT_SETTINGS),
        SettingEntry("Cast", "cast screen mirror chromecast tv", Settings.ACTION_CAST_SETTINGS),
        SettingEntry("VPN", "vpn private network proxy", Settings.ACTION_VPN_SETTINGS),
        SettingEntry("Private DNS", "private dns adguard network security", "android.settings.PRIVATE_DNS_SETTINGS"),
        SettingEntry("Wireless and networks", "wireless network more", Settings.ACTION_WIRELESS_SETTINGS),
        SettingEntry("Display", "display screen brightness timeout", Settings.ACTION_DISPLAY_SETTINGS),
        SettingEntry("Brightness", "brightness adaptive auto brightness light", Settings.ACTION_DISPLAY_SETTINGS),
        SettingEntry("Screen timeout", "screen timeout sleep turn off display", Settings.ACTION_DISPLAY_SETTINGS),
        SettingEntry("Dark theme", "dark mode night theme black", "android.settings.DARK_THEME_SETTINGS"),
        SettingEntry("Night Light", "night light blue light eye comfort filter", Settings.ACTION_NIGHT_DISPLAY_SETTINGS),
        SettingEntry("Font size", "font size text size bigger smaller", "android.settings.TEXT_READING_SETTINGS"),
        SettingEntry("Display size and text", "display size zoom text reading", "android.settings.TEXT_READING_SETTINGS"),
        SettingEntry("Screen saver", "screen saver daydream", Settings.ACTION_DREAM_SETTINGS),
        SettingEntry("Wallpaper and style", "wallpaper style theme colours colors icons", Settings.ACTION_DISPLAY_SETTINGS),
        SettingEntry("Lock screen", "lock screen notifications always on display aod", "android.settings.LOCK_SCREEN_SETTINGS"),
        SettingEntry("Sound and vibration", "sound volume ringtone vibration", Settings.ACTION_SOUND_SETTINGS),
        SettingEntry("Volume", "volume media ring alarm loud", Settings.ACTION_SOUND_SETTINGS),
        SettingEntry("Ringtone", "ringtone call sound phone ring", Settings.ACTION_SOUND_SETTINGS),
        SettingEntry("Do not disturb", "do not disturb dnd silent quiet focus", Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS),
        SettingEntry("Notifications", "notifications alerts badges", Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS),
        SettingEntry("Notification history", "notification history dismissed", "android.settings.NOTIFICATION_HISTORY"),
        SettingEntry("Notification access", "notification access listener", Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
        SettingEntry("Battery", "battery usage power charge", Intent_POWER),
        SettingEntry("Battery saver", "battery saver power saving low power", Settings.ACTION_BATTERY_SAVER_SETTINGS),
        SettingEntry("Battery optimisation", "battery optimization optimisation background restrict doze", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        SettingEntry("Storage", "storage space free up memory files", Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        SettingEntry("Memory card", "sd card memory card external storage", Settings.ACTION_MEMORY_CARD_SETTINGS),
        SettingEntry("Apps", "apps applications manage installed uninstall", Settings.ACTION_APPLICATION_SETTINGS),
        SettingEntry("All apps", "all apps list installed", Settings.ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS),
        SettingEntry("Default apps", "default apps browser launcher home sms phone", Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
        SettingEntry("Home app", "home app launcher default", Settings.ACTION_HOME_SETTINGS),
        SettingEntry("Special app access", "special access permissions", "android.settings.SPECIAL_APP_ACCESS_SETTINGS"),
        SettingEntry("Display over other apps", "overlay draw over other apps bubbles", Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
        SettingEntry("Install unknown apps", "unknown sources install apk", Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES),
        SettingEntry("Modify system settings", "write settings modify system", Settings.ACTION_MANAGE_WRITE_SETTINGS),
        SettingEntry("Usage access", "usage access statistics", Settings.ACTION_USAGE_ACCESS_SETTINGS),
        SettingEntry("All files access", "all files access storage manage", Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
        SettingEntry("Picture-in-picture", "picture in picture pip floating video", "android.settings.PICTURE_IN_PICTURE_SETTINGS"),
        SettingEntry("Alarms and reminders", "alarms reminders exact alarm", "android.settings.REQUEST_SCHEDULE_EXACT_ALARM"),
        SettingEntry("Permission manager", "permissions camera microphone location contacts", "android.intent.action.MANAGE_PERMISSIONS"),
        SettingEntry("Privacy", "privacy permissions dashboard", Settings.ACTION_PRIVACY_SETTINGS),
        SettingEntry("Location", "location gps maps position", Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        SettingEntry("Security", "security lock screen pin password pattern fingerprint face", Settings.ACTION_SECURITY_SETTINGS),
        SettingEntry("Fingerprint", "fingerprint biometric unlock", Settings.ACTION_FINGERPRINT_ENROLL),
        SettingEntry("Biometrics", "biometric face unlock fingerprint", Settings.ACTION_BIOMETRIC_ENROLL),
        SettingEntry("Screen lock", "screen lock pin pattern password", "android.app.action.SET_NEW_PASSWORD"),
        SettingEntry("Device admin apps", "device admin administrators", "android.settings.DEVICE_ADMIN_SETTINGS"),
        SettingEntry("Accessibility", "accessibility talkback magnification captions", Settings.ACTION_ACCESSIBILITY_SETTINGS),
        SettingEntry("Captions", "captions subtitles live caption", Settings.ACTION_CAPTIONING_SETTINGS),
        SettingEntry("Accounts", "accounts sync google email", Settings.ACTION_SYNC_SETTINGS),
        SettingEntry("Add account", "add account login", Settings.ACTION_ADD_ACCOUNT),
        SettingEntry("Google", "google account services", "com.google.android.gms.settings.GOOGLE_SETTINGS"),
        SettingEntry("Languages", "language locale translate input", Settings.ACTION_LOCALE_SETTINGS),
        SettingEntry("App languages", "app language per app locale", "android.settings.APP_LOCALE_SETTINGS"),
        SettingEntry("Keyboard and input", "keyboard input method gboard typing", Settings.ACTION_INPUT_METHOD_SETTINGS),
        SettingEntry("On-screen keyboard", "keyboard virtual ime", Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS),
        SettingEntry("Spell checker", "spell check autocorrect", "android.settings.SPELL_CHECKER_SETTINGS"),
        SettingEntry("Text-to-speech", "text to speech tts voice", "com.android.settings.TTS_SETTINGS"),
        SettingEntry("Gestures", "gestures navigation swipe buttons", "android.settings.GESTURE_SETTINGS"),
        SettingEntry("Navigation mode", "navigation bar gestures three button", "android.settings.NAVIGATION_MODE_SETTINGS"),
        SettingEntry("Date and time", "date time timezone clock 24 hour", Settings.ACTION_DATE_SETTINGS),
        SettingEntry("Backup", "backup restore cloud", "android.settings.BACKUP_AND_RESTORE_SETTINGS"),
        SettingEntry("Reset options", "reset factory reset network reset", "android.settings.RESET_SETTINGS"),
        SettingEntry("System update", "system update software update upgrade", "android.settings.SYSTEM_UPDATE_SETTINGS"),
        SettingEntry("About phone", "about phone device info model build version imei", Settings.ACTION_DEVICE_INFO_SETTINGS),
        SettingEntry("Developer options", "developer options usb debugging adb animation", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        SettingEntry("Users", "users multiple guest profile", "android.settings.USER_SETTINGS"),
        SettingEntry("Digital wellbeing", "digital wellbeing screen time focus bedtime parental", "android.settings.DIGITAL_WELLBEING_SETTINGS"),
        SettingEntry("Print", "print printer", Settings.ACTION_PRINT_SETTINGS),
        SettingEntry("Dream", "dream screen saver", Settings.ACTION_DREAM_SETTINGS),
        SettingEntry("Search in Settings", "search settings find", "com.android.settings.action.SETTINGS_SEARCH"),
    )

    fun search(query: String): List<SettingEntry> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        val words = q.split(' ').filter { it.isNotBlank() }
        return entries.map { e ->
            val title = e.title.lowercase()
            val hay = title + " " + e.keywords
            val score = when {
                title.startsWith(q) -> 0
                title.contains(q) -> 1
                words.all { w -> hay.split(' ', '-').any { it.startsWith(w) } } -> 2
                words.all { hay.contains(it) } -> 3
                else -> 99
            }
            e to score
        }.filter { it.second < 99 }.sortedBy { it.second }.map { it.first }.distinctBy { it.title }.take(6)
    }
}

private const val Intent_POWER = "android.intent.action.POWER_USAGE_SUMMARY"
