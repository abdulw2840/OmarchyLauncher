package com.omarchylauncher.app.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.omarchylauncher.app.data.AppEntry
import com.omarchylauncher.app.data.LauncherSettings
import com.omarchylauncher.app.ui.ClockState
import com.omarchylauncher.app.ui.components.KaruiAppTile
import com.omarchylauncher.app.ui.components.KaruiBox
import com.omarchylauncher.app.ui.components.KaruiButton
import com.omarchylauncher.app.ui.components.KaruiChip
import com.omarchylauncher.app.ui.components.KaruiDashedDivider
import com.omarchylauncher.app.ui.components.KaruiSubtleButton
import com.omarchylauncher.app.ui.components.OmarchyAppWidgetHostView
import com.omarchylauncher.app.ui.components.karuiAppHoldOrDrag
import com.omarchylauncher.app.ui.components.karuiShape
import com.omarchylauncher.app.ui.theme.LocalKaruiPalette
import com.omarchylauncher.app.ui.theme.LocalUiScale
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeWorkspaceScreen(
    settings: LauncherSettings,
    clockState: ClockState,
    isWeatherRefreshing: Boolean,
    pinnedSlots: List<AppEntry?>,
    dockApps: List<AppEntry>,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    homeResetSignal: Int,
    externalDragActive: Boolean = false,
    externalDragPos: Offset = Offset.Zero,
    externalDragIsSystem: Boolean = false,
    externalDragPkg: String? = null,
    onDockTopReported: (Float) -> Unit = {},
    onRegisterDropSlotResolver: ((Offset) -> Int?) -> Unit = {},
    onDrawerDragFraction: (Float) -> Unit = {},
    onDrawerDragSettle: (Boolean, Boolean) -> Unit = { _, _ -> },
    contentPadding: PaddingValues,
    onOpenDrawer: (focusSearch: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onResizeWidget: (Int, Int) -> Unit,
    onSetWidgetHalfWidth: (Int, Boolean) -> Unit,
    onReorderWidgets: (List<Int>) -> Unit,
    onAddPage: () -> Unit,
    onRemovePage: () -> Unit,
    onSavePinnedSlots: (List<String?>) -> Unit,
    onSaveDockOrder: (List<String>) -> Unit,
    onMoveHomeToDock: (String) -> Unit,
    onRemoveHomeApp: (String) -> Unit,
    onRemoveDockApp: (String) -> Unit,
    onUninstallApp: (String) -> Unit,
    onLockScreen: () -> Unit,
    onExpandNotifications: () -> Unit,
    onRefreshWeather: () -> Unit,
    onSetWeatherCity: (String) -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onDismissLongPressApp: () -> Unit
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val topDropLimitPx = with(density) { (contentPadding.calculateTopPadding() + 86.dp).toPx() }
    var homeOverlayCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val latestPointerInOverlay = remember { java.util.concurrent.atomic.AtomicReference(Offset.Zero) }
    var gridCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var dockTopY by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var dockLeftX by remember { mutableFloatStateOf(0f) }
    var dockWidthPx by remember { mutableFloatStateOf(screenWidthPx) }
    var activeDragOverlayPos by remember { mutableStateOf(Offset.Zero) }
    var activeDragIsSystem by remember { mutableStateOf(false) }
    var activeDragApp by remember { mutableStateOf<AppEntry?>(null) }

    var showHomeMenu by remember { mutableStateOf(false) }
    var activeIconInteraction by remember { mutableStateOf(false) }
    var isPageEditMode by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    var widgetToManage by remember { mutableStateOf<Int?>(null) }
    val gridState = rememberLazyGridState()

    var draggingPkg by remember { mutableStateOf<String?>(null) }
    var swapCount by remember { mutableIntStateOf(0) }
    var lastSwapTime by remember { mutableLongStateOf(0L) }

    val totalGridSlots = (settings.gridRows * settings.gridColumns).coerceAtLeast(pinnedSlots.size)
    val localSlots = remember { mutableStateListOf<AppEntry?>() }
    var gridDragSnapshot by remember { mutableStateOf<List<AppEntry?>?>(null) }
    LaunchedEffect(pinnedSlots, draggingPkg, totalGridSlots) {
        if (draggingPkg == null) {
            localSlots.clear()
            for (i in 0 until totalGridSlots) {
                localSlots.add(pinnedSlots.getOrNull(i))
            }
        }
    }

    var draggingDockPkg by remember { mutableStateOf<String?>(null) }
    var dockDragX by remember { mutableFloatStateOf(0f) }
    var lastDockPointerX by remember { mutableFloatStateOf(0f) }
    var dockSwapCount by remember { mutableIntStateOf(0) }
    val localDock = remember { mutableStateListOf<AppEntry>() }
    var dockDragSnapshot by remember { mutableStateOf<List<AppEntry>?>(null) }
    LaunchedEffect(dockApps, draggingDockPkg) {
        if (draggingDockPkg == null) {
            localDock.clear()
            localDock.addAll(dockApps)
        }
    }

    val isAnyDragActive = draggingPkg != null || draggingDockPkg != null || externalDragActive
    val isZoomedOut = isAnyDragActive || isPageEditMode
    val workspaceScale by animateFloatAsState(
        targetValue = when {
            isPageEditMode -> 0.80f
            isAnyDragActive -> 0.88f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "wsScale"
    )
    val workspaceOffsetYPx by animateFloatAsState(
        targetValue = if (isZoomedOut) with(density) { 46.dp.toPx() } else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "wsOffsetY"
    )

    val basePages = 1 + maxOf(settings.extraPagesCount, if (settings.widgetIds.isNotEmpty()) 1 else 0)
    val totalPages = if (isZoomedOut) basePages + 1 else basePages
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })

    LaunchedEffect(homeResetSignal) {
        if (homeResetSignal > 0) {
            isPageEditMode = false
            if (pagerState.currentPage != 0) {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    // [EXP-DRAWER-DROP-SLOT] Expose exact closest empty grid slot resolver for Drawer -> Home drops
    SideEffect {
        onRegisterDropSlotResolver { rootPos ->
            val gCoords = gridCoords ?: return@onRegisterDropSlotResolver null
            val localInGrid = gCoords.windowToLocal(rootPos)
            val visible = gridState.layoutInfo.visibleItemsInfo
            val closestEmpty = visible
                .filter { it.index in localSlots.indices && localSlots[it.index] == null }
                .minByOrNull { item ->
                    val ix = item.offset.x + item.size.width / 2f
                    val iy = item.offset.y + item.size.height / 2f
                    hypot(localInGrid.x - ix, localInGrid.y - iy)
                }
            closestEmpty?.index ?: visible.minByOrNull { item ->
                val ix = item.offset.x + item.size.width / 2f
                val iy = item.offset.y + item.size.height / 2f
                hypot(localInGrid.x - ix, localInGrid.y - iy)
            }?.index
        }
    }

    val is24h = DateFormat.is24HourFormat(context)
    val timeFormat = remember(is24h) {
        SimpleDateFormat(if (is24h) "HH:mm" else "h:mm a", Locale.getDefault())
    }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM dd", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(settings.doubleTapToSleep) {
                detectTapGestures(
                    onDoubleTap = {
                        if (settings.doubleTapToSleep) onLockScreen()
                    },
                    onLongPress = {
                        if (!activeIconInteraction && !isAnyDragActive) {
                            showHomeMenu = true
                        }
                    }
                )
            }
            // [EXP-DRAWER-SWIPE] Gradual finger-driven swipe-up to reveal App Drawer + swipe-down notifications
            .pointerInput(settings.swipeDownNotifications, settings.autoFocusDrawerSearch) {
                val touchSlop = viewConfiguration.touchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var totalDx = 0f
                    var totalDy = 0f
                    var lastDy = 0f
                    var lastPos = down.position
                    var triggered = false
                    var pullingDrawer = false
                    var lastFrac = 0f

                    try {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (pullingDrawer) {
                                    val open = lastDy < -4f || lastFrac > 0.22f
                                    onDrawerDragSettle(open, open && settings.autoFocusDrawerSearch)
                                }
                                break
                            }

                            val delta = change.position - lastPos
                            lastPos = change.position
                            lastDy = delta.y
                            totalDx += delta.x
                            totalDy += delta.y

                            if (!activeIconInteraction && draggingPkg == null && !isAnyDragActive && !pullingDrawer && !triggered) {
                                if (totalDy < -touchSlop * 2f && abs(totalDy) > abs(totalDx) * 1.4f && !gridState.canScrollForward) {
                                    pullingDrawer = true
                                } else if (totalDy > 125f && abs(totalDy) > abs(totalDx) * 2.0f && settings.swipeDownNotifications && !gridState.canScrollBackward) {
                                    triggered = true
                                    onExpandNotifications()
                                }
                            }

                            if (pullingDrawer) {
                                if (activeIconInteraction || draggingPkg != null) {
                                    pullingDrawer = false
                                    onDrawerDragFraction(0f)
                                } else {
                                    change.consume()
                                    val frac = ((-totalDy - touchSlop * 2f) / (screenHeightPx * 0.36f)).coerceIn(0f, 1f)
                                    lastFrac = frac
                                    onDrawerDragFraction(frac)
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        if (pullingDrawer) onDrawerDragSettle(false, false)
                    }
                }
            }
            .padding(
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 14.dp,
                start = 16.dp,
                end = 16.dp
            )
            .onGloballyPositioned { coords ->
                homeOverlayCoords = coords
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    latestPointerInOverlay.set(down.position)
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val ch = event.changes.firstOrNull { it.id == down.id } ?: break
                        latestPointerInOverlay.set(ch.position)
                        if (!ch.pressed) break
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = workspaceScale
                    scaleY = workspaceScale
                    translationY = workspaceOffsetYPx
                }
        ) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 24.dp,
                beyondViewportPageCount = 1,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageDottedModifier = if (isZoomedOut && page < basePages) {
                    val borderColor = palette.border
                    val radiusPx = with(density) { settings.cornerRadiusDp.dp.toPx() }
                    Modifier
                        .drawBehind {
                            drawRoundRect(
                                color = borderColor,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                                ),
                                cornerRadius = CornerRadius(radiusPx, radiusPx)
                            )
                        }
                        .padding(8.dp)
                } else {
                    Modifier
                }

                Box(modifier = Modifier.fillMaxSize().then(pageDottedModifier)) {
                    if (page == basePages && isZoomedOut) {
                        // Trailing "+ ADD NEW PAGE" card in zoom-out mode
                        val borderColor = palette.accent
                        val radiusPx = with(density) { settings.cornerRadiusDp.dp.toPx() }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    drawRoundRect(
                                        color = borderColor,
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                                        ),
                                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                                    )
                                }
                                .clickable {
                                    onAddPage()
                                    scope.launch { pagerState.animateScrollToPage(basePages) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add New Page",
                                    tint = palette.accent,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "ADD NEW PAGE",
                                    color = palette.accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = (13 * scale).sp
                                )
                            }
                        }
                    } else if (page == 0) {
                    // Default Home Screen
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (settings.showAtAGlance) {
                            KaruiBox(title = "omarchy // status", contentPadding = 12.dp) {
                                Text(
                                    text = timeFormat.format(Date(clockState.timeMillis)),
                                    color = palette.accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (30 * scale).sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = (32 * scale).sp
                                )
                                Text(
                                    text = "${dateFormat.format(Date(clockState.timeMillis)).uppercase()}  •  BAT ${clockState.batteryPercent}%",
                                    color = palette.text,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (12 * scale).sp
                                )

                                if (settings.showScreenTime || settings.showSteps) {
                                    val parts = buildList {
                                        if (settings.showScreenTime) {
                                            val st = clockState.screenTimeText.ifEmpty { "--" }
                                            add("SCREEN $st")
                                        }
                                        if (settings.showSteps) {
                                            val steps = if (clockState.stepsCount >= 0) "${clockState.stepsCount}" else "--"
                                            add("STEPS $steps")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = parts.joinToString("  •  "),
                                        color = palette.gray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (11 * scale).sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                KaruiDashedDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showCityDialog = true },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.WbSunny,
                                            contentDescription = "Weather",
                                            tint = palette.accent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val weatherLabel = when {
                                            isWeatherRefreshing -> "Updating weather..."
                                            settings.weatherLocation.isBlank() -> "Tap to set weather city"
                                            settings.weatherTempText.isNotBlank() ->
                                                "${settings.weatherTempText}  •  ${settings.weatherDescText}"
                                            else -> "${settings.weatherLocation} (tap refresh)"
                                        }
                                        Text(
                                            text = weatherLabel,
                                            color = if (settings.weatherLocation.isBlank()) palette.gray else palette.text,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = (12 * scale).sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(karuiShape())
                                            .background(palette.surface)
                                            .border(1.dp, palette.border, karuiShape())
                                            .clickable {
                                                if (settings.weatherLocation.isBlank()) showCityDialog = true
                                                else onRefreshWeather()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = "Refresh Weather",
                                            tint = palette.accent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(settings.gridColumns),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onGloballyPositioned { coords -> gridCoords = coords },
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(
                                    localSlots,
                                    key = { idx, app -> if (app != null) "${app.packageName}/${app.activityName}" else "empty_$idx" }
                                ) { index, app ->
                                    if (app == null) {
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height((settings.iconSizeDp + if (settings.showHomeLabels) 26 else 10).dp)
                                        )
                                    } else {
                                        val isDragging = draggingPkg == app.packageName
                                        Box(
                                            modifier = Modifier
                                                .animateItem()
                                                .graphicsLayer { alpha = if (isDragging) 0f else 1f }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .karuiAppHoldOrDrag(
                                                        key = app.packageName,
                                                        haptic = if (settings.hapticFeedback) haptic else null,
                                                        onInteractionActiveChange = { activeIconInteraction = it },
                                                        onTap = { onLaunchApp(app) },
                                                    onShowMenu = { onLongPressApp(app) },
                                                    onDismissMenu = onDismissLongPressApp,
                                                    onDragStart = {
                                                        gridDragSnapshot = localSlots.toList()
                                                        draggingPkg = app.packageName
                                                        activeDragApp = app
                                                        swapCount = 0
                                                        lastSwapTime = 0L
                                                        activeDragIsSystem = app.isSystem
                                                        activeDragOverlayPos = latestPointerInOverlay.get()
                                                    },
                                                    onDrag = {
                                                        activeDragOverlayPos = latestPointerInOverlay.get()
                                                        val gCoords = gridCoords ?: return@karuiAppHoldOrDrag
                                                        val hCoords = homeOverlayCoords ?: return@karuiAppHoldOrDrag
                                                        val currentIdx = localSlots.indexOfFirst { it?.packageName == draggingPkg }
                                                        if (currentIdx < 0) return@karuiAppHoldOrDrag
                                                        val now = System.currentTimeMillis()
                                                        if (now - lastSwapTime < 110L) return@karuiAppHoldOrDrag

                                                        val rootPos = hCoords.localToRoot(latestPointerInOverlay.get())
                                                        val localInGrid = gCoords.windowToLocal(rootPos)
                                                        val visible = gridState.layoutInfo.visibleItemsInfo
                                                        val closest = visible
                                                            .filter { it.index in localSlots.indices }
                                                            .minByOrNull { item ->
                                                                val ix = item.offset.x + item.size.width / 2f
                                                                val iy = item.offset.y + item.size.height / 2f
                                                                hypot(localInGrid.x - ix, localInGrid.y - iy)
                                                            }
                                                        if (closest != null && closest.index != currentIdx) {
                                                            val dist = hypot(
                                                                localInGrid.x - (closest.offset.x + closest.size.width / 2f),
                                                                localInGrid.y - (closest.offset.y + closest.size.height / 2f)
                                                            )
                                                            if (dist < closest.size.width * 0.78f) {
                                                                val moved = localSlots[currentIdx]
                                                                localSlots[currentIdx] = localSlots[closest.index]
                                                                localSlots[closest.index] = moved
                                                                swapCount++
                                                                lastSwapTime = now
                                                            }
                                                        }
                                                    },
                                                    onDragEnd = {
                                                        val rootPos = homeOverlayCoords?.localToRoot(activeDragOverlayPos) ?: activeDragOverlayPos
                                                        val droppedY = rootPos.y
                                                        val droppedX = rootPos.x
                                                        when {
                                                            droppedY < topDropLimitPx -> {
                                                                // [EXP-OPTIMISTIC-REMOVE] Clear slot immediately so icon doesn't flash back before DataStore updates
                                                                val idx = localSlots.indexOfFirst { it?.packageName == app.packageName }
                                                                if (idx >= 0) localSlots[idx] = null
                                                                if (!app.isSystem && droppedX >= screenWidthPx / 2f) {
                                                                    onUninstallApp(app.packageName)
                                                                } else {
                                                                    onRemoveHomeApp(app.packageName)
                                                                }
                                                            }
                                                            settings.showDock && droppedY >= dockTopY - 32f -> {
                                                                // [EXP-OPTIMISTIC-REMOVE] Clear slot immediately before DataStore moves item to dock
                                                                val idx = localSlots.indexOfFirst { it?.packageName == app.packageName }
                                                                if (idx >= 0) localSlots[idx] = null
                                                                onMoveHomeToDock(app.packageName)
                                                            }
                                                            swapCount > 0 -> {
                                                                onSavePinnedSlots(localSlots.map { it?.packageName })
                                                            }
                                                        }
                                                        draggingPkg = null
                                                        activeDragApp = null
                                                        swapCount = 0
                                                    },
                                                    onDragCancel = {
                                                        draggingPkg = null
                                                        activeDragApp = null
                                                        swapCount = 0
                                                        gridDragSnapshot = null
                                                    },
                                                    onDragCancel = {
                                                        gridDragSnapshot?.let { snapshot ->
                                                            localSlots.clear()
                                                            localSlots.addAll(snapshot)
                                                        }
                                                        gridDragSnapshot = null
                                                        draggingPkg = null
                                                        activeDragApp = null
                                                        swapCount = 0
                                                    }
                                                )
                                            ) {
                                                KaruiAppTile(
                                                    app = app,
                                                    showLabel = settings.showHomeLabels,
                                                    monochrome = settings.monochromeIcons,
                                                    hapticsEnabled = settings.hapticFeedback,
                                                    iconShape = settings.iconShape,
                                                    iconSize = settings.iconSizeDp.dp,
                                                    onClick = null,
                                                    onLongClick = null
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Extra Widget Page(s) — Omarchy Tiling WM (2-column tiling layout)
                    Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = workspaceScale
                    scaleY = workspaceScale
                    translationY = workspaceOffsetYPx
                }
        ) {
                        val pageWidgets = if (page == 1) settings.widgetIds else emptyList()
                        if (pageWidgets.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = pageWidgets,
                                    key = { "widget_$it" },
                                    span = { id ->
                                        GridItemSpan(if (settings.widgetHalfWidth.contains(id)) 1 else 2)
                                    }
                                ) { widgetId ->
                                    val info = remember(widgetId) {
                                        runCatching { appWidgetManager.getAppWidgetInfo(widgetId) }.getOrNull()
                                    }
                                    val isHalf = settings.widgetHalfWidth.contains(widgetId)
                                    val customHeight = settings.widgetHeights[widgetId]
                                        ?: (info?.minHeight?.coerceIn(48, 260) ?: 100)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem()
                                            .clip(karuiShape())
                                            .border(1.dp, palette.border, karuiShape())
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = { widgetToManage = widgetId }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (info != null) {
                                            AndroidView(
                                                factory = {
                                                    appWidgetHost.createView(context, widgetId, info).apply {
                                                        setAppWidget(widgetId, info)
                                                        // [EXP-WIDGET-LONGPRESS] Long-press anywhere inside widget opens customization while normal taps work
                                                        (this as? OmarchyAppWidgetHostView)?.onLongPress = { widgetToManage = widgetId }
                                                    }
                                                },
                                                update = { view ->
                                                    (view as? OmarchyAppWidgetHostView)?.onLongPress = { widgetToManage = widgetId }
                                                    val wDp = if (isHalf) 170 else 360
                                                    val opts = Bundle().apply {
                                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, wDp)
                                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, wDp)
                                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, customHeight)
                                                        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, customHeight)
                                                    }
                                                    runCatching { view.updateAppWidgetOptions(opts) }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(customHeight.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "Widget unavailable (long-press to remove)",
                                                color = palette.gray,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }

            if (totalPages > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalPages) { i ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(width = if (i == pagerState.currentPage) 16.dp else 6.dp, height = 4.dp)
                                .background(if (i == pagerState.currentPage) palette.accent else palette.gray)
                        )
                    }
                }
            }

            if (settings.showSearchBar) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(karuiShape())
                        .background(palette.surface)
                        .border(2.dp, palette.border, karuiShape())
                        .clickable { onOpenDrawer(true) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ">_",
                            color = palette.accent,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = (14 * scale).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "search",
                            color = palette.gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (13 * scale).sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search",
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Rounded.Apps,
                            contentDescription = "App Drawer",
                            tint = palette.text,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onOpenDrawer(false) }
                        )
                    }
                }
            }

            // [EXP-DOCK-ANIM] Smooth animated BoxWithConstraints dock with wiggle & live hover slot spacing
            if (settings.showDock && (localDock.isNotEmpty() || externalDragActive || draggingPkg != null)) {
                Spacer(modifier = Modifier.height(10.dp))
                val dragRootPos = if (externalDragActive) externalDragPos else (homeOverlayCoords?.localToRoot(activeDragOverlayPos) ?: activeDragOverlayPos)
                val dockHighlighted = (draggingPkg != null || externalDragActive) && dragRootPos.y >= dockTopY - 32f
                val incomingPkg = if (externalDragActive) externalDragPkg else draggingPkg
                val isHoveringNewForDock = dockHighlighted && incomingPkg != null && localDock.none { it.packageName == incomingPkg } && localDock.size < 6
                val displaySlotCount = (localDock.size + if (isHoveringNewForDock) 1 else 0).coerceAtLeast(1)
                val hoverSlot = if (isHoveringNewForDock && dockWidthPx > 0f) {
                    (((dragRootPos.x - dockLeftX) / dockWidthPx) * displaySlotCount).toInt().coerceIn(0, localDock.size)
                } else null

                val wiggleTransition = rememberInfiniteTransition(label = "dockWiggle")
                val wiggleDeg by wiggleTransition.animateFloat(
                    initialValue = -2.6f,
                    targetValue = 2.6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(130, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dockWiggleDeg"
                )
                val dockBorderScale by animateFloatAsState(
                    targetValue = if (dockHighlighted) 1.02f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "dockScale"
                )
                val dockTileH = (settings.iconSizeDp * scale + 20).dp

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dockTileH)
                        .graphicsLayer { scaleX = dockBorderScale; scaleY = dockBorderScale }
                        .onGloballyPositioned { coords ->
                            val b = coords.boundsInRoot()
                            dockTopY = b.top
                            dockLeftX = b.left
                            dockWidthPx = b.width
                            onDockTopReported(dockTopY)
                        }
                        .clip(karuiShape())
                        .background(if (dockHighlighted) palette.accent.copy(alpha = 0.14f) else palette.surface)
                        .border(2.dp, if (dockHighlighted) palette.accent else palette.border, karuiShape())
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    val slotW = maxWidth / displaySlotCount

                    if (hoverSlot != null) {
                        val indicatorX by animateDpAsState(
                            targetValue = slotW * hoverSlot,
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium),
                            label = "hoverSlotX"
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorX)
                                .width(slotW)
                                .height(dockTileH - 12.dp)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .clip(karuiShape())
                                .border(2.dp, palette.accent, karuiShape())
                                .background(palette.accent.copy(alpha = 0.12f))
                        )
                    }

                    localDock.forEachIndexed { rawIdx, app ->
                        key(app.packageName) {
                            val visIdx = if (hoverSlot != null && rawIdx >= hoverSlot) rawIdx + 1 else rawIdx
                            val animX by animateDpAsState(
                                targetValue = slotW * visIdx,
                                animationSpec = spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMedium),
                                label = "dockSlotX_${app.packageName}"
                            )
                            val isDockDragging = draggingDockPkg == app.packageName
                            val shouldWiggle = (dockHighlighted || (draggingDockPkg != null && !isDockDragging))
                            val iconPopScale by animateFloatAsState(
                                targetValue = if (dockHighlighted) 0.94f else 1f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "dockPop_${app.packageName}"
                            )
                            var dockItemCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
                            Box(
                                modifier = Modifier
                                    .offset(x = animX)
                                    .width(slotW)
                                    .height(dockTileH - 12.dp)
                                    .zIndex(if (isDockDragging) 10f else 0f)
                                    .onGloballyPositioned { coords -> dockItemCoords = coords }
                                    .graphicsLayer {
                                        alpha = if (isDockDragging) 0f else 1f
                                        rotationZ = if (shouldWiggle) wiggleDeg else 0f
                                        scaleX = iconPopScale
                                        scaleY = iconPopScale
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .karuiAppHoldOrDrag(
                                            key = app.packageName,
                                            haptic = if (settings.hapticFeedback) haptic else null,
                                            onInteractionActiveChange = { activeIconInteraction = it },
                                            onTap = { onLaunchApp(app) },
                                            onShowMenu = { onLongPressApp(app) },
                                            onDismissMenu = onDismissLongPressApp,
                                            onDragStart = {
                                                dockDragSnapshot = localDock.toList()
                                                draggingDockPkg = app.packageName
                                                activeDragApp = app
                                                dockDragX = 0f
                                                lastDockPointerX = latestPointerInOverlay.get().x
                                                dockSwapCount = 0
                                                activeDragIsSystem = app.isSystem
                                                activeDragOverlayPos = latestPointerInOverlay.get()
                                            },
                                            onDrag = {
                                                activeDragOverlayPos = latestPointerInOverlay.get()
                                                dockDragX += (latestPointerInOverlay.get().x - lastDockPointerX)
                                                lastDockPointerX = latestPointerInOverlay.get().x
                                                val curIdx = localDock.indexOfFirst { it.packageName == draggingDockPkg }
                                                val cellW = (dockItemCoords?.size?.width?.toFloat() ?: 120f).coerceAtLeast(1f)
                                                if (curIdx >= 0) {
                                                    if (dockDragX > cellW * 0.65f && curIdx < localDock.lastIndex) {
                                                        val item = localDock.removeAt(curIdx)
                                                        localDock.add(curIdx + 1, item)
                                                        dockDragX -= cellW
                                                        dockSwapCount++
                                                    } else if (dockDragX < -cellW * 0.65f && curIdx > 0) {
                                                        val item = localDock.removeAt(curIdx)
                                                        localDock.add(curIdx - 1, item)
                                                        dockDragX += cellW
                                                        dockSwapCount++
                                                    }
                                                }
                                            },
                                            onDragEnd = {
                                                val rootPos = homeOverlayCoords?.localToRoot(activeDragOverlayPos) ?: activeDragOverlayPos
                                                val droppedY = rootPos.y
                                                val droppedX = rootPos.x
                                                when {
                                                    droppedY < topDropLimitPx -> {
                                                        // [EXP-OPTIMISTIC-REMOVE] Remove from dock immediately so icon doesn't flash back
                                                        localDock.removeAll { it.packageName == app.packageName }
                                                        if (!app.isSystem && droppedX >= screenWidthPx / 2f) {
                                                            onUninstallApp(app.packageName)
                                                        } else {
                                                            onRemoveDockApp(app.packageName)
                                                        }
                                                    }
                                                    dockSwapCount > 0 -> {
                                                        onSaveDockOrder(localDock.map { it.packageName })
                                                    }
                                                }
                                                draggingDockPkg = null
                                                activeDragApp = null
                                                dockDragX = 0f
                                                dockSwapCount = 0
                                                dockDragSnapshot = null
                                            },
                                            onDragCancel = {
                                                dockDragSnapshot?.let { snapshot ->
                                                    localDock.clear()
                                                    localDock.addAll(snapshot)
                                                }
                                                dockDragSnapshot = null
                                                draggingDockPkg = null
                                                activeDragApp = null
                                                dockDragX = 0f
                                                dockSwapCount = 0
                                            }
                                        )
                                ) {
                                    KaruiAppTile(
                                        app = app,
                                        showLabel = false,
                                        monochrome = settings.monochromeIcons,
                                        hapticsEnabled = settings.hapticFeedback,
                                        iconShape = settings.iconShape,
                                        iconSize = settings.iconSizeDp.dp,
                                        onClick = null,
                                        onLongClick = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Top Action Drop Bar when dragging Home/Dock/Drawer icon (Remove / Uninstall)
        AnimatedVisibility(
            visible = isAnyDragActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val dropPos = if (externalDragActive) externalDragPos else (homeOverlayCoords?.localToRoot(activeDragOverlayPos) ?: activeDragOverlayPos)
            val dropIsSys = if (externalDragActive) externalDragIsSystem else activeDragIsSystem
            val inTopZone = dropPos.y < topDropLimitPx
            val hoverRemove = inTopZone && (dropIsSys || dropPos.x < screenWidthPx / 2f)
            val hoverUninstall = inTopZone && !dropIsSys && dropPos.x >= screenWidthPx / 2f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(karuiShape())
                        .background(if (hoverRemove) palette.accent else palette.surface)
                        .border(2.dp, palette.border, karuiShape())
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove",
                        tint = if (hoverRemove) palette.accentTxt else palette.text,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "REMOVE",
                        color = if (hoverRemove) palette.accentTxt else palette.text,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (!dropIsSys) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(karuiShape())
                            .background(if (hoverUninstall) Color(0xFFEF5350) else palette.surface)
                            .border(2.dp, Color(0xFFEF5350), karuiShape())
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Uninstall",
                            tint = if (hoverUninstall) Color.White else Color(0xFFEF5350),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "UNINSTALL",
                            color = if (hoverUninstall) Color.White else Color(0xFFEF5350),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Page Edit Mode Top Bar
        AnimatedVisibility(
            visible = isPageEditMode && !isAnyDragActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PAGES ($basePages)",
                    color = palette.accent,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                if (pagerState.currentPage in 1 until basePages) {
                    KaruiSubtleButton(
                        text = "DELETE PAGE",
                        onClick = {
                            if (pagerState.currentPage == 1) {
                                settings.widgetIds.forEach { onRemoveWidget(it) }
                            }
                            scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
                            onRemovePage()
                        }
                    )
                }
                KaruiButton(
                    text = "DONE",
                    onClick = {
                        isPageEditMode = false
                        if (pagerState.currentPage >= basePages) {
                            scope.launch { pagerState.animateScrollToPage((basePages - 1).coerceAtLeast(0)) }
                        }
                    }
                )
            }
        }

        // Unclipped Floating Drag Overlay for Home & Dock icons
        activeDragApp?.let { dragged ->
            val tileW = (settings.iconSizeDp * scale + 12).dp
            val halfTilePx = with(density) { tileW.toPx() / 2f }
            val ox = with(density) { (activeDragOverlayPos.x - halfTilePx).toDp() }
            val oy = with(density) { (activeDragOverlayPos.y - halfTilePx).toDp() }
            Box(
                modifier = Modifier
                    .offset(x = ox, y = oy)
                    .width(tileW)
                    .zIndex(50f)
                    .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
            ) {
                KaruiAppTile(
                    app = dragged,
                    showLabel = false,
                    monochrome = settings.monochromeIcons,
                    hapticsEnabled = false,
                    iconShape = settings.iconShape,
                    iconSize = settings.iconSizeDp.dp,
                    onClick = {},
                    onLongClick = null
                )
            }
        }
    }

    if (showHomeMenu) {
        Dialog(onDismissRequest = { showHomeMenu = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(karuiShape())
                    .background(palette.surface)
                    .border(2.dp, palette.border, karuiShape())
                    .padding(14.dp)
            ) {
                Text(
                    text = "WORKSPACE MENU",
                    color = palette.accent,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (14 * scale).sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                KaruiDashedDivider()
                Spacer(modifier = Modifier.height(10.dp))

                KaruiSubtleButton("Omarchy Settings & Themes", { showHomeMenu = false; onOpenSettings() }, Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                KaruiSubtleButton("Add Widget", { showHomeMenu = false; onAddWidget() }, Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                KaruiSubtleButton("Edit Pages (Zoom Out)", { showHomeMenu = false; isPageEditMode = true }, Modifier.fillMaxWidth())
                if (pagerState.currentPage > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    KaruiSubtleButton("Delete This Page", {
                        showHomeMenu = false
                        if (pagerState.currentPage == 1) {
                            settings.widgetIds.forEach { onRemoveWidget(it) }
                        }
                        scope.launch { pagerState.animateScrollToPage(0) }
                        onRemovePage()
                    }, Modifier.fillMaxWidth())
                }
                Spacer(modifier = Modifier.height(8.dp))
                KaruiSubtleButton("System Wallpaper", { showHomeMenu = false; onOpenWallpaper() }, Modifier.fillMaxWidth())
            }
        }
    }

    widgetToManage?.let { wId ->
        val curHeight = settings.widgetHeights[wId] ?: 110
        val isHalf = settings.widgetHalfWidth.contains(wId)
        val wIdx = settings.widgetIds.indexOf(wId)
        Dialog(onDismissRequest = { widgetToManage = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(karuiShape())
                    .background(palette.surface)
                    .border(2.dp, palette.border, karuiShape())
                    .padding(14.dp)
            ) {
                Text(
                    text = "WIDGET TILING (${curHeight}dp)",
                    color = palette.accent,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (14 * scale).sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("TILE WIDTH", color = palette.gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    KaruiChip("Half (1/2)", isHalf, { onSetWidgetHalfWidth(wId, true) }, Modifier.weight(1f))
                    KaruiChip("Full (2/2)", !isHalf, { onSetWidgetHalfWidth(wId, false) }, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("TILE HEIGHT", color = palette.gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("S" to 64, "M" to 110, "L" to 180, "XL" to 260).forEach { (lbl, h) ->
                        KaruiChip(lbl, curHeight == h, { onResizeWidget(wId, h) }, Modifier.weight(1f))
                    }
                }
                if (settings.widgetIds.size > 1 && wIdx >= 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("ORDER", color = palette.gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        KaruiSubtleButton("↑ Move Up", {
                            if (wIdx > 0) {
                                val list = settings.widgetIds.toMutableList()
                                val item = list.removeAt(wIdx)
                                list.add(wIdx - 1, item)
                                onReorderWidgets(list)
                            }
                        }, Modifier.weight(1f))
                        KaruiSubtleButton("↓ Move Down", {
                            if (wIdx < settings.widgetIds.lastIndex) {
                                val list = settings.widgetIds.toMutableList()
                                val item = list.removeAt(wIdx)
                                list.add(wIdx + 1, item)
                                onReorderWidgets(list)
                            }
                        }, Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KaruiSubtleButton("Close", { widgetToManage = null }, Modifier.weight(1f))
                    KaruiButton("Remove", {
                        onRemoveWidget(wId)
                        widgetToManage = null
                    }, Modifier.weight(1f))
                }
            }
        }

    }

    if (showCityDialog) {
        var cityInput by remember { mutableStateOf(settings.weatherLocation) }
        Dialog(onDismissRequest = { showCityDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(karuiShape())
                    .background(palette.surface)
                    .border(2.dp, palette.border, karuiShape())
                    .padding(14.dp)
            ) {
                Text(
                    text = "WEATHER CITY (NO GPS)",
                    color = palette.accent,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (14 * scale).sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(karuiShape())
                        .background(palette.bg)
                        .border(2.dp, palette.border, karuiShape())
                        .padding(10.dp)
                ) {
                    if (cityInput.isEmpty()) {
                        Text("e.g. Tokyo, London, Mumbai", color = palette.gray, fontSize = 13.sp)
                    }
                    BasicTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it },
                        singleLine = true,
                        textStyle = TextStyle(color = palette.text, fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KaruiSubtleButton("Cancel", { showCityDialog = false }, Modifier.weight(1f))
                    KaruiButton("Save & Fetch", {
                        onSetWeatherCity(cityInput)
                        showCityDialog = false
                    }, Modifier.weight(1f))
                }
            }
        }
    }
}
