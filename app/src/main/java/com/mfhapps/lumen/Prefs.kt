package com.mfhapps.lumen

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject

enum class ClockStyle { Stacked, Shape, Line, Breathe, Orbit, Tiles, Tall }

enum class IconShape { Squircle, Circle, Cookie, Clover, Sunny, Gem, Puffy, Arch }

enum class GestureAction { None, Drawer, Control, Notifications, Lock, Search, Customise, Screenshot }

enum class DockStyle { Glass, Solid, Clear, Hidden }

enum class DrawerSort { Alphabetical, MostUsed, Newest }

enum class AccentSource { Wallpaper, Custom }

enum class LockNotices { Icons, Cards, Hidden }

enum class LockShortcut { None, Torch, Camera, DoNotDisturb, Voice }

class HomeItem(val id: String, val app: String?, val widget: Int, var x: Int, var y: Int, var w: Int, var h: Int, var page: Int = 0) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("app", app ?: "").put("widget", widget).put("x", x).put("y", y).put("w", w).put("h", h).put("page", page)

    companion object {
        fun from(o: JSONObject) = HomeItem(o.getString("id"), o.optString("app").ifEmpty { null }, o.optInt("widget", -1), o.getInt("x"), o.getInt("y"), o.getInt("w"), o.getInt("h"), o.optInt("page", 0))
    }
}

class Folder(val id: String, val title: String, val apps: List<String>) {
    fun toJson(): JSONObject = JSONObject().put("id", id).put("title", title).put("apps", JSONArray(apps))

    companion object {
        const val PREFIX = "f:"
        fun from(o: JSONObject) = Folder(o.getString("id"), o.optString("title"), o.getJSONArray("apps").let { a -> (0 until a.length()).map { a.getString(it) } })
    }
}

class LumenPrefs(context: Context) {
    private val store: SharedPreferences = context.getSharedPreferences("lumen", Context.MODE_PRIVATE)

    private fun bool(key: String, default: Boolean) = mutableStateOf(store.getBoolean(key, default)).also { keys[key] = it }
    private fun float(key: String, default: Float) = mutableStateOf(store.getFloat(key, default)).also { keys[key] = it }
    private fun int(key: String, default: Int) = mutableStateOf(store.getInt(key, default)).also { keys[key] = it }
    private inline fun <reified T : Enum<T>> choice(key: String, default: T) =
        mutableStateOf(store.getString(key, null)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default).also { keys[key] = it }

    private val keys = HashMap<String, MutableState<*>>()

    val clockState = choice("clock", ClockStyle.Stacked)
    val shapeState = choice("shape", IconShape.Squircle)
    val themedState = bool("themed", true)
    val columnsState = int("columns", 5)
    val bounceState = float("bounce", 0.55f)
    val speedState = float("speed", 0.5f)
    val blurState = float("blur", 0.7f)
    val glowState = bool("glow", true)
    val ownTopState = bool("own_top", true)
    val lockCoverState = bool("lock_cover", false)
    val statusBarState = bool("status_bar", true)
    val statusPercentState = bool("status_percent", true)
    val clockScaleState = float("clock_scale", 0.5f)
    val showDateState = bool("show_date", true)
    val showChipsState = bool("show_chips", true)
    val iconScaleState = float("icon_scale", 0.5f)
    val homeLabelsState = bool("home_labels", true)
    val drawerLabelsState = bool("drawer_labels", true)
    val dockStyleState = choice("dock_style", DockStyle.Clear)
    val dockSearchState = bool("dock_search", true)
    val homeColumnsState = int("home_columns", 4)
    val homeRowsState = int("home_rows", 4)
    val sortState = choice("sort", DrawerSort.Alphabetical)
    val categoriesState = bool("categories", true)
    val favouritesState = bool("favourites", true)
    val badgesState = bool("badges", true)
    val accentSourceState = choice("accent_source", AccentSource.Wallpaper)
    val accentState = int("accent", 0xFF4F8DFF.toInt())
    val swipeUpState = choice("swipe_up", GestureAction.Drawer)
    val swipeDownState = choice("swipe_down", GestureAction.Control)
    val doubleTapState = choice("double_tap", GestureAction.Lock)
    val pinchState = choice("pinch", GestureAction.Customise)
    val longPressState = choice("long_press", GestureAction.Customise)
    val lockClockState = choice("lock_clock", ClockStyle.Shape)
    val clockWeightState = float("clock_weight", 0.78f)
    val clockWidthState = float("clock_width", 0.45f)
    val clockColourState = int("clock_colour", 0)
    val vividState = float("vivid", 0.7f)
    val logoStyleState = int("logo_style", 1)
    val logoPaletteState = int("logo_colour", 9)
    val weatherState = bool("weather", false)
    val lockNoticesState = choice("lock_notices", LockNotices.Icons)
    val lockCentreState = bool("lock_centre", false)
    val lockMediaState = bool("lock_media", true)
    val lockWidgetsState = bool("lock_widgets", true)
    val lockLeftState = choice("lock_left", LockShortcut.Torch)
    val lockRightState = choice("lock_right", LockShortcut.Camera)
    val lockFingerprintState = bool("lock_fingerprint", true)
    var ownerMessage by mutableStateOf(store.getString("owner_message", "") ?: "")
        private set

    var recents by mutableStateOf(store.getString("recents", "")!!.split("\u0001").filter { it.isNotBlank() })
        private set

    fun addRecent(text: String) {
        if (text.isBlank()) return
        recents = (listOf(text) + recents.filter { !it.equals(text, true) }).take(8)
        store.edit().putString("recents", recents.joinToString("\u0001")).apply()
    }

    fun clearRecents() {
        recents = emptyList()
        store.edit().remove("recents").apply()
    }

    val homeSeeded: Boolean get() = store.getBoolean("home_seeded", false)

    fun markHomeSeeded() = store.edit().putBoolean("home_seeded", true).apply()

    fun updateOwnerMessage(value: String) {
        ownerMessage = value
        store.edit().putString("owner_message", value).apply()
    }
    val lockShortcutsState = bool("lock_shortcuts", true)

    val clock by clockState
    val shape by shapeState
    val themed by themedState
    val columns by columnsState
    val bounce by bounceState
    val speed by speedState
    val blur by blurState
    val glow by glowState
    val ownTop by ownTopState
    val lockCover by lockCoverState

    fun <T> set(state: MutableState<T>, value: T) {
        state.value = value
        val key = keys.entries.first { it.value === state }.key
        store.edit().apply {
            when (value) {
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is Int -> putInt(key, value)
                is Enum<*> -> putString(key, value.name)
            }
        }.apply()
    }

    fun updateClock(value: ClockStyle) = set(clockState, value)
    fun updateShape(value: IconShape) = set(shapeState, value)
    fun updateThemed(value: Boolean) = set(themedState, value)
    fun updateColumns(value: Int) = set(columnsState, value)
    fun updateBounce(value: Float) = set(bounceState, value)
    fun updateSpeed(value: Float) = set(speedState, value)
    fun updateBlur(value: Float) = set(blurState, value)
    fun updateGlow(value: Boolean) = set(glowState, value)
    fun updateOwnTop(value: Boolean) = set(ownTopState, value)
    fun updateLockCover(value: Boolean) = set(lockCoverState, value)

    var dock by mutableStateOf(
        store.getString(KEY_DOCK, null)?.split(SEPARATOR)?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() || store.getBoolean(KEY_DOCK_EDITED, false) },
    )
        private set

    fun updateDock(keys: List<String>, edited: Boolean = true) {
        dock = keys
        store.edit().putString(KEY_DOCK, keys.joinToString(SEPARATOR)).putBoolean(KEY_DOCK_EDITED, edited).apply()
    }

    var hidden by mutableStateOf(store.getStringSet(KEY_HIDDEN, emptySet())!!.toSet())
        private set

    fun toggleHidden(key: String) {
        hidden = if (key in hidden) hidden - key else hidden + key
        store.edit().putStringSet(KEY_HIDDEN, hidden).apply()
    }

    var home by mutableStateOf(readHome())
        private set

    private fun readHome(): List<HomeItem> = runCatching {
        val array = JSONArray(store.getString(KEY_HOME, "[]"))
        (0 until array.length()).map { HomeItem.from(array.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun updateHome(items: List<HomeItem>) {
        home = items.map { HomeItem(it.id, it.app, it.widget, it.x, it.y, it.w, it.h, it.page) }
        store.edit().putString(KEY_HOME, JSONArray(home.map { it.toJson() }).toString()).apply()
    }

    val pagesState = int("pages", 1)
    var iconPack by mutableStateOf(store.getString("icon_pack", "") ?: "")
        private set

    fun updateIconPack(pkg: String) {
        iconPack = pkg
        store.edit().putString("icon_pack", pkg).apply()
    }

    var folders by mutableStateOf(readFolders())
        private set

    private fun readFolders(): Map<String, Folder> = runCatching {
        val array = JSONArray(store.getString("folders", "[]"))
        (0 until array.length()).map { Folder.from(array.getJSONObject(it)) }.associateBy { it.id }
    }.getOrDefault(emptyMap())

    fun saveFolder(folder: Folder?, removeId: String? = null) {
        val next = folders.toMutableMap()
        removeId?.let { next.remove(it) }
        folder?.let { next[it.id] = it }
        folders = next
        store.edit().putString("folders", JSONArray(next.values.map { it.toJson() }).toString()).apply()
    }

    private val launches: SharedPreferences = context.getSharedPreferences("lumen_launches", Context.MODE_PRIVATE)
    var launchVersion by mutableIntStateOf(0)
        private set

    fun recordLaunch(key: String) {
        launches.edit().putInt(key, launches.getInt(key, 0) + 1).apply()
        launchVersion++
    }

    fun launchCount(key: String): Int = launches.getInt(key, 0)

    fun topLaunched(limit: Int): List<String> =
        launches.all.entries.mapNotNull { (k, v) -> (v as? Int)?.let { k to it } }.sortedByDescending { it.second }.take(limit).map { it.first }

    fun export(): String {
        val out = JSONObject().put("lumen", 1)
        store.all.forEach { (k, v) ->
            val entry = when (v) {
                is Boolean -> JSONObject().put("t", "b").put("v", v)
                is Int -> JSONObject().put("t", "i").put("v", v)
                is Long -> JSONObject().put("t", "l").put("v", v)
                is Float -> JSONObject().put("t", "f").put("v", v.toDouble())
                is String -> JSONObject().put("t", "s").put("v", v)
                is Set<*> -> JSONObject().put("t", "set").put("v", JSONArray(v.toList()))
                else -> null
            }
            if (entry != null) out.put(k, entry)
        }
        return out.toString()
    }

    fun import(text: String): Boolean = runCatching {
        val o = JSONObject(text.trim())
        require(o.optInt("lumen") == 1)
        val editor = store.edit().clear()
        o.keys().forEach { k ->
            if (k == "lumen") return@forEach
            val e = o.getJSONObject(k)
            when (e.getString("t")) {
                "b" -> editor.putBoolean(k, e.getBoolean("v"))
                "i" -> editor.putInt(k, e.getInt("v"))
                "l" -> editor.putLong(k, e.getLong("v"))
                "f" -> editor.putFloat(k, e.getDouble("v").toFloat())
                "s" -> editor.putString(k, e.getString("v"))
                "set" -> editor.putStringSet(k, e.getJSONArray("v").let { a -> (0 until a.length()).map { a.getString(it) }.toSet() })
            }
        }
        editor.commit()
    }.isSuccess

    private companion object {
        const val KEY_DOCK = "dock"
        const val KEY_DOCK_EDITED = "dock_edited"
        const val KEY_HIDDEN = "hidden"
        const val KEY_HOME = "home"
        const val SEPARATOR = "|"
    }
}
