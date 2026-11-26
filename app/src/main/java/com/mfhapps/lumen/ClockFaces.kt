package com.mfhapps.lumen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

object Flex {
    private val cache = HashMap<Long, FontFamily>()

    @androidx.compose.ui.text.ExperimentalTextApi
    fun family(weight: Int, width: Int): FontFamily {
        val w = (weight / 25 * 25).coerceIn(100, 1000)
        val wd = (width / 4 * 4).coerceIn(25, 151)
        val key = w.toLong() * 1000 + wd
        return cache.getOrPut(key) {
            FontFamily(
                Font(
                    R.font.roboto_flex,
                    weight = FontWeight(w),
                    variationSettings = FontVariation.Settings(FontVariation.weight(w), FontVariation.width(wd.toFloat()), FontVariation.Setting("opsz", 144f)),
                ),
            )
        }
    }
}

fun Modifier.trimLines(fraction: Float = 0.13f): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val cut = (placeable.height * fraction).toInt()
        layout(placeable.width, (placeable.height - cut * 2).coerceAtLeast(0)) { placeable.place(0, -cut) }
    },
)

fun clockWeight(value: Float): Int = (150 + value * 850).toInt()

fun clockWidth(value: Float): Int = (25 + value * 126).toInt()

@Composable
fun rememberSeconds(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L - now % 1000L)
        }
    }
    return now
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
fun flexStyle(sizeSp: Float, weight: Int, width: Int, color: Color, shadow: Shadow? = null): TextStyle = TextStyle(
    fontFamily = Flex.family(weight, width),
    fontWeight = FontWeight((weight / 25 * 25).coerceIn(100, 1000)),
    fontSize = sizeSp.sp,
    lineHeight = (sizeSp * 0.84f).sp,
    color = color,
    shadow = shadow,
    letterSpacing = (-sizeSp / 34f).sp,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
)

@Composable
fun clockLabel(style: ClockStyle): String = stringResource(
    when (style) {
        ClockStyle.Stacked -> R.string.clock_stacked
        ClockStyle.Shape -> R.string.clock_shape
        ClockStyle.Line -> R.string.clock_line
        ClockStyle.Breathe -> R.string.clock_breathe
        ClockStyle.Orbit -> R.string.clock_orbit
        ClockStyle.Tiles -> R.string.clock_tiles
        ClockStyle.Tall -> R.string.clock_tall
    },
)

@Composable
fun ClockFace(
    style: ClockStyle,
    hours: String,
    minutes: String,
    ink: Color,
    accent: Color,
    scale: Float,
    weight: Int,
    width: Int,
    shadow: Shadow? = Shadow(Color.Black.copy(alpha = 0.32f), blurRadius = 24f),
) {
    val big = 112f * scale
    when (style) {
        ClockStyle.Stacked -> Column {
            Text(hours, style = flexStyle(big, weight, width, ink, shadow), modifier = Modifier.trimLines())
            Text(minutes, style = flexStyle(big, (weight - 520).coerceAtLeast(120), width, accent, shadow), modifier = Modifier.trimLines())
        }
        ClockStyle.Line -> Row(verticalAlignment = Alignment.Bottom) {
            Text(hours, style = flexStyle(big * 0.78f, weight, width, ink, shadow))
            Text(":", style = flexStyle(big * 0.78f, (weight - 300).coerceAtLeast(150), width, accent, shadow))
            Text(minutes, style = flexStyle(big * 0.78f, (weight - 300).coerceAtLeast(150), width, ink, shadow))
        }
        ClockStyle.Breathe -> BreatheClock(hours, minutes, ink, accent, big, width, shadow)
        ClockStyle.Shape -> MorphClock(hours, minutes, (210 * scale).dp, weight, width)
        ClockStyle.Orbit -> OrbitClock((220 * scale).dp)
        ClockStyle.Tiles -> TilesClock(hours, minutes, (scale * 78f).dp, weight, width)
        ClockStyle.Tall -> TallClock(hours, minutes, scale)
    }
}

@Composable
private fun BreatheClock(hours: String, minutes: String, ink: Color, accent: Color, big: Float, width: Int, shadow: Shadow?) {
    val transition = rememberInfiniteTransition(label = "breathe")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5200), RepeatMode.Reverse), label = "phase")
    val eased = phase * phase * (3f - 2f * phase)
    val heavy = (180 + eased * 780).toInt()
    val light = (940 - eased * 760).toInt()
    Column {
        Text(hours, style = flexStyle(big, heavy, width, ink, shadow), modifier = Modifier.trimLines())
        Text(minutes, style = flexStyle(big, light, width, accent, shadow), modifier = Modifier.trimLines())
    }
}

@Composable
private fun MorphClock(hours: String, minutes: String, size: Dp, weight: Int, width: Int) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "morph")
    val phase by transition.animateFloat(0f, 3f, infiniteRepeatable(tween(18_000, easing = LinearEasing), RepeatMode.Restart), label = "phase")
    val turn by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(48_000, easing = LinearEasing), RepeatMode.Restart), label = "turn")
    val morphs = remember { LumenShapes.clockCycle.let { list -> list.indices.map { Morph(list[it], list[(it + 1) % list.size]) } } }
    val segment = phase.toInt().coerceIn(0, morphs.lastIndex)
    val local = phase - segment
    val eased = local * local * (3f - 2f * local)
    val type = size.value * 0.34f
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer { shape = MorphShape(morphs[segment], eased, turn); clip = true }
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer))),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(hours, style = flexStyle(type, weight, width, scheme.onPrimaryContainer), modifier = Modifier.trimLines(0.1f))
            Text(minutes, style = flexStyle(type, (weight - 480).coerceAtLeast(150), width, scheme.onTertiaryContainer), modifier = Modifier.trimLines(0.1f))
        }
    }
}

@Composable
private fun TallClock(hours: String, minutes: String, scale: Float) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(hours.padStart(2, '0') to (scheme.primary to scheme.onPrimary), minutes to (scheme.primaryContainer to scheme.onPrimaryContainer)).forEach { (text, colours) ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = (96 * scale).dp, height = (150 * scale).dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape((28 * scale).dp))
                    .background(colours.first),
            ) {
                Text(text, style = flexStyle(118f * scale, 260, 25, colours.second).copy(letterSpacing = (-2).sp))
            }
        }
    }
}

@Composable
fun OrbitClock(size: Dp, showDate: Boolean = true) {
    val springs = LocalSprings.current
    val scheme = MaterialTheme.colorScheme
    val now = rememberSeconds()
    val calendar = remember(now) { java.util.Calendar.getInstance().apply { timeInMillis = now } }
    val hour = calendar.get(java.util.Calendar.HOUR) + calendar.get(java.util.Calendar.MINUTE) / 60f
    val minute = calendar.get(java.util.Calendar.MINUTE) + calendar.get(java.util.Calendar.SECOND) / 60f
    val second = calendar.get(java.util.Calendar.SECOND).toFloat()
    val sweep = remember { Animatable(second) }
    LaunchedEffect(second) {
        val target = if (second < sweep.value - 30f) second + 60f else second
        sweep.animateTo(target, springs.fastSpatial())
        if (sweep.value >= 60f) sweep.snapTo(sweep.value - 60f)
    }
    val dial = remember { PolygonShape(LumenShapes.clockBadge) }
    val face = scheme.primaryContainer
    val hourHand = scheme.onPrimaryContainer
    val minuteHand = scheme.primary
    val orbit = scheme.tertiary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Box(Modifier.size(size).clip(dial).background(androidx.compose.ui.graphics.Brush.radialGradient(listOf(face, face.copy(alpha = 0.82f)))))
        Canvas(Modifier.size(size)) {
            val c = center
            val r = this.size.minDimension / 2f
            for (i in 0 until 12) {
                val a = Math.toRadians(i * 30.0 - 90.0)
                val p = Offset(c.x + (r * 0.74f * cos(a)).toFloat(), c.y + (r * 0.74f * sin(a)).toFloat())
                drawCircle(hourHand.copy(alpha = if (i % 3 == 0) 0.8f else 0.28f), radius = if (i % 3 == 0) r * 0.035f else r * 0.022f, center = p)
            }
            rotate(hour * 30f, c) {
                drawLine(hourHand, c, Offset(c.x, c.y - r * 0.42f), strokeWidth = r * 0.13f, cap = StrokeCap.Round)
            }
            rotate(minute * 6f, c) {
                drawLine(minuteHand, c, Offset(c.x, c.y - r * 0.64f), strokeWidth = r * 0.08f, cap = StrokeCap.Round)
            }
            drawCircle(hourHand, radius = r * 0.07f, center = c)
            if (showDate) {
                val label = android.text.format.DateFormat.format("EEE d", calendar).toString().uppercase()
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb((hourHand.alpha * 255).toInt(), (hourHand.red * 255).toInt(), (hourHand.green * 255).toInt(), (hourHand.blue * 255).toInt())
                    textSize = r * 0.15f
                    isFakeBoldText = true
                    letterSpacing = 0.12f
                }
                val arc = android.graphics.Path().apply {
                    addArc(c.x - r * 0.6f, c.y - r * 0.6f, c.x + r * 0.6f, c.y + r * 0.6f, -150f, 120f)
                }
                drawContext.canvas.nativeCanvas.drawTextOnPath(label, arc, 0f, 0f, paint)
            }
            val sa = Math.toRadians(sweep.value * 6.0 - 90.0)
            drawCircle(orbit, radius = r * 0.055f, center = Offset(c.x + (r * 0.88f * cos(sa)).toFloat(), c.y + (r * 0.88f * sin(sa)).toFloat()))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TilesClock(hours: String, minutes: String, tile: Dp, weight: Int, width: Int) {
    val springs = LocalSprings.current
    val scheme = MaterialTheme.colorScheme
    val digits = (hours.padStart(2, '0') + minutes).toList()
    val shapes = remember { listOf(LumenShapes.clockBadge, androidx.compose.material3.MaterialShapes.Clover4Leaf, androidx.compose.material3.MaterialShapes.Arch, androidx.compose.material3.MaterialShapes.Sunny).map { PolygonShape(it) } }
    val fills = listOf(scheme.primaryContainer, scheme.tertiaryContainer, scheme.secondaryContainer, scheme.primary)
    val inks = listOf(scheme.onPrimaryContainer, scheme.onTertiaryContainer, scheme.onSecondaryContainer, scheme.onPrimary)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(0, 2).forEach { start ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (start until start + 2).forEach { i ->
                    val digit = digits[i]
                    val spin = remember { Animatable(0f) }
                    LaunchedEffect(digit) {
                        spin.snapTo(-14f)
                        spin.animateTo(0f, springs.spatial())
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(tile).graphicsLayer { rotationZ = spin.value }.clip(shapes[i]).background(fills[i]),
                    ) {
                        Text(digit.toString(), style = flexStyle(tile.value * 0.62f, weight, width, inks[i]))
                    }
                }
            }
        }
    }
}

@Composable
fun DateWidget(size: Dp) {
    val scheme = MaterialTheme.colorScheme
    val now = rememberNow()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape).background(scheme.tertiaryContainer)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(android.text.format.DateFormat.format("EEE", now).toString().uppercase(), style = flexStyle(size.value * 0.16f, 700, 100, scheme.onTertiaryContainer).copy(letterSpacing = 1.5.sp))
            Text(android.text.format.DateFormat.format("d", now).toString(), style = flexStyle(size.value * 0.46f, 900, 60, scheme.onTertiaryContainer))
        }
    }
}

@Composable
fun BatteryRingWidget(size: Dp) {
    val scheme = MaterialTheme.colorScheme
    val (level, charging) = rememberBattery()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size).clip(PolygonShape(LumenShapes.clockBadge)).background(scheme.secondaryContainer)) {
        BatteryWave(level, charging, scheme.onSecondaryContainer, size * 0.74f, showPercent = true, percentInside = true)
    }
}
