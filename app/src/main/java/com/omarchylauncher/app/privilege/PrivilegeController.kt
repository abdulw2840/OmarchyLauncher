package com.omarchylauncher.app.privilege

import android.annotation.SuppressLint
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class PrivilegeStatus(
    val isRootAvailable: Boolean = false,
    val isShizukuAlive: Boolean = false,
    val isShizukuGranted: Boolean = false,
    val isAccessibilityActive: Boolean = false,
    val isDefaultHome: Boolean = false
)

class PrivilegeController(private val context: Context) {

    private val _status = MutableStateFlow(PrivilegeStatus())
    val status: StateFlow<PrivilegeStatus> = _status.asStateFlow()

    suspend fun refreshStatus() = withContext(Dispatchers.IO) {
        val rootOk = runCatching { Shell.getShell().isRoot }.getOrDefault(false)
        val shizukuAlive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val shizukuGranted = if (shizukuAlive) {
            runCatching { Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED }
                .getOrDefault(false)
        } else false
        val a11y = LauncherAccessibilityService.instance != null
        val roleMgr = context.getSystemService(RoleManager::class.java)
        val isHome = runCatching { roleMgr.isRoleHeld(RoleManager.ROLE_HOME) }.getOrDefault(false)

        _status.value = PrivilegeStatus(
            isRootAvailable = rootOk,
            isShizukuAlive = shizukuAlive,
            isShizukuGranted = shizukuGranted,
            isAccessibilityActive = a11y,
            isDefaultHome = isHome
        )
    }

    fun requestShizukuPermission() {
        runCatching {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1001)
            }
        }
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun lockScreen(): Boolean = withContext(Dispatchers.IO) {
        if (_status.value.isRootAvailable) {
            val res = runCatching { Shell.cmd("input keyevent 223").exec().isSuccess }.getOrDefault(false)
            if (res) return@withContext true
        }
        LauncherAccessibilityService.instance?.lockScreen() ?: false
    }

    @SuppressLint("WrongConstant")
    suspend fun expandNotifications(): Boolean = withContext(Dispatchers.IO) {
        // 1. Try StatusBarManager with EXPAND_STATUS_BAR permission on main/binder
        val sbOk = runCatching {
            val sbService = context.getSystemService("statusbar")
            val clazz = Class.forName("android.app.StatusBarManager")
            clazz.getMethod("expandNotificationsPanel").invoke(sbService)
            true
        }.getOrDefault(false)
        if (sbOk) return@withContext true

        // 2. Try Root
        if (_status.value.isRootAvailable) {
            val res = runCatching {
                Shell.cmd("cmd statusbar expand-notifications").exec().isSuccess
            }.getOrDefault(false)
            if (res) return@withContext true
        }

        // 3. Accessibility fallback
        LauncherAccessibilityService.instance?.expandNotifications() ?: false
    }

    suspend fun hideAppWithRoot(packageName: String, hide: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!_status.value.isRootAvailable) return@withContext false
        val primary = if (hide) "pm hide $packageName" else "pm unhide $packageName"
        val ok = runCatching { Shell.cmd(primary).exec().isSuccess }.getOrDefault(false)
        if (ok) return@withContext true
        val fallback = if (hide) "pm disable-user --user 0 $packageName" else "pm enable $packageName"
        runCatching { Shell.cmd(fallback).exec().isSuccess }.getOrDefault(false)
    }

    suspend fun forceStopApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!_status.value.isRootAvailable) return@withContext false
        runCatching { Shell.cmd("am force-stop $packageName").exec().isSuccess }.getOrDefault(false)
    }

    suspend fun freezeApp(packageName: String, freeze: Boolean): Boolean = withContext(Dispatchers.IO) {
        if (!_status.value.isRootAvailable) return@withContext false
        val cmd = if (freeze) "pm disable-user --user 0 $packageName" else "pm enable $packageName"
        runCatching { Shell.cmd(cmd).exec().isSuccess }.getOrDefault(false)
    }

    suspend fun rootUninstall(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (!_status.value.isRootAvailable) return@withContext false
        runCatching { Shell.cmd("pm uninstall --user 0 $packageName").exec().isSuccess }.getOrDefault(false)
    }
}
