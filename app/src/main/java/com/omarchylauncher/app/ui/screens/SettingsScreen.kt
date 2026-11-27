package com.omarchylauncher.app.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omarchylauncher.app.data.HiddenAppInfo
import com.omarchylauncher.app.data.IconShape
import com.omarchylauncher.app.data.LauncherSettings
import com.omarchylauncher.app.data.WebSearchProvider
import com.omarchylauncher.app.privilege.PrivilegeStatus
import com.omarchylauncher.app.ui.components.KaruiBox
import com.omarchylauncher.app.ui.components.KaruiButton
import com.omarchylauncher.app.ui.components.KaruiChip
import com.omarchylauncher.app.ui.components.KaruiDashedDivider
import com.omarchylauncher.app.ui.components.KaruiPivotHeader
import com.omarchylauncher.app.ui.components.KaruiSubtleButton
import com.omarchylauncher.app.ui.components.KaruiSwitch
import com.omarchylauncher.app.ui.components.iconShapeToComposeShape
import com.omarchylauncher.app.ui.components.karuiShape
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
    onSetIconShape: (IconShape) -> Unit,
    onSetIconSizeDp: (Int) -> Unit,
    onSetShowSteps: (Boolean) -> Unit,
    onSetShowScreenTime: (Boolean) -> Unit,
    onSetDrawerColumns: (Int) -> Unit,
    onSetDrawerListMode: (Boolean) -> Unit,
    onSetDrawerBgAlpha: (Float) -> Unit,
    onSetShowDrawerTabs: (Boolean) -> Unit,
    onSetShowDrawerLabels: (Boolean) -> Unit,
    onSetAutoFocusSearch: (Boolean) -> Unit,
    onSetWebSearchProvider: (WebSearchProvider) -> Unit,
    onSetHiddenPin: (String) -> Unit,
    onSetWeatherLocation: (String) -> Unit,
    onSetWeatherRefreshMinutes: (Int) -> Unit,
    onRefreshWeather: () -> Unit,
    onSetThemeName: (String) -> Unit,
    onSetSolidBg: (Boolean) -> Unit,
    onSetWallpaperDim: (Float) -> Unit,
    onSetUiScale: (Float) -> Unit,
    onSetCornerRadiusDp: (Int) -> Unit,
    onResolveFrozenApps: suspend (Set<String>) -> List<HiddenAppInfo>,
    onUnfreezeApp: (String) -> Unit
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
                        KaruiBox(title = "Weather (No GPS)") {
                            var cityDraft by remember(settings.weatherLocation) { mutableStateOf(settings.weatherLocation) }
                            Text("CITY NAME", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(palette.surface)
                                        .border(2.dp, palette.border, RectangleShape)
                                        .padding(10.dp)
                                ) {
                                    if (cityDraft.isEmpty()) {
                                        Text("e.g. Tokyo, London", color = palette.gray, fontSize = 13.sp)
                                    }
                                    BasicTextField(
                                        value = cityDraft,
                                        onValueChange = { cityDraft = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = palette.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                        cursorBrush = SolidColor(palette.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                KaruiSubtleButton("Save", { onSetWeatherLocation(cityDraft) })
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("REFRESH INTERVAL", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("15m" to 15, "30m" to 30, "1h" to 60, "3h" to 180, "Manual" to 0).forEach { (lbl, mins) ->
                                    KaruiChip(lbl, settings.weatherRefreshMinutes == mins, { onSetWeatherRefreshMinutes(mins) }, Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiSubtleButton("Refresh Weather Now", onRefreshWeather, modifier = Modifier.fillMaxWidth())
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
                            ToggleRow("At a Glance Bar", "Clock, date, battery & weather", settings.showAtAGlance, onSetShowAtAGlance)
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Show Screen Time", "Today's usage time inside status box", settings.showScreenTime, onSetShowScreenTime)
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Show Steps Count", "Today's step counter inside status box", settings.showSteps, onSetShowSteps)
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
                            ToggleRow("Pixel Themed Icons", "Tint adaptive monochrome icons to match active theme", settings.monochromeIcons, onSetMonochromeIcons)
                            Spacer(modifier = Modifier.height(12.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("ICON SHAPE", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconShape.entries.forEach { shape ->
                                    KaruiChip(shape.label, settings.iconShape == shape, { onSetIconShape(shape) }, Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("ICON SIZE", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("S" to 36, "M" to 44, "L" to 52, "XL" to 60).forEach { (lbl, dpVal) ->
                                    KaruiChip(lbl, settings.iconSizeDp == dpVal, { onSetIconSizeDp(dpVal) }, Modifier.weight(1f))
                                }
                            }
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
                            ToggleRow("Show Category Tabs", "All / User / System tabs at top of drawer", settings.showDrawerTabs, onSetShowDrawerTabs)
                            Spacer(modifier = Modifier.height(10.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            ToggleRow("Auto-Focus Search on Swipe Up", "Immediately open keyboard like Rofi/Walker", settings.autoFocusDrawerSearch, onSetAutoFocusSearch)
                            Spacer(modifier = Modifier.height(12.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            var drawerAlphaDraft by remember(settings.drawerBgAlpha) { mutableFloatStateOf(settings.drawerBgAlpha) }
                            Text("DRAWER BACKGROUND OPACITY (${(drawerAlphaDraft * 100).toInt()}%)", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = drawerAlphaDraft,
                                onValueChange = { drawerAlphaDraft = it },
                                onValueChangeFinished = { onSetDrawerBgAlpha(drawerAlphaDraft) },
                                valueRange = 0.4f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.accent,
                                    activeTrackColor = palette.accent,
                                    inactiveTrackColor = palette.surface
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("DEFAULT WEB SEARCH", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                WebSearchProvider.entries.forEach { prov ->
                                    KaruiChip(prov.label, settings.webSearchProvider == prov, { onSetWebSearchProvider(prov) }, Modifier.weight(1f))
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            var pinVerified by remember(settings.hiddenPin) { mutableStateOf(settings.hiddenPin.isEmpty()) }
                            var verifyInput by remember { mutableStateOf("") }
                            var verifyErr by remember { mutableStateOf(false) }
                            var newPinDraft by remember { mutableStateOf("") }

                            Text("HIDDEN APPS SECURITY", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Warning: When using Root, hidden apps are disabled system-wide (pm hide) and will not run in background or send notifications until unhidden.",
                                color = palette.gray,
                                fontSize = (11 * scale).sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (!pinVerified) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(karuiShape())
                                            .background(palette.surface)
                                            .border(2.dp, palette.border, karuiShape())
                                            .padding(10.dp)
                                    ) {
                                        if (verifyInput.isEmpty()) {
                                            Text("Enter current PIN to change", color = palette.gray, fontSize = 12.sp)
                                        }
                                        BasicTextField(
                                            value = verifyInput,
                                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { verifyInput = it; verifyErr = false } },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                            textStyle = TextStyle(color = palette.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                            cursorBrush = SolidColor(palette.accent),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    KaruiSubtleButton("Verify", {
                                        if (verifyInput == settings.hiddenPin) {
                                            pinVerified = true
                                            verifyInput = ""
                                        } else {
                                            verifyErr = true
                                        }
                                    })
                                }
                                if (verifyErr) {
                                    Spacer(Modifier.height(4.dp))
                                    Text("Incorrect PIN", color = Color(0xFFEF5350), fontSize = 11.sp)
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(karuiShape())
                                            .background(palette.surface)
                                            .border(2.dp, palette.border, karuiShape())
                                            .padding(10.dp)
                                    ) {
                                        if (newPinDraft.isEmpty()) {
                                            Text("New 4-6 digit PIN", color = palette.gray, fontSize = 12.sp)
                                        }
                                        BasicTextField(
                                            value = newPinDraft,
                                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinDraft = it },
                                            singleLine = true,
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                            textStyle = TextStyle(color = palette.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                            cursorBrush = SolidColor(palette.accent),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    KaruiSubtleButton("Set PIN", {
                                        if (newPinDraft.length in 4..6) {
                                            onSetHiddenPin(newPinDraft)
                                            newPinDraft = ""
                                        }
                                    })
                                }
                            }
                        }
                    }
                    item {
                        var frozenAppsList by remember { mutableStateOf<List<HiddenAppInfo>>(emptyList()) }
                        LaunchedEffect(settings.frozenPackages) {
                            frozenAppsList = if (settings.frozenPackages.isNotEmpty()) {
                                onResolveFrozenApps(settings.frozenPackages)
                            } else emptyList()
                        }
                        KaruiBox(title = "Frozen Apps (${frozenAppsList.size})") {
                            if (frozenAppsList.isEmpty()) {
                                Text("No frozen apps.", color = palette.gray, fontSize = (12 * scale).sp)
                            } else {
                                val frameShape = iconShapeToComposeShape(settings.iconShape)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    frozenAppsList.forEach { item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(karuiShape())
                                                .background(palette.surface)
                                                .border(1.dp, palette.border, karuiShape())
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f).padding(end = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(frameShape)
                                                        .background(palette.bg)
                                                        .border(1.dp, palette.border, frameShape)
                                                        .padding(2.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (item.icon != null) {
                                                        Image(item.icon, item.label, Modifier.fillMaxSize().clip(frameShape))
                                                    } else {
                                                        Icon(Icons.Rounded.Apps, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Text(item.label, color = palette.text, fontSize = (13 * scale).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                            KaruiSubtleButton("Unfreeze", { onUnfreezeApp(item.packageName) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    item {
                        KaruiBox(title = "Background & Scaling") {
                            ToggleRow("Solid Omarchy Background", "Use pure theme background instead of wallpaper", settings.solidBackground, onSetSolidBg)
                            if (!settings.solidBackground) {
                                Spacer(modifier = Modifier.height(10.dp))
                                var dimDraft by remember(settings.wallpaperDimAlpha) { mutableFloatStateOf(settings.wallpaperDimAlpha) }
                                Text("WALLPAPER DIM OVERLAY (${(dimDraft * 100).toInt()}%)", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = dimDraft,
                                    onValueChange = { dimDraft = it },
                                    onValueChangeFinished = { onSetWallpaperDim(dimDraft) },
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
                            Spacer(modifier = Modifier.height(12.dp))
                            KaruiDashedDivider()
                            Spacer(modifier = Modifier.height(10.dp))
                            var radiusDraft by remember(settings.cornerRadiusDp) { mutableFloatStateOf(settings.cornerRadiusDp.toFloat()) }
                            Text("CORNER ROUNDNESS (${radiusDraft.toInt()}dp)", color = palette.gray, fontSize = (11 * scale).sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = radiusDraft,
                                onValueChange = { radiusDraft = it },
                                onValueChangeFinished = { onSetCornerRadiusDp(radiusDraft.toInt()) },
                                valueRange = 0f..24f,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.accent,
                                    activeTrackColor = palette.accent,
                                    inactiveTrackColor = palette.surface
                                )
                            )
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
