package com.omarchylauncher.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omarchylauncher.app.data.LauncherSettings
import com.omarchylauncher.app.privilege.PrivilegeStatus
import com.omarchylauncher.app.ui.components.KaruiBox
import com.omarchylauncher.app.ui.components.KaruiButton
import com.omarchylauncher.app.ui.components.KaruiChip
import com.omarchylauncher.app.ui.components.KaruiDashedDivider
import com.omarchylauncher.app.ui.components.KaruiPivotHeader
import com.omarchylauncher.app.ui.components.KaruiSubtleButton
import com.omarchylauncher.app.ui.components.KaruiSwitch
import com.omarchylauncher.app.ui.theme.KaruiThemes
import com.omarchylauncher.app.ui.theme.LocalKaruiPalette
import com.omarchylauncher.app.ui.theme.LocalUiScale
import kotlin.math.abs

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    privilegeStatus: PrivilegeStatus,
    contentPadding: PaddingValues,
    onRequestDefaultHome: () -> Unit,
    onRefreshPrivileges: () -> Unit,
    onRequestShizuku: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onSetDoubleTapSleep: (Boolean) -> Unit,
    onSetSwipeDownNotif: (Boolean) -> Unit,
    onSetHaptic: (Boolean) -> Unit,
    onSetGridColumns: (Int) -> Unit,
    onSetShowAtAGlance: (Boolean) -> Unit,
    onSetShowSearchBar: (Boolean) -> Unit,
    onSetShowDock: (Boolean) -> Unit,
    onSetShowHomeLabels: (Boolean) -> Unit,
    onSetMonochromeIcons: (Boolean) -> Unit,
    onSetDrawerColumns: (Int) -> Unit,
    onSetDrawerListMode: (Boolean) -> Unit,
    onSetShowDrawerLabels: (Boolean) -> Unit,
    onSetAutoFocusSearch: (Boolean) -> Unit,
    onSetThemeName: (String) -> Unit,
    onSetSolidBg: (Boolean) -> Unit,
    onSetWallpaperDim: (Float) -> Unit,
    onSetUiScale: (Float) -> Unit
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("general", "home", "drawer", "themes")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + (108 * scale).dp,
                bottom = contentPadding.calculateBottomPadding() + 32.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    item {
                        KaruiBox(title = "Default Launcher") {
                            StatusBadgeRow(
                                label = "Home Role",
                                value = if (privilegeStatus.isDefaultHome) "ACTIVE" else "NOT SET",
                                healthy = privilegeStatus.isDefaultHome
                            )
                            if (!privilegeStatus.isDefaultHome) {
                                Spacer(modifier = Modifier.height(10.dp))
                                KaruiButton(
                                    text = "Set OmarchyLauncher as Default",
                                    onClick = onRequestDefaultHome,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    item {
                        KaruiBox(title = "Privileges & Root") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Diagnostics", color = palette.text, fontWeight = FontWeight.Bold, fontSize = (14 * scale).sp)
                                KaruiSubtleButton("Refresh", onClick = onRefreshPrivileges)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            StatusBadgeRow("Root (KSU / Magisk)", if (privilegeStatus.isRootAvailable) "GRANTED" else "NONE", privilegeStatus.isRootAvailable)
                            StatusBadgeRow(
                                "Shizuku",
                                when {
                                    privilegeStatus.isShizukuGranted -> "GRANTED"
                                    privilegeStatus.isShizukuAlive -> "TAP TO GRANT"
                                    else -> "OFFLINE"
                                },
                                privilegeStatus.isShizukuGranted,
                                onClick = if (privilegeStatus.isShizukuAlive && !privilegeStatus.isShizukuGranted) onRequestShizuku else null
                            )
                            StatusBadgeRow(
                                "Accessibility Fallback",
                                if (privilegeStatus.isAccessibilityActive) "ACTIVE" else "DISABLED",
                                privilegeStatus.isAccessibilityActive,
                                onClick = onOpenAccessibility
                            )
                        }
                    }
                    item {
                        KaruiBox(title = "Gestures & Input") {
                            ToggleRow("Double-Tap to Lock Screen", "Uses Root or Accessibility Service", settings.doubleTapToSleep, onSetDoubleTapSleep)
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Swipe Down for Notifications", "Pull down anywhere on workspace", settings.swipeDownNotifications, onSetSwipeDownNotif)
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Haptic Feedback", "Vibrate on long-press and toggles", settings.hapticFeedback, onSetHaptic)
                        }
                    }
                }
                1 -> {
                    item {
                        KaruiBox(title = "Workspace Layout") {
                            Text("GRID COLUMNS", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(3, 4, 5, 6).forEach { col ->
                                    KaruiChip("$col", settings.gridColumns == col, { onSetGridColumns(col) }, Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("At a Glance Bar", "Clock, date, battery & privilege badge", settings.showAtAGlance, onSetShowAtAGlance)
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Omarchy Search Prompt", "Terminal search bar above dock", settings.showSearchBar, onSetShowSearchBar)
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Show Dock", "Pinned quick-launch row at bottom", settings.showDock, onSetShowDock)
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Show Home Icon Labels", "Display app names under workspace icons", settings.showHomeLabels, onSetShowHomeLabels)
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Monochrome Icons", "Grayscale icons inside Karui frames", settings.monochromeIcons, onSetMonochromeIcons)
                        }
                    }
                }
                2 -> {
                    item {
                        KaruiBox(title = "App Drawer") {
                            ToggleRow("Omarchy Terminal List Mode", "Compact list with package names instead of grid", settings.drawerListMode, onSetDrawerListMode)
                            Spacer(modifier = Modifier.height(12.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("DRAWER GRID COLUMNS", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(3, 4, 5, 6).forEach { col ->
                                    KaruiChip("$col", settings.drawerColumns == col, { onSetDrawerColumns(col) }, Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Show Drawer Labels", "Display app names in grid view", settings.showDrawerLabels, onSetShowDrawerLabels)
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Auto-Focus Search on Swipe Up", "Immediately open keyboard like Rofi/Walker", settings.autoFocusDrawerSearch, onSetAutoFocusSearch)
                        }
                    }
                }
                3 -> {
                    item {
                        KaruiBox(title = "Background & Scaling") {
                            ToggleRow("Solid Omarchy Background", "Use pure theme background instead of wallpaper", settings.solidBackground, onSetSolidBg)
                            if (!settings.solidBackground) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("WALLPAPER DIM OVERLAY (${(settings.wallpaperDimAlpha * 100).toInt()}%)", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = settings.wallpaperDimAlpha,
                                    onValueChange = onSetWallpaperDim,
                                    valueRange = 0.2f..0.95f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = palette.accent,
                                        activeTrackColor = palette.accent,
                                        inactiveTrackColor = palette.surface
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("UI SCALE", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("S" to 0.85f, "M" to 1.0f, "L" to 1.15f, "XL" to 1.3f).forEach { (lbl, v) ->
                                    KaruiChip(lbl, abs(settings.uiScale - v) < 0.03f, { onSetUiScale(v) }, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    item {
                        KaruiBox(title = "Themes (${KaruiThemes.size})") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                KaruiThemes.chunked(3).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        row.forEach { t ->
                                            val active = t.key == settings.themeName
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (active) palette.accent else palette.surface)
                                                    .border(2.dp, palette.border, RectangleShape)
                                                    .clickable { onSetThemeName(t.key) }
                                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                        Box(Modifier.size(8.dp).background(t.bg).border(1.dp, t.border, RectangleShape))
                                                        Box(Modifier.size(8.dp).background(t.accent))
                                                    }
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        text = t.key.uppercase(),
                                                        color = if (active) palette.accentTxt else palette.gray,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = (10 * scale).sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        KaruiPivotHeader(
            title = "omarchy settings",
            tabs = tabs,
            selectedIndex = selectedTab,
            onSelect = { selectedTab = it },
            topInset = contentPadding.calculateTopPadding(),
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = palette.text, fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp)
            Text(subtitle, color = palette.gray, fontSize = (12 * scale).sp)
        }
        KaruiSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatusBadgeRow(
    label: String,
    value: String,
    healthy: Boolean,
    onClick: (() -> Unit)? = null
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.text.copy(alpha = 0.85f), fontSize = (13 * scale).sp)
        Box(
            modifier = Modifier
                .background(if (healthy) palette.accent else palette.surface)
                .border(1.dp, palette.border, RectangleShape)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = value,
                color = if (healthy) palette.accentTxt else palette.gray,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = (11 * scale).sp
            )
        }
    }
}
