package com.omarchylauncher.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omarchylauncher.app.data.AppEntry
import com.omarchylauncher.app.data.LauncherSettings
import com.omarchylauncher.app.data.WebSearchProvider
import com.omarchylauncher.app.ui.components.KaruiAppTile
import com.omarchylauncher.app.ui.components.KaruiButton
import com.omarchylauncher.app.ui.components.KaruiDashedDivider
import com.omarchylauncher.app.ui.components.KaruiPivotHeader
import com.omarchylauncher.app.ui.components.KaruiSubtleButton
import com.omarchylauncher.app.ui.components.iconShapeToComposeShape
import com.omarchylauncher.app.ui.theme.LocalKaruiPalette
import com.omarchylauncher.app.ui.theme.LocalUiScale
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun AppDrawerScreen(
    apps: List<AppEntry>,
    isLoading: Boolean,
    settings: LauncherSettings,
    focusSearchOnOpen: Boolean,
    contentPadding: PaddingValues,
    onClose: () -> Unit,
    onLaunchApp: (AppEntry) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onToggleListMode: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onLockScreen: () -> Unit,
    onRefreshApps: () -> Unit,
    onSearchWeb: (String, WebSearchProvider) -> Unit,
    onLaunchHiddenApp: (String) -> Unit,
    onUnhideApp: (String) -> Unit
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    val tabs = listOf("all", "user", "system")
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val focusRequester = remember { FocusRequester() }

    var showPinDialog by remember { mutableStateOf(false) }
    var showHiddenVault by remember { mutableStateOf(false) }

    LaunchedEffect(focusSearchOnOpen) {
        if (focusSearchOnOpen) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val mathResult = remember(query) {
        val raw = query.trim()
        val expr = if (raw.startsWith("=")) raw.drop(1) else raw
        if (expr.isNotEmpty() && expr.any { it in "+-*/" } && expr.any { it.isDigit() }) {
            evalSimpleMath(expr)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bg)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            KaruiPivotHeader(
                title = "apps",
                tabs = tabs,
                selectedIndex = pagerState.currentPage,
                onSelect = { idx -> scope.launch { pagerState.animateScrollToPage(idx) } },
                topInset = contentPadding.calculateTopPadding()
            )

            // Invisible/subtle long-press target over the top-left "apps" title for Hidden Vault
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = contentPadding.calculateTopPadding() + 8.dp, start = 12.dp)
                    .size(width = 120.dp, height = 52.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            if (settings.hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showPinDialog = true
                        }
                    )
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = contentPadding.calculateTopPadding() + 18.dp, end = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(palette.surface)
                        .border(2.dp, palette.border, RectangleShape)
                        .clickable { onToggleListMode(!settings.drawerListMode) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (settings.drawerListMode) Icons.Rounded.GridView else Icons.Rounded.ViewList,
                        contentDescription = "Toggle Layout",
                        tint = palette.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(palette.surface)
                        .border(2.dp, palette.border, RectangleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Drawer",
                        tint = palette.text,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Clean Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .background(palette.surface)
                .border(2.dp, palette.border, RectangleShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ">_",
                color = palette.accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = (14 * scale).sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "search",
                        color = palette.gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (13 * scale).sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = palette.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (14 * scale).sp
                    ),
                    cursorBrush = SolidColor(palette.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            when (query.trim().lowercase()) {
                                ":settings" -> { onClose(); onOpenSettings() }
                                ":lock" -> { onClose(); onLockScreen() }
                                ":refresh" -> { onRefreshApps(); query = "" }
                                else -> if (query.isNotBlank() && mathResult == null) {
                                    onSearchWeb(query.trim(), settings.webSearchProvider)
                                }
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { query = "" },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = "Clear",
                        tint = palette.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Live Math Result
        if (mathResult != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(palette.surface)
                    .border(2.dp, palette.accent, RectangleShape)
                    .padding(12.dp)
            ) {
                Text(
                    text = "= $mathResult",
                    color = palette.accent,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (18 * scale).sp
                )
            }
        }

        // Quick Commands
        if (query.startsWith(":")) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KaruiSubtleButton(":settings — Open Omarchy Settings", onClick = { onClose(); onOpenSettings() }, modifier = Modifier.fillMaxWidth())
                KaruiSubtleButton(":lock — Lock Screen", onClick = { onClose(); onLockScreen() }, modifier = Modifier.fillMaxWidth())
                KaruiSubtleButton(":refresh — Reload Installed Apps", onClick = { onRefreshApps(); query = "" }, modifier = Modifier.fillMaxWidth())
            }
        }

        // Web Search Bar Actions (Google App / Brave Search)
        if (query.isNotBlank() && !query.startsWith(":") && mathResult == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KaruiSubtleButton(
                    text = "Google App",
                    onClick = { onSearchWeb(query.trim(), WebSearchProvider.GOOGLE_APP) },
                    modifier = Modifier.weight(1f)
                )
                KaruiSubtleButton(
                    text = "Brave Search",
                    onClick = { onSearchWeb(query.trim(), WebSearchProvider.BRAVE) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.accent)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val cleanQuery = if (query.startsWith(":")) "" else query.trim()
                val pageApps = remember(apps, settings.hiddenPackages, cleanQuery, page) {
                    apps.filter { app ->
                        val isHidden = settings.hiddenPackages.contains(app.packageName)
                        val tabMatch = when (page) {
                            0 -> !isHidden
                            1 -> !isHidden && !app.isSystem
                            2 -> !isHidden && app.isSystem
                            else -> !isHidden
                        }
                        val queryMatch = cleanQuery.isEmpty() ||
                            app.label.contains(cleanQuery, ignoreCase = true) ||
                            app.packageName.contains(cleanQuery, ignoreCase = true)
                        tabMatch && queryMatch
                    }
                }

                if (pageApps.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "NO APPS",
                            color = palette.gray,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = (13 * scale).sp
                        )
                    }
                } else if (settings.drawerListMode) {
                    val frameShape = iconShapeToComposeShape(settings.iconShape)
                    val monoFilter = if (settings.monochromeIcons) {
                        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                    } else null

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = contentPadding.calculateBottomPadding() + 24.dp
                        )
                    ) {
                        items(pageApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = {
                                            if (settings.hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLongPressApp(app)
                                        }
                                    )
                                    .drawBehind {
                                        drawLine(
                                            color = palette.border,
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = 1.dp.toPx()
                                        )
                                    }
                                    .padding(vertical = (10 * scale).dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(frameShape)
                                        .background(palette.surface)
                                        .border(1.dp, palette.border, frameShape)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (app.icon != null) {
                                        Image(
                                            bitmap = app.icon,
                                            contentDescription = app.label,
                                            colorFilter = monoFilter,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(frameShape)
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Apps, null, tint = palette.accent)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        color = palette.text,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = (15 * scale).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = palette.gray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = (11 * scale).sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings.drawerColumns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            end = 12.dp,
                            bottom = contentPadding.calculateBottomPadding() + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(pageApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                            KaruiAppTile(
                                app = app,
                                showLabel = settings.showDrawerLabels,
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
        }
    }

    // PIN Code Prompt for Hidden Apps Vault
    if (showPinDialog) {
        var pinInput by remember { mutableStateOf("") }
        var pinError by remember { mutableStateOf(false) }
        Dialog(onDismissRequest = { showPinDialog = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(2.dp, palette.border, RectangleShape)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, null, tint = palette.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "HIDDEN VAULT PIN",
                        color = palette.accent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = (14 * scale).sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.bg)
                        .border(2.dp, palette.border, RectangleShape)
                        .padding(12.dp)
                ) {
                    if (pinInput.isEmpty()) {
                        Text("Enter PIN (default 0000)", color = palette.gray, fontSize = 13.sp)
                    }
                    BasicTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it; pinError = false },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        textStyle = TextStyle(color = palette.text, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                        cursorBrush = SolidColor(palette.accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (pinError) {
                    Spacer(Modifier.height(6.dp))
                    Text("Incorrect PIN", color = Color(0xFFEF5350), fontSize = 12.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KaruiSubtleButton("Cancel", { showPinDialog = false }, Modifier.weight(1f))
                    KaruiButton("Unlock", {
                        if (pinInput == settings.hiddenPin) {
                            showPinDialog = false
                            showHiddenVault = true
                        } else {
                            pinError = true
                        }
                    }, Modifier.weight(1f))
                }
            }
        }
    }

    // Hidden Apps Vault Dialog (Amarok-style)
    if (showHiddenVault) {
        val hiddenList = settings.hiddenPackages.sorted()
        Dialog(onDismissRequest = { showHiddenVault = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(2.dp, palette.border, RectangleShape)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HIDDEN VAULT (${hiddenList.size})",
                        color = palette.accent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = (14 * scale).sp
                    )
                    KaruiSubtleButton("Close", { showHiddenVault = false })
                }
                Spacer(Modifier.height(10.dp))
                KaruiDashedDivider()
                Spacer(Modifier.height(10.dp))

                if (hiddenList.isEmpty()) {
                    Text(
                        text = "No hidden apps. Long-press any app and tap HIDE APP.",
                        color = palette.gray,
                        fontSize = (12 * scale).sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(hiddenList, key = { it }) { pkg ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.bg)
                                    .border(1.dp, palette.border, RectangleShape)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = pkg,
                                    color = palette.text,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = (12 * scale).sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KaruiSubtleButton("Open", {
                                        showHiddenVault = false
                                        onLaunchHiddenApp(pkg)
                                    })
                                    KaruiSubtleButton("Unhide", { onUnhideApp(pkg) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun evalSimpleMath(expr: String): String? {
    return runCatching {
        val clean = expr.replace(" ", "")
        if (clean.isEmpty() || !clean.all { it.isDigit() || it in "+-*/.()" }) return null
        val result = object {
            var pos = -1
            var ch = 0
            fun nextChar() { ch = if (++pos < clean.length) clean[pos].code else -1 }
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) { nextChar(); return true }
                return false
            }
            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < clean.length) throw IllegalArgumentException()
                return x
            }
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    x = when {
                        eat('+'.code) -> x + parseTerm()
                        eat('-'.code) -> x - parseTerm()
                        else -> return x
                    }
                }
            }
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    x = when {
                        eat('*'.code) -> x * parseFactor()
                        eat('/'.code) -> x / parseFactor()
                        else -> return x
                    }
                }
            }
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                val startPos = pos
                val x: Double
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch in '0'.code..'9'.code || ch == '.'.code) {
                    while (ch in '0'.code..'9'.code || ch == '.'.code) nextChar()
                    x = clean.substring(startPos, pos).toDouble()
                } else throw IllegalArgumentException()
                return x
            }
        }.parse()
        if (result % 1.0 == 0.0) result.toLong().toString() else String.format(Locale.US, "%.4f", result).trimEnd('0').trimEnd('.')
    }.getOrNull()
}
