package com.mfhapps.lumen

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.text.format.DateFormat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Date

@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - now % 60_000L)
        }
    }
    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) { now = System.currentTimeMillis() }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return now
}

@Composable
fun rememberBattery(): Pair<Int, Boolean> {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                intent ?: return
                val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                level = if (raw >= 0 && scale > 0) raw * 100 / scale else -1
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), Context.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    return level to charging
}

@Composable
fun ClockArea(style: ClockStyle, ink: Color, modifier: Modifier = Modifier, prefs: LumenPrefs? = null) {
    val context = LocalContext.current
    val now = rememberNow()
    val is24 = DateFormat.is24HourFormat(context)
    val hours = DateFormat.format(if (is24) "HH" else "h", now).toString()
    val minutes = DateFormat.format("mm", now).toString()
    val shadow = Shadow(Color.Black.copy(alpha = 0.35f), blurRadius = 24f)
    val accent = MaterialTheme.colorScheme.primaryFixedDim
    val screen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    val landscapeFit = if (config.screenWidthDp > config.screenHeightDp) (config.screenHeightDp / 520f).coerceIn(0.45f, 1f) else 1f
    val scale = (0.7f + (prefs?.clockScaleState?.value ?: 0.5f) * 0.6f) * landscapeFit
    val big = ((screen * 0.25f).coerceIn(76f, 108f) * scale).toInt()
    val openClock = { runCatching { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    val openCalendar = {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI.buildUpon().appendPath("time").build()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
    val colour = prefs?.clockColourState?.value ?: 0
    val face = if (colour == 0) ink else Color(colour)
    val faceAccent = if (colour == 0) accent else Color(colour).copy(alpha = 0.72f)
    Column(modifier, horizontalAlignment = Alignment.Start) {
        if (prefs?.showDateState?.value != false) Text(
            DateFormat.format("EEEE, d MMMM", now).toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, shadow = shadow, letterSpacing = 0.2.sp),
            color = ink.copy(alpha = 0.92f),
            modifier = Modifier.clickable(interactionSource = null, indication = null) { openCalendar() },
        )
        val weather = rememberWeather(prefs?.weatherState?.value == true)
        if (weather != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                androidx.compose.material3.Icon(weatherIcon(weather), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(6.dp))
                Text("${weather.temperature}° · ${weatherLabel(weather)}", style = MaterialTheme.typography.titleSmall.copy(shadow = shadow), color = ink.copy(alpha = 0.9f))
            }
        }
        Spacer(Modifier.size(8.dp))
        Box(Modifier.clickable(interactionSource = null, indication = null) { openClock() }) {
            ClockFace(
                style,
                hours.padStart(2, '0'),
                minutes,
                face,
                faceAccent,
                scale,
                clockWeight(prefs?.clockWeightState?.value ?: 0.78f),
                clockWidth(prefs?.clockWidthState?.value ?: 0.45f),
                shadow,
            )
        }
        if (prefs?.showChipsState?.value != false) {
            Spacer(Modifier.size(16.dp))
            GlanceChips(now)
        }
    }
}


fun clockText(size: Int, weight: FontWeight, color: Color, shadow: Shadow) = TextStyle(
    fontSize = size.sp,
    lineHeight = (size * 0.86f).sp,
    fontWeight = weight,
    color = color,
    shadow = shadow,
    letterSpacing = (-size / 28f).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

@Composable
fun ShapeClock(hours: String, minutes: String, size: androidx.compose.ui.unit.Dp = 210.dp) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "badge")
    val phase by transition.animateFloat(0f, 3f, infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val turn by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(48_000, easing = LinearEasing), RepeatMode.Restart), label = "turn")
    val morphs = remember { LumenShapes.clockCycle.let { list -> list.indices.map { androidx.graphics.shapes.Morph(list[it], list[(it + 1) % list.size]) } } }
    val segment = phase.toInt().coerceIn(0, morphs.lastIndex)
    val local = phase - segment
    val eased = local * local * (3f - 2f * local)
    val fontScale = size.value / 210f
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Box(
            Modifier
                .size(size)
                .clip(MorphShape(morphs[segment], eased, turn))
                .background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer))),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(hours, style = clockText((68 * fontScale).toInt(), FontWeight.Black, scheme.onPrimaryContainer, Shadow()))
            Text(minutes, style = clockText((68 * fontScale).toInt(), FontWeight.Light, scheme.onTertiaryContainer, Shadow()))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun GlanceChips(now: Long) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val (level, charging) = rememberBattery()
    val alarm = remember(now) { context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime }
    val is24 = DateFormat.is24HourFormat(context)
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (alarm != null) {
            val sameDay = DateFormat.format("yyyyDDD", alarm) == DateFormat.format("yyyyDDD", now)
            val pattern = (if (sameDay) "" else "EEE ") + (if (is24) "HH:mm" else "h:mm a")
            Chip(DateFormat.format(pattern, Date(alarm)).toString(), { GlyphIcon(Glyph.Alarm, scheme.onSecondaryContainer, 17.dp) }) {
                runCatching { context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        if (level >= 0) {
            Chip(context.getString(R.string.battery_level, level), { BatteryWaveBar(level, charging, scheme.onSecondaryContainer, 70.dp) }) {
                runCatching { context.startActivity(Intent(Intent.ACTION_POWER_USAGE_SUMMARY).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }
}

@Composable
private fun Chip(text: String, glyph: @Composable () -> Unit, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(scheme.secondaryContainer.copy(alpha = 0.82f))
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    ) {
        glyph()
        Spacer(Modifier.size(8.dp))
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = scheme.onSecondaryContainer)
    }
}

@Composable
private fun AlarmGlyph(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
        val stroke = 1.8.dp.toPx()
        val r = size.minDimension * 0.4f
        val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.54f)
        drawCircle(color, r, c, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawLine(color, c, androidx.compose.ui.geometry.Offset(c.x, c.y - r * 0.55f), stroke, androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, c, androidx.compose.ui.geometry.Offset(c.x + r * 0.45f, c.y), stroke, androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
private fun BatteryGlyph(level: Int, charging: Boolean, color: Color, fill: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(width = 20.dp, height = 12.dp)) {
        val stroke = 1.6.dp.toPx()
        val body = androidx.compose.ui.geometry.Size(size.width - 3.dp.toPx(), size.height)
        val corner = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
        drawRoundRect(color, size = body, cornerRadius = corner, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        drawRoundRect(color, topLeft = androidx.compose.ui.geometry.Offset(body.width + 0.5.dp.toPx(), size.height * 0.3f), size = androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height * 0.4f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()))
        val inset = stroke + 1.dp.toPx()
        val width = (body.width - inset * 2f) * (level.coerceIn(0, 100) / 100f)
        drawRoundRect(
            if (charging) fill else if (level <= 20) Color(0xFFFF6B6B) else color,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(width.coerceAtLeast(1f), body.height - inset * 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx()),
        )
    }
}
