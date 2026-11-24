package com.mfhapps.lumen

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.provider.MediaStore
import android.util.LruCache
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator

enum class AppCategory { Social, Media, Games, Work, Travel, Other }

class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val category: AppCategory,
    val info: LauncherActivityInfo,
    val installed: Long = 0L,
) {
    val packageName: String get() = component.packageName
}

class AppCatalog(private val context: Context, private val scope: CoroutineScope) {
    private val launcher = context.getSystemService(LauncherApps::class.java)
    private val icons = object : LruCache<String, ImageBitmap>(ICON_CACHE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    var apps by mutableStateOf<List<AppEntry>>(emptyList())
        private set

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) = refresh()
    }

    fun start() {
        launcher.registerCallback(callback, Handler(Looper.getMainLooper()))
        refresh()
    }

    fun stop() = launcher.unregisterCallback(callback)

    fun refresh() {
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val collator = Collator.getInstance()
                launcher.profiles.flatMap { user -> launcher.getActivityList(null, user) }
                    .filter { it.componentName.packageName != context.packageName }
                    .map { info ->
                        AppEntry(
                            key = keyOf(info.componentName, info.user),
                            label = info.label.toString().trim(),
                            component = info.componentName,
                            user = info.user,
                            category = categoryOf(info.applicationInfo),
                            info = info,
                            installed = runCatching { info.firstInstallTime }.getOrDefault(0L),
                        )
                    }
                    .sortedWith { a, b -> collator.compare(a.label, b.label) }
            }
            icons.evictAll()
            shortcutCache = null
            apps = loaded
        }
    }

    fun cachedIcon(app: AppEntry, sizePx: Int, themed: Boolean): ImageBitmap? = icons.get(iconKey(app, sizePx, themed))

    suspend fun icon(app: AppEntry, sizePx: Int, themed: Boolean, tint: Int, ground: Int): ImageBitmap {
        val key = iconKey(app, sizePx, themed)
        icons.get(key)?.let { return it }
        val made = withContext(Dispatchers.Default) { render(app.info.getIcon(0), sizePx, themed, tint, ground) }
        icons.put(key, made)
        return made
    }

    fun launch(app: AppEntry, view: View, bounds: Rect?) {
        val options = bounds?.let {
            ActivityOptions.makeScaleUpAnimation(view, it.left, it.top, it.width(), it.height()).toBundle()
        }
        runCatching { launcher.startMainActivity(app.component, app.user, bounds, options) }
    }

    fun shortcuts(app: AppEntry): List<android.content.pm.ShortcutInfo> = runCatching {
        if (!launcher.hasShortcutHostPermission()) return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setActivity(app.component)
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        launcher.getShortcuts(query, app.user).orEmpty().sortedBy { it.rank }
    }.getOrDefault(emptyList())

    @Volatile private var shortcutCache: List<Pair<AppEntry, android.content.pm.ShortcutInfo>>? = null

    fun searchShortcuts(query: String): List<Pair<AppEntry, android.content.pm.ShortcutInfo>> {
        val all = shortcutCache ?: runCatching {
            if (!launcher.hasShortcutHostPermission()) return emptyList()
            apps.flatMap { app -> shortcuts(app).map { app to it } }
        }.getOrDefault(emptyList()).also { shortcutCache = it }
        val q = query.lowercase()
        return all.filter { (_, sc) -> (sc.shortLabel ?: "").toString().lowercase().contains(q) || (sc.longLabel ?: "").toString().lowercase().contains(q) }
    }

    fun shortcutIcon(shortcut: android.content.pm.ShortcutInfo): ImageBitmap? = runCatching {
        val drawable = launcher.getShortcutIconDrawable(shortcut, context.resources.displayMetrics.densityDpi) ?: return null
        val size = (32 * context.resources.displayMetrics.density).toInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(bitmap))
        bitmap.asImageBitmap()
    }.getOrNull()

    fun startShortcut(shortcut: android.content.pm.ShortcutInfo, bounds: Rect?) {
        runCatching { launcher.startShortcut(shortcut, bounds, null) }
    }

    fun openInfo(app: AppEntry, bounds: Rect?) {
        runCatching { launcher.startAppDetailsActivity(app.component, app.user, bounds, null) }
    }

    fun uninstall(app: AppEntry) {
        val intent = Intent(Intent.ACTION_DELETE, Uri.fromParts("package", app.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun defaultDock(): List<String> {
        val pm = context.packageManager
        val intents = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
        )
        val packages = intents.mapNotNull { intent ->
            runCatching { pm.resolveActivity(intent, 0)?.activityInfo?.packageName }.getOrNull()
                ?.takeIf { it != "android" }
        }.distinct()
        val current = apps
        val picked = packages.mapNotNull { pkg -> current.firstOrNull { it.packageName == pkg }?.key }.toMutableList()
        listOf("com.android.vending", "com.google.android.apps.photos", "com.google.android.gm").forEach { pkg ->
            if (picked.size < DOCK_SIZE) current.firstOrNull { it.packageName == pkg }?.key?.let { if (it !in picked) picked += it }
        }
        return picked.take(DOCK_SIZE)
    }

    private fun iconKey(app: AppEntry, sizePx: Int, themed: Boolean) = "${app.key}:$sizePx:$themed"

    private fun render(drawable: Drawable, size: Int, themed: Boolean, tint: Int, ground: Int): ImageBitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val adaptive = drawable as? AdaptiveIconDrawable
        val mono = if (themed) adaptive?.monochrome else null
        when {
            mono != null -> {
                canvas.drawColor(ground)
                val outer = (size * 0.25f).toInt()
                mono.setBounds(-outer, -outer, size + outer, size + outer)
                mono.setTint(tint)
                mono.draw(canvas)
            }
            adaptive != null -> {
                val outer = (size * 0.25f).toInt()
                adaptive.background?.let { it.setBounds(-outer, -outer, size + outer, size + outer); it.draw(canvas) }
                adaptive.foreground?.let { it.setBounds(-outer, -outer, size + outer, size + outer); it.draw(canvas) }
            }
            else -> {
                canvas.drawColor(ground)
                val inset = (size * 0.16f).toInt()
                drawable.setBounds(inset, inset, size - inset, size - inset)
                drawable.draw(canvas)
            }
        }
        return bitmap.asImageBitmap()
    }

    private fun categoryOf(info: ApplicationInfo): AppCategory = when (info.category) {
        ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.Social
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE, ApplicationInfo.CATEGORY_NEWS -> AppCategory.Media
        ApplicationInfo.CATEGORY_GAME -> AppCategory.Games
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.Work
        ApplicationInfo.CATEGORY_MAPS -> AppCategory.Travel
        else -> if (info.flags and ApplicationInfo.FLAG_IS_GAME != 0) AppCategory.Games else AppCategory.Other
    }

    companion object {
        const val DOCK_SIZE = 5
        private const val ICON_CACHE_BYTES = 48 * 1024 * 1024

        fun keyOf(component: ComponentName, user: UserHandle): String = "${component.flattenToShortString()}#${user.hashCode()}"
    }
}
