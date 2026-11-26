package com.mfhapps.lumen

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private lateinit var prefs: LumenPrefs
    private lateinit var catalog: AppCatalog
    private val homePresses = mutableIntStateOf(0)
    private val resets = mutableIntStateOf(0)
    private var lastRadius = -1
    private var lastScrim = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
        window.setBackgroundDrawable(ColorDrawable(0))
        prefs = LumenPrefs(this)
        if (prefs.iconSetState.value != AppIcons.SET) {
            prefs.set(prefs.logoStyleState, 0)
            AppIcons.select(this, 0, prefs.logoPaletteState.value)
            prefs.set(prefs.iconSetState, AppIcons.SET)
        }
        Widgets.init(this)
        catalog = AppCatalog(this, lifecycleScope)
        catalog.start()
        setContent {
            androidx.compose.runtime.LaunchedEffect(prefs.ownTop) { applyBars() }
            LumenTheme(prefs) {
                LumenHome(
                    catalog = catalog,
                    prefs = prefs,
                    homePresses = homePresses.intValue,
                    resets = resets.intValue,
                    isDefaultHome = ::isDefaultHome,
                    onAddWidget = ::addWidget,
                    onDepth = ::applyDepth,
                    onShade = ::expandShade,
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyBars()
    }

    private fun applyBars() {
        val controller = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        if (prefs.ownTop) {
            controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        } else {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
    }

    private var pendingWidget = -1
    private var pendingInfo: android.appwidget.AppWidgetProviderInfo? = null

    private val bindWidget = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) configureWidget() else cancelWidget()
    }

    override fun onStart() {
        super.onStart()
        runCatching { Widgets.host.startListening() }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) resets.intValue++
        runCatching { Widgets.host.stopListening() }
    }

    private fun addWidget(info: android.appwidget.AppWidgetProviderInfo) {
        val id = Widgets.host.allocateAppWidgetId()
        pendingWidget = id
        pendingInfo = info
        if (Widgets.manager.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)) {
            configureWidget()
        } else {
            bindWidget.launch(
                Intent(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile),
            )
        }
    }

    private fun configureWidget() {
        val info = pendingInfo ?: return
        if (info.configure != null) {
            runCatching { Widgets.host.startAppWidgetConfigureActivityForResult(this, pendingWidget, 0, REQUEST_CONFIGURE, null) }
                .onFailure { placeWidget() }
        } else {
            placeWidget()
        }
    }

    @Deprecated("AppWidgetHost configuration still reports through onActivityResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CONFIGURE) {
            if (resultCode == RESULT_OK) placeWidget() else cancelWidget()
        }
    }

    private fun placeWidget() {
        val info = pendingInfo ?: return
        val columns = prefs.homeColumnsState.value
        val rows = prefs.homeRowsState.value
        val width = resources.displayMetrics.widthPixels - 16 * resources.displayMetrics.density
        val cellW = width / columns
        val cellH = cellW * 1.15f
        val (w, h) = Widgets.span(info, cellW, cellH, resources.displayMetrics.density, columns, rows)
        val spot = freeSpot(prefs.home, columns, rows, w, h)
        if (spot == null) {
            android.widget.Toast.makeText(this, R.string.home_full, android.widget.Toast.LENGTH_LONG).show()
            cancelWidget()
            return
        }
        prefs.updateHome(prefs.home + HomeItem("w$pendingWidget", null, pendingWidget, spot.first, spot.second, w, h, HomePager.page))
        pendingWidget = -1
        pendingInfo = null
    }

    private fun cancelWidget() {
        if (pendingWidget != -1) runCatching { Widgets.host.deleteAppWidgetId(pendingWidget) }
        pendingWidget = -1
        pendingInfo = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.intValue++
    }

    override fun onDestroy() {
        catalog.stop()
        super.onDestroy()
    }

    private fun applyDepth(progress: Float) {
        val blurEnabled = windowManager.isCrossWindowBlurEnabled
        val step = (progress * BLUR_STEPS).toInt().coerceIn(0, BLUR_STEPS)
        val radius = if (blurEnabled) (step.toFloat() / BLUR_STEPS * prefs.blur * MAX_BLUR_DP * resources.displayMetrics.density).toInt() else 0
        val scrimAlpha = (((if (blurEnabled) 0.34f else 0.78f) * progress * 255f).toInt() / 8 * 8).coerceIn(0, 255)
        if (radius != lastRadius) {
            window.setBackgroundBlurRadius(radius)
            lastRadius = radius
        }
        if (scrimAlpha != lastScrim) {
            val dark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val base = if (dark) 0x000000 else 0xFFFFFF
            window.setBackgroundDrawable(ColorDrawable((scrimAlpha shl 24) or base))
            lastScrim = scrimAlpha
        }
        zoomWallpaper(progress)
    }

    private fun isDefaultHome(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return packageManager.resolveActivity(home, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == packageName
    }

    private fun zoomWallpaper(progress: Float) {
        val token = window.decorView.windowToken ?: return
        runCatching {
            val manager = WallpaperManager.getInstance(this)
            WallpaperManager::class.java.getMethod("setWallpaperZoomOut", android.os.IBinder::class.java, Float::class.javaPrimitiveType)
                .invoke(manager, token, progress.coerceIn(0f, 1f))
        }
    }

    @SuppressLint("WrongConstant")
    private fun expandShade() {
        runCatching {
            val service = getSystemService("statusbar")
            service.javaClass.getMethod("expandNotificationsPanel").invoke(service)
        }
    }

    private companion object {
        const val MAX_BLUR_DP = 42f
        const val BLUR_STEPS = 5
        const val REQUEST_CONFIGURE = 41
    }
}
