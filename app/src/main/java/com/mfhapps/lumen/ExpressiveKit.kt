package com.mfhapps.lumen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FabPill(text: String, glyph: Glyph?, modifier: Modifier = Modifier, emphasis: Boolean = false, danger: Boolean = false, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val fill = when {
        danger -> scheme.errorContainer
        emphasis -> scheme.primary
        else -> scheme.primaryContainer
    }
    val ink = when {
        danger -> scheme.onErrorContainer
        emphasis -> scheme.onPrimary
        else -> scheme.onPrimaryContainer
    }
    val source = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val corner by androidx.compose.animation.core.animateDpAsState(if (pressed) 14.dp else 28.dp, LocalSprings.current.spec(1.4f), label = "pill")
    val stretch by androidx.compose.animation.core.animateDpAsState(if (pressed) 32.dp else 24.dp, LocalSprings.current.spec(1.4f), label = "stretch")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(56.dp).clip(RoundedCornerShape(corner)).background(fill).clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), onClick = onClick).padding(start = 20.dp, end = stretch),
    ) {
        if (glyph != null) {
            GlyphIcon(glyph, ink, 22.dp)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = ink, maxLines = 1)
    }
}

@Composable
fun FabClose(onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(56.dp).clip(CircleShape).background(scheme.primary).clickable(onClick = onClick),
    ) {
        androidx.compose.material3.Icon(Icons.Rounded.Close, contentDescription = null, tint = scheme.onPrimary)
    }
}

class ToolbarAction(val glyph: Glyph, val label: String, val danger: Boolean = false, val active: Boolean = false, val onClick: () -> Unit)

@Composable
fun FloatingToolbar(actions: List<ToolbarAction>, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.clip(CircleShape).background(scheme.surfaceContainerHighest).padding(6.dp),
    ) {
        actions.forEach { action ->
            val fill = when {
                action.active -> scheme.primary
                action.danger -> scheme.errorContainer
                else -> Color.Transparent
            }
            val ink = when {
                action.active -> scheme.onPrimary
                action.danger -> scheme.onErrorContainer
                else -> scheme.onSurface
            }
            val source = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val w by androidx.compose.animation.core.animateDpAsState(if (pressed) 72.dp else if (action.active) 64.dp else 48.dp, LocalSprings.current.spec(1.4f), label = "tool")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = w, height = 48.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), onClickLabel = action.label, onClick = action.onClick),
            ) { GlyphIcon(action.glyph, ink, 22.dp) }
        }
    }
}

@Composable
fun <T> ConnectedGroup(options: List<T>, selected: T, label: @Composable (T) -> String, modifier: Modifier = Modifier, onPlaced: ((Int, Int, Int) -> Unit)? = null, onSelect: (T) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = modifier) {
        options.forEachIndexed { index, option ->
            val on = option == selected
            val source = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val outer = 22.dp
            val inner by androidx.compose.animation.core.animateDpAsState(if (on || pressed) 22.dp else 6.dp, LocalSprings.current.spec(1.3f), label = "inner")
            val pad by androidx.compose.animation.core.animateDpAsState(if (pressed) 24.dp else 16.dp, LocalSprings.current.spec(1.3f), label = "pad")
            val shape = RoundedCornerShape(
                topStart = if (index == 0) outer else inner,
                bottomStart = if (index == 0) outer else inner,
                topEnd = if (index == options.lastIndex) outer else inner,
                bottomEnd = if (index == options.lastIndex) outer else inner,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .then(if (onPlaced != null) Modifier.onGloballyPositioned { onPlaced(index, it.positionInParent().x.toInt(), it.size.width) } else Modifier)
                    .height(40.dp)
                    .clip(shape)
                    .background(if (on) scheme.primary else scheme.surfaceContainerHighest)
                    .clickable(interactionSource = source, indication = androidx.compose.material3.ripple()) { onSelect(option) }
                    .padding(horizontal = pad),
            ) {
                if (on) {
                    androidx.compose.material3.Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.onPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label(option), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = if (on) scheme.onPrimary else scheme.onSurface)
            }
        }
    }
}

@Composable
fun DisplayTitle(text: String, color: Color = MaterialTheme.colorScheme.onSurface, size: Float = 44f, modifier: Modifier = Modifier) {
    Text(text, style = flexStyle(size, 900, 110, color).copy(letterSpacing = (-1).toSp()), modifier = modifier)
}

private fun Int.toSp() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

@Composable
fun BloomBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier.background(
            Brush.radialGradient(
                listOf(scheme.tertiary.copy(alpha = 0.28f), scheme.primary.copy(alpha = 0.14f), Color.Transparent),
                radius = 1400f,
            ),
        ),
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteryWave(level: Int, charging: Boolean, ink: Color, size: Dp = 18.dp, showPercent: Boolean = true, percentInside: Boolean = false) {
    val good = Color(0xFF34C759)
    val low = Color(0xFFFF453A)
    val tone = when {
        charging -> good
        level <= 15 -> low
        else -> ink
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (showPercent && !percentInside) {
            Text("$level%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = ink)
            Spacer(Modifier.width(5.dp))
        }
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
            androidx.compose.material3.CircularWavyProgressIndicator(
                progress = { level.coerceIn(0, 100) / 100f },
                color = tone,
                trackColor = ink.copy(alpha = 0.22f),
                modifier = Modifier.size(size),
            )
            when {
                percentInside -> Text("$level", style = flexStyle(size.value * 0.3f, 800, 90, ink))
                charging -> GlyphIcon(Glyph.Bolt, tone, size * 0.55f)
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteryWaveBar(level: Int, charging: Boolean, ink: Color, width: Dp = 110.dp) {
    val tone = when {
        charging -> Color(0xFF34C759)
        level <= 15 -> Color(0xFFFF453A)
        else -> ink
    }
    androidx.compose.material3.LinearWavyProgressIndicator(
        progress = { level.coerceIn(0, 100) / 100f },
        color = tone,
        trackColor = ink.copy(alpha = 0.22f),
        modifier = Modifier.width(width).height(10.dp),
    )
}

@Composable
fun ExtendedFab(text: String, glyph: Glyph, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val source = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val corner by androidx.compose.animation.core.animateDpAsState(if (pressed) 18.dp else 28.dp, LocalSprings.current.spec(1.4f), label = "fab")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(72.dp)
            .graphicsLayer { shadowElevation = 12.dp.toPx(); shape = RoundedCornerShape(corner); clip = true }
            .background(scheme.primaryContainer)
            .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .padding(start = 24.dp, end = 28.dp),
    ) {
        GlyphIcon(glyph, scheme.onPrimaryContainer, 26.dp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = flexStyle(20f, 750, 100, scheme.onPrimaryContainer))
    }
}
