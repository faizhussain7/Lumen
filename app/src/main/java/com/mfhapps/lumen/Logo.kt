package com.mfhapps.lumen

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

private fun tones(base: Color, ink: Color) = mapOf(
    "ink" to ink,
    "white" to ink,
    "light" to lerp(base, Color.White, 0.62f),
    "dark" to lerp(base, Color.Black, 0.45f),
    "accent" to base,
)

@Composable
fun LumenWordmark(height: Dp, colour: Color? = null, modifier: Modifier = Modifier, tile: Boolean = false, ink: Color? = null, ground: Color? = null) {
    val scheme = MaterialTheme.colorScheme
    val shape = if (tile) Wordmarks.tile else Wordmarks.integrated
    val paths = remember(tile) { shape.parts.map { PathParser().parsePathString(it.path).toPath() to it } }
    val base = colour ?: scheme.primary
    val dark = scheme.surface.luminance() < 0.5f
    val light = base.luminance() > 0.42f
    val roles = mapOf(
        "mark" to base,
        "tile" to base,
        "on" to if (light) lerp(base, Color.Black, 0.72f) else Color.White,
        "word" to (ink ?: scheme.onSurface),
        "glow" to lerp(base, if (dark) Color.Black else Color.White, 0.4f),
    )
    val gap = ground ?: scheme.surface
    Canvas(modifier.size(width = height * (shape.width / shape.height), height = height)) {
        val k = size.height / shape.height
        withTransform({ scale(k, k, pivot = Offset.Zero); translate(-shape.x, -shape.y) }) {
            paths.forEach { (path, part) ->
                val c = roles[part.role] ?: Color.White
                if (part.gap > 0f) drawPath(path, gap, style = Stroke(width = part.gap, join = StrokeJoin.Round))
                if (part.stroke > 0f) drawPath(path, c, style = Stroke(width = part.stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = StrokeJoin.Round))
                else drawPath(path, c)
            }
        }
    }
}

class MarkColours(val background: Color, val roles: Map<String, Color>)

fun markColours(style: Int, colour: Color): MarkColours {
    val light = colour.luminance() > 0.42f
    val tint = lerp(colour, Color.White, 0.86f)
    val onColour = if (light) lerp(colour, Color.Black, 0.72f) else Color.White
    val deep = lerp(colour, Color.Black, if (light) 0.45f else 0.2f)
    return when (style) {
        0 -> MarkColours(colour, mapOf("ink" to onColour, "glow" to if (light) lerp(colour, Color.Black, 0.25f) else lerp(colour, Color.White, 0.3f)))
        1 -> MarkColours(tint, mapOf("ink" to deep, "glow" to colour))
        2 -> MarkColours(colour, mapOf("ink" to onColour))
        else -> MarkColours(tint, mapOf("ink" to if (light) deep else colour))
    }
}

@Composable
fun LumenIcon(style: Int, size: Dp, colour: Color, modifier: Modifier = Modifier) {
    val parts = LogoMarks.styles[style.coerceIn(0, LogoMarks.styles.lastIndex)]
    val paths = remember(style) {
        parts.map { part ->
            PathParser().parsePathString(part.path).toPath().apply { if (part.evenOdd) fillType = PathFillType.EvenOdd } to part
        }
    }
    val colours = markColours(style, colour)
    Canvas(modifier.size(size)) {
        drawRect(colours.background)
        val k = this.size.minDimension / 72f
        withTransform({ scale(k, k, pivot = Offset.Zero); translate(-18f, -18f) }) {
            paths.forEach { (path, part) ->
                if (part.gap > 0f) drawPath(path, colours.roles["gap"] ?: Color.White, style = Stroke(width = part.gap, join = StrokeJoin.Round))
                drawPath(path, colours.roles[part.role] ?: Color.White)
            }
        }
    }
}

object AppIcons {
    const val STYLES = 4
    const val SET = 2
    val COLOURS get() = LogoMarks.colours.size

    fun select(context: Context, style: Int, colour: Int) {
        val pm = context.packageManager
        for (s in 1..STYLES) for (c in 0 until COLOURS) {
            val on = s == style + 1 && c == colour
            runCatching {
                pm.setComponentEnabledSetting(
                    ComponentName(context, "com.mfhapps.lumen.Icon${s}_$c"),
                    if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }
    }
}
