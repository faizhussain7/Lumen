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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
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
fun LumenWordmark(style: Int, height: Dp, colour: Color? = null, ink: Color? = null, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val parts = LogoData.words[style.coerceIn(0, LogoData.words.lastIndex)]
    val paths = remember(style) { parts.map { PathParser().parsePathString(it.path).toPath() to it.role } }
    val map = tones(colour ?: scheme.primary, ink ?: scheme.onSurface)
    Canvas(modifier.size(width = height * 3f, height = height)) {
        scale(size.height / 100f, pivot = Offset.Zero) { paths.forEach { (p, r) -> drawPath(p, map[r] ?: Color.White) } }
    }
}

@Composable
fun LumenIcon(style: Int, size: Dp, colour: Color, modifier: Modifier = Modifier) {
    val parts = LogoData.icons[style.coerceIn(0, LogoData.icons.lastIndex)]
    val paths = remember(style) { parts.map { PathParser().parsePathString(it.path).toPath() to it.role } }
    val map = tones(colour, Color.White)
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 100f, pivot = Offset.Zero) { paths.forEach { (p, r) -> drawPath(p, map[r] ?: Color.White) } }
    }
}

object AppIcons {
    const val STYLES = 6
    val COLOURS get() = LogoData.colours.size

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
