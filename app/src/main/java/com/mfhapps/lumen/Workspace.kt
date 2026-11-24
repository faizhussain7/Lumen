package com.mfhapps.lumen

import android.appwidget.AppWidgetHost
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.roundToInt

class LumenWidgetView(context: Context) : AppWidgetHostView(context) {
    var onLongPress: (() -> Unit)? = null
    private var downX = 0f
    private var downY = 0f
    private var fired = false
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val trigger = Runnable {
        fired = true
        performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fired = false
                downX = ev.x
                downY = ev.y
                postDelayed(trigger, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> if (kotlin.math.abs(ev.x - downX) > slop || kotlin.math.abs(ev.y - downY) > slop) removeCallbacks(trigger)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(trigger)
        }
        return fired
    }
}

class LumenWidgetHost(context: Context) : AppWidgetHost(context, HOST_ID) {
    override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView = LumenWidgetView(context)

    companion object {
        const val HOST_ID = 4096
    }
}

object Widgets {
    lateinit var host: LumenWidgetHost
        private set
    lateinit var manager: AppWidgetManager
        private set

    fun init(context: Context) {
        if (::host.isInitialized) return
        host = LumenWidgetHost(context.applicationContext)
        manager = AppWidgetManager.getInstance(context.applicationContext)
    }

    fun span(info: AppWidgetProviderInfo, cellW: Float, cellH: Float, density: Float, columns: Int, rows: Int): Pair<Int, Int> {
        val w = ceil((info.minWidth * density) / cellW).toInt().coerceIn(1, columns)
        val h = ceil((info.minHeight * density) / cellH).toInt().coerceIn(1, rows)
        val tw = if (info.targetCellWidth > 0) info.targetCellWidth.coerceIn(1, columns) else w
        val th = if (info.targetCellHeight > 0) info.targetCellHeight.coerceIn(1, rows) else h
        return tw to th
    }
}

const val WIDGET_CLOCK = -10
const val WIDGET_DATE = -11
const val WIDGET_BATTERY = -12
const val WIDGET_WEATHER = -13
val BUILT_IN = setOf(WIDGET_CLOCK, WIDGET_DATE, WIDGET_BATTERY, WIDGET_WEATHER)

fun addBuiltIn(prefs: LumenPrefs, widget: Int): Boolean {
    val span = if (widget == WIDGET_CLOCK) 2 else 2
    val spot = freeSpot(prefs.home, prefs.homeColumnsState.value, prefs.homeRowsState.value, span, span) ?: return false
    prefs.updateHome(prefs.home + HomeItem("b" + System.nanoTime(), null, widget, spot.first, spot.second, span, span))
    return true
}

fun freeSpot(items: List<HomeItem>, columns: Int, rows: Int, w: Int, h: Int, ignore: String? = null): Pair<Int, Int>? {
    for (y in 0..rows - h) for (x in 0..columns - w) {
        if (items.none { it.id != ignore && overlaps(it, x, y, w, h) }) return x to y
    }
    return null
}

fun overlaps(item: HomeItem, x: Int, y: Int, w: Int, h: Int): Boolean =
    x < item.x + item.w && item.x < x + w && y < item.y + item.h && item.y < y + h

@Composable
fun WorkspaceGrid(
    catalog: AppCatalog,
    prefs: LumenPrefs,
    ink: Color,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onMenu: (AppEntry, Rect?, String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val columns = prefs.homeColumnsState.value
    val rows = prefs.homeRowsState.value
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var editing by remember { mutableStateOf<String?>(null) }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cellW = constraints.maxWidth / columns.toFloat()
        val cellH = constraints.maxHeight / rows.toFloat()
        val iconSize = (56f * (0.75f + prefs.iconScaleState.value * 0.5f)).dp
        prefs.home.forEach { item ->
            val app = item.app?.let { key -> catalog.apps.firstOrNull { it.key == key } }
            if (item.app != null && app == null) return@forEach
            val isDragging = dragging == item.id
            val lift by animateFloatAsState(if (isDragging) 1.08f else 1f, label = "lift")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset((item.x * cellW).roundToInt(), (item.y * cellH).roundToInt()) }
                    .size(with(density) { (cellW * item.w).toDp() }, with(density) { (cellH * item.h).toDp() })
                    .graphicsLayer {
                        if (isDragging) {
                            translationX = dragOffset.x
                            translationY = dragOffset.y
                            shadowElevation = 16.dp.toPx()
                        }
                        scaleX = lift
                        scaleY = lift
                    }
                    .pointerInput(item.id, columns, rows) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                dragging = item.id
                                dragOffset = Offset.Zero
                                GestureGate.dragging = true
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragOffset += amount
                            },
                            onDragEnd = {
                                val nx = (item.x + (dragOffset.x / cellW).roundToInt()).coerceIn(0, columns - item.w)
                                val ny = (item.y + (dragOffset.y / cellH).roundToInt()).coerceIn(0, rows - item.h)
                                val moved = kotlin.math.abs(dragOffset.x) + kotlin.math.abs(dragOffset.y) > 24.dp.toPx()
                                if (moved) {
                                    val others = prefs.home.filter { it.id != item.id }
                                    val blocker = others.filter { overlaps(it, nx, ny, item.w, item.h) }
                                    val updated = when {
                                        blocker.isEmpty() -> prefs.home.map { if (it.id == item.id) it.also { m -> m.x = nx; m.y = ny } else it }
                                        blocker.size == 1 && blocker[0].w == item.w && blocker[0].h == item.h -> prefs.home.map {
                                            when (it.id) {
                                                item.id -> it.also { m -> m.x = nx; m.y = ny }
                                                blocker[0].id -> it.also { m -> m.x = item.x; m.y = item.y }
                                                else -> it
                                            }
                                        }
                                        else -> null
                                    }
                                    if (updated != null) {
                                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        prefs.updateHome(updated)
                                    }
                                } else if (app != null) {
                                    onMenu(app, null, item.id)
                                } else {
                                    editing = item.id
                                }
                                dragging = null
                                dragOffset = Offset.Zero
                                GestureGate.dragging = false
                            },
                            onDragCancel = {
                                dragging = null
                                dragOffset = Offset.Zero
                                GestureGate.dragging = false
                            },
                        )
                    },
            ) {
                if (app != null) {
                    AppIcon(app, catalog, prefs, iconSize, showLabel = prefs.homeLabelsState.value, labelColor = ink, onLaunch = { onLaunch(app, it) }, onMenu = {})
                } else {
                    WidgetCell(item, prefs, editing == item.id, onEdit = { editing = item.id }, onDone = { editing = null }, cellW = cellW, cellH = cellH, columns = columns, rows = rows)
                }
            }
        }
    }
}

@Composable
private fun WidgetCell(item: HomeItem, prefs: LumenPrefs, editing: Boolean, onEdit: () -> Unit, onDone: () -> Unit, cellW: Float, cellH: Float, columns: Int, rows: Int) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val density = LocalDensity.current
    val builtIn = item.widget in BUILT_IN
    val info = remember(item.widget) { if (builtIn) null else runCatching { Widgets.manager.getAppWidgetInfo(item.widget) }.getOrNull() }
    if (builtIn) {
        val side = with(density) { minOf(cellW * item.w, cellH * item.h).toDp() } - 12.dp
        Box(
            Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onLongPress = { onEdit() }) },
            contentAlignment = Alignment.Center,
        ) {
            when (item.widget) {
                WIDGET_CLOCK -> OrbitClock(side)
                WIDGET_DATE -> DateWidget(side)
                WIDGET_WEATHER -> WeatherWidget(side, rememberWeather(true))
                else -> BatteryRingWidget(side)
            }
        }
    } else if (info == null) {
        Box(Modifier.fillMaxSize().padding(6.dp).clip(RoundedCornerShape(24.dp)).background(scheme.surfaceContainerHigh.copy(alpha = 0.6f)).clickable { onEdit() })
    } else {
        val widthDp = with(density) { (cellW * item.w).toDp() }
        val heightDp = with(density) { (cellH * item.h).toDp() }
        AndroidView(
            factory = {
                (Widgets.host.createView(context.applicationContext, item.widget, info) as LumenWidgetView).apply {
                    setAppWidget(item.widget, info)
                }
            },
            update = { view ->
                view.onLongPress = onEdit
                val options = Bundle()
                runCatching {
                    view.updateAppWidgetSize(options, widthDp.value.toInt(), heightDp.value.toInt(), widthDp.value.toInt(), heightDp.value.toInt())
                }
            },
            modifier = Modifier.fillMaxSize().padding(4.dp).clip(RoundedCornerShape(24.dp)),
        )
    }
    if (editing) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.surfaceContainerHighest.copy(alpha = 0.92f))
                .border(2.dp, scheme.primary, RoundedCornerShape(24.dp)),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                EditChip("W−") { resize(prefs, item, -1, 0, columns, rows) }
                EditChip("W+") { resize(prefs, item, 1, 0, columns, rows) }
                EditChip("H−") { resize(prefs, item, 0, -1, columns, rows) }
                EditChip("H+") { resize(prefs, item, 0, 1, columns, rows) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EditChip(stringResource(R.string.remove)) {
                    if (item.widget >= 0) runCatching { Widgets.host.deleteAppWidgetId(item.widget) }
                    prefs.updateHome(prefs.home.filter { it.id != item.id })
                    onDone()
                }
                EditChip(stringResource(R.string.done), primary = true) { onDone() }
            }
        }
    }
}

private fun resize(prefs: LumenPrefs, item: HomeItem, dw: Int, dh: Int, columns: Int, rows: Int) {
    val w = (item.w + dw).coerceIn(1, columns - item.x)
    val h = (item.h + dh).coerceIn(1, rows - item.y)
    if (prefs.home.any { it.id != item.id && overlaps(it, item.x, item.y, w, h) }) return
    prefs.updateHome(prefs.home.map { if (it.id == item.id) it.also { m -> m.w = w; m.h = h } else it })
}

@Composable
private fun EditChip(text: String, primary: Boolean = false, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = if (primary) scheme.onPrimary else scheme.onSecondaryContainer,
        modifier = Modifier.clip(CircleShape).background(if (primary) scheme.primary else scheme.secondaryContainer).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

private class WidgetApp(val label: String, val packageName: String, val icon: androidx.compose.ui.graphics.ImageBitmap?, val widgets: List<AppWidgetProviderInfo>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetPicker(onPick: (AppWidgetProviderInfo) -> Unit, onBuiltIn: (Int) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf<String?>(null) }
    val apps by produceState(emptyList<WidgetApp>()) {
        value = withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val size = (40 * context.resources.displayMetrics.density).toInt()
            Widgets.manager.installedProviders
                .groupBy { it.provider.packageName }
                .map { (pkg, list) ->
                    val appInfo = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
                    WidgetApp(
                        label = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg,
                        packageName = pkg,
                        icon = appInfo?.let { runCatching { pm.getApplicationIcon(it).toBitmap(size, size).asImageBitmap() }.getOrNull() },
                        widgets = list.sortedBy { it.loadLabel(pm) },
                    )
                }
                .sortedBy { it.label.lowercase() }
        }
    }
    val shown = remember(apps, query) {
        if (query.isBlank()) apps else apps.filter { app ->
            app.label.contains(query, ignoreCase = true) || app.widgets.any { it.loadLabel(context.packageManager).contains(query, ignoreCase = true) }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = scheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Text(stringResource(R.string.widgets), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = scheme.onSurface)
            Spacer(Modifier.height(14.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(scheme.surfaceContainerHighest).padding(horizontal = 18.dp),
            ) {
                GlyphIcon(Glyph.Search, scheme.onSurfaceVariant, 22.dp)
                Spacer(Modifier.size(12.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.search_widgets), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(scheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (query.isBlank()) item(key = "lumen") {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(scheme.primaryContainer.copy(alpha = 0.55f)).padding(16.dp)) {
                    Text(stringResource(R.string.lumen_widgets), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = scheme.onSurface)
                    Spacer(Modifier.height(12.dp))
                    val builtIns = listOf(WIDGET_CLOCK to R.string.widget_clock, WIDGET_WEATHER to R.string.widget_weather, WIDGET_DATE to R.string.widget_date, WIDGET_BATTERY to R.string.widget_battery)
                    val carousel = androidx.compose.material3.carousel.rememberCarouselState { builtIns.size }
                    androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel(
                        state = carousel,
                        preferredItemWidth = 200.dp,
                        itemSpacing = 8.dp,
                        modifier = Modifier.fillMaxWidth().height(230.dp),
                    ) { index ->
                        val (id, label) = builtIns[index]
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().maskClip(RoundedCornerShape(28.dp)).background(scheme.surfaceContainerHighest).clickable { onBuiltIn(id) }.padding(12.dp),
                        ) {
                            Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                                when (id) {
                                    WIDGET_CLOCK -> OrbitClock(140.dp)
                                    WIDGET_DATE -> DateWidget(140.dp)
                                    WIDGET_WEATHER -> WeatherWidget(140.dp, Weather.state.collectAsState().value)
                                    else -> BatteryRingWidget(140.dp)
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(stringResource(label), style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, maxLines = 1)
                        }
                    }
                }
            }
            items(shown, key = { it.packageName }) { app ->
                val open = expanded == app.packageName || query.isNotBlank()
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(if (open) scheme.surfaceContainerHigh else scheme.surfaceContainer)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { expanded = if (expanded == app.packageName) null else app.packageName }.padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape)) { app.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(40.dp)) } }
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = scheme.onSurface)
                            Text(context.resources.getQuantityString(R.plurals.widget_count, app.widgets.size, app.widgets.size), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        androidx.compose.material3.Icon(
                            if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                    if (open) {
                        androidx.compose.foundation.lazy.LazyRow(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(app.widgets, key = { it.provider.flattenToShortString() }) { info -> WidgetCard(info) { onPick(info) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetCard(info: AppWidgetProviderInfo, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val cols = if (info.targetCellWidth > 0) info.targetCellWidth else ceil(info.minWidth / 74f).toInt().coerceAtLeast(1)
    val rows = if (info.targetCellHeight > 0) info.targetCellHeight else ceil(info.minHeight / 74f).toInt().coerceAtLeast(1)
    val boxW = 210.dp
    val boxH = 150.dp
    Column(
        Modifier.width(boxW + 24.dp).clip(RoundedCornerShape(22.dp)).background(scheme.surfaceContainerHighest).clickable(onClick = onClick).padding(12.dp),
    ) {
        Box(Modifier.size(boxW, boxH).clip(RoundedCornerShape(16.dp)).background(scheme.surfaceContainerLow), contentAlignment = Alignment.Center) {
            if (info.previewLayout != 0) {
                AndroidView(
                    factory = { ctx ->
                        val frame = android.widget.FrameLayout(ctx)
                        runCatching {
                            val view = android.widget.RemoteViews(info.provider.packageName, info.previewLayout).apply(ctx, frame)
                            frame.addView(view)
                        }
                        frame
                    },
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
            } else {
                val preview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, info) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            val drawable = info.loadPreviewImage(context, 0) ?: info.loadIcon(context, 0) ?: return@runCatching null
                            val iw = drawable.intrinsicWidth.coerceAtLeast(1)
                            val ih = drawable.intrinsicHeight.coerceAtLeast(1)
                            val maxW = (boxW.value * density).toInt()
                            val maxH = (boxH.value * density).toInt()
                            val scale = minOf(maxW.toFloat() / iw, maxH.toFloat() / ih, 2f)
                            drawable.toBitmap((iw * scale).toInt().coerceAtLeast(1), (ih * scale).toInt().coerceAtLeast(1)).asImageBitmap()
                        }.getOrNull()
                    }
                }
                preview?.let { Image(it, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp)) }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(info.loadLabel(context.packageManager), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = scheme.onSurface, maxLines = 1)
        Text("$cols × $rows", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }
}
