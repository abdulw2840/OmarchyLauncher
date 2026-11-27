package com.omarchylauncher.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "omarchy_launcher_prefs")

data class LauncherSettings(
    val themeName: String = "tokyo-night",
    val uiScale: Float = 1.0f,
    val solidBackground: Boolean = true,
    val wallpaperDimAlpha: Float = 0.72f,
    val gridColumns: Int = 4,
    val gridRows: Int = 5,
    val drawerColumns: Int = 4,
    val drawerListMode: Boolean = false,
    val showAtAGlance: Boolean = true,
    val showSearchBar: Boolean = true,
    val showDock: Boolean = true,
    val showHomeLabels: Boolean = true,
    val showDrawerLabels: Boolean = true,
    val autoFocusDrawerSearch: Boolean = false,
    val doubleTapToSleep: Boolean = true,
    val swipeDownNotifications: Boolean = true,
    val hapticFeedback: Boolean = true,
    val monochromeIcons: Boolean = false,
    val pinnedHomePackages: List<String> = emptyList(),
    val dockPackages: List<String> = emptyList(),
    val hiddenPackages: Set<String> = emptySet()
)

class LauncherPreferences(private val context: Context) {

    private object Keys {
        val THEME_NAME = stringPreferencesKey("theme_name")
        val UI_SCALE = floatPreferencesKey("ui_scale")
        val SOLID_BG = booleanPreferencesKey("solid_bg")
        val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
        val GRID_COLS = intPreferencesKey("grid_cols")
        val GRID_ROWS = intPreferencesKey("grid_rows")
        val DRAWER_COLS = intPreferencesKey("drawer_cols")
        val DRAWER_LIST_MODE = booleanPreferencesKey("drawer_list_mode")
        val SHOW_AT_A_GLANCE = booleanPreferencesKey("show_at_a_glance")
        val SHOW_SEARCH_BAR = booleanPreferencesKey("show_search_bar")
        val SHOW_DOCK = booleanPreferencesKey("show_dock")
        val SHOW_HOME_LABELS = booleanPreferencesKey("show_home_labels")
        val SHOW_DRAWER_LABELS = booleanPreferencesKey("show_drawer_labels")
        val AUTO_FOCUS_SEARCH = booleanPreferencesKey("auto_focus_search")
        val DOUBLE_TAP_SLEEP = booleanPreferencesKey("double_tap_sleep")
        val SWIPE_DOWN_NOTIF = booleanPreferencesKey("swipe_down_notif")
        val HAPTIC = booleanPreferencesKey("haptic")
        val MONOCHROME_ICONS = booleanPreferencesKey("monochrome_icons")
        val PINNED_HOME = stringPreferencesKey("pinned_home_csv")
        val DOCK_PKGS = stringPreferencesKey("dock_pkgs_csv")
        val HIDDEN_PKGS = stringSetPreferencesKey("hidden_pkgs")
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data.map { p ->
        LauncherSettings(
            themeName = p[Keys.THEME_NAME] ?: "tokyo-night",
            uiScale = (p[Keys.UI_SCALE] ?: 1.0f).coerceIn(0.85f, 1.3f),
            solidBackground = p[Keys.SOLID_BG] ?: true,
            wallpaperDimAlpha = (p[Keys.WALLPAPER_DIM] ?: 0.72f).coerceIn(0.2f, 0.95f),
            gridColumns = (p[Keys.GRID_COLS] ?: 4).coerceIn(3, 6),
            gridRows = (p[Keys.GRID_ROWS] ?: 5).coerceIn(4, 7),
            drawerColumns = (p[Keys.DRAWER_COLS] ?: 4).coerceIn(3, 6),
            drawerListMode = p[Keys.DRAWER_LIST_MODE] ?: false,
            showAtAGlance = p[Keys.SHOW_AT_A_GLANCE] ?: true,
            showSearchBar = p[Keys.SHOW_SEARCH_BAR] ?: true,
            showDock = p[Keys.SHOW_DOCK] ?: true,
            showHomeLabels = p[Keys.SHOW_HOME_LABELS] ?: true,
            showDrawerLabels = p[Keys.SHOW_DRAWER_LABELS] ?: true,
            autoFocusDrawerSearch = p[Keys.AUTO_FOCUS_SEARCH] ?: false,
            doubleTapToSleep = p[Keys.DOUBLE_TAP_SLEEP] ?: true,
            swipeDownNotifications = p[Keys.SWIPE_DOWN_NOTIF] ?: true,
            hapticFeedback = p[Keys.HAPTIC] ?: true,
            monochromeIcons = p[Keys.MONOCHROME_ICONS] ?: false,
            pinnedHomePackages = parseCsv(p[Keys.PINNED_HOME]),
            dockPackages = parseCsv(p[Keys.DOCK_PKGS]),
            hiddenPackages = p[Keys.HIDDEN_PKGS] ?: emptySet()
        )
    }

    private fun parseCsv(raw: String?): List<String> =
        raw?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    suspend fun setThemeName(value: String) = context.dataStore.edit { it[Keys.THEME_NAME] = value }
    suspend fun setUiScale(value: Float) = context.dataStore.edit { it[Keys.UI_SCALE] = value.coerceIn(0.85f, 1.3f) }
    suspend fun setSolidBackground(value: Boolean) = context.dataStore.edit { it[Keys.SOLID_BG] = value }
    suspend fun setWallpaperDimAlpha(value: Float) = context.dataStore.edit { it[Keys.WALLPAPER_DIM] = value.coerceIn(0.2f, 0.95f) }
    suspend fun setGridColumns(value: Int) = context.dataStore.edit { it[Keys.GRID_COLS] = value.coerceIn(3, 6) }
    suspend fun setGridRows(value: Int) = context.dataStore.edit { it[Keys.GRID_ROWS] = value.coerceIn(4, 7) }
    suspend fun setDrawerColumns(value: Int) = context.dataStore.edit { it[Keys.DRAWER_COLS] = value.coerceIn(3, 6) }
    suspend fun setDrawerListMode(value: Boolean) = context.dataStore.edit { it[Keys.DRAWER_LIST_MODE] = value }
    suspend fun setShowAtAGlance(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_AT_A_GLANCE] = value }
    suspend fun setShowSearchBar(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_SEARCH_BAR] = value }
    suspend fun setShowDock(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_DOCK] = value }
    suspend fun setShowHomeLabels(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_HOME_LABELS] = value }
    suspend fun setShowDrawerLabels(value: Boolean) = context.dataStore.edit { it[Keys.SHOW_DRAWER_LABELS] = value }
    suspend fun setAutoFocusDrawerSearch(value: Boolean) = context.dataStore.edit { it[Keys.AUTO_FOCUS_SEARCH] = value }
    suspend fun setDoubleTapToSleep(value: Boolean) = context.dataStore.edit { it[Keys.DOUBLE_TAP_SLEEP] = value }
    suspend fun setSwipeDownNotifications(value: Boolean) = context.dataStore.edit { it[Keys.SWIPE_DOWN_NOTIF] = value }
    suspend fun setHapticFeedback(value: Boolean) = context.dataStore.edit { it[Keys.HAPTIC] = value }
    suspend fun setMonochromeIcons(value: Boolean) = context.dataStore.edit { it[Keys.MONOCHROME_ICONS] = value }

    suspend fun togglePinnedHome(pkg: String) {
        context.dataStore.edit { p ->
            val list = parseCsv(p[Keys.PINNED_HOME]).toMutableList()
            if (list.contains(pkg)) list.remove(pkg) else list.add(pkg)
            p[Keys.PINNED_HOME] = list.joinToString(",")
        }
    }

    suspend fun toggleDockPackage(pkg: String) {
        context.dataStore.edit { p ->
            val list = parseCsv(p[Keys.DOCK_PKGS]).toMutableList()
            if (list.contains(pkg)) list.remove(pkg) else if (list.size < 6) list.add(pkg)
            p[Keys.DOCK_PKGS] = list.joinToString(",")
        }
    }

    suspend fun toggleHiddenPackage(pkg: String) {
        context.dataStore.edit { p ->
            val set = (p[Keys.HIDDEN_PKGS] ?: emptySet()).toMutableSet()
            if (!set.add(pkg)) set.remove(pkg)
            p[Keys.HIDDEN_PKGS] = set
        }
    }

    suspend fun seedDefaultsIfEmpty(dockDefaults: List<String>, homeDefaults: List<String>) {
        context.dataStore.edit { p ->
            if (!p.contains(Keys.DOCK_PKGS) && dockDefaults.isNotEmpty()) {
                p[Keys.DOCK_PKGS] = dockDefaults.joinToString(",")
            }
            if (!p.contains(Keys.PINNED_HOME) && homeDefaults.isNotEmpty()) {
                p[Keys.PINNED_HOME] = homeDefaults.joinToString(",")
            }
        }
    }
}
