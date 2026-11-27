package com.omarchylauncher.app

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omarchylauncher.app.data.AppEntry
import com.omarchylauncher.app.data.IconPackInfo
import com.omarchylauncher.app.ui.LauncherViewModel
import com.omarchylauncher.app.ui.components.KaruiAppContextDialog
import com.omarchylauncher.app.ui.components.KaruiAppTile
import com.omarchylauncher.app.ui.components.KaruiButton
import com.omarchylauncher.app.ui.components.KaruiChip
import com.omarchylauncher.app.ui.components.KaruiDashedDivider
import com.omarchylauncher.app.ui.components.KaruiSubtleButton
import com.omarchylauncher.app.ui.components.karuiShape
import com.omarchylauncher.app.ui.screens.AppDrawerScreen
import com.omarchylauncher.app.ui.screens.HomeWorkspaceScreen
import com.omarchylauncher.app.ui.theme.LocalKaruiPalette
import com.omarchylauncher.app.ui.theme.OmarchyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private var homeResetSignal by mutableIntStateOf(0)

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost

    private val widgetConfigureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val widgetId = res.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (res.resultCode == Activity.RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            finalizeWidgetAdd(widgetId)
        } else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(widgetId)
        }
    }

    private val widgetBindLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val widgetId = res.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (res.resultCode == Activity.RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            configureOrAddWidget(widgetId)
        } else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(widgetId)
        }
    }

    private fun bindSelectedWidget(provider: AppWidgetProviderInfo) {
        val newId = appWidgetHost.allocateAppWidgetId()
        val bound = runCatching {
            appWidgetManager.bindAppWidgetIdIfAllowed(newId, provider.profile, provider.provider, null)
        }.getOrDefault(false)

        if (bound) {
            configureOrAddWidget(newId)
        } else {
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider.provider)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, provider.profile)
            }
            widgetBindLauncher.launch(bindIntent)
        }
    }

    private fun configureOrAddWidget(widgetId: Int) {
        val info = appWidgetManager.getAppWidgetInfo(widgetId)
        if (info?.configure != null) {
            val cfgIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            runCatching { widgetConfigureLauncher.launch(cfgIntent) }.onFailure {
                finalizeWidgetAdd(widgetId)
            }
        } else {
            finalizeWidgetAdd(widgetId)
        }
    }

    private fun finalizeWidgetAdd(widgetId: Int) {
        viewModel.addWidgetId(widgetId)
        if (viewModel.settings.value.extraPagesCount == 0) {
            viewModel.setExtraPagesCount(1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        runCatching {
            val mode = display?.supportedModes?.maxByOrNull { it.refreshRate }
            if (mode != null) {
                window.attributes = window.attributes.apply { preferredDisplayModeId = mode.modeId }
            }
        }

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, 1024)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val apps by viewModel.overriddenApps.collectAsStateWithLifecycle()
            val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
            val privilegeStatus by viewModel.privilegeStatus.collectAsStateWithLifecycle()
            val clockState by viewModel.clockState.collectAsStateWithLifecycle()
            val isWeatherRefreshing by viewModel.isWeatherRefreshing.collectAsStateWithLifecycle()
            val pinnedHomeSlots by viewModel.pinnedHomeSlots.collectAsStateWithLifecycle()
            val dockApps by viewModel.dockApps.collectAsStateWithLifecycle()

            var drawerOpen by remember { mutableStateOf(false) }
            var focusDrawerSearch by remember { mutableStateOf(false) }
            var contextApp by remember { mutableStateOf<AppEntry?>(null) }
            var editingApp by remember { mutableStateOf<AppEntry?>(null) }
            var confirmUninstallSystemApp by remember { mutableStateOf<Pair<AppEntry, Boolean>?>(null) }
            var pendingHidePkgForPin by remember { mutableStateOf<String?>(null) }
            var showWidgetPicker by remember { mutableStateOf(false) }
            var draggingDrawerApp by remember { mutableStateOf<AppEntry?>(null) }
            var draggingDrawerPos by remember { mutableStateOf(Offset.Zero) }
            var dockTopY by remember { mutableStateOf(Float.MAX_VALUE) }

            LaunchedEffect(homeResetSignal) {
                if (homeResetSignal > 0) drawerOpen = false
            }

            BackHandler(enabled = drawerOpen) {
                drawerOpen = false
            }

            OmarchyTheme(
                themeName = settings.themeName,
                uiScale = settings.uiScale,
                cornerRadiusDp = settings.cornerRadiusDp
            ) {
                val palette = LocalKaruiPalette.current
                val bgColor = if (settings.solidBackground) {
                    palette.bg
                } else {
                    palette.bg.copy(alpha = settings.wallpaperDimAlpha)
                }

                val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                val insets = PaddingValues(top = top, bottom = bottom)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                ) {
                    HomeWorkspaceScreen(
                        settings = settings,
                        clockState = clockState,
                        isWeatherRefreshing = isWeatherRefreshing,
                        pinnedSlots = pinnedHomeSlots,
                        dockApps = dockApps,
                        appWidgetHost = appWidgetHost,
                        appWidgetManager = appWidgetManager,
                        homeResetSignal = homeResetSignal,
                        externalDragActive = draggingDrawerApp != null,
                        externalDragPos = draggingDrawerPos,
                        externalDragIsSystem = draggingDrawerApp?.isSystem == true,
                        onDockTopReported = { dockTopY = it },
                        contentPadding = insets,
                        onOpenDrawer = { focusSearch ->
                            focusDrawerSearch = focusSearch
                            drawerOpen = true
                        },
                        onOpenSettings = viewModel::openSettings,
                        onOpenWallpaper = viewModel::openWallpaperPicker,
                        onAddWidget = { showWidgetPicker = true },
                        onRemoveWidget = { id ->
                            appWidgetHost.deleteAppWidgetId(id)
                            viewModel.removeWidgetId(id)
                        },
                        onResizeWidget = viewModel::setWidgetHeight,
                        onSetWidgetHalfWidth = viewModel::setWidgetHalfWidth,
                        onReorderWidgets = viewModel::setWidgetOrder,
                        onAddPage = { viewModel.setExtraPagesCount(settings.extraPagesCount + 1) },
                        onRemovePage = { viewModel.setExtraPagesCount((settings.extraPagesCount - 1).coerceAtLeast(0)) },
                        onSavePinnedSlots = viewModel::setPinnedHomeSlots,
                        onSaveDockOrder = viewModel::setDockPackages,
                        onMoveHomeToDock = viewModel::moveHomeToDock,
                        onRemoveHomeApp = viewModel::removePinnedHome,
                        onRemoveDockApp = viewModel::toggleDockPackage,
                        onUninstallApp = { pkg -> viewModel.uninstallApp(pkg, useRoot = false) },
                        onLockScreen = viewModel::lockScreen,
                        onExpandNotifications = viewModel::expandNotifications,
                        onRefreshWeather = { viewModel.refreshWeather(silent = false) },
                        onSetWeatherCity = viewModel::setWeatherLocation,
                        onLaunchApp = viewModel::launchApp,
                        onLongPressApp = { contextApp = it },
                        onDismissLongPressApp = { contextApp = null }
                    )

                    val density = LocalDensity.current
                    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
                    val topDropLimitPx = with(density) { (insets.calculateTopPadding() + 86.dp).toPx() }

                    AnimatedVisibility(
                        visible = drawerOpen,
                        enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { it / 4 } + fadeIn(tween(180)),
                        exit = slideOutVertically(tween(170)) { it / 4 } + fadeOut(tween(140)),
                        modifier = Modifier.graphicsLayer {
                            alpha = if (draggingDrawerApp != null) 0f else 1f
                        }
                    ) {
                        AppDrawerScreen(
                            apps = apps,
                            isLoading = isLoadingApps,
                            settings = settings,
                            focusSearchOnOpen = focusDrawerSearch,
                            contentPadding = insets,
                            onClose = { drawerOpen = false },
                            onLaunchApp = {
                                drawerOpen = false
                                viewModel.launchApp(it)
                            },
                            onLongPressApp = { contextApp = it },
                            onDismissLongPressApp = { contextApp = null },
                            onToggleListMode = viewModel::setDrawerListMode,
                            onOpenSettings = viewModel::openSettings,
                            onLockScreen = viewModel::lockScreen,
                            onRefreshApps = viewModel::refreshApps,
                            onSearchWeb = viewModel::searchWeb,
                            onResolveHiddenApps = viewModel::getHiddenAppDetails,
                            onLaunchHiddenApp = { pkg ->
                                drawerOpen = false
                                viewModel.launchHiddenApp(pkg)
                            },
                            onUnhideApp = viewModel::toggleHiddenPackage,
                            onUnfreezeApp = { pkg -> viewModel.freezeApp(pkg, false) },
                            onDragToHomeStart = { app, pos ->
                                draggingDrawerApp = app
                                draggingDrawerPos = pos
                            },
                            onDragToHomeMove = { delta ->
                                draggingDrawerPos += delta
                            },
                            onDragToHomeEnd = { commit ->
                                val dropped = draggingDrawerApp
                                val dropY = draggingDrawerPos.y
                                val dropX = draggingDrawerPos.x
                                draggingDrawerApp = null
                                if (commit && dropped != null) {
                                    drawerOpen = false
                                    when {
                                        dropY < topDropLimitPx -> {
                                            if (!dropped.isSystem && dropX >= screenWidthPx / 2f) {
                                                viewModel.uninstallApp(dropped.packageName, useRoot = false)
                                            } else {
                                                viewModel.removePinnedHome(dropped.packageName)
                                            }
                                        }
                                        settings.showDock && dropY >= dockTopY - 32f -> {
                                            viewModel.addDockPackage(dropped.packageName)
                                        }
                                        else -> {
                                            viewModel.addPinnedHomeAt(dropped.packageName)
                                        }
                                    }
                                }
                            }
                        )
                    }

                    draggingDrawerApp?.let { dragged ->
                        val halfIconPx = with(density) { (settings.iconSizeDp.dp.toPx() / 2f) }
                        val offsetX = with(density) { (draggingDrawerPos.x - halfIconPx).toDp() }
                        val offsetY = with(density) { (draggingDrawerPos.y - halfIconPx).toDp() }
                        Box(
                            modifier = Modifier
                                .offset(x = offsetX, y = offsetY)
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

                    contextApp?.let { target ->
                        val shortcuts = remember(target.packageName) {
                            viewModel.getShortcuts(target)
                        }
                        KaruiAppContextDialog(
                            app = target,
                            shortcuts = shortcuts,
                            isPinnedHome = settings.pinnedHomePackages.contains(target.packageName),
                            isPinnedDock = settings.dockPackages.contains(target.packageName),
                            isHidden = settings.hiddenPackages.contains(target.packageName),
                            isRootAvailable = privilegeStatus.isRootAvailable,
                            onDismiss = { contextApp = null },
                            onLaunchShortcut = {
                                drawerOpen = false
                                viewModel.launchShortcut(it)
                            },
                            onTogglePinHome = { viewModel.togglePinnedHome(target.packageName) },
                            onToggleDock = { viewModel.toggleDockPackage(target.packageName) },
                            onToggleHidden = {
                                if (!settings.hiddenPackages.contains(target.packageName) && settings.hiddenPin.isEmpty()) {
                                    pendingHidePkgForPin = target.packageName
                                } else {
                                    viewModel.toggleHiddenPackage(target.packageName)
                                }
                            },
                            onOpenInfo = { viewModel.openAppInfo(target) },
                            onEditIconLabel = { editingApp = target },
                            onForceStop = { viewModel.forceStopApp(target.packageName) },
                            onFreeze = { viewModel.freezeApp(target.packageName, true) },
                            onUninstall = { useRoot ->
                                if (target.isSystem) {
                                    confirmUninstallSystemApp = target to useRoot
                                } else {
                                    viewModel.uninstallApp(target.packageName, useRoot)
                                }
                            }
                        )
                    }

                    confirmUninstallSystemApp?.let { (sysApp, useRoot) ->
                        Dialog(onDismissRequest = { confirmUninstallSystemApp = null }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.surface)
                                    .border(2.dp, Color(0xFFEF5350), karuiShape())
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "SYSTEM APP WARNING",
                                    color = Color(0xFFEF5350),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "${sysApp.label} (${sysApp.packageName}) is a system app. Uninstalling it may break system features or cause instability. Continue?",
                                    color = palette.text,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    KaruiButton(
                                        text = "CANCEL",
                                        onClick = { confirmUninstallSystemApp = null },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(karuiShape())
                                            .background(Color(0xFFEF5350))
                                            .clickable {
                                                confirmUninstallSystemApp = null
                                                viewModel.uninstallApp(sysApp.packageName, useRoot)
                                            }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "UNINSTALL",
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Edit Icon & Label + Icon Pack Picker Dialog
                    editingApp?.let { target ->
                        var labelDraft by remember(target.packageName) { mutableStateOf(target.label) }
                        val iconPacks = remember { viewModel.getInstalledIconPacks() }
                        var selectedPack by remember { mutableStateOf<IconPackInfo?>(iconPacks.firstOrNull()) }
                        var packDrawables by remember { mutableStateOf<List<String>>(emptyList()) }
                        var iconSearch by remember { mutableStateOf("") }

                        LaunchedEffect(selectedPack) {
                            packDrawables = selectedPack?.let { viewModel.getIconPackDrawables(it.packageName) } ?: emptyList()
                        }

                        val filteredDrawables = remember(packDrawables, iconSearch) {
                            val q = iconSearch.trim()
                            if (q.isEmpty()) packDrawables.take(120)
                            else packDrawables.filter { it.contains(q, ignoreCase = true) }.take(120)
                        }

                        Dialog(onDismissRequest = { editingApp = null }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.surface)
                                    .border(2.dp, palette.border, karuiShape())
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "EDIT ICON & NAME",
                                    color = palette.accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.bg)
                                        .border(2.dp, palette.border, karuiShape())
                                        .padding(10.dp)
                                ) {
                                    BasicTextField(
                                        value = labelDraft,
                                        onValueChange = { labelDraft = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = palette.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                        cursorBrush = SolidColor(palette.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    KaruiSubtleButton("Reset Icon", {
                                        viewModel.setCustomIcon(target.packageName, null)
                                    }, Modifier.weight(1f))
                                    KaruiButton("Save Name", {
                                        viewModel.setCustomLabel(target.packageName, labelDraft)
                                        editingApp = null
                                    }, Modifier.weight(1f))
                                }

                                Spacer(Modifier.height(10.dp))
                                KaruiDashedDivider()
                                Spacer(Modifier.height(8.dp))

                                if (iconPacks.isEmpty()) {
                                    Text(
                                        text = "No external icon packs installed (e.g. Lawnicons, Material You packs).",
                                        color = palette.gray,
                                        fontSize = 12.sp
                                    )
                                } else {
                                    Text("ICON PACKS (${iconPacks.size})", color = palette.gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        iconPacks.forEach { pack ->
                                            KaruiChip(
                                                label = pack.label,
                                                selected = selectedPack?.packageName == pack.packageName,
                                                onClick = { selectedPack = pack }
                                            )
                                        }
                                    }
                                    selectedPack?.let { sp ->
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "PACK: ${sp.label.uppercase()}",
                                            color = palette.accent,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(palette.bg)
                                            .border(1.dp, palette.border, karuiShape())
                                            .padding(8.dp)
                                    ) {
                                        if (iconSearch.isEmpty()) {
                                            Text("search pack icons...", color = palette.gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                        }
                                        BasicTextField(
                                            value = iconSearch,
                                            onValueChange = { iconSearch = it },
                                            singleLine = true,
                                            textStyle = TextStyle(color = palette.text, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                                            cursorBrush = SolidColor(palette.accent),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(4),
                                        modifier = Modifier.height(210.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(filteredDrawables) { dName ->
                                            val spec = "${selectedPack?.packageName}/$dName"
                                            val bmp = remember(spec) { viewModel.loadIconFromSpec(spec) }
                                            if (bmp != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(48.dp)
                                                        .background(palette.bg)
                                                        .border(1.dp, palette.border, karuiShape())
                                                        .clickable {
                                                            viewModel.setCustomIcon(target.packageName, spec)
                                                            editingApp = null
                                                        }
                                                        .padding(6.dp)
                                                ) {
                                                    Image(bmp, dName, Modifier.fillMaxSize())
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                KaruiSubtleButton("Close", { editingApp = null }, Modifier.fillMaxWidth())
                            }
                        }
                    }

                    // First-time Set PIN popup when hiding first app
                    pendingHidePkgForPin?.let { pkgToHide ->
                        var newPin by remember { mutableStateOf("") }
                        var pinErr by remember { mutableStateOf(false) }
                        Dialog(onDismissRequest = { pendingHidePkgForPin = null }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.surface)
                                    .border(2.dp, palette.border, RectangleShape)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "SET HIDDEN VAULT PIN",
                                    color = palette.accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Create a 4-6 digit PIN to protect hidden apps.",
                                    color = palette.gray,
                                    fontSize = 12.sp
                                )
                                if (privilegeStatus.isRootAvailable) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Warning: Root hiding (pm hide) disables the app system-wide — it will not run in background or send notifications until unhidden.",
                                        color = Color(0xFFFFB000),
                                        fontSize = 11.sp
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
                                    if (newPin.isEmpty()) {
                                        Text("4-6 digits", color = palette.gray, fontSize = 13.sp)
                                    }
                                    BasicTextField(
                                        value = newPin,
                                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) { newPin = it; pinErr = false } },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        textStyle = TextStyle(color = palette.text, fontFamily = FontFamily.Monospace, fontSize = 16.sp),
                                        cursorBrush = SolidColor(palette.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (pinErr) {
                                    Spacer(Modifier.height(6.dp))
                                    Text("PIN must be 4 to 6 digits", color = Color(0xFFEF5350), fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    KaruiSubtleButton("Cancel", { pendingHidePkgForPin = null }, Modifier.weight(1f))
                                    KaruiButton("Set & Hide", {
                                        if (newPin.length in 4..6) {
                                            viewModel.setHiddenPinAndHide(newPin, pkgToHide)
                                            pendingHidePkgForPin = null
                                        } else {
                                            pinErr = true
                                        }
                                    }, Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    // Karui Installed Widgets Picker Dialog (Searchable + Deduplicated)
                    if (showWidgetPicker) {
                        var widgetQuery by remember { mutableStateOf("") }
                        val providers = remember {
                            runCatching {
                                appWidgetManager.installedProviders
                                    .distinctBy { "${it.provider.packageName}/${it.loadLabel(packageManager)}" }
                                    .sortedBy { it.loadLabel(packageManager).lowercase() }
                            }.getOrDefault(emptyList())
                        }
                        val filteredProviders = remember(providers, widgetQuery) {
                            val q = widgetQuery.trim()
                            if (q.isEmpty()) providers
                            else providers.filter {
                                it.loadLabel(packageManager).contains(q, ignoreCase = true) ||
                                    it.provider.packageName.contains(q, ignoreCase = true)
                            }
                        }
                        Dialog(onDismissRequest = { showWidgetPicker = false }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(palette.surface)
                                    .border(2.dp, palette.border, RectangleShape)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "ADD WIDGET (${filteredProviders.size})",
                                    color = palette.accent,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(palette.bg)
                                        .border(2.dp, palette.border, RectangleShape)
                                        .padding(10.dp)
                                ) {
                                    if (widgetQuery.isEmpty()) {
                                        Text("search widgets...", color = palette.gray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                    BasicTextField(
                                        value = widgetQuery,
                                        onValueChange = { widgetQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = palette.text, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                        cursorBrush = SolidColor(palette.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                KaruiDashedDivider()
                                Spacer(Modifier.height(8.dp))
                                LazyColumn(
                                    modifier = Modifier.height(320.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredProviders) { info ->
                                        val label = remember(info) { info.loadLabel(packageManager) }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(palette.bg)
                                                .border(1.dp, palette.border, RectangleShape)
                                                .clickable {
                                                    showWidgetPicker = false
                                                    bindSelectedWidget(info)
                                                }
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = label,
                                                    color = palette.text,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = info.provider.packageName,
                                                    color = palette.gray,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                KaruiSubtleButton("Close", { showWidgetPicker = false }, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        runCatching { appWidgetHost.startListening() }
    }

    override fun onStop() {
        super.onStop()
        runCatching { appWidgetHost.stopListening() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homeResetSignal++
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPrivileges()
    }
}
