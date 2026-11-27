package com.omarchylauncher.app.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.BatteryManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omarchylauncher.app.OmarchyLauncherApp
import com.omarchylauncher.app.SettingsActivity
import com.omarchylauncher.app.data.AppEntry
import com.omarchylauncher.app.data.LauncherSettings
import com.omarchylauncher.app.privilege.PrivilegeStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class ClockState(
    val timeMillis: Long = System.currentTimeMillis(),
    val batteryPercent: Int = 100
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as OmarchyLauncherApp

    val settings: StateFlow<LauncherSettings> = app.preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LauncherSettings())

    val apps: StateFlow<List<AppEntry>> = app.appRepository.apps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isLoadingApps: StateFlow<Boolean> = app.appRepository.isLoading
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val privilegeStatus: StateFlow<PrivilegeStatus> = app.privilegeController.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrivilegeStatus())

    private val _clockState = MutableStateFlow(ClockState())
    val clockState: StateFlow<ClockState> = _clockState.asStateFlow()

    val pinnedHomeApps: StateFlow<List<AppEntry>> = combine(apps, settings) { allApps, s ->
        val map = allApps.associateBy { it.packageName }
        s.pinnedHomePackages.mapNotNull { map[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dockApps: StateFlow<List<AppEntry>> = combine(apps, settings) { allApps, s ->
        val map = allApps.associateBy { it.packageName }
        s.dockPackages.mapNotNull { map[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshPrivileges()
        seedDefaultAppsOnce()
        startClockTicker()
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            val bm = app.getSystemService(BatteryManager::class.java)
            while (isActive) {
                val pct = runCatching {
                    bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                }.getOrDefault(100).coerceIn(0, 100)
                _clockState.value = ClockState(
                    timeMillis = System.currentTimeMillis(),
                    batteryPercent = pct
                )
                delay(15_000L)
            }
        }
    }

    private fun seedDefaultAppsOnce() {
        viewModelScope.launch {
            apps.collect { list ->
                if (list.isNotEmpty()) {
                    val dockCandidates = listOf(
                        "com.google.android.dialer", "com.android.dialer",
                        "com.google.android.apps.messaging", "com.android.messaging",
                        "com.android.chrome", "org.mozilla.firefox", "com.brave.browser",
                        "com.google.android.GoogleCamera", "com.android.camera2"
                    )
                    val availablePkgs = list.map { it.packageName }.toSet()
                    val dockDefaults = dockCandidates.filter { it in availablePkgs }.take(4)
                        .ifEmpty { list.take(4).map { it.packageName } }
                    val homeDefaults = list.filter { it.packageName !in dockDefaults }.take(8).map { it.packageName }
                    app.preferences.seedDefaultsIfEmpty(dockDefaults, homeDefaults)
                    return@collect
                }
            }
        }
    }

    fun refreshPrivileges() {
        viewModelScope.launch { app.privilegeController.refreshStatus() }
    }

    fun refreshApps() = app.appRepository.refresh()
    fun launchApp(entry: AppEntry) = app.appRepository.launchApp(entry)
    fun getShortcuts(entry: AppEntry): List<ShortcutInfo> = app.appRepository.getAppShortcuts(entry)
    fun launchShortcut(shortcut: ShortcutInfo) = app.appRepository.launchShortcut(shortcut)
    fun openAppInfo(entry: AppEntry) = app.appRepository.openAppInfo(entry)

    fun openSettings() {
        val intent = Intent(app, SettingsActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        app.startActivity(intent)
    }

    fun openWallpaperPicker() {
        runCatching {
            val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(Intent.createChooser(intent, "Set Wallpaper").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun searchWeb(query: String) {
        val encoded = Uri.encode(query)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://duckduckgo.com/?q=$encoded")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { app.startActivity(intent) }
    }

    fun lockScreen() {
        viewModelScope.launch {
            val ok = app.privilegeController.lockScreen()
            if (!ok) {
                Toast.makeText(app, "Enable Root or Accessibility Service in Settings to lock screen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun expandNotifications() {
        viewModelScope.launch {
            app.privilegeController.expandNotifications()
        }
    }

    fun forceStopApp(packageName: String) {
        viewModelScope.launch {
            val ok = app.privilegeController.forceStopApp(packageName)
            Toast.makeText(
                app,
                if (ok) "Force-stopped $packageName" else "Root required to force-stop",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun freezeApp(packageName: String, freeze: Boolean) {
        viewModelScope.launch {
            val ok = app.privilegeController.freezeApp(packageName, freeze)
            if (ok) app.appRepository.refresh()
            Toast.makeText(
                app,
                if (ok) (if (freeze) "Frozen $packageName" else "Unfrozen $packageName")
                else "Root required to freeze/unfreeze",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun uninstallApp(packageName: String, useRoot: Boolean) {
        if (useRoot && privilegeStatus.value.isRootAvailable) {
            viewModelScope.launch {
                val ok = app.privilegeController.rootUninstall(packageName)
                if (ok) app.appRepository.refresh()
                else app.appRepository.requestUninstall(packageName)
            }
        } else {
            app.appRepository.requestUninstall(packageName)
        }
    }

    fun requestShizukuPermission() = app.privilegeController.requestShizukuPermission()
    fun openAccessibilitySettings() = app.privilegeController.openAccessibilitySettings()

    // Preference mutators
    fun setThemeName(v: String) = viewModelScope.launch { app.preferences.setThemeName(v) }
    fun setUiScale(v: Float) = viewModelScope.launch { app.preferences.setUiScale(v) }
    fun setSolidBackground(v: Boolean) = viewModelScope.launch { app.preferences.setSolidBackground(v) }
    fun setWallpaperDimAlpha(v: Float) = viewModelScope.launch { app.preferences.setWallpaperDimAlpha(v) }
    fun setGridColumns(v: Int) = viewModelScope.launch { app.preferences.setGridColumns(v) }
    fun setGridRows(v: Int) = viewModelScope.launch { app.preferences.setGridRows(v) }
    fun setDrawerColumns(v: Int) = viewModelScope.launch { app.preferences.setDrawerColumns(v) }
    fun setDrawerListMode(v: Boolean) = viewModelScope.launch { app.preferences.setDrawerListMode(v) }
    fun setShowAtAGlance(v: Boolean) = viewModelScope.launch { app.preferences.setShowAtAGlance(v) }
    fun setShowSearchBar(v: Boolean) = viewModelScope.launch { app.preferences.setShowSearchBar(v) }
    fun setShowDock(v: Boolean) = viewModelScope.launch { app.preferences.setShowDock(v) }
    fun setShowHomeLabels(v: Boolean) = viewModelScope.launch { app.preferences.setShowHomeLabels(v) }
    fun setShowDrawerLabels(v: Boolean) = viewModelScope.launch { app.preferences.setShowDrawerLabels(v) }
    fun setAutoFocusDrawerSearch(v: Boolean) = viewModelScope.launch { app.preferences.setAutoFocusDrawerSearch(v) }
    fun setDoubleTapToSleep(v: Boolean) = viewModelScope.launch { app.preferences.setDoubleTapToSleep(v) }
    fun setSwipeDownNotifications(v: Boolean) = viewModelScope.launch { app.preferences.setSwipeDownNotifications(v) }
    fun setHapticFeedback(v: Boolean) = viewModelScope.launch { app.preferences.setHapticFeedback(v) }
    fun setMonochromeIcons(v: Boolean) = viewModelScope.launch { app.preferences.setMonochromeIcons(v) }
    fun togglePinnedHome(pkg: String) = viewModelScope.launch { app.preferences.togglePinnedHome(pkg) }
    fun toggleDockPackage(pkg: String) = viewModelScope.launch { app.preferences.toggleDockPackage(pkg) }
    fun toggleHiddenPackage(pkg: String) = viewModelScope.launch { app.preferences.toggleHiddenPackage(pkg) }
}
