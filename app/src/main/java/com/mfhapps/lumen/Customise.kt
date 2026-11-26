@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mfhapps.lumen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlin.math.roundToInt

val AccentPalette = listOf(
    0xFF4F8DFF, 0xFF00B8D9, 0xFF00C2A8, 0xFF3DDC84, 0xFFB5D334, 0xFFFFC233,
    0xFFFF8A3D, 0xFFFF5A5F, 0xFFFF4FA3, 0xFFC064FF, 0xFF7C6CFF, 0xFF9AA0A6,
).map { it.toInt() }

@Composable
fun CustomisePage(catalog: AppCatalog, prefs: LumenPrefs, progress: () -> Float, isDefaultHome: () -> Boolean, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var isDefault by remember { mutableStateOf(isDefaultHome()) }
    var badgesGranted by remember { mutableStateOf(Badges.granted(context)) }
    LifecycleResumeEffect(Unit) {
        isDefault = isDefaultHome()
        badgesGranted = Badges.granted(context)
        onPauseOrDispose { }
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress().coerceIn(0f, 1f)
                alpha = (p * 1.6f).coerceIn(0f, 1f)
                translationY = (1f - p) * 120.dp.toPx()
                val s = 0.94f + 0.06f * p
                scaleX = s
                scaleY = s
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        BloomBackground(Modifier.fillMaxSize())
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        val jump = androidx.compose.runtime.rememberCoroutineScope()
        val sections = listOf(
            R.string.settings_clock, R.string.section_home, R.string.section_dock, R.string.settings_icons, R.string.section_drawer,
            R.string.section_colours, R.string.section_gestures, R.string.settings_motion, R.string.settings_effects,
            R.string.section_status, R.string.section_lock, R.string.section_app_icon, R.string.section_backup,
        )
        val base = 3 + if (isDefault) 0 else 1
        val current by androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { (listState.firstVisibleItemIndex - base).coerceIn(0, sections.lastIndex) } }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize()
                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility)
                .navigationBarsPadding(),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 8.dp)) {
                    Column(Modifier.weight(1f)) {
                        LumenWordmark(prefs.logoStyleState.value, 46.dp)
                        DisplayTitle(stringResource(R.string.customise))
                    }
                }
            }
            if (!isDefault) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(scheme.primaryContainer).padding(18.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.default_banner_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = scheme.onPrimaryContainer)
                            Text(stringResource(R.string.default_banner_caption), style = MaterialTheme.typography.bodySmall, color = scheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = { runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }, shape = CircleShape) { Text(stringResource(R.string.set_default)) }
                    }
                }
            }
            item { Preview(catalog, prefs) }
            stickyHeader {
                val tabScroll = androidx.compose.foundation.rememberScrollState()
                val tabSpots = remember { HashMap<Int, Pair<Int, Int>>() }
                val density = androidx.compose.ui.platform.LocalDensity.current
                LaunchedEffect(current) {
                    tabSpots[current]?.let { (x, w) ->
                        val margin = with(density) { 48.dp.roundToPx() }
                        val viewport = tabScroll.viewportSize
                        val target = (x + w / 2 - viewport / 2).coerceIn(0, tabScroll.maxValue)
                        if (x < tabScroll.value + margin || x + w > tabScroll.value + viewport - margin) tabScroll.animateScrollTo(target)
                    }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceContainerHigh.copy(alpha = 0.96f))
                        .padding(6.dp)
                        .horizontalScroll(tabScroll),
                ) {
                    ConnectedGroup(sections.indices.toList(), current, { stringResource(sections[it]) }, onPlaced = { i, x, w -> tabSpots[i] = x to w }) { index ->
                        jump.launch { listState.animateScrollToItem(base + index) }
                    }
                }
            }
            item {
                Card(stringResource(R.string.settings_clock)) {
                    ClockStyle.entries.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            row.forEach { style -> ClockTile(style, prefs.clock == style, prefs, Modifier.weight(1f)) { prefs.updateClock(style) } }
                        }
                    }
                    SliderRow(stringResource(R.string.opt_size), prefs.clockScaleState.value) { prefs.set(prefs.clockScaleState, it) }
                    SliderRow(stringResource(R.string.opt_weight), prefs.clockWeightState.value) { prefs.set(prefs.clockWeightState, it) }
                    SliderRow(stringResource(R.string.opt_width), prefs.clockWidthState.value) { prefs.set(prefs.clockWidthState, it) }
                    Label(stringResource(R.string.opt_colour))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        (listOf(0, 0xFFFFFFFF.toInt()) + AccentPalette).forEach { colour ->
                            val selected = prefs.clockColourState.value == colour
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(if (selected) PolygonShape(LumenShapes.clockBadge) else CircleShape)
                                    .background(if (colour == 0) Brush.sweepGradient(listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary)) else Brush.linearGradient(listOf(Color(colour), Color(colour))))
                                    .border(1.dp, scheme.outlineVariant, if (selected) PolygonShape(LumenShapes.clockBadge) else CircleShape)
                                    .clickable { prefs.set(prefs.clockColourState, colour) },
                            ) {}
                        }
                    }
                    Toggle(stringResource(R.string.opt_show_date), null, prefs.showDateState, prefs)
                    val askLocation = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
                        prefs.set(prefs.weatherState, granted)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.5.dp).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(scheme.surfaceContainerHigh).clickable {
                        if (prefs.weatherState.value) prefs.set(prefs.weatherState, false)
                        else if (Weather.permitted(context)) prefs.set(prefs.weatherState, true)
                        else askLocation.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    }.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(scheme.tertiaryContainer), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Sun, scheme.onTertiaryContainer, 22.dp) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.opt_weather), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                            Text(stringResource(R.string.opt_weather_caption), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = prefs.weatherState.value, onCheckedChange = null)
                    }
                    if (prefs.weatherState.value) WeatherSetup()
                    Toggle(stringResource(R.string.opt_show_chips), null, prefs.showChipsState, prefs)
                }
            }
            item {
                Card(stringResource(R.string.section_home)) {
                    Label(stringResource(R.string.opt_home_columns))
                    Choices((3..7).toList(), prefs.homeColumnsState.value, { "$it" }) { prefs.set(prefs.homeColumnsState, it) }
                    Label(stringResource(R.string.opt_pages))
                    Choices((1..5).toList(), prefs.pagesState.value, { "$it" }) { prefs.set(prefs.pagesState, it) }
                    Label(stringResource(R.string.opt_home_rows))
                    Choices((3..8).toList(), prefs.homeRowsState.value, { "$it" }) { prefs.set(prefs.homeRowsState, it) }
                    SliderRow(stringResource(R.string.opt_icon_size), prefs.iconScaleState.value) { prefs.set(prefs.iconScaleState, it) }
                    Toggle(stringResource(R.string.opt_home_labels), null, prefs.homeLabelsState, prefs)
                    Toggle(stringResource(R.string.opt_badges), if (badgesGranted) null else stringResource(R.string.opt_badges_caption), prefs.badgesState, prefs)
                    if (!badgesGranted) FilledTonalButton(onClick = { Badges.requestAccess(context) }, shape = CircleShape) { Text(stringResource(R.string.opt_grant)) }
                }
            }
            item {
                Card(stringResource(R.string.section_dock)) {
                    Label(stringResource(R.string.opt_dock_style))
                    Choices(DockStyle.entries, prefs.dockStyleState.value, { stringResource(when (it) { DockStyle.Glass -> R.string.dock_glass; DockStyle.Solid -> R.string.dock_solid; DockStyle.Clear -> R.string.dock_clear; DockStyle.Hidden -> R.string.dock_hidden }) }) { prefs.set(prefs.dockStyleState, it) }
                    Toggle(stringResource(R.string.opt_dock_search), null, prefs.dockSearchState, prefs)
                }
            }
            item {
                Card(stringResource(R.string.settings_icons)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(IconShape.entries) { shape ->
                            val selected = prefs.shape == shape
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                                    .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(20.dp))
                                    .clickable { prefs.updateShape(shape) },
                            ) {
                                Box(Modifier.size(40.dp).clip(PolygonShape(LumenShapes.polygon(shape))).background(if (selected) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.7f)))
                            }
                        }
                    }
                    Toggle(stringResource(R.string.settings_themed_icons), stringResource(R.string.settings_themed_icons_caption), prefs.themedState, prefs)
                    val packs = remember { IconPacks.installed(context) }
                    Label(stringResource(R.string.opt_icon_pack))
                    Choices(listOf(IconPackInfo("", context.getString(R.string.opt_icon_pack_system))) + packs, packs.firstOrNull { it.packageName == prefs.iconPack } ?: IconPackInfo("", ""), { it.label }) { prefs.updateIconPack(it.packageName) }
                    if (packs.isEmpty()) Text(stringResource(R.string.opt_icon_pack_none), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
            item {
                Card(stringResource(R.string.section_drawer)) {
                    Label(stringResource(R.string.settings_columns))
                    Choices(listOf(0, 4, 5, 6, 7), prefs.columns, { if (it == 0) stringResource(R.string.opt_auto) else "$it" }) { prefs.updateColumns(it) }
                    Label(stringResource(R.string.opt_sort))
                    Choices(DrawerSort.entries, prefs.sortState.value, { stringResource(when (it) { DrawerSort.Alphabetical -> R.string.sort_az; DrawerSort.MostUsed -> R.string.sort_used; DrawerSort.Newest -> R.string.sort_new }) }) { prefs.set(prefs.sortState, it) }
                    Toggle(stringResource(R.string.opt_categories), null, prefs.categoriesState, prefs)
                    Toggle(stringResource(R.string.opt_favourites), null, prefs.favouritesState, prefs)
                    Toggle(stringResource(R.string.opt_drawer_labels), null, prefs.drawerLabelsState, prefs)
                    Label(stringResource(R.string.opt_hidden))
                    val hidden = catalog.apps.filter { it.key in prefs.hidden }
                    if (hidden.isEmpty()) {
                        Text(stringResource(R.string.opt_hidden_none), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            hidden.forEach { app -> FilterChip(selected = true, onClick = { prefs.toggleHidden(app.key) }, label = { Text(app.label) }, shape = CircleShape) }
                        }
                    }
                }
            }
            item {
                Card(stringResource(R.string.section_colours)) {
                    SliderRow(stringResource(R.string.opt_vivid), prefs.vividState.value) { prefs.set(prefs.vividState, it) }
                    Label(stringResource(R.string.opt_accent_source))
                    Choices(AccentSource.entries, prefs.accentSourceState.value, { stringResource(if (it == AccentSource.Wallpaper) R.string.opt_wallpaper else R.string.opt_custom) }) { prefs.set(prefs.accentSourceState, it) }
                    if (prefs.accentSourceState.value == AccentSource.Custom) {
                        Spacer(Modifier.height(10.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AccentPalette.forEach { colour ->
                                val selected = prefs.accentState.value == colour
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(if (selected) PolygonShape(LumenShapes.clockBadge) else CircleShape)
                                        .background(Color(colour))
                                        .clickable { prefs.set(prefs.accentState, colour) },
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(stringResource(R.string.section_gestures)) {
                    GestureRow(stringResource(R.string.opt_swipe_up), prefs.swipeUpState, prefs)
                    GestureRow(stringResource(R.string.opt_swipe_down), prefs.swipeDownState, prefs)
                    GestureRow(stringResource(R.string.opt_double_tap), prefs.doubleTapState, prefs)
                    GestureRow(stringResource(R.string.opt_pinch), prefs.pinchState, prefs)
                }
            }
            item {
                Card(stringResource(R.string.settings_motion)) {
                    MotionDemo(prefs)
                    SliderRow(stringResource(R.string.settings_bounce), prefs.bounce) { prefs.updateBounce(it) }
                    SliderRow(stringResource(R.string.settings_speed), prefs.speed) { prefs.updateSpeed(it) }
                    SliderRow(stringResource(R.string.settings_blur), prefs.blur) { prefs.updateBlur(it) }
                }
            }
            item {
                Card(stringResource(R.string.settings_effects)) {
                    Toggle(stringResource(R.string.settings_glow), stringResource(R.string.settings_glow_caption), prefs.glowState, prefs)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.settings_wallpaper)) }
                }
            }
            item {
                Card(stringResource(R.string.section_status)) {
                    Toggle(stringResource(R.string.settings_own_top), stringResource(R.string.settings_own_top_caption), prefs.ownTopState, prefs)
                    Toggle(stringResource(R.string.opt_status_bar), null, prefs.statusBarState, prefs)
                    Toggle(stringResource(R.string.opt_status_percent), null, prefs.statusPercentState, prefs)
                }
            }
            item {
                Card(stringResource(R.string.section_lock)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable {
                        val on = !prefs.lockCover
                        prefs.updateLockCover(on)
                        if (on && !LumenAccess.ready) {
                            Toast.makeText(context, R.string.access_needed, Toast.LENGTH_LONG).show()
                            LumenAccess.openSettings(context)
                        }
                    }.padding(vertical = 10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_lock), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                            Text(stringResource(R.string.settings_lock_caption), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = prefs.lockCover, onCheckedChange = null)
                    }
                    Label(stringResource(R.string.opt_lock_clock))
                    Choices(ClockStyle.entries, prefs.lockClockState.value, { clockLabel(it) }) { prefs.set(prefs.lockClockState, it) }
                    Toggle(stringResource(R.string.opt_lock_centre), null, prefs.lockCentreState, prefs)
                    Label(stringResource(R.string.opt_lock_notices))
                    ConnectedGroup(LockNotices.entries, prefs.lockNoticesState.value, { stringResource(when (it) { LockNotices.Icons -> R.string.notices_icons; LockNotices.Cards -> R.string.notices_cards; LockNotices.Hidden -> R.string.notices_hidden }) }) { prefs.set(prefs.lockNoticesState, it) }
                    Toggle(stringResource(R.string.opt_lock_media), if (badgesGranted) null else stringResource(R.string.opt_badges_caption), prefs.lockMediaState, prefs)
                    Toggle(stringResource(R.string.opt_lock_widgets), null, prefs.lockWidgetsState, prefs)
                    Toggle(stringResource(R.string.opt_lock_fingerprint), stringResource(R.string.opt_lock_fingerprint_caption), prefs.lockFingerprintState, prefs)
                    val shortcutLabel: @Composable (LockShortcut) -> String = { stringResource(when (it) { LockShortcut.None -> R.string.shortcut_none; LockShortcut.Torch -> R.string.lock_torch; LockShortcut.Camera -> R.string.lock_camera; LockShortcut.DoNotDisturb -> R.string.shortcut_dnd; LockShortcut.Voice -> R.string.shortcut_voice }) }
                    Label(stringResource(R.string.opt_lock_left))
                    Choices(LockShortcut.entries, prefs.lockLeftState.value, shortcutLabel) { prefs.set(prefs.lockLeftState, it) }
                    Label(stringResource(R.string.opt_lock_right))
                    Choices(LockShortcut.entries, prefs.lockRightState.value, shortcutLabel) { prefs.set(prefs.lockRightState, it) }
                    Label(stringResource(R.string.opt_owner))
                    Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(20.dp)).background(scheme.surfaceContainerHighest).padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
                        if (prefs.ownerMessage.isEmpty()) Text(stringResource(R.string.opt_owner_hint), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                        androidx.compose.foundation.text.BasicTextField(
                            value = prefs.ownerMessage,
                            onValueChange = { prefs.updateOwnerMessage(it.take(80)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item {
                Card(stringResource(R.string.section_app_icon)) {
                    LumenWordmark(prefs.logoStyleState.value, 80.dp, Color(LogoData.colours[prefs.logoPaletteState.value]))
                    Label(stringResource(R.string.opt_icon_style))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        (0 until AppIcons.STYLES).forEach { style ->
                            val on = prefs.logoStyleState.value == style
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(84.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(Color(LogoData.colours[prefs.logoPaletteState.value]))
                                    .border(if (on) 3.dp else 0.dp, if (on) scheme.onSurface else Color.Transparent, RoundedCornerShape(26.dp))
                                    .clickable {
                                        prefs.set(prefs.logoStyleState, style)
                                        AppIcons.select(context, style, prefs.logoPaletteState.value)
                                    },
                            ) { LumenIcon(style, 60.dp, Color(LogoData.colours[prefs.logoPaletteState.value])) }
                        }
                    }
                    Label(stringResource(R.string.opt_icon_colour))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LogoData.colours.forEachIndexed { index, colour ->
                            val on = prefs.logoPaletteState.value == index
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(if (on) PolygonShape(LumenShapes.clockBadge) else CircleShape)
                                    .background(Color(colour))
                                    .clickable {
                                        prefs.set(prefs.logoPaletteState, index)
                                        AppIcons.select(context, prefs.logoStyleState.value, index)
                                    },
                            ) {}
                        }
                    }
                    Text(stringResource(R.string.opt_icon_note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                }
            }
            item {
                Card(stringResource(R.string.section_backup)) {
                    FilledTonalButton(
                        onClick = {
                            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Lumen", prefs.export()))
                            Toast.makeText(context, R.string.opt_exported, Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.opt_export)) }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val text = context.getSystemService(ClipboardManager::class.java)?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (prefs.import(text)) {
                                Toast.makeText(context, R.string.opt_restored, Toast.LENGTH_SHORT).show()
                                (context as? android.app.Activity)?.recreate()
                            } else {
                                Toast.makeText(context, R.string.opt_restore_failed, Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.opt_restore)) }
                }
            }
        }
        val scrolling = listState.isScrollInProgress
        val fabScale by androidx.compose.animation.core.animateFloatAsState(if (scrolling) 0f else 1f, LocalSprings.current.spec(), label = "fab")
        ExtendedFab(stringResource(R.string.done), Glyph.Check, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).graphicsLayer { scaleX = fabScale; scaleY = fabScale; alpha = fabScale }, onClose)
    }
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = flexStyle(24f, 850, 100, scheme.primary), modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 10.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(scheme.surfaceContainerLow.copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 10.dp)) {
            content()
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
}

@Composable
private fun <T> Choices(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) }, shape = CircleShape)
        }
    }
}

@Composable
private fun Toggle(title: String, caption: String?, state: MutableState<Boolean>, prefs: LumenPrefs, glyph: Glyph = glyphFor(title)) {
    val scheme = MaterialTheme.colorScheme
    val tones = listOf(scheme.primaryContainer to scheme.onPrimaryContainer, scheme.tertiaryContainer to scheme.onTertiaryContainer, scheme.secondaryContainer to scheme.onSecondaryContainer)
    val (fill, ink) = tones[(title.hashCode() and 0x7fffffff) % tones.size]
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 1.5.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(scheme.surfaceContainerHigh)
            .clickable { prefs.set(state, !state.value) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(fill), contentAlignment = Alignment.Center) { GlyphIcon(glyph, ink, 22.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = state.value, onCheckedChange = { prefs.set(state, it) })
    }
}

private fun glyphFor(title: String): Glyph {
    val t = title.lowercase()
    return when {
        "date" in t -> Glyph.Alarm
        "alarm" in t || "battery" in t -> Glyph.Bolt
        "name" in t || "label" in t -> Glyph.Info
        "dot" in t || "notification" in t -> Glyph.Bell
        "search" in t -> Glyph.Search
        "themed" in t -> Glyph.Spark
        "categor" in t -> Glyph.Widgets
        "your apps" in t -> Glyph.Home
        "glow" in t -> Glyph.Sun
        "status" in t || "top" in t -> Glyph.Data
        "percent" in t -> Glyph.Bolt
        "lock" in t || "centre" in t -> Glyph.Lock
        "media" in t || "now bar" in t -> Glyph.Volume
        "complication" in t || "weather" in t -> Glyph.Sun
        "fingerprint" in t -> Glyph.Lock
        "torch" in t -> Glyph.Torch
        else -> Glyph.Gear
    }
}

@Composable
private fun GestureRow(title: String, state: MutableState<GestureAction>, prefs: LumenPrefs) {
    var open by remember { mutableStateOf(false) }
    val scheme = MaterialTheme.colorScheme
    Box {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { open = true }.padding(vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Text(actionLabel(state.value), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = scheme.onSecondaryContainer, modifier = Modifier.clip(CircleShape).background(scheme.secondaryContainer).padding(horizontal = 14.dp, vertical = 8.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(20.dp)) {
            GestureAction.entries.forEach { action ->
                DropdownMenuItem(text = { Text(actionLabel(action)) }, onClick = { prefs.set(state, action); open = false })
            }
        }
    }
}

@Composable
private fun actionLabel(action: GestureAction): String = stringResource(
    when (action) {
        GestureAction.None -> R.string.action_none
        GestureAction.Drawer -> R.string.action_drawer
        GestureAction.Control -> R.string.action_control
        GestureAction.Notifications -> R.string.action_notifications
        GestureAction.Lock -> R.string.action_lock
        GestureAction.Search -> R.string.action_search
        GestureAction.Customise -> R.string.action_customise
        GestureAction.Screenshot -> R.string.action_screenshot
    },
)

@Composable
private fun Preview(catalog: AppCatalog, prefs: LumenPrefs) {
    val scheme = MaterialTheme.colorScheme
    val apps = prefs.dock.orEmpty().mapNotNull { key -> catalog.apps.firstOrNull { it.key == key } }
    val shape = RoundedCornerShape(32.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(scheme.primary.copy(alpha = 0.10f), scheme.tertiary.copy(alpha = 0.16f))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.04f))), shape)
            .padding(20.dp),
    ) {
        Text(stringResource(R.string.settings_preview).uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold), color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        MiniClock(prefs.clock, big = true, prefs)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(scheme.surfaceContainerHighest.copy(alpha = 0.7f)).padding(vertical = 10.dp)) {
            apps.forEach { app -> AppIcon(app, catalog, prefs, 38.dp, showLabel = false, labelColor = Color.White, onLaunch = {}, onMenu = {}) }
        }
    }
}

@Composable
private fun MiniClock(style: ClockStyle, big: Boolean, prefs: LumenPrefs) {
    val scheme = MaterialTheme.colorScheme
    val now = rememberNow()
    val context = LocalContext.current
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val h = android.text.format.DateFormat.format(if (is24) "HH" else "hh", now).toString()
    val m = android.text.format.DateFormat.format("mm", now).toString()
    val colour = prefs.clockColourState.value
    ClockFace(
        style, h, m,
        if (colour == 0) scheme.onSurface else Color(colour),
        if (colour == 0) scheme.primary else Color(colour).copy(alpha = 0.72f),
        when {
            big -> 0.52f
            style == ClockStyle.Tall -> 0.46f
            style == ClockStyle.Tiles || style == ClockStyle.Orbit || style == ClockStyle.Shape -> 0.32f
            else -> 0.3f
        },
        clockWeight(prefs.clockWeightState.value),
        clockWidth(prefs.clockWidthState.value),
        null,
    )
}

@Composable
private fun ClockTile(style: ClockStyle, selected: Boolean, prefs: LumenPrefs, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(shape)
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerHighest)
            .border(if (selected) 2.dp else 0.dp, if (selected) scheme.primary else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Box(Modifier.height(78.dp), contentAlignment = Alignment.Center) { MiniClock(style, big = false, prefs) }
        Spacer(Modifier.height(8.dp))
        Text(
            clockLabel(style),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurface,
        )
    }
}

@Composable
private fun SliderRow(title: String, value: Float, onChange: (Float) -> Unit) {
    Column(Modifier.padding(top = 6.dp)) {
        Row {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("${(value * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = value, onValueChange = onChange)
    }
}

@Composable
private fun MotionDemo(prefs: LumenPrefs) {
    val scheme = MaterialTheme.colorScheme
    val springs = Springs(prefs.bounce, prefs.speed)
    val x = remember { Animatable(0f) }
    LaunchedEffect(prefs.bounce, prefs.speed) {
        while (true) {
            x.animateTo(1f, springs.spec())
            kotlinx.coroutines.delay(450)
            x.animateTo(0f, springs.spec())
            kotlinx.coroutines.delay(450)
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth().height(64.dp).clip(CircleShape).background(scheme.surfaceContainerHighest).padding(8.dp)) {
        val travel = constraints.maxWidth - with(LocalDensity.current) { 48.dp.roundToPx() }
        Box(
            Modifier
                .offset { IntOffset((x.value * travel).roundToInt(), 0) }
                .size(48.dp)
                .clip(PolygonShape(LumenShapes.softBurst, x.value * 120f))
                .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary))),
        )
    }
}

@Composable
private fun WeatherSetup() {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val status by Weather.status.collectAsState()
    val now by Weather.state.collectAsState()
    var place by remember { mutableStateOf(Weather.manualPlace(context)) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<Place>()) }
    LaunchedEffect(query) {
        kotlinx.coroutines.delay(350)
        results = Weather.searchPlaces(query)
    }
    LaunchedEffect(place) { Weather.refresh(context, force = true) }
    Column(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(20.dp)).background(scheme.surfaceContainerHighest).padding(14.dp)) {
        val line = when {
            status == WeatherStatus.Ok && now != null -> "${now!!.temperature}° · " + weatherLabel(now!!) + (place?.let { " · ${it.name}" } ?: "")
            status == WeatherStatus.LocationOff -> stringResource(R.string.weather_location_off)
            status == WeatherStatus.NoPermission -> stringResource(R.string.weather_no_permission)
            status == WeatherStatus.NoFix -> stringResource(R.string.weather_no_fix)
            status == WeatherStatus.Offline -> stringResource(R.string.weather_offline)
            else -> stringResource(R.string.weather_loading)
        }
        Text(line, style = MaterialTheme.typography.titleSmall, color = if (status == WeatherStatus.Ok) scheme.onSurface else scheme.error)
        if (status == WeatherStatus.LocationOff && place == null) {
            Spacer(Modifier.height(8.dp))
            FabPill(stringResource(R.string.weather_turn_on), Glyph.Location) {
                runCatching { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.weather_city), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        if (place != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(place!!.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface, modifier = Modifier.weight(1f))
                FabPill(stringResource(R.string.weather_use_location), Glyph.Location) {
                    Weather.setManualPlace(context, null)
                    place = null
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().height(50.dp).clip(CircleShape).background(scheme.surfaceContainerLow).padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) Text(stringResource(R.string.weather_city_hint), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it.take(40) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            results.forEach { r ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                        Weather.setManualPlace(context, r)
                        place = r
                        query = ""
                        results = emptyList()
                    }.padding(horizontal = 8.dp, vertical = 10.dp),
                ) {
                    GlyphIcon(Glyph.Location, scheme.primary, 20.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(r.name, style = MaterialTheme.typography.bodyLarge, color = scheme.onSurface)
                        if (r.detail.isNotBlank()) Text(r.detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
