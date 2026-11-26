@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mfhapps.lumen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.graphics.shapes.Morph
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.launch

private val HERO_SHAPES get() = listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Clover4Leaf, MaterialShapes.SoftBurst, MaterialShapes.Cookie12Sided)
private val HERO_GLYPHS = listOf(Glyph.Spark, Glyph.Home, Glyph.Bell, Glyph.Wallpaper)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Onboarding(prefs: LumenPrefs, isDefaultHome: () -> Boolean, onDone: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val springs = LocalSprings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(0) }
    var fromPage by remember { mutableIntStateOf(0) }
    val morph = remember { Animatable(1f) }
    val leave = remember { Animatable(0f) }
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, springs.slowSpatial()) }

    var isDefault by remember { mutableStateOf(isDefaultHome()) }
    var notices by remember { mutableStateOf(Badges.granted(context)) }
    var location by remember { mutableStateOf(Weather.permitted(context)) }
    var contacts by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) }
    LifecycleResumeEffect(Unit) {
        isDefault = isDefaultHome()
        notices = Badges.granted(context)
        location = Weather.permitted(context)
        onPauseOrDispose { }
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        location = granted
        if (granted) prefs.set(prefs.weatherState, true)
    }
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> contacts = granted }

    fun go(to: Int) {
        if (to == page) return
        fromPage = page
        page = to
        scope.launch { morph.snapTo(0f); morph.animateTo(1f, springs.spatial()) }
    }
    fun finish() {
        prefs.set(prefs.onboardedState, true)
        scope.launch { leave.animateTo(1f, springs.close()); onDone() }
    }
    PredictiveBackHandler(enabled = page > 0) { progress ->
        try { progress.collect { } ; go(page - 1) } catch (_: Exception) { }
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = (1f - leave.value) * enter.value.coerceIn(0f, 1f)
                val s = 1f - 0.06f * leave.value + 0.04f * (1f - enter.value)
                scaleX = s; scaleY = s
            }
            .background(scheme.surface),
        contentAlignment = Alignment.TopCenter,
    ) {
        BloomBackground(Modifier.fillMaxSize())
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 560.dp).fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                LumenWordmark(30.dp)
                Spacer(Modifier.weight(1f))
                if (page < 3) Text(
                    stringResource(R.string.onb_skip),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.tap(CircleShape, depth = 0.08f) { finish() }.padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(28.dp))
            val heroMorph = remember(fromPage, page) { Morph(HERO_SHAPES[fromPage], HERO_SHAPES[page]) }
            val bob by animateFloatAsState(if (page % 2 == 0) 1f else 0f, springs.slowSpatial(), label = "bob")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(176.dp)
                    .graphicsLayer {
                        val k = enter.value
                        scaleX = 0.6f + 0.4f * k; scaleY = 0.6f + 0.4f * k
                        rotationZ = (morph.value - 1f) * 40f + bob * 6f
                        shape = MorphShape(heroMorph, morph.value.coerceIn(0f, 1f))
                        clip = true
                    }
                    .background(scheme.primaryContainer),
            ) {
                Box(Modifier.graphicsLayer { val k = morph.value; scaleX = 0.5f + 0.5f * k; scaleY = 0.5f + 0.5f * k; alpha = k.coerceIn(0f, 1f) }) {
                    GlyphIcon(HERO_GLYPHS[page], scheme.onPrimaryContainer, 72.dp)
                }
            }
            Spacer(Modifier.height(28.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            ) {
                androidx.compose.runtime.key(page) {
                    val title = stringResource(listOf(R.string.onb_welcome_title, R.string.onb_home_title, R.string.onb_perm_title, R.string.onb_yours_title)[page])
                    val body = stringResource(listOf(R.string.onb_welcome_body, R.string.onb_home_body, R.string.onb_perm_body, R.string.onb_yours_body)[page])
                    DisplayTitle(title, size = 38f, modifier = Modifier.stagger(0))
                    Spacer(Modifier.height(10.dp))
                    Text(body, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.stagger(1))
                    Spacer(Modifier.height(24.dp))
                    when (page) {
                        1 -> StepCard(
                            Glyph.Home, stringResource(if (isDefault) R.string.onb_home_done else R.string.onb_home_action), null, isDefault, Modifier.stagger(2),
                        ) { runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                        2 -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StepCard(Glyph.Bell, stringResource(R.string.onb_perm_notices), stringResource(R.string.onb_perm_notices_why), notices, Modifier.stagger(2)) { Badges.requestAccess(context) }
                            StepCard(Glyph.Sun, stringResource(R.string.onb_perm_weather), stringResource(R.string.onb_perm_weather_why), location, Modifier.stagger(3)) { askLocation.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
                            StepCard(Glyph.Person, stringResource(R.string.onb_perm_contacts), stringResource(R.string.onb_perm_contacts_why), contacts, Modifier.stagger(4)) { askContacts.launch(Manifest.permission.READ_CONTACTS) }
                        }
                        3 -> Column(Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.settings_clock), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, modifier = Modifier.stagger(2).padding(bottom = 8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.stagger(3)) {
                                ClockStyle.entries.take(4).forEach { style ->
                                    androidx.compose.material3.FilterChip(selected = prefs.clock == style, onClick = { prefs.updateClock(style) }, label = { Text(clockLabel(style)) }, shape = CircleShape)
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(stringResource(R.string.section_colours), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, modifier = Modifier.stagger(4).padding(bottom = 8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.stagger(5)) {
                                val wall = prefs.accentSourceState.value == AccentSource.Wallpaper
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(48.dp).tap(if (wall) PolygonShape(LumenShapes.clockBadge) else CircleShape, depth = 0.12f) { prefs.set(prefs.accentSourceState, AccentSource.Wallpaper) }
                                        .background(androidx.compose.ui.graphics.Brush.sweepGradient(listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary))),
                                ) { GlyphIcon(Glyph.Wallpaper, scheme.onPrimary, 22.dp) }
                                AccentPalette.take(7).forEach { colour ->
                                    val on = !wall && prefs.accentState.value == colour
                                    Box(Modifier.size(48.dp).tap(if (on) PolygonShape(LumenShapes.clockBadge) else CircleShape, depth = 0.12f) {
                                        prefs.set(prefs.accentSourceState, AccentSource.Custom); prefs.set(prefs.accentState, colour)
                                    }.background(Color(colour)))
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            Text(stringResource(R.string.settings_icons), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, modifier = Modifier.stagger(6).padding(bottom = 8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.stagger(7)) {
                                IconShape.entries.take(5).forEach { shape ->
                                    val on = prefs.shape == shape
                                    val tone by androidx.compose.animation.animateColorAsState(if (on) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.5f), springs.effects(), label = "shape")
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(56.dp).tap(RoundedCornerShape(18.dp), depth = 0.1f) { prefs.updateShape(shape) }
                                            .background(if (on) scheme.primary.copy(alpha = 0.14f) else Color.Transparent),
                                    ) { Box(Modifier.size(34.dp).graphicsLayer { this.shape = PolygonShape(LumenShapes.polygon(shape)); clip = true }.background(tone)) }
                                }
                            }
                        }
                        else -> Unit
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(4) { i ->
                        val w by animateDpAsState(if (i == page) 26.dp else 8.dp, springs.fastSpatial(), label = "dot")
                        Box(Modifier.width(w).height(8.dp).clip(CircleShape).background(if (i == page) scheme.primary else scheme.outlineVariant))
                    }
                }
                Spacer(Modifier.weight(1f))
                if (page > 0) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(56.dp).tap(CircleShape, label = stringResource(R.string.onb_back), depth = 0.1f) { go(page - 1) }.background(scheme.surfaceContainerHighest),
                    ) { GlyphIcon(Glyph.Back, scheme.onSurface, 24.dp) }
                    Spacer(Modifier.width(10.dp))
                }
                FabPill(
                    stringResource(when (page) { 0 -> R.string.onb_start; 3 -> R.string.onb_finish; else -> R.string.onb_next }),
                    if (page == 3) Glyph.Check else Glyph.Next,
                    emphasis = true,
                ) { if (page == 3) finish() else go(page + 1) }
            }
        }
    }
}

@Composable
private fun StepCard(glyph: Glyph, title: String, why: String?, done: Boolean, modifier: Modifier = Modifier, onAllow: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val springs = LocalSprings.current
    val tone by androidx.compose.animation.animateColorAsState(if (done) scheme.primaryContainer else scheme.surfaceContainerHigh, springs.effects(), label = "step")
    val badge = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
    val k by animateFloatAsState(if (done) 1f else 0f, springs.fastSpatial(), label = "badge")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .listTap(rest = 28.dp) { if (!done) onAllow() }
            .background(tone)
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(48.dp).graphicsLayer { shape = MorphShape(badge, k, k * 30f); clip = true }.background(if (done) scheme.primary else scheme.secondaryContainer),
        ) { GlyphIcon(if (done) Glyph.Check else glyph, if (done) scheme.onPrimary else scheme.onSecondaryContainer, 24.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = scheme.onSurface)
            if (why != null) Text(why, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        if (why != null) {
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(if (done) R.string.onb_on else R.string.onb_allow),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (done) scheme.primary else scheme.onPrimary,
                modifier = Modifier.clip(CircleShape).background(if (done) Color.Transparent else scheme.primary).padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}
