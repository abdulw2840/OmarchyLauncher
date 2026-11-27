package com.omarchylauncher.app.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isSystem: Boolean,
    val userHandle: UserHandle,
    val icon: ImageBitmap?
) {
    val componentName: ComponentName get() = ComponentName(packageName, activityName)
}

data class HiddenAppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

class AppRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val launcherApps = context.getSystemService(LauncherApps::class.java)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = refresh()
    }

    init {
        runCatching { launcherApps.registerCallback(callback) }
        refresh()
    }

    fun refresh() {
        scope.launch {
            _isLoading.value = true
            val myPkg = context.packageName
            val activities = runCatching {
                launcherApps.getActivityList(null, Process.myUserHandle())
            }.getOrDefault(emptyList())

            val entries = activities
                .filter { it.applicationInfo.packageName != myPkg }
                .map { info ->
                    val isSystem = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val icon = runCatching {
                        info.getBadgedIcon(0).toBitmap(96, 96).asImageBitmap()
                    }.getOrNull()
                    AppEntry(
                        packageName = info.applicationInfo.packageName,
                        activityName = info.componentName.className,
                        label = info.label?.toString() ?: info.applicationInfo.packageName,
                        isSystem = isSystem,
                        userHandle = info.user,
                        icon = icon
                    )
                }
                .distinctBy { "${it.packageName}/${it.activityName}" }
                .sortedBy { it.label.lowercase() }

            _apps.value = entries
            _isLoading.value = false
        }
    }

    fun launchApp(entry: AppEntry) {
        runCatching {
            launcherApps.startMainActivity(entry.componentName, entry.userHandle, null, null)
        }
    }

    fun getAppShortcuts(entry: AppEntry): List<ShortcutInfo> {
        return runCatching {
            if (!launcherApps.hasShortcutHostPermission()) return emptyList()
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(entry.packageName)
                setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            }
            launcherApps.getShortcuts(query, entry.userHandle)?.take(4) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    fun launchShortcut(shortcut: ShortcutInfo) {
        runCatching {
            launcherApps.startShortcut(shortcut, null, null)
        }
    }

    fun openAppInfo(entry: AppEntry) {
        runCatching {
            launcherApps.startAppDetailsActivity(entry.componentName, entry.userHandle, null, null)
        }.onFailure {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${entry.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun requestUninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun resolveHiddenApps(packages: Set<String>): List<HiddenAppInfo> {
        val pm = context.packageManager
        val flags = PackageManager.MATCH_DISABLED_COMPONENTS or PackageManager.MATCH_UNINSTALLED_PACKAGES
        return packages.map { pkg ->
            val info = runCatching { pm.getApplicationInfo(pkg, flags) }.getOrNull()
            val label = if (info != null) {
                runCatching { pm.getApplicationLabel(info).toString() }.getOrDefault(pkg)
            } else pkg
            val icon = if (info != null) {
                runCatching { pm.getApplicationIcon(info).toBitmap(72, 72).asImageBitmap() }.getOrNull()
            } else null
            HiddenAppInfo(packageName = pkg, label = label, icon = icon)
        }.sortedBy { it.label.lowercase() }
    }
}
