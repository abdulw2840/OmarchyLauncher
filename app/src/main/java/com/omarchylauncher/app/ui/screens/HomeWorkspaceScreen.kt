package com.omarchylauncher.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omarchylauncher.app.data.AppEntry
import com.omarchylauncher.app.data.LauncherSettings
import com.omarchylauncher.app.privilege.PrivilegeStatus
import com.omarchylauncher.app.ui.ClockState
import com.omarchylauncher.app.ui.components.KaruiAppTile
import com.omarchylauncher.app.ui.components.KaruiBox
import com.omarchylauncher.app.ui.components.KaruiDashedDivider
import com.omarchylauncher.app.ui.components.KaruiSubtleButton
import com.omarchylauncher.app.ui.theme.LocalKaruiPalette
import com.omarchylauncher.app.ui.theme.LocalUiScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeWorkspaceScreen(
    settings: LauncherSettings,
    clockState: ClockState,
    privilegeStatus: PrivilegeStatus,
    pinnedApps: List<AppEntry>,
    dockApps: List<AppEntry>,
    contentPadding: PaddingValues,
    onOpenDrawer: (focusSearch: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onLockScreen: () -> Unit,
    onExpandNotifications: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    var showHomeMenu by remember { mutableStateOf(false) }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM dd", Locale.getDefault()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(settings.doubleTapToSleep, settings.swipeDownNotifications) {
                detectTapGestures(
                    onDoubleTap = {
                        if (settings.doubleTapToSleep) onLockScreen()
                    },
                    onLongPress = {
                        showHomeMenu = true
                    }
                )
            }
            .pointerInput(settings.swipeDownNotifications) {
                detectVerticalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        dragAccumulator += dragAmount
                    },
                    onDragEnd = {
                        when {
                            dragAccumulator < -90f -> onOpenDrawer(settings.autoFocusDrawerSearch)
                            dragAccumulator > 90f && settings.swipeDownNotifications -> onExpandNotifications()
                        }
                        dragAccumulator = 0f
                    }
                )
            }
            .padding(
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 14.dp,
                start = 16.dp,
                end = 16.dp
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // At a Glance (Omarchy + Pixel style)
            if (settings.showAtAGlance) {
                KaruiBox(title = "omarchy // status", contentPadding = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
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

                        Column(horizontalAlignment = Alignment.End) {
                            val privLabel = when {
                                privilegeStatus.isRootAvailable -> "ROOT"
                                privilegeStatus.isShizukuGranted -> "SHIZUKU"
                                else -> "USER"
                            }
                            Box(
                                modifier = Modifier
                                    .background(palette.surface)
                                    .border(1.dp, palette.border, RectangleShape)
                                    .clickable { onOpenSettings() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "[$privLabel]",
                                        color = palette.accent,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = (11 * scale).sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings",
                                        tint = palette.text,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Pinned Home Grid
            Box(modifier = Modifier.weight(1f)) {
                if (pinnedApps.isEmpty()) {
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
                            text = "Swipe up for drawer • Long-press to configure",
                            color = palette.gray,
                            fontSize = (12 * scale).sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings.gridColumns),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(pinnedApps, key = { it.packageName }) { app ->
                            KaruiAppTile(
                                app = app,
                                showLabel = settings.showHomeLabels,
                                monochrome = settings.monochromeIcons,
                                hapticsEnabled = settings.hapticFeedback,
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) }
                            )
                        }
                    }
                }
            }

            // Pixel-style Search Bar (Omarchy Terminal Prompt)
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
                            text = "search apps, =calc, :cmd...",
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
}
