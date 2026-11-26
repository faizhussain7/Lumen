package com.mfhapps.lumen

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.luminance
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
    fun <T> slowEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 800f * speedK)
    fun <T> size(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 420f * speedK)
    fun <T> close(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1200f * speedK)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private class SpringMotion(private val s: Springs) : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = s.spatial()
    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = s.fastSpatial()
    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = s.slowSpatial()
    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = s.effects()
    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = s.fastEffects()
    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = s.slowEffects()
}

private class FractionCorner(private val fraction: Float) : androidx.compose.foundation.shape.CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float = shapeSize.minDimension * fraction
}

@Composable
fun Modifier.listTap(
    rest: androidx.compose.ui.unit.Dp = 6.dp,
    label: String? = null,
    interaction: androidx.compose.foundation.interaction.MutableInteractionSource? = null,
    onClick: () -> Unit,
): Modifier {
    val springs = LocalSprings.current
    val own = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val source = interaction ?: own
    val pressed by source.collectIsPressedAsState()
    val corner by androidx.compose.animation.core.animateDpAsState(if (pressed) 24.dp else rest, springs.fastSpatial(), label = "row")
    val live = androidx.compose.foundation.shape.RoundedCornerShape(corner.coerceAtLeast(0.dp))
    return this.graphicsLayer { shape = live; clip = true }
        .clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), onClickLabel = label, onClick = onClick)
}

@Composable
fun Modifier.tap(
    shape: Shape = androidx.compose.foundation.shape.CircleShape,
    label: String? = null,
    enabled: Boolean = true,
    depth: Float = 0.06f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val springs = LocalSprings.current
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val p by androidx.compose.animation.core.animateFloatAsState(if (pressed) 1f else 0f, springs.fastSpatial(), label = "tap")
    val round = shape == androidx.compose.foundation.shape.CircleShape
    val live = if (round) androidx.compose.foundation.shape.RoundedCornerShape(FractionCorner(0.5f - 0.18f * p.coerceIn(0f, 1.2f))) else shape
    val base = this.graphicsLayer {
        val k = 1f - depth * p
        scaleX = k
        scaleY = k
        this.shape = live
        clip = true
    }
    return if (onLongClick != null) {
        base.combinedClickable(interactionSource = source, indication = androidx.compose.material3.ripple(), enabled = enabled, onClickLabel = label, onLongClick = onLongClick, onClick = onClick)
    } else {
        base.clickable(interactionSource = source, indication = androidx.compose.material3.ripple(), enabled = enabled, onClickLabel = label, onClick = onClick)
    }
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
    val scheme = remember(picked, prefs.vividState.value) { legible(vivid(picked, prefs.vividState.value, dark)) }
    val wallpaperDark by rememberWallpaperDark()
    val springs = remember(prefs.bounce, prefs.speed) { Springs(prefs.bounce, prefs.speed) }
    val typography = remember { flexTypography() }
    MaterialExpressiveTheme(colorScheme = scheme, motionScheme = remember(springs) { SpringMotion(springs) }, typography = typography) {
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
    val primary = tone(accent, if (dark) 80f else 40f)
    val onPrimary = tone(accent, if (dark) 20f else 100f)
    val container = tone(accent, if (dark) 30f else 90f)
    val onContainer = tone(accent, if (dark) 90f else 10f)
    val soft = tone(accent, if (dark) 30f else 90f, 0.45f)
    val onSoft = tone(accent, if (dark) 90f else 10f, 0.45f)
    return base.copy(
        primary = primary,
        onPrimary = readable(onPrimary, primary),
        primaryContainer = container,
        onPrimaryContainer = readable(onContainer, container),
        secondary = tone(accent, if (dark) 80f else 40f, 0.45f),
        secondaryContainer = soft,
        onSecondaryContainer = readable(onSoft, soft),
        tertiaryContainer = tone(accent, if (dark) 34f else 88f, 0.7f),
        onTertiaryContainer = readable(onContainer, tone(accent, if (dark) 34f else 88f, 0.7f)),
        primaryFixedDim = tone(accent, 80f),
    )
}

private fun oklabToLinear(l: Float, a: Float, b: Float): FloatArray {
    val l1 = l + 0.39633778f * a + 0.21580376f * b
    val m1 = l - 0.10556135f * a - 0.06385417f * b
    val s1 = l - 0.08948418f * a - 1.2914855f * b
    val ll = l1 * l1 * l1
    val mm = m1 * m1 * m1
    val ss = s1 * s1 * s1
    return floatArrayOf(
        4.0767417f * ll - 3.3077116f * mm + 0.23096993f * ss,
        -1.268438f * ll + 2.6097574f * mm - 0.34131938f * ss,
        -0.0041960863f * ll - 0.7034186f * mm + 1.7076147f * ss,
    )
}

fun tone(color: androidx.compose.ui.graphics.Color, lstar: Float, chroma: Float = 1f): androidx.compose.ui.graphics.Color {
    val c = color.convert(androidx.compose.ui.graphics.colorspace.ColorSpaces.Oklab)
    val target = if (lstar <= 8f) lstar / 903.3f else ((lstar + 16f) / 116f).let { it * it * it }
    fun solve(k: Float): FloatArray {
        var lo = 0f
        var hi = 1f
        var rgb = oklabToLinear(0.5f, c.green * k, c.blue * k)
        repeat(24) {
            val mid = (lo + hi) / 2f
            rgb = oklabToLinear(mid, c.green * k, c.blue * k)
            if (0.2126f * rgb[0] + 0.7152f * rgb[1] + 0.0722f * rgb[2] < target) lo = mid else hi = mid
        }
        return rgb
    }
    var k = chroma
    var rgb = solve(k)
    while (k > 0.001f && rgb.any { it < -0.0005f || it > 1.0005f }) {
        k *= 0.92f
        rgb = solve(k)
    }
    if (rgb.any { it < -0.0005f || it > 1.0005f }) rgb = solve(0f)
    return androidx.compose.ui.graphics.Color(rgb[0].coerceIn(0f, 1f), rgb[1].coerceIn(0f, 1f), rgb[2].coerceIn(0f, 1f), 1f, androidx.compose.ui.graphics.colorspace.ColorSpaces.LinearSrgb)
        .convert(androidx.compose.ui.graphics.colorspace.ColorSpaces.Srgb)
}

fun legible(s: androidx.compose.material3.ColorScheme): androidx.compose.material3.ColorScheme = s.copy(
    onPrimary = readable(s.onPrimary, s.primary),
    onPrimaryContainer = readable(s.onPrimaryContainer, s.primaryContainer),
    onSecondary = readable(s.onSecondary, s.secondary),
    onSecondaryContainer = readable(s.onSecondaryContainer, s.secondaryContainer),
    onTertiary = readable(s.onTertiary, s.tertiary),
    onTertiaryContainer = readable(s.onTertiaryContainer, s.tertiaryContainer),
    onSurfaceVariant = readable(s.onSurfaceVariant, s.surfaceContainerHighest),
    onError = readable(s.onError, s.error),
    onErrorContainer = readable(s.onErrorContainer, s.errorContainer),
)

fun contrast(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

fun readable(ink: androidx.compose.ui.graphics.Color, ground: androidx.compose.ui.graphics.Color, min: Float = 4.5f): androidx.compose.ui.graphics.Color {
    if (contrast(ink, ground) >= min) return ink
    val white = androidx.compose.ui.graphics.Color.White
    val black = androidx.compose.ui.graphics.Color.Black
    return if (contrast(white, ground) >= contrast(black, ground)) white else black
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

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
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
        displayLargeEmphasized = base.displayLargeEmphasized.flex(900, 108),
        displayMediumEmphasized = base.displayMediumEmphasized.flex(900, 108),
        displaySmallEmphasized = base.displaySmallEmphasized.flex(900, 108),
        headlineLargeEmphasized = base.headlineLargeEmphasized.flex(850, 104),
        headlineMediumEmphasized = base.headlineMediumEmphasized.flex(850, 104),
        headlineSmallEmphasized = base.headlineSmallEmphasized.flex(820, 104),
        titleLargeEmphasized = base.titleLargeEmphasized.flex(800, 102),
        titleMediumEmphasized = base.titleMediumEmphasized.flex(760),
        titleSmallEmphasized = base.titleSmallEmphasized.flex(750),
        bodyLargeEmphasized = base.bodyLargeEmphasized.flex(560),
        bodyMediumEmphasized = base.bodyMediumEmphasized.flex(560),
        bodySmallEmphasized = base.bodySmallEmphasized.flex(560),
        labelLargeEmphasized = base.labelLargeEmphasized.flex(760),
        labelMediumEmphasized = base.labelMediumEmphasized.flex(760),
        labelSmallEmphasized = base.labelSmallEmphasized.flex(760),
    )
}
