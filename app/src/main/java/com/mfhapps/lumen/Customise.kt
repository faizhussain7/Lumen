@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mfhapps.lumen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.foundation.gestures.animateScrollBy
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
        var query by remember { mutableStateOf("") }
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        var searchFocused by remember { mutableStateOf(false) }
        androidx.activity.compose.BackHandler(enabled = searchFocused) { focusManager.clearFocus() }
        LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) focusManager.clearFocus() }
        var open by remember { androidx.compose.runtime.mutableIntStateOf(-1) }
        val openedAt = remember { android.os.SystemClock.uptimeMillis() }
        val sections: List<Section> = listOf(
            Section(R.string.settings_clock, Glyph.Alarm, listOf(R.string.opt_size, R.string.opt_colour, R.string.opt_weather), listOf(R.string.opt_weight, R.string.opt_width, R.string.opt_show_date, R.string.opt_show_chips)) {
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
                                    .tap(if (selected) PolygonShape(LumenShapes.clockBadge) else CircleShape, depth = 0.12f) { prefs.set(prefs.clockColourState, colour) }
                                    .background(if (colour == 0) Brush.sweepGradient(listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary)) else Brush.linearGradient(listOf(Color(colour), Color(colour))))
                                    .border(1.dp, scheme.outlineVariant, if (selected) PolygonShape(LumenShapes.clockBadge) else CircleShape),
                            ) {}
                        }
                    }
                    Toggle(stringResource(R.string.opt_show_date), null, prefs.showDateState, prefs)
                    val askLocation = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
                        prefs.set(prefs.weatherState, granted)
                    }
                    val weatherSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 1.5.dp).fillMaxWidth().listTap(rest = 8.dp, interaction = weatherSource) {
                        if (prefs.weatherState.value) prefs.set(prefs.weatherState, false)
                        else if (Weather.permitted(context)) prefs.set(prefs.weatherState, true)
                        else askLocation.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                    }.background(scheme.surfaceContainerHigh).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(scheme.tertiaryContainer), contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Sun, scheme.onTertiaryContainer, 22.dp) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.opt_weather), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                            Text(stringResource(R.string.opt_weather_caption), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = prefs.weatherState.value, onCheckedChange = null, interactionSource = weatherSource, thumbContent = if (prefs.weatherState.value) { { androidx.compose.material3.Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize)) } } else null)
                    }
                    if (prefs.weatherState.value) WeatherSetup()
                    Toggle(stringResource(R.string.opt_show_chips), null, prefs.showChipsState, prefs)
                },
            Section(R.string.section_home, Glyph.Home, listOf(R.string.opt_home_columns, R.string.opt_pages, R.string.opt_icon_size), listOf(R.string.opt_home_rows, R.string.opt_home_labels, R.string.opt_badges)) {
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
                },
            Section(R.string.section_dock, Glyph.Dock, listOf(R.string.opt_dock_style, R.string.opt_dock_search), listOf()) {
                    Label(stringResource(R.string.opt_dock_style))
                    Choices(DockStyle.entries, prefs.dockStyleState.value, { stringResource(when (it) { DockStyle.Glass -> R.string.dock_glass; DockStyle.Solid -> R.string.dock_solid; DockStyle.Clear -> R.string.dock_clear; DockStyle.Hidden -> R.string.dock_hidden }) }) { prefs.set(prefs.dockStyleState, it) }
                    Toggle(stringResource(R.string.opt_dock_search), null, prefs.dockSearchState, prefs)
                },
            Section(R.string.settings_icons, Glyph.Widgets, listOf(R.string.settings_themed_icons, R.string.opt_icon_pack), listOf()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(IconShape.entries) { shape ->
                            val selected = prefs.shape == shape
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .tap(RoundedCornerShape(20.dp), depth = 0.1f) { prefs.updateShape(shape) }
                                    .background(if (selected) scheme.primary.copy(alpha = 0.16f) else Color.Transparent)
                                    .border(if (selected) 2.dp else 1.dp, if (selected) scheme.primary else scheme.outlineVariant, RoundedCornerShape(20.dp)),
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
                },
            Section(R.string.section_drawer, Glyph.Calculate, listOf(R.string.opt_sort, R.string.opt_categories, R.string.opt_hidden), listOf(R.string.settings_columns, R.string.opt_favourites, R.string.opt_drawer_labels)) {
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
                },
            Section(R.string.section_colours, Glyph.Wallpaper, listOf(R.string.opt_vivid, R.string.opt_accent_source), listOf(R.string.opt_wallpaper, R.string.opt_custom)) {
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
                                        .tap(if (selected) PolygonShape(LumenShapes.clockBadge) else CircleShape, depth = 0.12f) { prefs.set(prefs.accentState, colour) }
                                        .background(Color(colour)),
                                )
                            }
                        }
                    }
                },
            Section(R.string.section_gestures, Glyph.Next, listOf(R.string.opt_swipe_up, R.string.opt_double_tap), listOf(R.string.opt_swipe_down, R.string.opt_pinch)) {
                    GestureRow(stringResource(R.string.opt_swipe_up), prefs.swipeUpState, prefs)
                    GestureRow(stringResource(R.string.opt_swipe_down), prefs.swipeDownState, prefs)
                    GestureRow(stringResource(R.string.opt_double_tap), prefs.doubleTapState, prefs)
                    GestureRow(stringResource(R.string.opt_pinch), prefs.pinchState, prefs)
                },
            Section(R.string.settings_motion, Glyph.Bolt, listOf(R.string.settings_bounce, R.string.settings_speed, R.string.settings_blur), listOf()) {
                    MotionDemo(prefs)
                    SliderRow(stringResource(R.string.settings_bounce), prefs.bounce) { prefs.updateBounce(it) }
                    SliderRow(stringResource(R.string.settings_speed), prefs.speed) { prefs.updateSpeed(it) }
                    SliderRow(stringResource(R.string.settings_blur), prefs.blur) { prefs.updateBlur(it) }
                },
            Section(R.string.settings_effects, Glyph.Sun, listOf(R.string.settings_glow, R.string.settings_wallpaper), listOf()) {
                    Toggle(stringResource(R.string.settings_glow), stringResource(R.string.settings_glow_caption), prefs.glowState, prefs)
                    Spacer(Modifier.height(8.dp))
                    FilledTonalButton(
                        onClick = { runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                        shape = CircleShape,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) { Text(stringResource(R.string.settings_wallpaper)) }
                },
            Section(R.string.section_status, Glyph.Data, listOf(R.string.opt_status_bar, R.string.opt_status_percent), listOf(R.string.settings_own_top)) {
                    Toggle(stringResource(R.string.settings_own_top), stringResource(R.string.settings_own_top_caption), prefs.ownTopState, prefs)
                    Toggle(stringResource(R.string.opt_status_bar), null, prefs.statusBarState, prefs)
                    Toggle(stringResource(R.string.opt_status_percent), null, prefs.statusPercentState, prefs)
                },
            Section(R.string.section_lock, Glyph.Lock, listOf(R.string.opt_lock_clock, R.string.opt_lock_notices, R.string.opt_lock_media), listOf(R.string.settings_lock, R.string.opt_lock_centre, R.string.opt_lock_widgets, R.string.opt_lock_fingerprint, R.string.opt_owner, R.string.opt_lock_left, R.string.opt_lock_right)) {
                    val lockSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().listTap(rest = 8.dp, interaction = lockSource) {
                        val on = !prefs.lockCover
                        prefs.updateLockCover(on)
                        if (on && !LumenAccess.ready) {
                            Toast.makeText(context, R.string.access_needed, Toast.LENGTH_LONG).show()
                            LumenAccess.openSettings(context)
                        }
                    }.background(scheme.surfaceContainerHigh).padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_lock), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
                            Text(stringResource(R.string.settings_lock_caption), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = prefs.lockCover, onCheckedChange = null, interactionSource = lockSource, thumbContent = if (prefs.lockCover) { { androidx.compose.material3.Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize)) } } else null)
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
                },
            Section(R.string.section_app_icon, Glyph.Spark, listOf(R.string.opt_icon_style, R.string.opt_icon_colour), listOf()) {
                    LumenWordmark(64.dp, Color(LogoMarks.colours[prefs.logoPaletteState.value]), tile = true, ground = MaterialTheme.colorScheme.surfaceContainerLow)
                    Label(stringResource(R.string.opt_icon_style))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        (0 until AppIcons.STYLES).forEach { style ->
                            val on = prefs.logoStyleState.value == style
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(84.dp)
                                    .tap(RoundedCornerShape(26.dp), depth = 0.1f) {
                                        prefs.set(prefs.logoStyleState, style)
                                        AppIcons.select(context, style, prefs.logoPaletteState.value)
                                    }
                                    .border(if (on) 3.dp else 0.dp, if (on) scheme.onSurface else Color.Transparent, RoundedCornerShape(26.dp)),
                            ) { LumenIcon(style, 84.dp, Color(LogoMarks.colours[prefs.logoPaletteState.value])) }
                        }
                    }
                    Label(stringResource(R.string.opt_icon_colour))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        LogoMarks.colours.forEachIndexed { index, colour ->
                            val on = prefs.logoPaletteState.value == index
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .tap(if (on) PolygonShape(LumenShapes.clockBadge) else CircleShape, depth = 0.12f) {
                                        prefs.set(prefs.logoPaletteState, index)
                                        AppIcons.select(context, prefs.logoStyleState.value, index)
                                    }
                                    .background(Color(colour)),
                            ) {}
                        }
                    }
                    Text(stringResource(R.string.opt_icon_note), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                },
            Section(R.string.section_backup, Glyph.Info, listOf(R.string.opt_export, R.string.opt_restore), listOf(R.string.onb_replay)) {
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
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = { onClose(); prefs.set(prefs.onboardedState, false) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.onb_replay)) }
},
        )
        val q = query.trim().lowercase()
        val keyText = remember(sections.size) { sections.map { sec -> (listOf(sec.title) + sec.summary + sec.extra).joinToString(" ") { context.getString(it) }.lowercase() } }
        val visible = sections.indices.filter { q.isEmpty() || keyText[it].contains(q) }
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
                        LumenWordmark(40.dp)
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
            item {
                CustomiseSearch(query, { query = it }, onFocus = { searchFocused = it }, onDone = { focusManager.clearFocus() })
            }
            if (q.isEmpty()) item { Preview(catalog, prefs) }
            items(visible, key = { it }) { i ->
                val section = sections[i]
                SectionCard(
                    section,
                    expanded = q.isNotEmpty() || open == i,
                    index = visible.indexOf(i),
                    animateIn = remember { android.os.SystemClock.uptimeMillis() - openedAt < 900 },
                    modifier = Modifier.animateItem(fadeInSpec = LocalSprings.current.effects(), placementSpec = LocalSprings.current.size(), fadeOutSpec = LocalSprings.current.effects()),
                ) {
                    focusManager.clearFocus()
                    open = if (open == i) -1 else i
                    if (open == i) jump.launch {
                        kotlinx.coroutines.delay(420)
                        val info = listState.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.key == i } ?: return@launch
                        val viewEnd = info.viewportEndOffset - info.afterContentPadding
                        val overflow = item.offset + item.size - viewEnd
                        val room = item.offset - info.viewportStartOffset - 24
                        val by = minOf(overflow, room)
                        if (by > 0) listState.animateScrollBy(by.toFloat(), androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 300f))
                    }
                }
            }
            if (visible.isEmpty()) item {
                Text(stringResource(R.string.cus_no_results, query.trim()), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(24.dp).stagger(0), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        val scrolling = listState.isScrollInProgress
        val fabScale by androidx.compose.animation.core.animateFloatAsState(if (scrolling) 0f else 1f, LocalSprings.current.spec(), label = "fab")
        ExtendedFab(stringResource(R.string.done), Glyph.Check, Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(20.dp).graphicsLayer { scaleX = fabScale; scaleY = fabScale; alpha = fabScale }, onClose)
    }
}

class Section(val title: Int, val glyph: Glyph, val summary: List<Int>, val extra: List<Int>, val content: @Composable () -> Unit)

@Composable
private fun SectionCard(section: Section, expanded: Boolean, index: Int, animateIn: Boolean, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val springs = LocalSprings.current
    val k by androidx.compose.animation.core.animateFloatAsState(if (expanded) 1f else 0f, springs.spatial(), label = "open")
    val badge = remember { androidx.graphics.shapes.Morph(androidx.compose.material3.MaterialShapes.Circle, androidx.compose.material3.MaterialShapes.Cookie9Sided) }
    val container by androidx.compose.animation.animateColorAsState(if (expanded) scheme.surfaceContainer else scheme.surfaceContainerLow.copy(alpha = 0.75f), springs.effects(), label = "card")
    val corner by androidx.compose.animation.core.animateDpAsState(if (expanded) 32.dp else 26.dp, springs.size(), label = "corner")
    Column(
        modifier
            .then(if (animateIn) Modifier.stagger(index) else Modifier)
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(container),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().listTap(rest = corner, label = stringResource(section.title), onClick = onToggle).padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer { shape = MorphShape(badge, k, k * 40f); clip = true }
                    .background(androidx.compose.ui.graphics.lerp(scheme.secondaryContainer, scheme.primary, k)),
            ) { GlyphIcon(section.glyph, androidx.compose.ui.graphics.lerp(scheme.onSecondaryContainer, scheme.onPrimary, k), 24.dp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(section.title), style = flexStyle(20f, (650 + 200 * k).toInt(), 100, scheme.onSurface))
                Text(
                    section.summary.map { stringResource(it) }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(32.dp).graphicsLayer { rotationZ = 90f * k }, contentAlignment = Alignment.Center) { GlyphIcon(Glyph.Next, scheme.onSurfaceVariant, 22.dp) }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = androidx.compose.animation.expandVertically(springs.size(), expandFrom = Alignment.Top) + androidx.compose.animation.fadeIn(springs.effects()),
            exit = androidx.compose.animation.shrinkVertically(springs.size(), shrinkTowards = Alignment.Top) + androidx.compose.animation.fadeOut(springs.fastEffects()),
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 12.dp)) { section.content() }
        }
    }
}

@Composable
private fun CustomiseSearch(query: String, onChange: (String) -> Unit, onFocus: (Boolean) -> Unit, onDone: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val springs = LocalSprings.current
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    LaunchedEffect(focused) { onFocus(focused) }
    val active = focused || query.isNotEmpty()
    val k by androidx.compose.animation.core.animateFloatAsState(if (active) 1f else 0f, springs.spatial(), label = "search")
    val badge = remember { androidx.graphics.shapes.Morph(androidx.compose.material3.MaterialShapes.Circle, androidx.compose.material3.MaterialShapes.Cookie9Sided) }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height((56 + 4 * k).dp)
            .graphicsLayer { scaleX = 0.97f + 0.03f * k; scaleY = scaleX }
            .tap(CircleShape, depth = 0.02f) { focus.requestFocus() }
            .background(androidx.compose.ui.graphics.lerp(scheme.surfaceContainerHigh, scheme.surfaceContainerHighest, k))
            .border((1.5f * k).dp, scheme.primary.copy(alpha = 0.6f * k), CircleShape)
            .padding(start = 8.dp, end = 10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(42.dp).graphicsLayer { shape = MorphShape(badge, k, k * 60f); clip = true }.background(androidx.compose.ui.graphics.lerp(scheme.secondaryContainer, scheme.primary, k)),
        ) { GlyphIcon(Glyph.Search, androidx.compose.ui.graphics.lerp(scheme.onSecondaryContainer, scheme.onPrimary, k), 22.dp) }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text(stringResource(R.string.cus_search), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                interactionSource = source,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onDone() }, onDone = { onDone() }),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.primary),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = androidx.compose.animation.scaleIn(springs.fastSpatial()) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.scaleOut(springs.fastSpatial()) + androidx.compose.animation.fadeOut(),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).tap(CircleShape, depth = 0.12f) { onChange("") }.background(scheme.surfaceContainerHigh)) {
                androidx.compose.material3.Icon(Icons.Rounded.Close, contentDescription = null, tint = scheme.onSurface, modifier = Modifier.size(20.dp))
            }
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
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val springs = LocalSprings.current
    val on by androidx.compose.animation.core.animateFloatAsState(if (state.value) 1f else 0f, springs.fastSpatial(), label = "on")
    val badge = remember { androidx.graphics.shapes.Morph(androidx.compose.material3.MaterialShapes.Circle, androidx.compose.material3.MaterialShapes.Cookie9Sided) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(vertical = 1.5.dp)
            .fillMaxWidth()
            .listTap(rest = 8.dp, interaction = source) { prefs.set(state, !state.value) }
            .background(scheme.surfaceContainerHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(Modifier.size(40.dp).graphicsLayer { shape = MorphShape(badge, on, on * 30f); clip = true }.background(fill), contentAlignment = Alignment.Center) { GlyphIcon(glyph, ink, 22.dp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface)
            if (caption != null) Text(caption, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = state.value,
            onCheckedChange = null,
            interactionSource = source,
            thumbContent = if (state.value) { { androidx.compose.material3.Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize)) } } else null,
        )
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
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().listTap(rest = 8.dp) { open = true }.padding(horizontal = 6.dp, vertical = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, modifier = Modifier.weight(1f))
            Text(actionLabel(state.value), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = scheme.onSecondaryContainer, modifier = Modifier.clip(CircleShape).background(scheme.secondaryContainer).padding(horizontal = 14.dp, vertical = 8.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(20.dp)) {
            GestureAction.entries.forEach { action ->
                val chosen = state.value == action
                DropdownMenuItem(
                    text = { Text(actionLabel(action), fontWeight = if (chosen) FontWeight.SemiBold else null) },
                    trailingIcon = if (chosen) { { androidx.compose.material3.Icon(Icons.Rounded.Check, contentDescription = null, tint = scheme.primary) } } else null,
                    modifier = if (chosen) Modifier.padding(horizontal = 6.dp).clip(RoundedCornerShape(14.dp)).background(scheme.secondaryContainer) else Modifier,
                    onClick = { prefs.set(state, action); open = false },
                )
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
            .tap(shape, depth = 0.05f, onClick = onClick)
            .background(if (selected) scheme.primaryContainer else scheme.surfaceContainerHighest)
            .border(if (selected) 2.dp else 0.dp, if (selected) scheme.primary else Color.Transparent, shape)
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
                    modifier = Modifier.fillMaxWidth().listTap(rest = 14.dp) {
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
