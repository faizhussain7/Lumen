package com.mfhapps.lumen

import android.graphics.Rect
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.border
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph

@Composable
fun AppIcon(
    app: AppEntry,
    catalog: AppCatalog,
    prefs: LumenPrefs,
    size: Dp,
    showLabel: Boolean,
    labelColor: Color,
    onLaunch: (Rect?) -> Unit,
    onMenu: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val springs = LocalSprings.current
    val haptics = LocalHapticFeedback.current
    val px = with(LocalDensity.current) { size.roundToPx() }
    val tint = scheme.onPrimaryContainer.toArgb()
    val ground = scheme.primaryContainer.toArgb()
    val pack = prefs.iconPack
    val shapeKey = prefs.shape
    val bitmap by produceState(catalog.cachedIcon(app, px, prefs.themed, pack, shapeKey), app.key, px, prefs.themed, tint, ground, pack, shapeKey) {
        value = catalog.icon(app, px, prefs.themed, tint, ground, pack, shapeKey)
    }
    var pressed by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val morph = remember(prefs.shape) { Morph(LumenShapes.polygon(prefs.shape), LumenShapes.pressed) }
    val restShape = remember(prefs.shape) { PolygonShape(LumenShapes.polygon(prefs.shape)) }
    val progress by animateFloatAsState(if (pressed) 1f else 0f, springs.spec(1.4f), label = "morph")
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, springs.spec(1.2f), label = "press")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .semantics { contentDescription = app.label }
            .pointerInput(app.key) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                        onLaunch(bounds)
                    },
                    onLongPress = {
                        pressed = false
                        if (GestureGate.dragging) return@detectTapGestures
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMenu(bounds)
                    },
                )
            },
    ) {
        Box(
            Modifier
                .size(size)
                .onGloballyPositioned { coordinates ->
                    val b = coordinates.boundsInWindow()
                    bounds = Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt())
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    val pressing = progress >= 0.001f
                    shape = if (pressing) MorphShape(morph, progress, progress * 30f) else restShape
                    clip = pressing || bitmap == null
                }
                .then(if (bitmap == null) Modifier.background(scheme.surfaceContainerHigh) else Modifier),
        ) {
            bitmap?.let { Image(it, contentDescription = null, modifier = Modifier.size(size)) }
        }
        val badges by Badges.state.collectAsState()
        if (prefs.badgesState.value && (badges[app.packageName] ?: 0) > 0) {
            Box(
                Modifier
                    .padding(start = size * 0.62f)
                    .graphicsLayer { translationY = -(size.toPx() + 6.dp.toPx()) }
                    .size(12.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(scheme.primary)
                    .border(2.dp, scheme.surface, androidx.compose.foundation.shape.CircleShape),
            )
        }
        if (showLabel) {
            Text(
                app.label,
                style = TextStyle(
                    fontSize = MaterialTheme.typography.labelMedium.fontSize,
                    fontWeight = MaterialTheme.typography.labelMedium.fontWeight,
                    color = labelColor,
                    shadow = if (labelColor.luminanceIsLight()) Shadow(Color.Black.copy(alpha = 0.45f), blurRadius = 8f) else null,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(size + 18.dp).padding(top = 6.dp),
            )
        }
    }
}

fun Color.luminanceIsLight(): Boolean = (0.2126f * red + 0.7152f * green + 0.0722f * blue) > 0.5f
