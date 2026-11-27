package com.omarchylauncher.app

import android.app.role.RoleManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omarchylauncher.app.ui.LauncherViewModel
import com.omarchylauncher.app.ui.screens.SettingsScreen
import com.omarchylauncher.app.ui.theme.OmarchyTheme

class SettingsActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshPrivileges()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val privilegeStatus by viewModel.privilegeStatus.collectAsStateWithLifecycle()
            val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            OmarchyTheme(
                themeName = settings.themeName,
                uiScale = settings.uiScale
            ) {
                SettingsScreen(
                    settings = settings,
                    privilegeStatus = privilegeStatus,
                    contentPadding = PaddingValues(top = top, bottom = bottom),
                    onRequestDefaultHome = {
                        val rm = getSystemService(RoleManager::class.java)
                        if (rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
                            homeRoleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                        }
                    },
                    onRefreshPrivileges = viewModel::refreshPrivileges,
                    onRequestShizuku = viewModel::requestShizukuPermission,
                    onOpenAccessibility = viewModel::openAccessibilitySettings,
                    onSetDoubleTapSleep = viewModel::setDoubleTapToSleep,
                    onSetSwipeDownNotif = viewModel::setSwipeDownNotifications,
                    onSetHaptic = viewModel::setHapticFeedback,
                    onSetGridColumns = viewModel::setGridColumns,
                    onSetShowAtAGlance = viewModel::setShowAtAGlance,
                    onSetShowSearchBar = viewModel::setShowSearchBar,
                    onSetShowDock = viewModel::setShowDock,
                    onSetShowHomeLabels = viewModel::setShowHomeLabels,
                    onSetMonochromeIcons = viewModel::setMonochromeIcons,
                    onSetIconShape = viewModel::setIconShape,
                    onSetShowSteps = viewModel::setShowSteps,
                    onSetShowScreenTime = viewModel::setShowScreenTime,
                    onSetDrawerColumns = viewModel::setDrawerColumns,
                    onSetDrawerListMode = viewModel::setDrawerListMode,
                    onSetShowDrawerLabels = viewModel::setShowDrawerLabels,
                    onSetAutoFocusSearch = viewModel::setAutoFocusDrawerSearch,
                    onSetWebSearchProvider = viewModel::setWebSearchProvider,
                    onSetHiddenPin = viewModel::setHiddenPin,
                    onSetWeatherLocation = viewModel::setWeatherLocation,
                    onSetWeatherRefreshMinutes = viewModel::setWeatherRefreshMinutes,
                    onRefreshWeather = { viewModel.refreshWeather(silent = false) },
                    onSetThemeName = viewModel::setThemeName,
                    onSetSolidBg = viewModel::setSolidBackground,
                    onSetWallpaperDim = viewModel::setWallpaperDimAlpha,
                    onSetUiScale = viewModel::setUiScale
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPrivileges()
    }
}
