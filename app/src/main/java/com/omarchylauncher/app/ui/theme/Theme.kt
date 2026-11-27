package com.omarchylauncher.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

@Immutable
data class KaruiPalette(
    val key: String,
    val bg: Color,
    val surface: Color,
    val border: Color,
    val text: Color,
    val accent: Color,
    val accentTxt: Color,
    val gray: Color
) {
    val isDark: Boolean get() = bg.luminance() < 0.5f
}

val KaruiThemes: List<KaruiPalette> = listOf(
    KaruiPalette("tokyo-night", Color(0xFF1A1B26), Color(0xFF24283C), Color(0xFF7AA2F7), Color(0xFFA9B1D6), Color(0xFF7AA2F7), Color(0xFF1D202F), Color(0xFF565F89)),
    KaruiPalette("gruvbox", Color(0xFF282828), Color(0xFF3C3836), Color(0xFFD79921), Color(0xFFEBDBB2), Color(0xFFFABD2F), Color(0xFF282828), Color(0xFF928374)),
    KaruiPalette("gruvbox-material", Color(0xFF282828), Color(0xFF32302F), Color(0xFF6B6259), Color(0xFFDDC7A1), Color(0xFFA9B665), Color(0xFF282828), Color(0xFF928374)),
    KaruiPalette("gruvbox-light", Color(0xFFFBF1C7), Color(0xFFF2E5BC), Color(0xFFBDAE93), Color(0xFF3C3836), Color(0xFFD65D0E), Color(0xFFFBF1C7), Color(0xFF928374)),
    KaruiPalette("catppuccin", Color(0xFF1E1E2E), Color(0xFF313244), Color(0xFFCBA6F7), Color(0xFFCDD6F4), Color(0xFFCBA6F7), Color(0xFF11111B), Color(0xFF6C7086)),
    KaruiPalette("catppuccin-frappe", Color(0xFF303446), Color(0xFF414559), Color(0xFFCA9EE6), Color(0xFFC6D0F5), Color(0xFFCA9EE6), Color(0xFF232634), Color(0xFF737994)),
    KaruiPalette("nord", Color(0xFF2E3440), Color(0xFF3B4252), Color(0xFF88C0D0), Color(0xFFECEFF4), Color(0xFF88C0D0), Color(0xFF2E3440), Color(0xFF616E88)),
    KaruiPalette("everforest", Color(0xFF2D353B), Color(0xFF3D484D), Color(0xFFA7C080), Color(0xFFD3C6AA), Color(0xFFA7C080), Color(0xFF232A2E), Color(0xFF859289)),
    KaruiPalette("everforest-light", Color(0xFFFDF6E3), Color(0xFFEEE8D5), Color(0xFF859900), Color(0xFF586E75), Color(0xFF859900), Color(0xFFFDF6E3), Color(0xFF93A1A1)),
    KaruiPalette("rose-pine", Color(0xFF191724), Color(0xFF1F1D2E), Color(0xFF524F67), Color(0xFFE0DEF4), Color(0xFFEBBCBA), Color(0xFF191724), Color(0xFF6E6A86)),
    KaruiPalette("monokai", Color(0xFF272822), Color(0xFF3E3D32), Color(0xFFF92672), Color(0xFFF8F8F2), Color(0xFFFD971F), Color(0xFF272822), Color(0xFFA6E22E)),
    KaruiPalette("monokai-pro", Color(0xFF2D2A2E), Color(0xFF403E41), Color(0xFF5B595C), Color(0xFFFCFCFA), Color(0xFFFFD866), Color(0xFF2D2A2E), Color(0xFF727072)),
    KaruiPalette("sol-dark", Color(0xFF002B36), Color(0xFF073642), Color(0xFF2AA198), Color(0xFFEEE8D5), Color(0xFFB58900), Color(0xFF002B36), Color(0xFF586E75)),
    KaruiPalette("sol-light", Color(0xFFFDF6E3), Color(0xFFEEE8D5), Color(0xFF93A1A1), Color(0xFF657B83), Color(0xFF268BD2), Color(0xFFFDF6E3), Color(0xFF93A1A1)),
    KaruiPalette("zenburn", Color(0xFF3F3F3F), Color(0xFF4A4A4A), Color(0xFF6B6B6B), Color(0xFFDCDCCC), Color(0xFFF0DFAF), Color(0xFF3F3F3F), Color(0xFF7F7F7F)),
    KaruiPalette("crt-amber", Color(0xFF1A0F00), Color(0xFF2B1A00), Color(0xFFFFB000), Color(0xFFFFB000), Color(0xFFFFCC44), Color(0xFF1A0F00), Color(0xFF8A5A00)),
    KaruiPalette("sepia-terminal", Color(0xFF1A1408), Color(0xFF2A2010), Color(0xFF8A7A3A), Color(0xFFD4C890), Color(0xFFF0E0A0), Color(0xFF1A1408), Color(0xFF6A5F30)),
    KaruiPalette("charcoal", Color(0xFF1C1C1E), Color(0xFF2A2A2D), Color(0xFF4A4A4E), Color(0xFFE5E5E7), Color(0xFFA1A1A6), Color(0xFF1C1C1E), Color(0xFF6E6E73)),
    KaruiPalette("amoled", Color(0xFF000000), Color(0xFF121212), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFFFFFFFF), Color(0xFF000000), Color(0xFF888888)),
    KaruiPalette("purple", Color(0xFFFFFFFF), Color(0xFFF5F5F5), Color(0xFF8E24AA), Color(0xFF333333), Color(0xFF8E24AA), Color(0xFFFFFFFF), Color(0xFF888888)),
    KaruiPalette("paper", Color(0xFFF2EDE3), Color(0xFFE6DFD1), Color(0xFF8A7A5C), Color(0xFF2B2B28), Color(0xFFB04A3A), Color(0xFFF2EDE3), Color(0xFFA49A86)),
    KaruiPalette("papercolor-light", Color(0xFFEEEEEE), Color(0xFFE4E4E4), Color(0xFFA8A8A8), Color(0xFF444444), Color(0xFF0087AF), Color(0xFFFFFFFF), Color(0xFF878787)),
    KaruiPalette("sepia", Color(0xFFF4ECD8), Color(0xFFEBE0C7), Color(0xFFB09B73), Color(0xFF3D3325), Color(0xFFA0622B), Color(0xFFF4ECD8), Color(0xFF8A7A5F)),
    KaruiPalette("slate", Color(0xFFF8FAFC), Color(0xFFEEF2F6), Color(0xFFA8B6C8), Color(0xFF1E293B), Color(0xFF475569), Color(0xFFF8FAFC), Color(0xFF7B8794)),
    KaruiPalette("newsprint", Color(0xFFF5F3EE), Color(0xFFEBE8E1), Color(0xFFB8B3A8), Color(0xFF1A1A1A), Color(0xFF1A1A1A), Color(0xFFF5F3EE), Color(0xFF7A776F))
)

fun findKaruiPalette(key: String): KaruiPalette =
    KaruiThemes.find { it.key == key } ?: KaruiThemes.first()

val LocalKaruiPalette = staticCompositionLocalOf { KaruiThemes.first() }
val LocalUiScale = staticCompositionLocalOf { 1.0f }

private val KaruiShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

@Composable
fun OmarchyTheme(
    themeName: String = "tokyo-night",
    uiScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val palette = findKaruiPalette(themeName)

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.accentTxt,
            primaryContainer = palette.surface,
            onPrimaryContainer = palette.accent,
            secondary = palette.border,
            onSecondary = palette.accentTxt,
            tertiary = palette.accent,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface,
            onSurfaceVariant = palette.gray,
            outline = palette.border,
            outlineVariant = palette.border,
            error = Color(0xFFEF5350),
            errorContainer = Color(0xFF5F1D1D),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.accentTxt,
            primaryContainer = palette.surface,
            onPrimaryContainer = palette.accent,
            secondary = palette.border,
            onSecondary = palette.accentTxt,
            tertiary = palette.accent,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface,
            onSurfaceVariant = palette.gray,
            outline = palette.border,
            outlineVariant = palette.border,
            error = Color(0xFFB3261E),
            errorContainer = Color(0xFFF9DEDC),
            onErrorContainer = Color(0xFF410E0B)
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !palette.isDark
                controller.isAppearanceLightNavigationBars = !palette.isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalKaruiPalette provides palette,
        LocalUiScale provides uiScale,
        LocalContentColor provides palette.text
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = KaruiShapes
        ) {
            CompositionLocalProvider(LocalContentColor provides palette.text) {
                content()
            }
        }
    }
}
