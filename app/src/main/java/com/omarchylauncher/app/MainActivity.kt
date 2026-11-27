package com.omarchylauncher.app

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omarchylauncher.app.data.AppEntry
import com.omarchylauncher.app.ui.LauncherViewModel
import com.omarchylauncher.app.ui.components.KaruiAppContextDialog
import com.omarchylauncher.app.ui.screens.AppDrawerScreen
import com.omarchylauncher.app.ui.screens.HomeWorkspaceScreen
import com.omarchylauncher.app.ui.theme.LocalKaruiPalette
import com.omarchylauncher.app.ui.theme.OmarchyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()
    private var closeDrawerSignal by mutableStateOf(0)

    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var appWidgetHost: AppWidgetHost

    private val widgetConfigureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val widgetId = res.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (res.resultCode == Activity.RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            viewModel.addWidgetId(widgetId)
        } else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(widgetId)
        }
    }

    private val widgetPickLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val widgetId = res.data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (res.resultCode == Activity.RESULT_OK && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val info = appWidgetManager.getAppWidgetInfo(widgetId)
            if (info?.configure != null) {
                val cfgIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                widgetConfigureLauncher.launch(cfgIntent)
            } else {
                viewModel.addWidgetId(widgetId)
            }
        } else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetHost.deleteAppWidgetId(widgetId)
        }
    }

    private fun launchWidgetPicker() {
        val newId = appWidgetHost.allocateAppWidgetId()
        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newId)
        }
        widgetPickLauncher.launch(pickIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, 1024)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val apps by viewModel.apps.collectAsStateWithLifecycle()
            val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
            val privilegeStatus by viewModel.privilegeStatus.collectAsStateWithLifecycle()
            val clockState by viewModel.clockState.collectAsStateWithLifecycle()
            val isWeatherRefreshing by viewModel.isWeatherRefreshing.collectAsStateWithLifecycle()
            val pinnedHomeApps by viewModel.pinnedHomeApps.collectAsStateWithLifecycle()
            val dockApps by viewModel.dockApps.collectAsStateWithLifecycle()

            var drawerOpen by remember { mutableStateOf(false) }
            var focusDrawerSearch by remember { mutableStateOf(false) }
            var contextApp by remember { mutableStateOf<AppEntry?>(null) }

            androidx.compose.runtime.LaunchedEffect(closeDrawerSignal) {
                if (closeDrawerSignal > 0) drawerOpen = false
            }

            BackHandler(enabled = drawerOpen) {
                drawerOpen = false
            }

            OmarchyTheme(
                themeName = settings.themeName,
                uiScale = settings.uiScale
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
                        pinnedApps = pinnedHomeApps,
                        dockApps = dockApps,
                        appWidgetHost = appWidgetHost,
                        appWidgetManager = appWidgetManager,
                        contentPadding = insets,
                        onOpenDrawer = { focusSearch ->
                            focusDrawerSearch = focusSearch
                            drawerOpen = true
                        },
                        onOpenSettings = viewModel::openSettings,
                        onOpenWallpaper = viewModel::openWallpaperPicker,
                        onAddWidget = ::launchWidgetPicker,
                        onRemoveWidget = { id ->
                            appWidgetHost.deleteAppWidgetId(id)
                            viewModel.removeWidgetId(id)
                        },
                        onLockScreen = viewModel::lockScreen,
                        onExpandNotifications = viewModel::expandNotifications,
                        onRefreshWeather = { viewModel.refreshWeather(silent = false) },
                        onSetWeatherCity = viewModel::setWeatherLocation,
                        onLaunchApp = viewModel::launchApp,
                        onLongPressApp = { contextApp = it }
                    )

                    AnimatedVisibility(
                        visible = drawerOpen,
                        enter = slideInVertically(tween(180)) { it / 3 } + fadeIn(tween(150)),
                        exit = slideOutVertically(tween(150)) { it / 3 } + fadeOut(tween(120))
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
                            onToggleListMode = viewModel::setDrawerListMode,
                            onOpenSettings = viewModel::openSettings,
                            onLockScreen = viewModel::lockScreen,
                            onRefreshApps = viewModel::refreshApps,
                            onSearchWeb = viewModel::searchWeb,
                            onLaunchHiddenApp = { pkg ->
                                drawerOpen = false
                                viewModel.launchHiddenApp(pkg)
                            },
                            onUnhideApp = viewModel::toggleHiddenPackage
                        )
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
                            onToggleHidden = { viewModel.toggleHiddenPackage(target.packageName) },
                            onOpenInfo = { viewModel.openAppInfo(target) },
                            onForceStop = { viewModel.forceStopApp(target.packageName) },
                            onFreeze = { viewModel.freezeApp(target.packageName, true) },
                            onUninstall = { useRoot -> viewModel.uninstallApp(target.packageName, useRoot) }
                        )
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
            closeDrawerSignal++
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPrivileges()
    }
}
