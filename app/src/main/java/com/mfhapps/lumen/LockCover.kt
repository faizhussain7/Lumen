@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mfhapps.lumen

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class LockCover : ComponentActivity() {
    private val unlocked = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0x33000000))
        if (windowManager.isCrossWindowBlurEnabled) window.setBackgroundBlurRadius((6 * resources.displayMetrics.density).toInt())
        registerReceiver(unlocked, IntentFilter(Intent.ACTION_USER_PRESENT), Context.RECEIVER_NOT_EXPORTED)
        val prefs = LumenPrefs(this)
        val device = DeviceState(this)
        device.start()
        setContent {
            LumenTheme(prefs) {
                CoverScreen(prefs, device, onUnlock = ::unlock, onCamera = ::camera, onOpen = ::openNotice)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!getSystemService(KeyguardManager::class.java).isKeyguardLocked) finish()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(unlocked) }
        super.onDestroy()
    }

    @Deprecated("Back closes nothing on the lock cover")
    override fun onBackPressed() = Unit

    private fun openNotice(intent: android.app.PendingIntent) {
        val keyguard = getSystemService(KeyguardManager::class.java)
        val fire = {
            runCatching {
                val options = android.app.ActivityOptions.makeBasic()
                if (android.os.Build.VERSION.SDK_INT >= 34) options.setPendingIntentBackgroundActivityStartMode(android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                intent.send(this, 0, null, null, null, null, options.toBundle())
            }
            finish()
        }
        if (!keyguard.isKeyguardLocked) {
            fire()
            return
        }
        keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = fire()
        })
    }

    private fun unlock() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!keyguard.isKeyguardLocked) {
            finish()
            return
        }
        keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = finish()
        })
    }

    private fun camera() {
        runCatching {
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        }
    }

    companion object {
        fun show(context: Context) {
            runCatching {
                context.startActivity(
                    Intent(context, LockCover::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                )
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CoverScreen(prefs: LumenPrefs, device: DeviceState, onUnlock: () -> Unit, onCamera: () -> Unit, onOpen: (android.app.PendingIntent) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val lift = remember { Animatable(0f) }
    val hint = rememberInfiniteTransition(label = "hint")
    val bob by hint.animateFloat(0f, 1f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "bob")
    val context = androidx.compose.ui.platform.LocalContext.current
    val now = rememberNow()
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val hours = android.text.format.DateFormat.format(if (is24) "HH" else "hh", now).toString()
    val minutes = android.text.format.DateFormat.format("mm", now).toString()
    val (level, charging) = rememberBattery()
    val notices by NoticeStore.list.collectAsState()
    val media = if (prefs.lockMediaState.value) rememberNowPlaying() else null
    val alarm = remember(now) { context.getSystemService(android.app.AlarmManager::class.java)?.nextAlarmClock?.triggerTime }
    val colour = prefs.clockColourState.value
    val ink = if (colour == 0) Color.White else Color(colour)
    val accent = if (colour == 0) scheme.primaryFixedDim else Color(colour).copy(alpha = 0.72f)
    val style = prefs.lockClockState.value
    val centred = prefs.lockCentreState.value || style in setOf(ClockStyle.Shape, ClockStyle.Orbit, ClockStyle.Tiles, ClockStyle.Tall)
    fun shortcut(kind: LockShortcut) {
        when (kind) {
            LockShortcut.None -> Unit
            LockShortcut.Torch -> device.toggleTorch()
            LockShortcut.Camera -> onCamera()
            LockShortcut.DoNotDisturb -> device.toggleDnd()
            LockShortcut.Voice -> {
                onUnlock()
                runCatching { context.startActivity(android.content.Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }
    val clockBlock = @Composable { align: Alignment.Horizontal ->
        Column(horizontalAlignment = align) {
            Text(
                android.text.format.DateFormat.format("EEEE, d MMMM", now).toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp),
                color = Color.White.copy(alpha = 0.92f),
            )
            Spacer(Modifier.height(12.dp))
            ClockFace(style, hours, minutes, ink, accent, 0.95f + prefs.clockScaleState.value * 0.45f, clockWeight(prefs.clockWeightState.value), clockWidth(prefs.clockWidthState.value))
            Spacer(Modifier.height(18.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, align),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (charging && level >= 0) {
                    Column(
                        Modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xFF1E8E3E).copy(alpha = 0.92f)).padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlyphIcon(Glyph.Bolt, Color.White, 18.dp)
                            Spacer(Modifier.size(6.dp))
                            Text(context.getString(R.string.lock_charging, level), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        }
                        androidx.compose.material3.LinearWavyProgressIndicator(
                            progress = { level / 100f },
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.padding(top = 6.dp).size(width = 150.dp, height = 10.dp),
                        )
                    }
                } else if (level >= 0) {
                    CoverChip({ BatteryWaveBar(level, false, Color.White, 70.dp) }, "$level%")
                }
                if (alarm != null) CoverChip({ GlyphIcon(Glyph.Alarm, Color.White, 17.dp) }, android.text.format.DateFormat.format(if (is24) "EEE HH:mm" else "EEE h:mm a", java.util.Date(alarm)).toString())
            }
            if (prefs.lockWidgetsState.value) {
                Spacer(Modifier.height(14.dp))
                val weather = rememberWeather(prefs.weatherState.value)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateWidget(64.dp)
                    if (weather != null) WeatherWidget(64.dp, weather)
                }
            }
            if (prefs.ownerMessage.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(prefs.ownerMessage, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
    val groups = remember(notices) { groupNotices(notices) }
    val openGroups = remember { mutableStateOf(setOf<String>()) }
    var showAll by remember { mutableStateOf(false) }
    val infoBlock = @Composable { modifier: Modifier ->
        Column(modifier) {
            if (media != null) Box(Modifier.padding(bottom = 10.dp)) { NowBar(media) }
            when {
                prefs.lockNoticesState.value == LockNotices.Hidden || notices.isEmpty() -> Unit
                prefs.lockNoticesState.value == LockNotices.Icons && !showAll -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(CircleShape).clickable { showAll = true }.padding(4.dp),
                ) {
                    notices.distinctBy { it.pkg }.take(6).forEach { notice -> NoticeIcon(notice.pkg, 32.dp) }
                    Text("${notices.size}", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
                else -> androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.clip(RoundedCornerShape(24.dp))) {
                    noticeGroups(groups, openGroups) { onOpen(it) }
                }
            }
        }
    }
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onDoubleTap = { LumenAccess.lock() }) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        scope.launch { lift.snapTo((lift.value - dy / size.height).coerceIn(0f, 1f)) }
                    },
                    onDragEnd = {
                        if (lift.value > 0.15f) {
                            scope.launch { lift.animateTo(1f) }
                            onUnlock()
                        } else {
                            scope.launch { lift.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 300f)) }
                        }
                    },
                )
            },
    ) {
        val wide = maxWidth > maxHeight && maxWidth >= 600.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = -lift.value * size.height * 0.3f
                    alpha = 1f - lift.value
                    val s = 1f - lift.value * 0.08f
                    scaleX = s
                    scaleY = s
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
        ) {
            if (wide) {
                Row(Modifier.weight(1f).padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Box(Modifier.weight(1f).verticalScroll(androidx.compose.foundation.rememberScrollState())) { clockBlock(Alignment.Start) }
                    infoBlock(Modifier.weight(1f).padding(top = 12.dp))
                }
            } else {
                Spacer(Modifier.height(32.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = if (centred) Alignment.TopCenter else Alignment.TopStart) {
                    clockBlock(if (centred) Alignment.CenterHorizontally else Alignment.Start)
                }
                Spacer(Modifier.height(18.dp))
                infoBlock(Modifier.weight(1f).fillMaxWidth())
            }
            Column(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.KeyboardArrowUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f + bob * 0.4f),
                    modifier = Modifier.size(28.dp).graphicsLayer { translationY = -bob * 8.dp.toPx() },
                )
                Text(stringResource(R.string.lock_unlock), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White.copy(alpha = 0.85f))
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 20.dp),
            ) {
                ShortcutButton(prefs.lockLeftState.value, device) { shortcut(prefs.lockLeftState.value) }
                if (prefs.lockFingerprintState.value) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f)).clickable(onClickLabel = stringResource(R.string.unlock_fingerprint)) { onUnlock() },
                    ) {
                        androidx.compose.material3.Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                }
                ShortcutButton(prefs.lockRightState.value, device) { shortcut(prefs.lockRightState.value) }
            }
        }
    }
}

@Composable
private fun ShortcutButton(kind: LockShortcut, device: DeviceState, onClick: () -> Unit) {
    when (kind) {
        LockShortcut.None -> Spacer(Modifier.size(64.dp))
        LockShortcut.Torch -> CoverButton(Glyph.Torch, device.torch, stringResource(R.string.lock_torch), onClick)
        LockShortcut.Camera -> CoverButton(Glyph.Camera, false, stringResource(R.string.lock_camera), onClick)
        LockShortcut.DoNotDisturb -> CoverButton(Glyph.Moon, device.dnd, stringResource(R.string.shortcut_dnd), onClick)
        LockShortcut.Voice -> CoverButton(Glyph.Mic, false, stringResource(R.string.shortcut_voice), onClick)
    }
}

@Composable
private fun NoticeIcon(pkg: String, size: androidx.compose.ui.unit.Dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val px = with(androidx.compose.ui.platform.LocalDensity.current) { size.roundToPx() }
    val bitmap = remember(pkg) {
        runCatching { context.packageManager.getApplicationIcon(pkg).toBitmap(px, px).asImageBitmap() }.getOrNull()
    }
    Box(Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        bitmap?.let { androidx.compose.foundation.Image(it, contentDescription = null, modifier = Modifier.size(size)) }
    }
}

@Composable
private fun NowBar(media: NowPlaying) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(CircleShape).background(scheme.primaryContainer.copy(alpha = 0.9f)).padding(6.dp),
    ) {
        Box(Modifier.size(52.dp).clip(PolygonShape(LumenShapes.clockBadge)).background(scheme.primary), contentAlignment = Alignment.Center) {
            if (media.art != null) androidx.compose.foundation.Image(media.art, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(52.dp))
            else GlyphIcon(Glyph.Volume, scheme.onPrimary, 24.dp)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(media.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = scheme.onPrimaryContainer, maxLines = 1)
            Text(media.artist.ifBlank { media.app }, style = MaterialTheme.typography.bodySmall, color = scheme.onPrimaryContainer.copy(alpha = 0.75f), maxLines = 1)
        }
        MediaButton(Icons.Rounded.SkipPrevious) { media.controller.transportControls.skipToPrevious() }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(width = 60.dp, height = 48.dp).clip(CircleShape).background(scheme.primary).clickable {
                if (media.playing) media.controller.transportControls.pause() else media.controller.transportControls.play()
            },
        ) { androidx.compose.material3.Icon(if (media.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, contentDescription = null, tint = scheme.onPrimary) }
        MediaButton(Icons.Rounded.SkipNext) { media.controller.transportControls.skipToNext() }
    }
}

@Composable
private fun MediaButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick)) {
        androidx.compose.material3.Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun CoverChip(icon: @Composable () -> Unit, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        icon()
        Spacer(Modifier.size(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

@Composable
private fun CoverButton(glyph: Glyph, active: Boolean, label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .clip(if (active) PolygonShape(LumenShapes.clockBadge) else CircleShape)
            .background(if (active) scheme.primary else Color.White.copy(alpha = 0.16f))
            .clickable(onClickLabel = label, onClick = onClick),
    ) { GlyphIcon(glyph, if (active) scheme.onPrimary else Color.White, 26.dp) }
}
