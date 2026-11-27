package com.omarchylauncher.app.ui

import android.app.Application
import android.app.SearchManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omarchylauncher.app.OmarchyLauncherApp
import com.omarchylauncher.app.SettingsActivity
import com.omarchylauncher.app.data.AppEntry
import com.omarchylauncher.app.data.HiddenAppInfo
import com.omarchylauncher.app.data.IconShape
import com.omarchylauncher.app.data.LauncherSettings
import com.omarchylauncher.app.data.WeatherService
import com.omarchylauncher.app.data.WebSearchProvider
import com.omarchylauncher.app.privilege.PrivilegeStatus
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class ClockState(
    val timeMillis: Long = System.currentTimeMillis(),
    val batteryPercent: Int = 100,
    val screenTimeText: String = "",
    val stepsCount: Int = -1
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

    private val _isWeatherRefreshing = MutableStateFlow(false)
    val isWeatherRefreshing: StateFlow<Boolean> = _isWeatherRefreshing.asStateFlow()

    val pinnedHomeApps: StateFlow<List<AppEntry>> = combine(apps, settings) { allApps, s ->
        val map = allApps.associateBy { it.packageName }
        s.pinnedHomePackages.distinct().mapNotNull { map[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dockApps: StateFlow<List<AppEntry>> = combine(apps, settings) { allApps, s ->
        val map = allApps.associateBy { it.packageName }
        s.dockPackages.distinct().mapNotNull { map[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var latestSensorSteps: Int = -1
    private val stepPrefs by lazy { app.getSharedPreferences("omarchy_steps", Context.MODE_PRIVATE) }

    init {
        refreshPrivileges()
        seedDefaultAppsOnce()
        registerStepSensor()
        startClockAndWeatherTicker()
    }

    private fun registerStepSensor() {
        runCatching {
            val sm = app.getSystemService(SensorManager::class.java)
            val sensor = sm?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return
            sm.registerListener(object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    val total = event?.values?.firstOrNull()?.toInt() ?: return
                    val today = Calendar.getInstance().let { "${it.get(Calendar.YEAR)}-${it.get(Calendar.DAY_OF_YEAR)}" }
                    val savedDay = stepPrefs.getString("day", "")
                    var baseline = stepPrefs.getInt("baseline", -1)
                    if (savedDay != today || baseline < 0 || total < baseline) {
                        baseline = total
                        stepPrefs.edit().putString("day", today).putInt("baseline", baseline).apply()
                    }
                    latestSensorSteps = (total - baseline).coerceAtLeast(0)
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun readTodayScreenTime(): String {
        return runCatching {
            val usm = app.getSystemService(UsageStatsManager::class.java) ?: return ""
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, now)
            if (stats.isNullOrEmpty()) return ""
            val totalMs = stats.sumOf { it.totalTimeInForeground }
            val mins = (totalMs / 60_000L).toInt()
            val h = mins / 60
            val m = mins % 60
            if (h > 0) "${h}h ${m}m" else "${m}m"
        }.getOrDefault("")
    }

    private fun startClockAndWeatherTicker() {
        viewModelScope.launch {
            val bm = app.getSystemService(BatteryManager::class.java)
            while (isActive) {
                val pct = runCatching {
                    bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                }.getOrDefault(100).coerceIn(0, 100)
                val s = settings.value
                val screenTime = if (s.showScreenTime) readTodayScreenTime() else ""
                _clockState.value = ClockState(
                    timeMillis = System.currentTimeMillis(),
                    batteryPercent = pct,
                    screenTimeText = screenTime,
                    stepsCount = if (s.showSteps) latestSensorSteps else -1
                )

                if (s.weatherLocation.isNotBlank() && s.weatherRefreshMinutes > 0) {
                    val elapsedMs = System.currentTimeMillis() - s.weatherLastUpdated
                    if (elapsedMs >= s.weatherRefreshMinutes * 60_000L) {
                        refreshWeather(silent = true)
                    }
                }
                delay(15_000L)
            }
        }
    }

    fun refreshWeather(silent: Boolean = false) {
        viewModelScope.launch {
            val city = app.preferences.settingsFlow.first().weatherLocation
            if (city.isBlank()) {
                if (!silent) Toast.makeText(app, "Set a weather city first", Toast.LENGTH_SHORT).show()
                return@launch
            }
            _isWeatherRefreshing.value = true
            val res = WeatherService.fetchWeather(city)
            _isWeatherRefreshing.value = false
            if (res != null) {
                app.preferences.saveWeatherCache(res.tempText, res.descText, System.currentTimeMillis())
            } else if (!silent) {
                Toast.makeText(app, "Could not fetch weather for \"$city\"", Toast.LENGTH_SHORT).show()
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
                    val dockDefaults = dockCandidates.filter { it in availablePkgs }.distinct().take(4)
                        .ifEmpty { list.map { it.packageName }.distinct().take(4) }
                    val homeDefaults = list.map { it.packageName }.distinct()
                        .filter { it !in dockDefaults }.take(8)
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

    fun searchWeb(query: String, provider: WebSearchProvider = settings.value.webSearchProvider) {
        val clean = query.trim()
        if (clean.isEmpty()) return

        when (provider) {
            WebSearchProvider.GOOGLE_APP -> {
                val googleIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, clean)
                    setPackage("com.google.android.googlequicksearchbox")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val launched = runCatching { app.startActivity(googleIntent); true }.getOrDefault(false)
                if (!launched) {
                    val genericSearch = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(SearchManager.QUERY, clean)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { app.startActivity(genericSearch) }.onFailure {
                        openUrl("https://www.google.com/search?q=${Uri.encode(clean)}")
                    }
                }
            }
            WebSearchProvider.BRAVE -> {
                val braveUrl = "https://search.brave.com/search?q=${Uri.encode(clean)}"
                val braveIntent = Intent(Intent.ACTION_VIEW, Uri.parse(braveUrl)).apply {
                    setPackage("com.brave.browser")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val launched = runCatching { app.startActivity(braveIntent); true }.getOrDefault(false)
                if (!launched) openUrl(braveUrl)
            }
        }
    }

    private fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
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
            if (ok) {
                app.preferences.setPackageFrozen(packageName, freeze)
                app.appRepository.refresh()
            }
            Toast.makeText(
                app,
                if (ok) (if (freeze) "Frozen $packageName" else "Unfrozen $packageName")
                else "Root required to freeze/unfreeze",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun setHiddenPinAndHide(pin: String, pkg: String) {
        viewModelScope.launch {
            app.preferences.setHiddenPin(pin)
            hidePackageInternal(pkg, true)
        }
    }

    fun toggleHiddenPackage(pkg: String) {
        viewModelScope.launch {
            val currentlyHidden = settings.value.hiddenPackages.contains(pkg)
            hidePackageInternal(pkg, !currentlyHidden)
        }
    }

    private suspend fun hidePackageInternal(pkg: String, hide: Boolean) {
        app.preferences.setPackageHidden(pkg, hide)
        if (privilegeStatus.value.isRootAvailable) {
            val ok = app.privilegeController.hideAppWithRoot(pkg, hide)
            if (ok) app.appRepository.refresh()
            Toast.makeText(
                app,
                if (hide) "Hidden (Root) & moved to Vault" else "Unhidden & restored",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                app,
                if (hide) "Hidden in Vault" else "Restored from Vault",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    suspend fun getHiddenAppDetails(packages: Set<String>): List<HiddenAppInfo> =
        withContext(Dispatchers.IO) { app.appRepository.resolveHiddenApps(packages) }

    fun setShowSteps(v: Boolean) {
        viewModelScope.launch {
            app.preferences.setShowSteps(v)
            if (v && privilegeStatus.value.isRootAvailable) {
                withContext(Dispatchers.IO) {
                    runCatching { Shell.cmd("pm grant ${app.packageName} android.permission.ACTIVITY_RECOGNITION").exec() }
                }
            }
        }
    }

    fun setShowScreenTime(v: Boolean) {
        viewModelScope.launch {
            app.preferences.setShowScreenTime(v)
            if (v) {
                val granted = withContext(Dispatchers.IO) {
                    if (privilegeStatus.value.isRootAvailable) {
                        runCatching {
                            Shell.cmd("appops set ${app.packageName} GET_USAGE_STATS allow").exec().isSuccess
                        }.getOrDefault(false)
                    } else false
                }
                if (!granted && readTodayScreenTime().isEmpty()) {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { app.startActivity(intent) }
                }
            }
        }
    }

    fun setExtraPagesCount(count: Int) = viewModelScope.launch { app.preferences.setExtraPagesCount(count) }

    fun launchHiddenApp(pkg: String) {
        viewModelScope.launch {
            if (privilegeStatus.value.isRootAvailable) {
                app.privilegeController.hideAppWithRoot(pkg, false)
                delay(250)
                app.appRepository.refresh()
            }
            val entry = app.appRepository.apps.value.find { it.packageName == pkg }
            if (entry != null) {
                app.appRepository.launchApp(entry)
            } else {
                val launchIntent = app.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (launchIntent != null) runCatching { app.startActivity(launchIntent) }
            }
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
    fun setCornerRadiusDp(v: Int) = viewModelScope.launch { app.preferences.setCornerRadiusDp(v) }
    fun setIconSizeDp(v: Int) = viewModelScope.launch { app.preferences.setIconSizeDp(v) }
    fun setDrawerBgAlpha(v: Float) = viewModelScope.launch { app.preferences.setDrawerBgAlpha(v) }
    fun reorderPinnedHome(fromIndex: Int, toIndex: Int) = viewModelScope.launch { app.preferences.reorderPinnedHome(fromIndex, toIndex) }
    fun setPinnedHomePackages(pkgs: List<String>) = viewModelScope.launch { app.preferences.setPinnedHomePackages(pkgs) }
    fun setDockPackages(pkgs: List<String>) = viewModelScope.launch { app.preferences.setDockPackages(pkgs) }
    fun addPinnedHomeAt(pkg: String, index: Int? = null) = viewModelScope.launch { app.preferences.addPinnedHomeAt(pkg, index) }
    fun addDockPackage(pkg: String) = viewModelScope.launch { app.preferences.addDockPackage(pkg) }
    fun setWidgetHeight(widgetId: Int, heightDp: Int) = viewModelScope.launch { app.preferences.setWidgetHeight(widgetId, heightDp) }
    fun setWidgetHalfWidth(widgetId: Int, half: Boolean) = viewModelScope.launch { app.preferences.setWidgetHalfWidth(widgetId, half) }
    fun setWidgetOrder(ids: List<Int>) = viewModelScope.launch { app.preferences.setWidgetOrder(ids) }
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
    fun setIconShape(v: IconShape) = viewModelScope.launch { app.preferences.setIconShape(v) }
    fun setHiddenPin(v: String) = viewModelScope.launch { app.preferences.setHiddenPin(v) }
    fun setWebSearchProvider(v: WebSearchProvider) = viewModelScope.launch { app.preferences.setWebSearchProvider(v) }
    fun setWeatherLocation(city: String) {
        viewModelScope.launch {
            app.preferences.setWeatherLocation(city)
            if (city.isNotBlank()) refreshWeather(silent = false)
        }
    }
    fun setWeatherRefreshMinutes(mins: Int) = viewModelScope.launch { app.preferences.setWeatherRefreshMinutes(mins) }
    fun togglePinnedHome(pkg: String) = viewModelScope.launch { app.preferences.togglePinnedHome(pkg) }
    fun toggleDockPackage(pkg: String) = viewModelScope.launch { app.preferences.toggleDockPackage(pkg) }
    fun addWidgetId(id: Int) = viewModelScope.launch { app.preferences.addWidgetId(id) }
    fun removeWidgetId(id: Int) = viewModelScope.launch { app.preferences.removeWidgetId(id) }
}
