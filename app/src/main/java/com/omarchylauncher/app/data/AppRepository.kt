package com.omarchylauncher.app.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import org.xmlpull.v1.XmlPullParser
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
    val isSystem: Boolean,
    val userHandle: UserHandle,
    val icon: ImageBitmap?,
    val monochromeIcon: ImageBitmap? = null,
    val hasCustomIcon: Boolean = false
) {
    val componentName: ComponentName get() = ComponentName(packageName, activityName)
}

data class HiddenAppInfo(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

data class IconPackInfo(
    val packageName: String,
    val label: String
)

private data class CachedLauncherIcons(
    val icon: ImageBitmap?,
    val monochromeIcon: ImageBitmap?
)

class AppRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Collapse bursts of package broadcasts/manual refresh requests into one scan.
    private val refreshRequests = Channel<Unit>(Channel.CONFLATED)
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val iconCache = android.util.LruCache<String, ImageBitmap>(256)
    private val iconMonoCache = android.util.LruCache<String, Boolean>(256)
    // Reuse decoded launcher icons across refreshes; the key changes when an app is updated.
    private val launcherIconCache = android.util.LruCache<String, CachedLauncherIcons>(192)

    private val _apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val apps: StateFlow<List<AppEntry>> = _apps.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _newlyInstalledPackages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val newlyInstalledPackages: SharedFlow<String> = _newlyInstalledPackages.asSharedFlow()

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            invalidateLauncherIconCache(packageName)
            refresh()
        }
        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            invalidateLauncherIconCache(packageName)
            if (!packageName.isNullOrBlank() && packageName != context.packageName) {
                _newlyInstalledPackages.tryEmit(packageName)
            }
            refresh()
        }
        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            invalidateLauncherIconCache(packageName)
            refresh()
        }
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            packageNames?.forEach(::invalidateLauncherIconCache)
            refresh()
        }
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) {
            packageNames?.forEach(::invalidateLauncherIconCache)
            refresh()
        }
    }

    private fun invalidateLauncherIconCache(packageName: String?) {
        if (packageName.isNullOrBlank()) return
        val marker = "|$packageName|"
        launcherIconCache.snapshot().keys
            .filter { it.contains(marker) }
            .forEach { key -> launcherIconCache.remove(key) }
    }

    init {
        scope.launch {
            for (ignored in refreshRequests) {
                try {
                    performRefresh()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    android.util.Log.e("AppRepository", "App refresh failed", failure)
                }
            }
        }
        runCatching { launcherApps.registerCallback(callback) }
        refresh()
    }

    fun refresh() {
        refreshRequests.trySend(Unit)
    }

    private suspend fun performRefresh() {
        _isLoading.value = true
        try {
            val myPkg = context.packageName
            val activities = runCatching {
                launcherApps.getActivityList(null, Process.myUserHandle())
            }.getOrDefault(emptyList())

            val themedPackMap = buildThemedPackMap()

            val entries = activities
                .filter { it.applicationInfo.packageName != myPkg }
                .map { info ->
                    val packageName = info.applicationInfo.packageName
                    val isSystem = (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val cacheKey = "${info.user.hashCode()}|$packageName|${info.componentName.className}|${info.applicationInfo.lastUpdateTime}"
                    val cachedIcons = launcherIconCache.get(cacheKey) ?: run {
                        val rawDrawable = runCatching { info.getBadgedIcon(0) }.getOrNull()
                        val icon = runCatching { rawDrawable?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()
                        val monochromeIcon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            runCatching {
                                val adaptive = rawDrawable as? AdaptiveIconDrawable
                                adaptive?.monochrome?.toBitmap(96, 96)?.asImageBitmap()
                            }.getOrNull()
                        } else null
                        CachedLauncherIcons(icon, monochromeIcon).also {
                            launcherIconCache.put(cacheKey, it)
                        }
                    }
                    val icon = cachedIcons.icon
                    var monoIcon = cachedIcons.monochromeIcon
                    if (monoIcon == null && themedPackMap.isNotEmpty()) {
                        val compKey = packageName + "/" + info.componentName.className
                        val spec = themedPackMap[compKey] ?: themedPackMap[packageName]
                        if (spec != null) {
                            val loaded = loadIconFromSpecWithMono(spec)
                            if (loaded != null && loaded.second) monoIcon = loaded.first
                        }
                    }
                    AppEntry(
                        packageName = info.applicationInfo.packageName,
                        activityName = info.componentName.className,
                        label = info.label?.toString() ?: info.applicationInfo.packageName,
                        isSystem = isSystem,
                        userHandle = info.user,
                        icon = icon,
                        monochromeIcon = monoIcon
                    )
                }
                .distinctBy { it.packageName + "/" + it.activityName }
                .sortedBy { it.label.lowercase() }

            _apps.value = entries
        } finally {
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

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val actions = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "app.lawnchair.icons.THEMED_ICON",
            "com.gau.go.launcherex.theme",
            "com.teslacoilsw.launcher.THEME"
        )
        val packs = mutableMapOf<String, IconPackInfo>()
        actions.forEach { action ->
            val resolved = runCatching {
                pm.queryIntentActivities(Intent(action), PackageManager.MATCH_DEFAULT_ONLY)
            }.getOrDefault(emptyList())
            resolved.forEach { ri ->
                val pkg = ri.activityInfo.packageName
                if (!packs.containsKey(pkg)) {
                    val label = runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg)
                    packs[pkg] = IconPackInfo(pkg, label)
                }
            }
        }
        return packs.values.sortedBy { it.label.lowercase() }
    }

    fun loadIconPackDrawables(packPkg: String): List<String> {
        return runCatching {
            val res = context.packageManager.getResourcesForApplication(packPkg)
            val xmlId = listOf("appfilter", "drawable").firstNotNullOfOrNull { name ->
                val id = res.getIdentifier(name, "xml", packPkg)
                if (id != 0) id else null
            } ?: return emptyList()

            val drawables = LinkedHashSet<String>()
            val parser = res.getXml(xmlId)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT && drawables.size < 600) {
                if (eventType == XmlPullParser.START_TAG) {
                    val d = parser.getAttributeValue(null, "drawable")
                    if (!d.isNullOrBlank()) drawables.add(d)
                }
                eventType = parser.next()
            }
            drawables.toList()
        }.getOrDefault(emptyList())
    }

    fun loadIconFromSpec(spec: String): ImageBitmap? = loadIconFromSpecWithMono(spec)?.first

    fun loadIconFromSpecWithMono(spec: String): Pair<ImageBitmap, Boolean>? {
        val cachedBmp = iconCache.get(spec)
        val cachedMono = iconMonoCache.get(spec)
        if (cachedBmp != null && cachedMono != null) return cachedBmp to cachedMono
        val parts = spec.split("/", limit = 2)
        if (parts.size != 2) return null
        val packPkg = parts[0]
        val drawableName = parts[1].removePrefix("@drawable/")
        return runCatching {
            val res = context.packageManager.getResourcesForApplication(packPkg)
            val id = res.getIdentifier(drawableName, "drawable", packPkg)
            if (id == 0) return null
            val drawable = res.getDrawable(id, null) ?: return null
            val rawBmp = drawable.toBitmap(96, 96)
            val isMono = isMonochromeBitmap(rawBmp)
            val bmp = rawBmp.asImageBitmap()
            iconCache.put(spec, bmp)
            iconMonoCache.put(spec, isMono)
            bmp to isMono
        }.getOrNull()
    }

    private fun isMonochromeBitmap(bmp: android.graphics.Bitmap): Boolean {
        val w = bmp.width
        val h = bmp.height
        if (w <= 0 || h <= 0) return false
        var total = 0
        var transparent = 0
        var opaque = 0
        var chromaSum = 0L
        val step = 4
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val px = bmp.getPixel(x, y)
                val a = (px ushr 24) and 0xFF
                total++
                if (a < 32) {
                    transparent++
                } else {
                    opaque++
                    val r = (px ushr 16) and 0xFF
                    val g = (px ushr 8) and 0xFF
                    val b = px and 0xFF
                    val maxC = maxOf(r, maxOf(g, b))
                    val minC = minOf(r, minOf(g, b))
                    chromaSum += (maxC - minC)
                }
                x += step
            }
            y += step
        }
        if (total == 0 || opaque == 0) return false
        val transparentRatio = transparent.toFloat() / total.toFloat()
        val avgChroma = chromaSum.toFloat() / opaque.toFloat()
        return transparentRatio >= 0.15f && avgChroma < 28f
    }

    private fun buildThemedPackMap(): Map<String, String> {
        val pm = context.packageManager
        val resolved = runCatching {
            pm.queryIntentActivities(Intent("app.lawnchair.icons.THEMED_ICON"), PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrDefault(emptyList())
        if (resolved.isEmpty()) return emptyMap()

        val map = HashMap<String, String>(512)
        for (ri in resolved) {
            val packPkg = ri.activityInfo.packageName
            runCatching {
                val res = pm.getResourcesForApplication(packPkg)
                for (xmlName in listOf("grayscale_icon_map", "appfilter")) {
                    val xmlId = res.getIdentifier(xmlName, "xml", packPkg)
                    if (xmlId == 0) continue
                    val parser = res.getXml(xmlId)
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            val drawable = parser.getAttributeValue(null, "drawable")?.removePrefix("@drawable/")
                            val pkg = parser.getAttributeValue(null, "package")
                            val comp = parser.getAttributeValue(null, "component")
                            if (!drawable.isNullOrBlank()) {
                                val spec = "$packPkg/$drawable"
                                if (!pkg.isNullOrBlank()) {
                                    map.putIfAbsent(pkg, spec)
                                }
                                if (!comp.isNullOrBlank() && comp.startsWith("ComponentInfo{") && comp.endsWith("}")) {
                                    val inner = comp.substring("ComponentInfo{".length, comp.length - 1)
                                    map.putIfAbsent(inner, spec)
                                    val slashIdx = inner.indexOf('/')
                                    if (slashIdx > 0) {
                                        map.putIfAbsent(inner.substring(0, slashIdx), spec)
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }
                }
            }
        }
        return map
    }
}
