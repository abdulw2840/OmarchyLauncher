package com.omarchylauncher.app.ui.screens

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.omarchylauncher.app.data.AppEntry
import com.omarchylauncher.app.data.LauncherSettings
import com.omarchylauncher.app.ui.ClockState
import com.omarchylauncher.app.ui.components.KaruiAppTile
import com.omarchylauncher.app.ui.components.KaruiBox
import com.omarchylauncher.app.ui.components.KaruiButton
import com.omarchylauncher.app.ui.components.KaruiDashedDivider
import com.omarchylauncher.app.ui.components.KaruiSubtleButton
import com.omarchylauncher.app.ui.theme.LocalKaruiPalette
import com.omarchylauncher.app.ui.theme.LocalUiScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeWorkspaceScreen(
    settings: LauncherSettings,
    clockState: ClockState,
    isWeatherRefreshing: Boolean,
    pinnedApps: List<AppEntry>,
    dockApps: List<AppEntry>,
    appWidgetHost: AppWidgetHost,
    appWidgetManager: AppWidgetManager,
    contentPadding: PaddingValues,
    onOpenDrawer: (focusSearch: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onAddWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
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

    var showHomeMenu by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
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

                        if (!triggered && abs(totalDy) > 110f && abs(totalDy) > abs(totalDx) * 1.4f) {
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
        Column(modifier = Modifier.fillMaxSize()) {
            // At a Glance + Weather (No GPS, No [USER] chip)
            if (settings.showAtAGlance) {
                KaruiBox(title = "omarchy // status", contentPadding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = timeFormat.format(Date(clockState.timeMillis)),
                                color = palette.accent,
                                fontFamily = FontFamily.Monospace,
                                fontSize = (32 * scale).sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = (34 * scale).sp
                            )
                            Text(
                                text = "${dateFormat.format(Date(clockState.timeMillis)).uppercase()}  •  BAT ${clockState.batteryPercent}%",
                                color = palette.text,
                                fontFamily = FontFamily.Monospace,
                                fontSize = (12 * scale).sp
                            )
                        }
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
                                .background(palette.surface)
                                .border(1.dp, palette.border, RectangleShape)
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

            // Pinned Home Grid + Widgets
            Box(modifier = Modifier.weight(1f)) {
                if (pinnedApps.isEmpty() && settings.widgetIds.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "[ EMPTY WORKSPACE ]",
                            color = palette.gray,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = (13 * scale).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Swipe up for drawer • Long-press for menu",
                            color = palette.gray,
                            fontSize = (12 * scale).sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(settings.gridColumns),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Widgets section
                        items(
                            items = settings.widgetIds,
                            key = { "widget_$it" },
                            span = { GridItemSpan(settings.gridColumns) }
                        ) { widgetId ->
                            val info = remember(widgetId) {
                                runCatching { appWidgetManager.getAppWidgetInfo(widgetId) }.getOrNull()
                            }
                            if (info != null) {
                                val minH = (info.minHeight.coerceIn(72, 260)).dp
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.surface)
                                        .border(2.dp, palette.border, RectangleShape)
                                        .padding(4.dp)
                                ) {
                                    AndroidView(
                                        factory = {
                                            appWidgetHost.createView(context, widgetId, info).apply {
                                                setAppWidget(widgetId, info)
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(minH)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(22.dp)
                                            .background(palette.bg)
                                            .border(1.dp, palette.border, RectangleShape)
                                            .clickable { onRemoveWidget(widgetId) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Remove Widget",
                                            tint = palette.accent,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Pinned Apps
                        items(pinnedApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                            KaruiAppTile(
                                app = app,
                                showLabel = settings.showHomeLabels,
                                monochrome = settings.monochromeIcons,
                                hapticsEnabled = settings.hapticFeedback,
                                iconShape = settings.iconShape,
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) }
                            )
                        }
                    }
                }
            }

            // Clean Search Bar
            if (settings.showSearchBar) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .border(2.dp, palette.border, RectangleShape)
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

            // Dock / Hotseat
            if (settings.showDock && dockApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface)
                        .border(2.dp, palette.border, RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dockApps.forEach { app ->
                        KaruiAppTile(
                            app = app,
                            showLabel = false,
                            monochrome = settings.monochromeIcons,
                            hapticsEnabled = settings.hapticFeedback,
                            iconShape = settings.iconShape,
                            iconSize = 40.dp,
                            onClick = { onLaunchApp(app) },
                            onLongClick = { onLongPressApp(app) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showHomeMenu) {
        Dialog(onDismissRequest = { showHomeMenu = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(2.dp, palette.border, RectangleShape)
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

                KaruiSubtleButton(
                    text = "Omarchy Settings & Themes",
                    onClick = { showHomeMenu = false; onOpenSettings() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                KaruiSubtleButton(
                    text = "Add Widget",
                    onClick = { showHomeMenu = false; onAddWidget() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                KaruiSubtleButton(
                    text = "Open App Drawer",
                    onClick = { showHomeMenu = false; onOpenDrawer(false) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                KaruiSubtleButton(
                    text = "System Wallpaper",
                    onClick = { showHomeMenu = false; onOpenWallpaper() },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                KaruiSubtleButton(
                    text = "Lock Screen",
                    onClick = { showHomeMenu = false; onLockScreen() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showCityDialog) {
        var cityInput by remember { mutableStateOf(settings.weatherLocation) }
        Dialog(onDismissRequest = { showCityDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(2.dp, palette.border, RectangleShape)
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
                        .background(palette.bg)
                        .border(2.dp, palette.border, RectangleShape)
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
