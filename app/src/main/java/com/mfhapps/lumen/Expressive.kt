package com.mfhapps.lumen

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

class Springs(bounce: Float, speed: Float) {
    private val bounceK = 0.6f + 0.8f * bounce.coerceIn(0f, 1f)
    private val speedK = 0.55f + 0.9f * speed.coerceIn(0f, 1f)
    private fun damp(token: Float) = (1f - (1f - token) * bounceK).coerceIn(0.35f, 1f)
    val damping: Float = damp(0.8f)
    val stiffness: Float = 380f * speedK
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = damp(0.6f), stiffness = 800f * speedK)
    fun <T> spatial(): FiniteAnimationSpec<T> = spring(dampingRatio = damp(0.8f), stiffness = 380f * speedK)
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = damp(0.8f), stiffness = 200f * speedK)
    fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 3800f * speedK)
    fun <T> effects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f * speedK)
    fun <T> spec(scale: Float = 1f): FiniteAnimationSpec<T> = when {
        scale >= 1.25f -> fastSpatial()
        scale <= 0.8f -> slowSpatial()
        else -> spatial()
    }
    fun <T> close(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1200f * speedK)
}

@androidx.compose.runtime.Composable
fun Modifier.stagger(index: Int): Modifier {
    val springs = LocalSprings.current
    val p = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index.coerceAtMost(10) * 28L))
        p.animateTo(1f, springs.spatial())
    }
    return this.graphicsLayer {
        alpha = p.value.coerceIn(0f, 1f)
        translationY = (1f - p.value) * 24.dp.toPx()
        val sc = 0.94f + 0.06f * p.value
        scaleX = sc
        scaleY = sc
    }
}

val LocalSprings = staticCompositionLocalOf { Springs(0.55f, 0.5f) }

val LocalWallpaperDark = staticCompositionLocalOf { true }

object GestureGate { @Volatile var dragging = false }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LumenTheme(prefs: LumenPrefs, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val base = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    val picked = if (prefs.accentSourceState.value == AccentSource.Custom) accentScheme(base, androidx.compose.ui.graphics.Color(prefs.accentState.value), dark) else base
    val scheme = remember(picked, prefs.vividState.value) { vivid(picked, prefs.vividState.value, dark) }
    val wallpaperDark by rememberWallpaperDark()
    val springs = remember(prefs.bounce, prefs.speed) { Springs(prefs.bounce, prefs.speed) }
    val typography = remember { flexTypography() }
    MaterialExpressiveTheme(colorScheme = scheme, motionScheme = MotionScheme.expressive(), typography = typography) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalSprings provides springs,
            LocalWallpaperDark provides wallpaperDark,
            content = content,
        )
    }
}

@Composable
private fun rememberWallpaperDark(): androidx.compose.runtime.State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(true) }
    DisposableEffect(context) {
        val manager = WallpaperManager.getInstance(context)
        fun read(colors: WallpaperColors?) {
            state.value = colors == null || colors.colorHints and WallpaperColors.HINT_SUPPORTS_DARK_TEXT == 0
        }
        read(runCatching { manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM) }.getOrNull())
        val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which and WallpaperManager.FLAG_SYSTEM != 0) read(colors)
        }
        manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
        onDispose { manager.removeOnColorsChangedListener(listener) }
    }
    return state
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object LumenShapes {
    private val squircle: RoundedPolygon = RoundedPolygon(4, rounding = CornerRounding(0.62f, smoothing = 1f)).let {
        it.transformed { x, y -> androidx.graphics.shapes.TransformResult(x * COS45 - y * COS45, x * COS45 + y * COS45) }
    }.normalized()

    fun polygon(shape: IconShape): RoundedPolygon = when (shape) {
        IconShape.Squircle -> squircle
        IconShape.Circle -> MaterialShapes.Circle
        IconShape.Cookie -> MaterialShapes.Cookie9Sided
        IconShape.Clover -> MaterialShapes.Clover4Leaf
        IconShape.Sunny -> MaterialShapes.Sunny
        IconShape.Gem -> MaterialShapes.Gem
        IconShape.Puffy -> MaterialShapes.Puffy
        IconShape.Arch -> MaterialShapes.Arch
    }

    val pressed: RoundedPolygon get() = MaterialShapes.Cookie12Sided
    val clockBadge: RoundedPolygon get() = MaterialShapes.Cookie12Sided
    val softBurst: RoundedPolygon get() = MaterialShapes.SoftBurst
    val clockCycle: List<RoundedPolygon> get() = listOf(MaterialShapes.Cookie12Sided, MaterialShapes.Clover8Leaf, MaterialShapes.VerySunny)

    private const val COS45 = 0.70710677f
}

class MorphShape(private val morph: Morph, private val progress: Float, private val rotation: Float = 0f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress).asComposePath()
        val matrix = Matrix()
        if (rotation != 0f) {
            matrix.translate(size.width / 2f, size.height / 2f)
            matrix.rotateZ(rotation)
            matrix.translate(-size.width / 2f, -size.height / 2f)
        }
        matrix.scale(size.width, size.height)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

class PolygonShape(private val polygon: RoundedPolygon, private val rotation: Float = 0f) : Shape {
    private var cachedSize = Size.Unspecified
    private var cached: Outline? = null
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val hit = cached
        if (hit != null && size == cachedSize) return hit
        val path = polygon.toPath().asComposePath()
        val matrix = Matrix()
        if (rotation != 0f) {
            matrix.translate(size.width / 2f, size.height / 2f)
            matrix.rotateZ(rotation)
            matrix.translate(-size.width / 2f, -size.height / 2f)
        }
        matrix.scale(size.width, size.height)
        path.transform(matrix)
        return Outline.Generic(path).also { cached = it; cachedSize = size }
    }
}

fun accentScheme(base: androidx.compose.material3.ColorScheme, accent: androidx.compose.ui.graphics.Color, dark: Boolean): androidx.compose.material3.ColorScheme {
    fun mix(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)
    val white = androidx.compose.ui.graphics.Color.White
    val black = androidx.compose.ui.graphics.Color.Black
    val primary = if (dark) mix(accent, white, 0.35f) else mix(accent, black, 0.15f)
    val onPrimary = if (dark) mix(accent, black, 0.8f) else white
    val container = if (dark) mix(accent, black, 0.62f) else mix(accent, white, 0.72f)
    val onContainer = if (dark) mix(accent, white, 0.82f) else mix(accent, black, 0.72f)
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        secondaryContainer = mix(container, base.surface, 0.35f),
        onSecondaryContainer = onContainer,
        tertiaryContainer = mix(container, base.surface, 0.2f),
        onTertiaryContainer = onContainer,
        primaryFixedDim = mix(accent, white, 0.4f),
    )
}

private fun boost(color: androidx.compose.ui.graphics.Color, amount: Float, lightShift: Float = 0f): androidx.compose.ui.graphics.Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgbInt(), hsv)
    hsv[1] = (hsv[1] * (1f + amount * 1.4f) + amount * 0.08f).coerceIn(0f, 1f)
    hsv[2] = (hsv[2] + lightShift).coerceIn(0f, 1f)
    return androidx.compose.ui.graphics.Color(android.graphics.Color.HSVToColor((color.alpha * 255).toInt(), hsv))
}

private fun androidx.compose.ui.graphics.Color.toArgbInt(): Int = android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

fun vivid(s: androidx.compose.material3.ColorScheme, amount: Float, dark: Boolean): androidx.compose.material3.ColorScheme {
    if (amount <= 0.01f) return s
    val tint = { c: androidx.compose.ui.graphics.Color, k: Float -> androidx.compose.ui.graphics.lerp(c, s.primary, k * amount) }
    return s.copy(
        primary = boost(s.primary, amount),
        primaryContainer = boost(s.primaryContainer, amount, if (dark) 0.04f * amount else 0f),
        secondary = boost(s.secondary, amount * 0.8f),
        secondaryContainer = boost(s.secondaryContainer, amount * 0.9f),
        tertiary = boost(s.tertiary, amount),
        tertiaryContainer = boost(s.tertiaryContainer, amount, if (dark) 0.05f * amount else 0f),
        surfaceContainerLowest = tint(s.surfaceContainerLowest, 0.04f),
        surfaceContainerLow = tint(s.surfaceContainerLow, 0.06f),
        surfaceContainer = tint(s.surfaceContainer, 0.08f),
        surfaceContainerHigh = tint(s.surfaceContainerHigh, 0.1f),
        surfaceContainerHighest = tint(s.surfaceContainerHighest, 0.12f),
        primaryFixedDim = boost(s.primaryFixedDim, amount),
    )
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
fun flexTypography(): androidx.compose.material3.Typography {
    val base = androidx.compose.material3.Typography()
    fun androidx.compose.ui.text.TextStyle.flex(weight: Int, width: Int = 100) = copy(fontFamily = Flex.family(weight, width), fontWeight = androidx.compose.ui.text.font.FontWeight(weight))
    return base.copy(
        displayLarge = base.displayLarge.flex(800, 104),
        displayMedium = base.displayMedium.flex(800, 104),
        displaySmall = base.displaySmall.flex(800, 104),
        headlineLarge = base.headlineLarge.flex(750),
        headlineMedium = base.headlineMedium.flex(750),
        headlineSmall = base.headlineSmall.flex(700),
        titleLarge = base.titleLarge.flex(650),
        titleMedium = base.titleMedium.flex(600),
        titleSmall = base.titleSmall.flex(600),
        bodyLarge = base.bodyLarge.flex(420),
        bodyMedium = base.bodyMedium.flex(420),
        bodySmall = base.bodySmall.flex(420),
        labelLarge = base.labelLarge.flex(620),
        labelMedium = base.labelMedium.flex(620),
        labelSmall = base.labelSmall.flex(620),
    )
}
