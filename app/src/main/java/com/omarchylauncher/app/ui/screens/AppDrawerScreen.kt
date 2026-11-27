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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.ViewList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omarchylauncher.app.data.AppEntry
import com.omarchylauncher.app.data.LauncherSettings
import com.omarchylauncher.app.ui.components.KaruiAppTile
import com.omarchylauncher.app.ui.components.KaruiPivotHeader
import com.omarchylauncher.app.ui.components.KaruiSubtleButton
import com.omarchylauncher.app.ui.theme.LocalKaruiPalette
import com.omarchylauncher.app.ui.theme.LocalUiScale
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
    onSearchWeb: (String) -> Unit
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current
    val haptic = LocalHapticFeedback.current

    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("all", "user", "system", "hidden")
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(focusSearchOnOpen) {
        if (focusSearchOnOpen) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    val mathResult = remember(query) {
        if (query.startsWith("=") && query.length > 1) {
            evalSimpleMath(query.drop(1))
        } else null
    }

    val filteredApps by remember(apps, settings.hiddenPackages, query, selectedTab) {
        derivedStateOf {
            val cleanQuery = if (query.startsWith(":") || query.startsWith("=")) "" else query.trim()
            apps.filter { app ->
                val isHidden = settings.hiddenPackages.contains(app.packageName)
                val tabMatch = when (selectedTab) {
                    0 -> !isHidden
                    1 -> !isHidden && !app.isSystem
                    2 -> !isHidden && app.isSystem
                    3 -> isHidden
                    else -> true
                }
                val queryMatch = cleanQuery.isEmpty() ||
                    app.label.contains(cleanQuery, ignoreCase = true) ||
                    app.packageName.contains(cleanQuery, ignoreCase = true)
                tabMatch && queryMatch
            }
        }
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
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
                topInset = contentPadding.calculateTopPadding()
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

        // Omarchy Terminal Prompt Search Bar
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
                        text = "search apps, =2+2, :settings, :lock",
                        color = palette.gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = (13 * scale).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            when (query.trim().lowercase()) {
                                ":settings" -> { onClose(); onOpenSettings() }
                                ":lock" -> { onClose(); onLockScreen() }
                                ":refresh" -> { onRefreshApps(); query = "" }
                                else -> if (filteredApps.size == 1) {
                                    onLaunchApp(filteredApps.first())
                                } else if (query.isNotBlank() && filteredApps.isEmpty() && mathResult == null) {
                                    onSearchWeb(query.trim())
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

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = palette.accent)
            }
        } else if (filteredApps.isEmpty() && query.isNotBlank() && !query.startsWith(":") && mathResult == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "NO MATCHING APPS",
                    color = palette.gray,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (13 * scale).sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                KaruiSubtleButton(
                    text = "Search Web for \"$query\"",
                    onClick = { onSearchWeb(query.trim()) }
                )
            }
        } else if (settings.drawerListMode) {
            // Omarchy Terminal List Mode
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = contentPadding.calculateBottomPadding() + 24.dp
                )
            ) {
                items(filteredApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                    val monoFilter = if (settings.monochromeIcons) {
                        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                    } else null
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
                                .background(palette.surface)
                                .border(1.dp, palette.border, RectangleShape)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon,
                                    contentDescription = app.label,
                                    colorFilter = monoFilter,
                                    modifier = Modifier.fillMaxSize()
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
            // Grid Mode
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
                items(filteredApps, key = { "${it.packageName}/${it.activityName}" }) { app ->
                    KaruiAppTile(
                        app = app,
                        showLabel = settings.showDrawerLabels,
                        monochrome = settings.monochromeIcons,
                        hapticsEnabled = settings.hapticFeedback,
                        onClick = { onLaunchApp(app) },
                        onLongClick = { onLongPressApp(app) }
                    )
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
