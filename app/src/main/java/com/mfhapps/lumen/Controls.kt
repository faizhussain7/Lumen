@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mfhapps.lumen

import android.app.NotificationManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpaceDashboard
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class Glyph { Wifi, Data, Bluetooth, Torch, Moon, Rotate, Lock, Shot, Sun, Volume, Gear, Camera, Bell, Search, Spark, Alarm, Widgets, Wallpaper, Home, Info, Delete, Hide, Dock, Calculate, Person, Web, Bolt, Mic, Remove, Check, Location, Hotspot, Nfc, Airplane, Saver, Dark, Phone, Next, Back }

class DeviceState(private val context: Context) {
    var wifi by mutableStateOf(false)
    var data by mutableStateOf(false)
    var dataLabel by mutableStateOf("")
    var bluetooth by mutableStateOf(false)
    var torch by mutableStateOf(false)
    var dnd by mutableStateOf(false)
    var rotation by mutableStateOf(false)
    var nfc by mutableStateOf(false)
    var location by mutableStateOf(false)
    var airplane by mutableStateOf(false)
    var saver by mutableStateOf(false)
    var darkMode by mutableStateOf(false)
    val hasFlash = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FLASH)
    val hasNfc = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_NFC)
    val hasBluetooth = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_BLUETOOTH)
    val hasPhone = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_TELEPHONY)
    val hasLocation = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_LOCATION)
    val hasRotation = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_SENSOR_ACCELEROMETER)
    var brightness by mutableFloatStateOf(0.5f)
    var volume by mutableFloatStateOf(0.5f)
    private var torchId: String? = null
    private val camera = context.getSystemService(CameraManager::class.java)
    private val audio = context.getSystemService(AudioManager::class.java)
    private val notifications = context.getSystemService(NotificationManager::class.java)
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == torchId) torch = enabled
        }
    }

    fun start() {
        torchId = runCatching {
            camera.cameraIdList.firstOrNull { camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
        }.getOrNull()
        runCatching { camera.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper())) }
        refresh()
    }

    fun stop() = runCatching { camera.unregisterTorchCallback(torchCallback) }

    fun refresh() {
        nfc = runCatching { android.nfc.NfcAdapter.getDefaultAdapter(context)?.isEnabled == true }.getOrDefault(false)
        location = runCatching { context.getSystemService(android.location.LocationManager::class.java).isLocationEnabled }.getOrDefault(false)
        airplane = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        saver = runCatching { context.getSystemService(android.os.PowerManager::class.java).isPowerSaveMode }.getOrDefault(false)
        darkMode = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        wifi = runCatching { context.getSystemService(WifiManager::class.java).isWifiEnabled }.getOrDefault(false)
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = runCatching { cm.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
        data = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        dataLabel = when {
            caps == null -> ""
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> ""
        }
        bluetooth = runCatching { context.getSystemService(BluetoothManager::class.java).adapter?.isEnabled == true }.getOrDefault(false)
        dnd = notifications.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        rotation = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1
        brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    }

    fun toggleTorch() {
        val id = torchId ?: return
        runCatching { camera.setTorchMode(id, !torch) }
    }

    fun toggleDnd(): Boolean {
        if (!notifications.isNotificationPolicyAccessGranted) {
            open(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            return false
        }
        notifications.setInterruptionFilter(if (dnd) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        dnd = !dnd
        return true
    }

    fun toggleRotation(): Boolean {
        if (!Settings.System.canWrite(context)) {
            open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(android.net.Uri.parse("package:${context.packageName}")))
            return false
        }
        Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (rotation) 0 else 1)
        rotation = !rotation
        return true
    }

    fun changeBrightness(value: Float): Boolean {
        if (!Settings.System.canWrite(context)) return false
        brightness = value.coerceIn(0.02f, 1f)
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, (brightness * 255f).toInt())
        return true
    }

    fun askWrite() = open(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).setData(android.net.Uri.parse("package:${context.packageName}")))

    fun changeVolume(value: Float) {
        volume = value.coerceIn(0f, 1f)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (volume * max).toInt(), 0)
    }

    fun open(intent: Intent) {
        runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
}

@Composable
fun ControlCentre(state: DeviceState, progress: () -> Float, onClose: () -> Unit, onDrag: (Float) -> Unit, onSettle: (Float) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        while (true) {
            state.refresh()
            delay(1500)
        }
    }
    val now = rememberNow()
    val (level, charging) = rememberBattery()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val wide = configuration.screenWidthDp >= 600 || configuration.screenWidthDp > configuration.screenHeightDp
    fun needAccess() {
        Toast.makeText(context, R.string.access_needed, Toast.LENGTH_LONG).show()
        LumenAccess.openSettings(context)
    }
    val on = stringResource(R.string.cc_on)
    val off = stringResource(R.string.cc_off)
    val pills = listOf(
        Tile(Glyph.Wifi, stringResource(R.string.cc_wifi), state.wifi, if (state.wifi) on else off) { state.open(Intent(Settings.Panel.ACTION_WIFI)) },
        Tile(Glyph.Data, stringResource(R.string.cc_data), state.data, state.dataLabel.ifEmpty { off }) { state.open(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)) },
    )
    val tiles = buildList {
        if (state.hasBluetooth) add(Tile(Glyph.Bluetooth, stringResource(R.string.cc_bluetooth), state.bluetooth, "") { state.open(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) })
        if (state.hasFlash) add(Tile(Glyph.Torch, stringResource(R.string.cc_torch), state.torch, "") { state.toggleTorch() })
        add(Tile(Glyph.Moon, stringResource(R.string.cc_dnd), state.dnd, "") { state.toggleDnd() })
        if (state.hasLocation) add(Tile(Glyph.Location, stringResource(R.string.cc_location), state.location, "") { state.open(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) })
        if (state.hasPhone) add(Tile(Glyph.Hotspot, stringResource(R.string.cc_hotspot), false, "") { state.open(Intent("android.settings.TETHER_SETTINGS")) })
        if (state.hasNfc) add(Tile(Glyph.Nfc, stringResource(R.string.cc_nfc), state.nfc, "") { state.open(Intent(Settings.Panel.ACTION_NFC)) })
        add(Tile(Glyph.Airplane, stringResource(R.string.cc_airplane), state.airplane, "") { state.open(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)) })
        add(Tile(Glyph.Saver, stringResource(R.string.cc_saver), state.saver, "") { state.open(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)) })
        add(Tile(Glyph.Dark, stringResource(R.string.cc_dark), state.darkMode, "") { state.open(Intent(Settings.ACTION_DISPLAY_SETTINGS)) })
        if (state.hasRotation) add(Tile(Glyph.Rotate, stringResource(R.string.cc_rotation), state.rotation, "") { state.toggleRotation() })
        add(Tile(Glyph.Shot, stringResource(R.string.cc_screenshot), false, "") {
            onClose()
            Handler(Looper.getMainLooper()).postDelayed({ if (!LumenAccess.screenshot()) needAccess() }, 650)
        })
        add(Tile(Glyph.Lock, stringResource(R.string.cc_lock), false, "") { onClose(); if (!LumenAccess.lock()) needAccess() })
        add(Tile(Glyph.Gear, stringResource(R.string.cc_settings), false, "") { onClose(); state.open(Intent(Settings.ACTION_SETTINGS)) })
    }
    val notices by NoticeStore.list.collectAsState()
    val groups = remember(notices) { groupNotices(notices) }
    val openGroups = remember { mutableStateOf(setOf<String>()) }
    val tap: (Tile) -> Unit = { haptics.performHapticFeedback(HapticFeedbackType.ContextClick); it.onClick() }
    val controls: @Composable () -> Unit = {
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(DateFormat.format(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm", now).toString(), style = flexStyle(44f, 950, 100, scheme.onSurface))
                    Text(DateFormat.format("EEEE, d MMMM", now).toString(), style = MaterialTheme.typography.titleSmall, color = scheme.onSurfaceVariant)
                }
                if (level >= 0) BatteryWave(level, charging, scheme.onSurface, 50.dp, showPercent = true, percentInside = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                pills.forEach { tile -> TileView(tile, Modifier.weight(1f)) { tap(tile) } }
            }
            androidx.compose.foundation.lazy.grid.LazyHorizontalGrid(
                rows = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(186.dp),
            ) {
                items(tiles.size) { i -> RoundToggle(tiles[i], i, Modifier.width(72.dp)) { tap(tiles[i]) } }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                ExpressiveSlider(Glyph.Sun, stringResource(R.string.cc_brightness), state.brightness) { value -> if (!state.changeBrightness(value)) state.askWrite() }
                ExpressiveSlider(Glyph.Volume, stringResource(R.string.cc_volume), state.volume) { state.changeVolume(it) }
            }
        }
    }
    val noticeList: androidx.compose.foundation.lazy.LazyListScope.() -> Unit = {
        item(key = "notice-head") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp, start = 4.dp)) {
                Text(stringResource(R.string.cc_notifications), style = flexStyle(24f, 900, 100, scheme.onSurface), modifier = Modifier.weight(1f))
                if (notices.any { it.clearable }) FabPill(stringResource(R.string.clear_all), null) { LumenNotifications.clearAll() }
            }
        }
        when {
            !Badges.granted(context) -> item(key = "grant") { FabPill(stringResource(R.string.opt_grant), Glyph.Bell, emphasis = true) { onClose(); Badges.requestAccess(context) } }
            notices.isEmpty() -> item(key = "empty") { Text(stringResource(R.string.no_notifications), style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(4.dp)) }
            else -> noticeGroups(groups, openGroups) { intent -> onClose(); sendNotice(context, intent) }
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { onClose() } }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dy -> change.consume(); onDrag(-dy) },
                    onDragEnd = { onSettle(0f) },
                )
            }
            .graphicsLayer { alpha = (progress().coerceIn(0f, 1f) * 1.7f).coerceIn(0f, 1f) },
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            Modifier
                .widthIn(max = if (wide) 1100.dp else 720.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val p = progress().coerceIn(0f, 1f)
                    translationY = -(1f - p) * size.height
                    val sc = 0.94f + 0.06f * p
                    scaleX = sc
                    scaleY = sc
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars.only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal))
                .padding(horizontal = 8.dp)
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility)
                .padding(top = 6.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(40.dp))
                .background(scheme.surfaceContainer.copy(alpha = 0.9f))
                .pointerInput(Unit) { detectTapGestures { } }
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.displayCutout.only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal))
                .heightIn(max = configuration.screenHeightDp.dp - 90.dp),
        ) {
            val pad = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 18.dp)
            if (wide) {
                Row {
                    Box(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState())) { controls() }
                    androidx.compose.foundation.lazy.LazyColumn(contentPadding = pad, modifier = Modifier.weight(1f)) { noticeList() }
                }
            } else {
                Column {
                    controls()
                    androidx.compose.foundation.lazy.LazyColumn(contentPadding = pad, modifier = Modifier.weight(1f, fill = false)) { noticeList() }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RoundToggle(tile: Tile, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val springs = LocalSprings.current
    val morph by androidx.compose.animation.core.animateFloatAsState(if (tile.active) 1f else 0f, springs.spec(), label = "toggle")
    val shape = remember { androidx.graphics.shapes.Morph(androidx.compose.material3.MaterialShapes.Square, LumenShapes.clockBadge) }
    val tones = listOf(scheme.primary to scheme.onPrimary, scheme.tertiary to scheme.onTertiary, scheme.secondary to scheme.onSecondary)
    val (fill, ink) = tones[index % tones.size]
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .tap(MorphShape(shape, morph, morph * 22f), label = tile.title, depth = 0.1f, onClick = onClick)
                .background(androidx.compose.ui.graphics.lerp(scheme.surfaceContainerHighest, fill, morph)),
        ) { GlyphIcon(tile.glyph, androidx.compose.ui.graphics.lerp(scheme.onSurface, ink, morph), 28.dp) }
        Spacer(Modifier.height(6.dp))
        Text(tile.title, style = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp), color = scheme.onSurfaceVariant, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

class Tile(val glyph: Glyph, val title: String, val active: Boolean, val status: String, val onClick: () -> Unit)

@Composable
private fun TileView(tile: Tile, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val springs = LocalSprings.current
    val corner by animateDpAsState(if (tile.active) 38.dp else 24.dp, springs.spec(), label = "corner")
    val fill by animateColorAsState(if (tile.active) scheme.primary else scheme.surfaceContainerHigh.copy(alpha = 0.92f), label = "fill")
    val ink = if (tile.active) scheme.onPrimary else scheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(76.dp)
            .tap(RoundedCornerShape(corner), depth = 0.04f, onClick = onClick)
            .background(fill)
            .padding(horizontal = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).clip(if (tile.active) PolygonShape(LumenShapes.clockBadge) else CircleShape).background(if (tile.active) scheme.onPrimary.copy(alpha = 0.18f) else scheme.surfaceContainerHighest),
        ) { GlyphIcon(tile.glyph, ink) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(tile.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (tile.status.isNotEmpty()) Text(tile.status, style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.75f), maxLines = 1)
        }
    }
}

@Composable
private fun ExpressiveSlider(glyph: Glyph, title: String, value: Float, onChange: (Float) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    var local by remember { mutableFloatStateOf(value) }
    var dragging by remember { mutableStateOf(false) }
    if (!dragging) local = value
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(scheme.surfaceContainerHigh.copy(alpha = 0.92f))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        val before = (local * 10).toInt()
                        local = (local + dx / size.width).coerceIn(0f, 1f)
                        if ((local * 10).toInt() != before) haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                        onChange(local)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    local = (offset.x / size.width).coerceIn(0f, 1f)
                    onChange(local)
                }
            },
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(local.coerceAtLeast(0.14f)).clip(RoundedCornerShape(32.dp)).background(scheme.primary))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            GlyphIcon(glyph, if (local > 0.1f) scheme.onPrimary else scheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = if (local > 0.35f) scheme.onPrimary else scheme.onSurface, modifier = Modifier.weight(1f))
            Text("${(local * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = if (local > 0.92f) scheme.onPrimary else scheme.onSurfaceVariant)
        }
    }
}

@Composable
fun GlyphIcon(glyph: Glyph, color: Color, size: androidx.compose.ui.unit.Dp = 22.dp) {
    androidx.compose.material3.Icon(glyph.vector(), contentDescription = null, tint = color, modifier = Modifier.size(size))
}

fun Glyph.vector(): androidx.compose.ui.graphics.vector.ImageVector = when (this) {
    Glyph.Wifi -> Icons.Rounded.Wifi
    Glyph.Data -> Icons.Rounded.SwapVert
    Glyph.Bluetooth -> Icons.Rounded.Bluetooth
    Glyph.Torch -> Icons.Rounded.FlashlightOn
    Glyph.Moon -> Icons.Rounded.DoNotDisturbOn
    Glyph.Rotate -> Icons.Rounded.ScreenRotation
    Glyph.Lock -> Icons.Rounded.Lock
    Glyph.Shot -> Icons.Rounded.Screenshot
    Glyph.Sun -> Icons.Rounded.LightMode
    Glyph.Volume -> Icons.AutoMirrored.Rounded.VolumeUp
    Glyph.Gear -> Icons.Rounded.Settings
    Glyph.Camera -> Icons.Rounded.PhotoCamera
    Glyph.Bell -> Icons.Rounded.Notifications
    Glyph.Search -> Icons.Rounded.Search
    Glyph.Spark -> Icons.Rounded.AutoAwesome
    Glyph.Alarm -> Icons.Rounded.Alarm
    Glyph.Widgets -> Icons.Rounded.Widgets
    Glyph.Wallpaper -> Icons.Rounded.Wallpaper
    Glyph.Home -> Icons.Rounded.Home
    Glyph.Info -> Icons.Rounded.Info
    Glyph.Delete -> Icons.Rounded.DeleteOutline
    Glyph.Hide -> Icons.Rounded.VisibilityOff
    Glyph.Dock -> Icons.Rounded.SpaceDashboard
    Glyph.Calculate -> Icons.Rounded.Calculate
    Glyph.Person -> Icons.Rounded.Person
    Glyph.Web -> Icons.Rounded.Public
    Glyph.Bolt -> Icons.Rounded.Bolt
    Glyph.Mic -> Icons.Rounded.Mic
    Glyph.Remove -> Icons.Rounded.RemoveCircleOutline
    Glyph.Check -> Icons.Rounded.Check
    Glyph.Location -> Icons.Rounded.LocationOn
    Glyph.Hotspot -> Icons.Rounded.WifiTethering
    Glyph.Nfc -> Icons.Rounded.Nfc
    Glyph.Airplane -> Icons.Rounded.AirplanemodeActive
    Glyph.Saver -> Icons.Rounded.BatterySaver
    Glyph.Dark -> Icons.Rounded.DarkMode
    Glyph.Phone -> Icons.Rounded.Call
    Glyph.Next -> Icons.Rounded.ChevronRight
    Glyph.Back -> Icons.Rounded.ChevronLeft
}
