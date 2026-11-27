package com.omarchylauncher.app.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
    pinnedApps: List<AppEntry>,
    dockApps: List<AppEntry>,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    homeResetSignal: Int,
    externalDragActive: Boolean = false,
    onDockTopReported: (Float) -> Unit = {},
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
    onSavePinnedOrder: (List<String>) -> Unit,
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
    onLongPressApp: (AppEntry) -> Unit
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val topDropLimitPx = with(density) { (contentPadding.calculateTopPadding() + 86.dp).toPx() }
    var dockTopY by remember { mutableFloatStateOf(Float.MAX_VALUE) }
    var activeDragRootPos by remember { mutableStateOf(Offset.Zero) }
    var activeDragIsSystem by remember { mutableStateOf(false) }
    var activeDragApp by remember { mutableStateOf<AppEntry?>(null) }

    var showHomeMenu by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    var widgetToManage by remember { mutableStateOf<Int?>(null) }
    val gridState = rememberLazyGridState()

    var draggingPkg by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var totalDragDist by remember { mutableFloatStateOf(0f) }
    var swapCount by remember { mutableIntStateOf(0) }
    var lastSwapTime by remember { mutableLongStateOf(0L) }

    val localPinned = remember { mutableStateListOf<AppEntry>() }
    LaunchedEffect(pinnedApps, draggingPkg) {
        if (draggingPkg == null) {
            localPinned.clear()
            localPinned.addAll(pinnedApps)
        }
    }

    var draggingDockPkg by remember { mutableStateOf<String?>(null) }
    var dockDragX by remember { mutableFloatStateOf(0f) }
    var dockTotalDist by remember { mutableFloatStateOf(0f) }
    var dockSwapCount by remember { mutableIntStateOf(0) }
    val localDock = remember { mutableStateListOf<AppEntry>() }
    LaunchedEffect(dockApps, draggingDockPkg) {
        if (draggingDockPkg == null) {
            localDock.clear()
            localDock.addAll(dockApps)
        }
    }

    val isAnyDragActive = draggingPkg != null || draggingDockPkg != null || externalDragActive
    val workspaceScale by animateFloatAsState(
        targetValue = if (isAnyDragActive) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "wsScale"
    )
    val workspaceOffsetYPx by animateFloatAsState(
        targetValue = if (isAnyDragActive) with(density) { 46.dp.toPx() } else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "wsOffsetY"
    )

    val hasWidgetsPage = settings.widgetIds.isNotEmpty() || settings.extraPagesCount > 0
    val totalPages = 1 + maxOf(settings.extraPagesCount, if (settings.widgetIds.isNotEmpty()) 1 else 0)
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalPages })

    LaunchedEffect(homeResetSignal) {
        if (homeResetSignal > 0 && pagerState.currentPage != 0) {
            pagerState.animateScrollToPage(0)
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
                        showHomeMenu = true
                    }
                )
            }
            .pointerInput(settings.swipeDownNotifications, settings.autoFocusDrawerSearch) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var totalDx = 0f
                    var totalDy = 0f
                    var lastPos = down.position
                    var triggered = false

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val delta = change.position - lastPos
                        lastPos = change.position
                        totalDx += delta.x
                        totalDy += delta.y

                        if (draggingPkg == null && !triggered && abs(totalDy) > 125f && abs(totalDy) > abs(totalDx) * 2.2f) {
                            if (totalDy < 0f && !gridState.canScrollForward) {
                                triggered = true
                                onOpenDrawer(settings.autoFocusDrawerSearch)
                            } else if (totalDy > 0f && settings.swipeDownNotifications && !gridState.canScrollBackward) {
                                triggered = true
                                onExpandNotifications()
                            }
                        }
                    }
                }
            }
            .padding(
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 14.dp,
                start = 16.dp,
                end = 16.dp
            )
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
                if (page == 0) {
                    // Default Home Screen
                    Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = workspaceScale
                    scaleY = workspaceScale
                    translationY = workspaceOffsetYPx
                }
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
                            if (localPinned.isNotEmpty()) {
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = GridCells.Fixed(settings.gridColumns),
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    itemsIndexed(localPinned, key = { _, app -> "${app.packageName}/${app.activityName}" }) { index, app ->
                                        val isDragging = draggingPkg == app.packageName
                                        var itemRootPos by remember { mutableStateOf(Offset.Zero) }
                                        Box(
                                            modifier = Modifier
                                                .then(if (!isDragging) Modifier.animateItem() else Modifier)
                                                .onGloballyPositioned { coords ->
                                                    itemRootPos = coords.positionInRoot()
                                                }
                                                .graphicsLayer {
                                                    alpha = if (isDragging) 0f else 1f
                                                }
                                                .pointerInput(app.packageName) {
                                                    detectDragGesturesAfterLongPress(
                                                        onDragStart = { offset ->
                                                            draggingPkg = app.packageName
                                                            activeDragApp = app
                                                            dragOffset = Offset.Zero
                                                            totalDragDist = 0f
                                                            swapCount = 0
                                                            lastSwapTime = 0L
                                                            activeDragIsSystem = app.isSystem
                                                            activeDragRootPos = itemRootPos + offset
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffset += dragAmount
                                                            totalDragDist += dragAmount.getDistance()
                                                            activeDragRootPos += dragAmount

                                                            val currentIdx = localPinned.indexOfFirst { it.packageName == draggingPkg }
                                                            if (currentIdx < 0) return@detectDragGesturesAfterLongPress
                                                            val currentItem = gridState.layoutInfo.visibleItemsInfo
                                                                .firstOrNull { it.index == currentIdx } ?: return@detectDragGesturesAfterLongPress

                                                            val now = System.currentTimeMillis()
                                                            if (now - lastSwapTime < 120L) return@detectDragGesturesAfterLongPress

                                                            val centerX = currentItem.offset.x + currentItem.size.width / 2f + dragOffset.x
                                                            val centerY = currentItem.offset.y + currentItem.size.height / 2f + dragOffset.y

                                                            val closest = gridState.layoutInfo.visibleItemsInfo
                                                                .filter { it.index in localPinned.indices }
                                                                .minByOrNull { item ->
                                                                    val ix = item.offset.x + item.size.width / 2f
                                                                    val iy = item.offset.y + item.size.height / 2f
                                                                    hypot(centerX - ix, centerY - iy)
                                                                }
                                                            if (closest != null && closest.index != currentIdx) {
                                                                val dist = hypot(
                                                                    centerX - (closest.offset.x + closest.size.width / 2f),
                                                                    centerY - (closest.offset.y + closest.size.height / 2f)
                                                                )
                                                                if (dist < currentItem.size.width * 0.8f) {
                                                                    val deltaX = (currentItem.offset.x - closest.offset.x).toFloat()
                                                                    val deltaY = (currentItem.offset.y - closest.offset.y).toFloat()
                                                                    val moved = localPinned.removeAt(currentIdx)
                                                                    localPinned.add(closest.index, moved)
                                                                    dragOffset = Offset(dragOffset.x + deltaX, dragOffset.y + deltaY)
                                                                    swapCount++
                                                                    lastSwapTime = now
                                                                }
                                                            }
                                                        },
                                                        onDragEnd = {
                                                            val droppedY = activeDragRootPos.y
                                                            val droppedX = activeDragRootPos.x
                                                            when {
                                                                swapCount == 0 && totalDragDist < 14f -> {
                                                                    onLongPressApp(app)
                                                                }
                                                                droppedY < topDropLimitPx -> {
                                                                    if (!app.isSystem && droppedX >= screenWidthPx / 2f) {
                                                                        onUninstallApp(app.packageName)
                                                                    } else {
                                                                        onRemoveHomeApp(app.packageName)
                                                                    }
                                                                }
                                                                settings.showDock && droppedY >= dockTopY - 32f -> {
                                                                    onMoveHomeToDock(app.packageName)
                                                                }
                                                                swapCount > 0 -> {
                                                                    onSavePinnedOrder(localPinned.map { it.packageName })
                                                                }
                                                            }
                                                            draggingPkg = null
                                                            activeDragApp = null
                                                            dragOffset = Offset.Zero
                                                            totalDragDist = 0f
                                                            swapCount = 0
                                                        },
                                                        onDragCancel = {
                                                            draggingPkg = null
                                                            activeDragApp = null
                                                            dragOffset = Offset.Zero
                                                            totalDragDist = 0f
                                                            swapCount = 0
                                                        }
                                                    )
                                                }
                                        ) {
                                            KaruiAppTile(
                                                app = app,
                                                showLabel = settings.showHomeLabels,
                                                monochrome = settings.monochromeIcons,
                                                hapticsEnabled = settings.hapticFeedback,
                                                iconShape = settings.iconShape,
                                                iconSize = settings.iconSizeDp.dp,
                                                onClick = { onLaunchApp(app) },
                                                onLongClick = null
                                            )
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
                                                    }
                                                },
                                                update = { view ->
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

            if (hasWidgetsPage && totalPages > 1) {
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

            if (settings.showDock && localDock.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                val dockHighlighted = draggingPkg != null && activeDragRootPos.y >= dockTopY - 32f
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            dockTopY = coords.boundsInRoot().top
                            onDockTopReported(dockTopY)
                        }
                        .clip(karuiShape())
                        .background(palette.surface)
                        .border(2.dp, if (dockHighlighted) palette.accent else palette.border, karuiShape())
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    localDock.forEach { app ->
                        val isDockDragging = draggingDockPkg == app.packageName
                        var dockItemRoot by remember { mutableStateOf(Offset.Zero) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .zIndex(if (isDockDragging) 10f else 0f)
                                .onGloballyPositioned { coords ->
                                    dockItemRoot = coords.positionInRoot()
                                }
                                .graphicsLayer {
                                    alpha = if (isDockDragging) 0f else 1f
                                }
                                .pointerInput(app.packageName) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            draggingDockPkg = app.packageName
                                            activeDragApp = app
                                            dockDragX = 0f
                                            dockTotalDist = 0f
                                            dockSwapCount = 0
                                            activeDragIsSystem = app.isSystem
                                            activeDragRootPos = dockItemRoot + offset
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dockDragX += dragAmount.x
                                            dockTotalDist += dragAmount.getDistance()
                                            activeDragRootPos += dragAmount
                                            val curIdx = localDock.indexOfFirst { it.packageName == draggingDockPkg }
                                            val cellW = size.width.toFloat().coerceAtLeast(1f)
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
                                            val droppedY = activeDragRootPos.y
                                            val droppedX = activeDragRootPos.x
                                            when {
                                                dockSwapCount == 0 && dockTotalDist < 14f -> {
                                                    onLongPressApp(app)
                                                }
                                                droppedY < topDropLimitPx -> {
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
                                            dockTotalDist = 0f
                                            dockSwapCount = 0
                                        },
                                        onDragCancel = {
                                            draggingDockPkg = null
                                            activeDragApp = null
                                            dockDragX = 0f
                                            dockTotalDist = 0f
                                            dockSwapCount = 0
                                        }
                                    )
                                }
                        ) {
                            KaruiAppTile(
                                app = app,
                                showLabel = false,
                                monochrome = settings.monochromeIcons,
                                hapticsEnabled = settings.hapticFeedback,
                                iconShape = settings.iconShape,
                                iconSize = settings.iconSizeDp.dp,
                                onClick = { onLaunchApp(app) },
                                onLongClick = null
                            )
                        }
                    }
                }
            }
        }

        // Top Action Drop Bar when dragging Home/Dock icon (Remove / Uninstall)
        AnimatedVisibility(
            visible = draggingPkg != null || draggingDockPkg != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            val inTopZone = activeDragRootPos.y < topDropLimitPx
            val hoverRemove = inTopZone && (activeDragIsSystem || activeDragRootPos.x < screenWidthPx / 2f)
            val hoverUninstall = inTopZone && !activeDragIsSystem && activeDragRootPos.x >= screenWidthPx / 2f

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

                if (!activeDragIsSystem) {
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

        // Unclipped Floating Drag Overlay for Home & Dock icons
        activeDragApp?.let { dragged ->
            val ox = with(density) { (activeDragRootPos.x - 36.dp.toPx()).toDp() }
            val oy = with(density) { (activeDragRootPos.y - 56.dp.toPx()).toDp() }
            Box(
                modifier = Modifier
                    .offset(x = ox, y = oy)
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
                KaruiSubtleButton("Add Home Page", { showHomeMenu = false; onAddPage() }, Modifier.fillMaxWidth())
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
                KaruiSubtleButton("Open App Drawer", { showHomeMenu = false; onOpenDrawer(false) }, Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                KaruiSubtleButton("System Wallpaper", { showHomeMenu = false; onOpenWallpaper() }, Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                KaruiSubtleButton("Lock Screen", { showHomeMenu = false; onLockScreen() }, Modifier.fillMaxWidth())
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
