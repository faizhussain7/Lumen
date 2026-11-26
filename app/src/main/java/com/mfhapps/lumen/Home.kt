@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.mfhapps.lumen

import android.app.SearchManager
import android.content.Intent
import android.graphics.Rect
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun LumenHome(
    catalog: AppCatalog,
    prefs: LumenPrefs,
    homePresses: Int,
    resets: Int,
    isDefaultHome: () -> Boolean,
    onAddWidget: (android.appwidget.AppWidgetProviderInfo) -> Unit,
    onDepth: (Float) -> Unit,
    onShade: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val springs = LocalSprings.current
    val wallpaperDark = LocalWallpaperDark.current
    val ink = if (wallpaperDark) Color.White else Color(0xFF111318)
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val haptics = LocalHapticFeedback.current
    val slop = LocalViewConfiguration.current.touchSlop
    val drawer = remember { Animatable(0f) }
    val customise = remember { Animatable(0f) }
    val control = remember { Animatable(0f) }
    val context = LocalContext.current
    val device = remember { DeviceState(context) }
    androidx.compose.runtime.DisposableEffect(device) {
        device.start()
        onDispose { device.stop() }
    }
    var islandRect by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var lastTap by remember { mutableStateOf(0L) }
    var query by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf<Triple<AppEntry, Rect?, String?>?>(null) }
    var homeMenu by remember { mutableStateOf<Offset?>(null) }
    var picker by remember { mutableStateOf(false) }
    var openFolder by remember { mutableStateOf<String?>(null) }
    var searchFocus by remember { mutableStateOf(false) }
    var glowPoint by remember { mutableStateOf(Offset.Zero) }
    var touching by remember { mutableStateOf(false) }
    val glowAlpha by animateFloatAsState(if (touching && prefs.glow && drawer.value < 0.05f) 1f else 0f, springs.spec(0.6f), label = "glow")

    LaunchedEffect(Unit) {
        if (prefs.home.isEmpty() && !prefs.homeSeeded) {
            prefs.markHomeSeeded()
            addBuiltIn(prefs, WIDGET_CLOCK)
            addBuiltIn(prefs, WIDGET_DATE)
        }
    }
    LaunchedEffect(catalog.apps) {
        if (prefs.dock == null && catalog.apps.isNotEmpty()) {
            val defaults = catalog.defaultDock()
            if (defaults.isNotEmpty()) prefs.updateDock(defaults, edited = false)
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { maxOf(drawer.value, customise.value, control.value) }.collect { onDepth(it.coerceIn(0f, 1f)) }
    }
    LaunchedEffect(resets) {
        if (resets > 0) {
            menu = null
            homeMenu = null
            picker = false
            query = ""
            searchFocus = false
            drawer.snapTo(0f)
            control.snapTo(0f)
            customise.snapTo(0f)
        }
    }
    LaunchedEffect(homePresses) {
        if (homePresses > 0) {
            menu = null
            homeMenu = null
            query = ""
            searchFocus = false
            launch { customise.animateTo(0f, springs.close()) }
            launch { control.animateTo(0f, springs.close()) }
            drawer.animateTo(0f, springs.close())
        }
    }
    val backTarget = when {
        customise.targetValue > 0f -> customise
        control.targetValue > 0f -> control
        drawer.targetValue > 0f -> drawer
        else -> null
    }
    androidx.activity.compose.PredictiveBackHandler(enabled = backTarget != null) { events ->
        val target = backTarget ?: return@PredictiveBackHandler
        try {
            events.collect { event -> target.snapTo(1f - event.progress * 0.35f) }
            if (target === drawer) query = ""
            target.animateTo(0f, springs.close())
        } catch (e: kotlinx.coroutines.CancellationException) {
            target.animateTo(1f, springs.spatial())
            throw e
        }
    }

    fun openDrawer(focus: Boolean) {
        searchFocus = focus
        scope.launch { drawer.animateTo(1f, springs.spec()) }
    }

    fun perform(action: GestureAction) {
        when (action) {
            GestureAction.None -> Unit
            GestureAction.Drawer -> openDrawer(false)
            GestureAction.Search -> openDrawer(true)
            GestureAction.Control -> scope.launch { control.animateTo(1f, springs.spec()) }
            GestureAction.Notifications -> if (!LumenAccess.notifications()) onShade()
            GestureAction.Lock -> if (!LumenAccess.lock()) android.widget.Toast.makeText(context, R.string.access_needed, android.widget.Toast.LENGTH_LONG).show()
            GestureAction.Screenshot -> if (!LumenAccess.screenshot()) android.widget.Toast.makeText(context, R.string.access_needed, android.widget.Toast.LENGTH_LONG).show()
            GestureAction.Customise -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); scope.launch { customise.animateTo(1f, springs.spec()) } }
        }
    }

    fun openCustomise() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch { customise.animateTo(1f, springs.spec()) }
    }

    val launch: (AppEntry, Rect?) -> Unit = { app, bounds ->
        prefs.recordLaunch(app.key)
        catalog.launch(app, view, bounds)
        scope.launch {
            delay(400)
            query = ""
            drawer.snapTo(0f)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heightPx = constraints.maxHeight.toFloat()
        val travel = heightPx * 0.42f

        fun settle(velocity: Float) {
            val target = when {
                velocity < -350f -> 1f
                velocity > 350f -> 0f
                else -> if (drawer.value > 0.22f) 1f else 0f
            }
            if (target == 0f) query = ""
            scope.launch { drawer.animateTo(target, if (target == 0f) springs.close() else springs.spec(), initialVelocity = -velocity / travel) }
        }

        fun dragBy(dy: Float) {
            val raw = drawer.value - dy / travel
            val resisted = if (raw > 1f) 1f + (raw - 1f) * 0.35f else raw
            scope.launch { drawer.snapTo(resisted.coerceIn(0f, 1.1f)) }
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (drawer.targetValue > 0f || customise.targetValue > 0f || control.targetValue > 0f) return@awaitEachGesture
                        if (drawer.value > 0f || control.value > 0f || customise.value > 0f) {
                            scope.launch { drawer.snapTo(0f) }
                            scope.launch { control.snapTo(0f) }
                            scope.launch { customise.snapTo(0f) }
                        }
                        val rightHalf = prefs.ownTop || down.position.x > size.width / 2f
                        val onEmpty = !down.isConsumed
                        touching = true
                        glowPoint = down.position
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        var mode = 0
                        var dx = 0f
                        var dy = 0f
                        var pinchStart = -1f
                        var pinched = false
                        var shaded = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Main)
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size >= 2) {
                                mode = 2
                                val distance = (pressed[0].position - pressed[1].position).getDistance()
                                if (pinchStart < 0f) pinchStart = distance
                                if (!pinched && distance / pinchStart < 0.8f) {
                                    pinched = true
                                    perform(prefs.pinchState.value)
                                }
                                event.changes.forEach { it.consume() }
                                continue
                            }
                            val change = pressed.first()
                            glowPoint = change.position
                            val delta = change.positionChange()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            if (mode == 0) {
                                dx += delta.x
                                dy += delta.y
                                if (abs(dy) > slop && abs(dy) > abs(dx)) {
                                    mode = if (dy < 0f) {
                                        if (prefs.swipeUpState.value == GestureAction.Drawer) 1 else 5
                                    } else {
                                        when (prefs.swipeDownState.value) {
                                            GestureAction.Control -> 3
                                            GestureAction.Notifications -> 6
                                            else -> 7
                                        }
                                    }
                                }
                                else if (abs(dx) > slop) mode = 4
                            }
                            GestureGate.dragging = mode == 1 || mode == 3 || mode >= 5
                            if (mode == 5 || mode == 7) {
                                change.consume()
                                dy += delta.y
                            }
                            if (mode == 6) {
                                change.consume()
                                dy += delta.y
                                if (!shaded && dy > 72.dp.toPx()) {
                                    shaded = true
                                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    if (!LumenAccess.notifications()) onShade()
                                }
                            }
                            when (mode) {
                                1 -> {
                                    change.consume()
                                    dragBy(delta.y)
                                }
                                3 -> {
                                    change.consume()
                                    if (rightHalf) {
                                        val next = (control.value + delta.y / (heightPx * 0.45f)).coerceIn(0f, 1.06f)
                                        scope.launch { control.snapTo(next) }
                                    } else {
                                        dy += delta.y
                                        if (!shaded && dy > 72.dp.toPx()) {
                                            shaded = true
                                            haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                            if (!LumenAccess.notifications()) onShade()
                                        }
                                    }
                                }
                            }
                        }
                        touching = false
                        GestureGate.dragging = false
                        if (mode == 1) settle(tracker.calculateVelocity().y)
                        if (mode == 3 && rightHalf) {
                            val v = tracker.calculateVelocity().y
                            val target = if (v > 600f || (v > -600f && control.value > 0.35f)) 1f else 0f
                            if (target == 1f) haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                            scope.launch { control.animateTo(target, if (target == 0f) springs.close() else springs.spec()) }
                        }
                        if (mode == 5 && dy < -64.dp.toPx()) perform(prefs.swipeUpState.value)
                        if (mode == 7 && dy > 64.dp.toPx()) perform(prefs.swipeDownState.value)
                        if (mode == 0 && onEmpty) {
                            val now = System.currentTimeMillis()
                            if (now - lastTap < 320L) {
                                lastTap = 0L
                                perform(prefs.doubleTapState.value)
                            } else {
                                lastTap = now
                            }
                        }
                    }
                },
        ) {
            val glowColor = scheme.primary
            if (glowAlpha > 0f) {
                Canvas(Modifier.fillMaxSize()) {
                    val radius = 180.dp.toPx()
                    drawCircle(
                        Brush.radialGradient(
                            listOf(glowColor.copy(alpha = 0.34f * glowAlpha), glowColor.copy(alpha = 0.10f * glowAlpha), Color.Transparent),
                            center = glowPoint,
                            radius = radius,
                        ),
                        radius = radius,
                        center = glowPoint,
                    )
                }
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = maxOf(drawer.value, customise.value, control.value).coerceIn(0f, 1.1f)
                        alpha = (1f - p * 1.5f).coerceIn(0f, 1f)
                        val s = 1f - 0.07f * p
                        scaleX = s
                        scaleY = s
                        translationY = -p * 36.dp.toPx()
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = { pos ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            homeMenu = pos
                        })
                    }
                    .navigationBarsPadding()
                    .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.displayCutout.only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal)),
            ) {
                val statusTop = androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
                Box(Modifier.fillMaxWidth().height(maxOf(statusTop, 28.dp)), contentAlignment = Alignment.Center) {
                    if (prefs.ownTop && prefs.statusBarState.value) LumenStatusBar(prefs, ink, onNotices = { scope.launch { control.animateTo(1f, springs.spec()) } })
                }
                val configuration = androidx.compose.ui.platform.LocalConfiguration.current
                val wide = configuration.screenWidthDp > configuration.screenHeightDp && configuration.screenWidthDp >= 600
                if (wide) {
                    Row(Modifier.weight(1f)) {
                        ClockArea(prefs.clock, ink, Modifier.weight(0.42f).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(start = 28.dp, top = 8.dp, end = 12.dp), prefs)
                        Column(Modifier.weight(0.58f).fillMaxHeight()) {
                            WorkspaceGrid(catalog, prefs, ink, onLaunch = launch, onMenu = { app, bounds, id -> menu = Triple(app, bounds, id) }, onOpenFolder = { openFolder = it }, modifier = Modifier.weight(1f).padding(8.dp))
                            Island(catalog, prefs, onPlaced = { if (drawer.value == 0f && customise.value == 0f && control.value == 0f) islandRect = it }, onLaunch = launch, onMenu = { app, bounds -> menu = Triple(app, bounds, null) }, onSearch = { openDrawer(true) }, onCustomise = ::openCustomise, onOpenFolder = { openFolder = it })
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                } else {
                    ClockArea(prefs.clock, ink, Modifier.padding(start = 28.dp, top = 20.dp, end = 28.dp), prefs)
                    WorkspaceGrid(
                        catalog,
                        prefs,
                        ink,
                        onLaunch = launch,
                        onMenu = { app, bounds, id -> menu = Triple(app, bounds, id) },
                        onOpenFolder = { openFolder = it },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                }
                if (!wide) {
                    Island(catalog, prefs, onPlaced = { if (drawer.value == 0f && customise.value == 0f && control.value == 0f) islandRect = it }, onLaunch = launch, onMenu = { app, bounds -> menu = Triple(app, bounds, null) }, onSearch = { openDrawer(true) }, onCustomise = ::openCustomise, onOpenFolder = { openFolder = it })
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
        val drawerShown by remember { androidx.compose.runtime.derivedStateOf { drawer.value > 0.001f && (drawer.targetValue > 0f || drawer.value > 0.05f) } }
        val controlShown by remember { androidx.compose.runtime.derivedStateOf { control.value > 0.001f && (control.targetValue > 0f || control.value > 0.05f) } }
        val customiseShown by remember { androidx.compose.runtime.derivedStateOf { customise.value > 0.001f && (customise.targetValue > 0f || customise.value > 0.05f) } }
        var drawerReady by remember { mutableStateOf(false) }
        if (drawerShown) drawerReady = true
        if (drawerReady) {
            val statusTop = with(LocalDensity.current) { androidx.compose.foundation.layout.WindowInsets.Companion.statusBarsIgnoringVisibility.getTop(this).toFloat() + 10.dp.toPx() }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val v = drawer.value
                        val closed = v < 0.002f && drawer.targetValue == 0f
                        alpha = if (closed) 0f else 1f
                        translationY = if (closed) size.height * 4f else 0f
                        shape = RevealShape(islandRect, statusTop, v)
                        clip = true
                    },
            ) {
            DrawerSheet(
                catalog = catalog,
                prefs = prefs,
                progress = { drawer.value },
                query = query,
                onQuery = { query = it },
                focus = searchFocus,
                resets = resets + homePresses,
                onDrag = ::dragBy,
                onSettle = ::settle,
                onLaunch = launch,
                onMenu = { app, bounds -> menu = Triple(app, bounds, null) },
            )
            }
        }
        if (controlShown) {
            ControlCentre(
                device,
                { control.value },
                onClose = { scope.launch { control.animateTo(0f, springs.close()) } },
                onDrag = { dy -> scope.launch { control.snapTo((control.value - dy / (heightPx * 0.45f)).coerceIn(0f, 1.06f)) } },
                onSettle = { scope.launch { if (control.value > 0.6f) control.animateTo(1f, springs.spec()) else control.animateTo(0f, springs.close()) } },
            )
        }
        if (customiseShown) {
            CustomisePage(catalog, prefs, { customise.value }, isDefaultHome) { scope.launch { customise.animateTo(0f, springs.close()) } }
        }
        menu?.let { (app, bounds, homeId) -> AppMenu(app, bounds, homeId, catalog, prefs, onDismiss = { menu = null }) }
        homeMenu?.let { pos ->
            HomeMenu(
                pos,
                onWidget = { homeMenu = null; picker = true },
                onCustomise = { homeMenu = null; openCustomise() },
                onWallpaper = {
                    homeMenu = null
                    runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                },
                onDismiss = { homeMenu = null },
            )
        }
        openFolder?.let { id -> prefs.folders[id]?.let { f -> FolderSheet(f, catalog, prefs, onLaunch = launch, onDismiss = { openFolder = null }) } ?: run { openFolder = null } }
        if (picker) WidgetPicker(
            onPick = { info -> picker = false; onAddWidget(info) },
            onBuiltIn = { id ->
                picker = false
                if (!addBuiltIn(prefs, id)) android.widget.Toast.makeText(context, R.string.home_full, android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { picker = false },
        )
    }
}

@Composable
private fun Island(
    catalog: AppCatalog,
    prefs: LumenPrefs,
    onPlaced: (androidx.compose.ui.geometry.Rect) -> Unit,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onMenu: (AppEntry, Rect?) -> Unit,
    onSearch: () -> Unit,
    onCustomise: () -> Unit,
    onOpenFolder: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val dockKeys = prefs.dock.orEmpty()
    val apps = dockKeys.mapNotNull { key -> catalog.apps.firstOrNull { it.key == key } }
    val style = prefs.dockStyleState.value
    val shape = RoundedCornerShape(34.dp)
    val iconSize = (54f * (0.8f + prefs.iconScaleState.value * 0.4f)).dp
    val background = when (style) {
        DockStyle.Glass -> Brush.verticalGradient(listOf(scheme.surfaceContainerHighest.copy(alpha = 0.58f), scheme.surfaceContainer.copy(alpha = 0.74f)))
        DockStyle.Solid -> Brush.verticalGradient(listOf(scheme.surfaceContainerHigh, scheme.surfaceContainerHigh))
        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Column(
        Modifier
            .padding(horizontal = 12.dp)
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .androidx_onPlaced(onPlaced)
            .clip(shape)
            .background(background)
            .then(if (style == DockStyle.Glass) Modifier.border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.03f))), shape) else Modifier)
            .padding(start = 10.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
    ) {
        if (dockKeys.isNotEmpty() && style != DockStyle.Hidden) {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                dockKeys.forEach { key ->
                    val folder = if (key.startsWith(Folder.PREFIX)) prefs.folders[key.removePrefix(Folder.PREFIX)] else null
                    val app = if (folder == null) catalog.apps.firstOrNull { it.key == key } else null
                    when {
                        folder != null -> FolderIcon(folder, catalog, prefs, iconSize, false, Color.White, onOpen = { onOpenFolder(folder.id) })
                        app != null -> AppIcon(app, catalog, prefs, iconSize, showLabel = false, labelColor = Color.White, onLaunch = { onLaunch(app, it) }, onMenu = { onMenu(app, it) })
                    }
                }
            }
            if (prefs.dockSearchState.value) Spacer(Modifier.height(12.dp))
        }
        if (prefs.dockSearchState.value) {
        val floating = style == DockStyle.Clear || style == DockStyle.Hidden
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (floating) 58.dp else 48.dp)
                .clip(CircleShape)
                .background(if (floating) scheme.surfaceContainerHigh.copy(alpha = 0.95f) else scheme.surfaceContainerHighest.copy(alpha = 0.85f))
                .clickable(onClick = onSearch)
                .padding(start = 18.dp, end = 6.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(34.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(scheme.primary, scheme.tertiary, scheme.secondary, scheme.primary))),
            ) { GlyphIcon(Glyph.Search, scheme.onPrimary, 19.dp) }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.search), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            val voiceContext = LocalContext.current
            val hasLens = remember { voiceContext.packageManager.queryIntentActivities(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("googleapp://lens")), 0).isNotEmpty() }
            if (hasLens) Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(38.dp).clip(CircleShape).clickable(onClickLabel = stringResource(R.string.lens)) {
                    runCatching {
                        voiceContext.startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("googleapp://lens"))
                                .setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.search.lens.deeplink.LensDeeplink")
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }.onFailure { runCatching { voiceContext.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("googleapp://lens")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                },
            ) { GlyphIcon(Glyph.Lens, scheme.onSurfaceVariant, 22.dp) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(38.dp).clip(CircleShape).clickable(onClickLabel = stringResource(R.string.voice_search)) {
                    runCatching { voiceContext.startActivity(Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                        .onFailure { runCatching { voiceContext.startActivity(Intent(android.speech.RecognizerIntent.ACTION_WEB_SEARCH).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                },
            ) { GlyphIcon(Glyph.Mic, scheme.onSurfaceVariant, 22.dp) }
            Spacer(Modifier.width(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(PolygonShape(LumenShapes.softBurst))
                    .background(scheme.primaryContainer)
                    .clickable(onClick = onCustomise),
            ) {
                SparkGlyph(scheme.onPrimaryContainer)
            }
        }
        }
    }
    }
}

@Composable
private fun HomeMenu(at: Offset, onWidget: () -> Unit, onCustomise: () -> Unit, onWallpaper: () -> Unit, onDismiss: () -> Unit) {
    val springs = LocalSprings.current
    val density = LocalDensity.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val screen = LocalContext.current.resources.displayMetrics.widthPixels
    val width = with(density) { 260.dp.roundToPx() }
    val items = listOf(
        Triple(stringResource(R.string.add_widget), Glyph.Widgets, onWidget),
        Triple(stringResource(R.string.customise), Glyph.Spark, onCustomise),
        Triple(stringResource(R.string.settings_wallpaper), Glyph.Wallpaper, onWallpaper),
    )
    Popup(offset = IntOffset((at.x.toInt() - width / 2).coerceIn(24, screen - width - 24), (at.y.toInt() - with(density) { 120.dp.roundToPx() }).coerceAtLeast(48)), onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.width(260.dp)) {
            items.forEachIndexed { index, (label, glyph, action) ->
                val enter by animateFloatAsState(if (shown) 1f else 0f, androidx.compose.animation.core.spring(dampingRatio = springs.damping, stiffness = springs.stiffness * (1.2f - index * 0.15f)), label = "pill$index")
                FabPill(label, glyph, Modifier.graphicsLayer {
                    alpha = enter
                    translationY = (1f - enter) * 40.dp.toPx() * (index + 1)
                    scaleX = 0.8f + 0.2f * enter
                    scaleY = 0.8f + 0.2f * enter
                    transformOrigin = TransformOrigin(1f, 0.5f)
                }, emphasis = index == 0, onClick = action)
            }
            val turn by animateFloatAsState(if (shown) 0f else -90f, springs.spec(), label = "close")
            Box(Modifier.graphicsLayer { rotationZ = turn }) { FabClose(onDismiss) }
        }
    }
}

@Composable
fun SearchGlyph(color: Color) = GlyphIcon(Glyph.Search, color, 22.dp)

@Composable
fun SparkGlyph(color: Color) = GlyphIcon(Glyph.Spark, color, 20.dp)

@Composable
private fun DrawerSheet(
    catalog: AppCatalog,
    prefs: LumenPrefs,
    progress: () -> Float,
    query: String,
    onQuery: (String) -> Unit,
    focus: Boolean,
    resets: Int,
    onDrag: (Float) -> Unit,
    onSettle: (Float) -> Unit,
    onLaunch: (AppEntry, Rect?) -> Unit,
    onMenu: (AppEntry, Rect?) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var category by remember { mutableStateOf<AppCategory?>(null) }
    val grid = rememberLazyGridState()
    LaunchedEffect(resets) {
        category = null
        grid.scrollToItem(0)
    }
    val all = remember(catalog.apps, prefs.hidden, prefs.sortState.value, prefs.launchVersion) {
        val visible = catalog.apps.filter { it.key !in prefs.hidden }
        when (prefs.sortState.value) {
            DrawerSort.Alphabetical -> visible
            DrawerSort.MostUsed -> visible.sortedByDescending { prefs.launchCount(it.key) }
            DrawerSort.Newest -> visible.sortedByDescending { it.installed }
        }
    }
    val present = remember(all) { AppCategory.entries.filter { c -> all.any { it.category == c } } }
    val shown = remember(all, query, category) {
        if (query.isNotBlank()) rank(all, query) else all.filter { category == null || it.category == category }
    }
    val screenDp = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp
    val columns = if (prefs.columns <= 0) (screenDp / 84).coerceIn(4, 10) else prefs.columns
    val favourites = remember(all, prefs.launchVersion, columns) {
        if (!prefs.favouritesState.value) emptyList() else prefs.topLaunched(columns).mapNotNull { key -> all.firstOrNull { it.key == key } }
    }
    val browsing = query.isBlank() && category == null
    val headerItems = if (browsing && favourites.isNotEmpty()) 2 else 0
    LaunchedEffect(focus) {
        if (focus) {
            snapshotFlow { progress() >= 0.98f }.collect { open -> if (open) { focusRequester.requestFocus(); keyboard?.show() } }
        }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { progress() < 0.6f }.collect { closing -> if (closing) { focusManager.clearFocus(); keyboard?.hide() } }
    }
    val slop = LocalViewConfiguration.current.touchSlop
    LaunchedEffect(Unit) { snapshotFlow { progress() in 0.02f..0.5f }.collect { early -> if (early) grid.scrollToItem(0) } }
    val sheetShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
    val iconSize = ((when { columns <= 4 -> 60f; columns == 5 -> 54f; else -> 48f }) * (0.8f + prefs.iconScaleState.value * 0.4f)).dp
    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBarsIgnoringVisibility)
            .padding(top = 10.dp)
            .graphicsLayer {
                translationY = -(progress() - 1f).coerceAtLeast(0f) * 120.dp.toPx()
            }
            .padding(horizontal = 6.dp)
            .clip(sheetShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    if (down.position.x > size.width - 44.dp.toPx()) return@awaitEachGesture
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var taking = false
                    var total = 0f
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.positionChange()
                        tracker.addPosition(change.uptimeMillis, change.position)
                        if (!taking) {
                            total += delta.y
                            val atTop = grid.firstVisibleItemIndex == 0 && grid.firstVisibleItemScrollOffset == 0
                            if (total > slop && atTop) taking = true
                            if (total < -slop) break
                        }
                        if (taking) {
                            change.consume()
                            onDrag(delta.y)
                        }
                    }
                    if (taking) onSettle(tracker.calculateVelocity().y)
                }
            }
            .background(scheme.surfaceContainer.copy(alpha = 0.97f))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent)), sheetShape),
    ) {
        Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding().windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.displayCutout.only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal))) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dy -> change.consume(); onDrag(dy) },
                            onDragEnd = { onSettle(0f) },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(width = 40.dp, height = 5.dp).clip(CircleShape).background(scheme.onSurfaceVariant.copy(alpha = 0.5f)))
            }
            if (query.isBlank()) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 12.dp)) {
                    DisplayTitle(stringResource(R.string.apps_title), size = 38f, modifier = Modifier.weight(1f))
                    Text("${all.size}", style = flexStyle(18f, 700, 100, scheme.onSurfaceVariant), modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            if (query.isBlank() && prefs.categoriesState.value) {
                Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(horizontal = 16.dp)) {
                    ConnectedGroup(listOf<AppCategory?>(null) + present, category, { if (it == null) stringResource(R.string.category_all) else categoryLabel(it) }) { category = it }
                }
            }
            var focused by remember { mutableStateOf(false) }
            val imeVisible = androidx.compose.foundation.layout.WindowInsets.isImeVisible
            LaunchedEffect(imeVisible) { if (!imeVisible && focused) focusManager.clearFocus() }
            BackHandler(enabled = focused) { focusManager.clearFocus(); keyboard?.hide() }
            Box(Modifier.weight(1f).graphicsLayer { alpha = ((progress() - 0.25f) * 2f).coerceIn(0f, 1f) }) {
                if (all.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { androidx.compose.material3.LoadingIndicator() }
                } else if (query.isBlank() && focused && prefs.recents.isNotEmpty()) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.search_recent), style = flexStyle(22f, 850, 100, scheme.onSurface), modifier = Modifier.weight(1f))
                            FabPill(stringResource(R.string.clear_all), null) { prefs.clearRecents() }
                        }
                        Spacer(Modifier.height(10.dp))
                        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            prefs.recents.forEachIndexed { i, r -> Box(Modifier.stagger(i)) { FabPill(r, Glyph.Search) { onQuery(r) } } }
                        }
                    }
                } else if (query.isNotBlank()) {
                    SearchResults(query, shown, catalog, prefs, iconSize, onLaunch, onMenu, Modifier.fillMaxSize())
                } else
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = grid,
                    contentPadding = PaddingValues(start = 10.dp, end = 30.dp, top = 14.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (headerItems > 0) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "fav-title") {
                            Text(
                                stringResource(R.string.your_apps),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
                                color = scheme.primary,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        item(span = { GridItemSpan(maxLineSpan) }, key = "fav-row") {
                            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                                favourites.forEach { app ->
                                    AppIcon(app, catalog, prefs, iconSize, showLabel = prefs.drawerLabelsState.value, labelColor = scheme.onSurface, onLaunch = { onLaunch(app, it) }, onMenu = { onMenu(app, it) })
                                }
                            }
                        }
                    }
                    if (shown.isEmpty() && query.isNotBlank()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "web") {
                            Text(
                                stringResource(R.string.search_web, query),
                                style = MaterialTheme.typography.titleMedium,
                                color = scheme.primary,
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { webSearch(context, query) }.padding(16.dp),
                            )
                        }
                    }
                    itemsIndexed(shown, key = { _, app -> app.key }) { index, app ->
                        val row = index / columns - grid.firstVisibleItemIndex / columns
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .graphicsLayer {
                                    val local = ((progress().coerceAtMost(1f) * 1.6f) - row.coerceAtLeast(0) * 0.07f).coerceIn(0f, 1f)
                                    alpha = local
                                    translationY = (1f - local) * 36.dp.toPx()
                                },
                        ) {
                            AppIcon(app, catalog, prefs, iconSize, showLabel = prefs.drawerLabelsState.value, labelColor = scheme.onSurface, onLaunch = { onLaunch(app, it) }, onMenu = { onMenu(app, it) })
                        }
                    }
                }
                if (browsing && shown.size > 20) {
                    LetterRail(
                        letters = remember(shown) { shown.map { it.label.firstOrNull()?.uppercaseChar()?.let { c -> if (c.isLetter()) c else '#' } ?: '#' }.distinct() },
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 12.dp),
                    ) { letter ->
                        val index = shown.indexOfFirst { (it.label.firstOrNull()?.uppercaseChar()?.let { c -> if (c.isLetter()) c else '#' } ?: '#') == letter }
                        if (index >= 0) {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            scope.launch { grid.scrollToItem(index + headerItems) }
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHighest)
                    .clickable { focusRequester.requestFocus(); keyboard?.show() }
                    .padding(horizontal = 20.dp),
            ) {
                SearchGlyph(scheme.onSurfaceVariant)
                Spacer(Modifier.width(14.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(stringResource(R.string.search_apps), style = MaterialTheme.typography.bodyLarge, color = scheme.onSurfaceVariant)
                    BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = scheme.onSurface),
                        cursorBrush = SolidColor(scheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = {
                            val first = shown.firstOrNull()
                            if (first != null) onLaunch(first, null) else webSearch(context, query)
                        }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged { focused = it.isFocused },
                    )
                }
            }
        }
    }
}

@Composable
private fun LetterRail(letters: List<Char>, modifier: Modifier, onLetter: (Char) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val springs = LocalSprings.current
    var active by remember { mutableStateOf<Char?>(null) }
    var activeY by remember { mutableStateOf(0f) }
    val grow by animateFloatAsState(if (active != null) 1f else 0f, springs.spec(), label = "rail")
    Box(modifier.width(30.dp)) {
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHighest.copy(alpha = 0.35f + 0.55f * grow))
                .pointerInput(letters) {
                    fun pick(y: Float) {
                        val index = (y / size.height * letters.size).toInt().coerceIn(0, letters.lastIndex)
                        val letter = letters[index]
                        activeY = y
                        if (letter != active) {
                            active = letter
                            onLetter(letter)
                        }
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        pick(down.position.y)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            change.consume()
                            pick(change.position.y)
                        }
                        active = null
                    }
                },
        ) {
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
                val capacity = (maxHeight / 16.dp).toInt().coerceAtLeast(4)
                val step = ((letters.size + capacity - 1) / capacity).coerceAtLeast(1)
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly, horizontalAlignment = Alignment.CenterHorizontally) {
                    letters.filterIndexed { index, _ -> index % step == 0 }.forEach { letter ->
                        val on = letter == active || (active != null && step > 1 && letters.indexOf(active!!) / step == letters.indexOf(letter) / step)
                        Text(if (step > 1 && !on && letter != letters.first()) "•" else letter.toString(), style = flexStyle(if (on) 15f else 11f, if (on) 900 else 600, 100, if (on) scheme.primary else scheme.onSurfaceVariant))
                    }
                }
            }
        }
        if (grow > 0.01f) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset { IntOffset((-70).dp.roundToPx(), (activeY - 32.dp.toPx()).roundToInt()) }
                    .graphicsLayer {
                        scaleX = 0.6f + 0.4f * grow
                        scaleY = 0.6f + 0.4f * grow
                        alpha = grow
                        transformOrigin = TransformOrigin(1f, 0.5f)
                    }
                    .requiredSize(width = 88.dp, height = 64.dp)
                    .clip(RoundedCornerShape(topStart = 32.dp, bottomStart = 32.dp, topEnd = 32.dp, bottomEnd = 8.dp))
                    .background(scheme.primary),
            ) {
                Text((active ?: ' ').toString(), style = flexStyle(36f, 900, 100, scheme.onPrimary))
            }
        }
    }
}

class ProgressRef { var value = 0f }

private fun rank(apps: List<AppEntry>, query: String): List<AppEntry> = rankBy(apps, query) { it.label }

internal fun <T> rankBy(items: List<T>, query: String, label: (T) -> String): List<T> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return items
    fun score(text: String): Int {
        val l = text.lowercase()
        if (l.startsWith(q)) return 0
        val words = l.split(' ', '-', '.', '_').filter { it.isNotEmpty() }
        if (words.any { it.startsWith(q) }) return 1
        if (words.joinToString("") { it.first().toString() }.startsWith(q)) return 2
        if (l.contains(q)) return 3
        var i = 0
        for (ch in l) if (i < q.length && ch == q[i]) i++
        return if (i == q.length) 4 else Int.MAX_VALUE
    }
    return items.map { it to score(label(it)) }.filter { it.second != Int.MAX_VALUE }.sortedBy { it.second }.map { it.first }
}

private fun webSearch(context: android.content.Context, query: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
private fun categoryLabel(category: AppCategory): String = stringResource(
    when (category) {
        AppCategory.Social -> R.string.category_social
        AppCategory.Media -> R.string.category_media
        AppCategory.Games -> R.string.category_games
        AppCategory.Work -> R.string.category_work
        AppCategory.Travel -> R.string.category_travel
        AppCategory.Other -> R.string.category_other
    },
)

@Composable
private fun AppMenu(app: AppEntry, bounds: Rect?, homeId: String?, catalog: AppCatalog, prefs: LumenPrefs, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val springs = LocalSprings.current
    val context = LocalContext.current
    val density = LocalDensity.current
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val shortcuts = remember(app.key) { catalog.shortcuts(app).take(4) }
    val docked = prefs.dock.orEmpty().contains(app.key)
    val dockFull = prefs.dock.orEmpty().size >= AppCatalog.DOCK_SIZE
    val menuWidth = with(density) { 300.dp.roundToPx() }
    val screenWidth = context.resources.displayMetrics.widthPixels
    val screenHeight = context.resources.displayMetrics.heightPixels
    val anchor = bounds ?: Rect(screenWidth / 2, screenHeight / 2, screenWidth / 2, screenHeight / 2)
    val x = (anchor.centerX() - menuWidth / 2).coerceIn(with(density) { 12.dp.roundToPx() }, screenWidth - menuWidth - with(density) { 12.dp.roundToPx() })
    val menuHeight = with(density) { (150 + shortcuts.size * 66).dp.roundToPx() }
    val below = anchor.bottom + with(density) { 10.dp.roundToPx() }
    val above = below + menuHeight > screenHeight
    val y = if (above) (anchor.top - menuHeight - with(density) { 10.dp.roundToPx() }).coerceAtLeast(24) else below
    val actions = buildList {
        add(ToolbarAction(Glyph.Info, context.getString(R.string.app_info)) { catalog.openInfo(app, bounds); onDismiss() })
        if (homeId != null) {
            add(ToolbarAction(Glyph.Home, context.getString(R.string.remove_from_home), active = true) { prefs.updateHome(prefs.home.filter { it.id != homeId }); onDismiss() })
            val item = prefs.home.firstOrNull { it.id == homeId }
            val pages = prefs.pagesState.value
            if (item != null && pages > 1) {
                if (item.page < pages - 1) add(ToolbarAction(Glyph.Next, context.getString(R.string.move_next_page)) { moveToPage(prefs, item, item.page + 1, context); onDismiss() })
                if (item.page > 0) add(ToolbarAction(Glyph.Back, context.getString(R.string.move_previous_page)) { moveToPage(prefs, item, item.page - 1, context); onDismiss() })
            }
        } else {
            add(ToolbarAction(Glyph.Home, context.getString(R.string.add_to_home)) {
                val spot = freeSpot(prefs.home, prefs.homeColumnsState.value, prefs.homeRowsState.value, 1, 1)
                if (spot == null) Toast.makeText(context, R.string.home_full, Toast.LENGTH_SHORT).show()
                else prefs.updateHome(prefs.home + HomeItem("a" + System.nanoTime(), app.key, -1, spot.first, spot.second, 1, 1, HomePager.page))
                onDismiss()
            })
        }
        add(ToolbarAction(Glyph.Dock, context.getString(if (docked) R.string.remove_from_dock else R.string.add_to_dock), active = docked) {
            val current = prefs.dock.orEmpty()
            when {
                docked -> prefs.updateDock(current - app.key)
                dockFull -> Toast.makeText(context, R.string.dock_full, Toast.LENGTH_SHORT).show()
                else -> prefs.updateDock(current + app.key)
            }
            onDismiss()
        })
        add(ToolbarAction(Glyph.Hide, context.getString(if (app.key in prefs.hidden) R.string.unhide_app else R.string.hide_app), active = app.key in prefs.hidden) { prefs.toggleHidden(app.key); onDismiss() })
        add(ToolbarAction(Glyph.Delete, context.getString(R.string.uninstall), danger = true) { catalog.uninstall(app); onDismiss() })
    }
    Popup(offset = IntOffset(x, y), onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.width(300.dp),
        ) {
            val parts = @Composable {
                Text(app.label, style = flexStyle(20f, 800, 100, scheme.onSurface), modifier = Modifier.clip(CircleShape).background(scheme.surfaceContainerHigh).padding(horizontal = 18.dp, vertical = 8.dp))
                shortcuts.forEachIndexed { index, shortcut ->
                    val enter by animateFloatAsState(if (shown) 1f else 0f, androidx.compose.animation.core.spring(dampingRatio = springs.damping, stiffness = springs.stiffness * (1.3f - index * 0.12f)), label = "sc$index")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .graphicsLayer { alpha = enter; translationY = (1f - enter) * (if (above) 24.dp.toPx() else -24.dp.toPx()) }
                            .fillMaxWidth()
                            .height(58.dp)
                            .clip(CircleShape)
                            .background(if (index == 0) scheme.primaryContainer else scheme.secondaryContainer)
                            .clickable { catalog.startShortcut(shortcut, bounds); onDismiss() }
                            .padding(start = 8.dp, end = 20.dp),
                    ) {
                        val icon = remember(shortcut.id) { catalog.shortcutIcon(shortcut) }
                        Box(Modifier.size(42.dp).clip(CircleShape).background(scheme.surface.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                            if (icon != null) androidx.compose.foundation.Image(icon, contentDescription = null, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text((shortcut.shortLabel ?: shortcut.longLabel ?: "").toString(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = if (index == 0) scheme.onPrimaryContainer else scheme.onSecondaryContainer, maxLines = 1)
                    }
                }
                val bar by animateFloatAsState(if (shown) 1f else 0.6f, springs.spec(), label = "bar")
                FloatingToolbar(actions, Modifier.graphicsLayer { scaleX = bar; scaleY = bar; alpha = ((bar - 0.6f) / 0.4f).coerceIn(0f, 1f) })
            }
            parts()
        }
    }
}

@Composable
private fun MenuItem(text: String, color: Color = MaterialTheme.colorScheme.onSurface, glyph: Glyph? = null, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        if (glyph != null) {
            GlyphIcon(glyph, color.copy(alpha = 0.85f), 20.dp)
            Spacer(Modifier.width(14.dp))
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

fun Modifier.androidx_onPlaced(onPlaced: (androidx.compose.ui.geometry.Rect) -> Unit): Modifier =
    this.then(Modifier.onGloballyPositioned { onPlaced(it.boundsInRoot()) })

class RevealShape(private val from: androidx.compose.ui.geometry.Rect, private val top: Float, private val progress: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val t = progress.coerceIn(0f, 1f)
        val e = t * t * (3f - 2f * t)
        val start = if (from.width > 0f) from else androidx.compose.ui.geometry.Rect(0f, size.height * 0.8f, size.width, size.height)
        val inset = with(density) { 6.dp.toPx() }
        fun lerp(a: Float, b: Float) = a + (b - a) * e
        val rect = androidx.compose.ui.geometry.Rect(lerp(start.left, inset), lerp(start.top, top), lerp(start.right, size.width - inset), lerp(start.bottom, size.height))
        val radius = with(density) { lerp(34.dp.toPx(), 36.dp.toPx()) }
        val bottomRadius = with(density) { lerp(34.dp.toPx(), 0f) }
        return androidx.compose.ui.graphics.Outline.Rounded(
            androidx.compose.ui.geometry.RoundRect(
                rect,
                topLeft = androidx.compose.ui.geometry.CornerRadius(radius),
                topRight = androidx.compose.ui.geometry.CornerRadius(radius),
                bottomRight = androidx.compose.ui.geometry.CornerRadius(bottomRadius),
                bottomLeft = androidx.compose.ui.geometry.CornerRadius(bottomRadius),
            ),
        )
    }
}

private fun moveToPage(prefs: LumenPrefs, item: HomeItem, page: Int, context: android.content.Context) {
    val spot = freeSpot(prefs.home, prefs.homeColumnsState.value, prefs.homeRowsState.value, item.w, item.h, item.id, page)
    if (spot == null) {
        Toast.makeText(context, R.string.home_full, Toast.LENGTH_SHORT).show()
        return
    }
    prefs.updateHome(prefs.home.map { if (it.id == item.id) HomeItem(it.id, it.app, it.widget, spot.first, spot.second, it.w, it.h, page) else it })
}
