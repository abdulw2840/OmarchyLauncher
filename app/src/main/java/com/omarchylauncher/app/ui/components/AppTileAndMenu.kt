package com.omarchylauncher.app.ui.components

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.omarchylauncher.app.data.AppEntry
import com.omarchylauncher.app.data.IconShape
import com.omarchylauncher.app.ui.theme.LocalKaruiPalette
import com.omarchylauncher.app.ui.theme.LocalUiScale

fun iconShapeToComposeShape(iconShape: IconShape): Shape = when (iconShape) {
    IconShape.SQUARE -> RectangleShape
    IconShape.CIRCLE -> CircleShape
    IconShape.IOS -> RoundedCornerShape(percent = 24)
    IconShape.ONEUI -> RoundedCornerShape(percent = 36)
}

@Composable
fun KaruiAppTile(
    app: AppEntry,
    showLabel: Boolean,
    monochrome: Boolean,
    hapticsEnabled: Boolean,
    iconShape: IconShape = IconShape.SQUARE,
    iconSize: Dp = 44.dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current
    val haptic = LocalHapticFeedback.current
    val frameShape = iconShapeToComposeShape(iconShape)

    val themedBitmap = if (!app.hasCustomIcon && monochrome && app.monochromeIcon != null) app.monochromeIcon else app.icon
    val themedFilter = when {
        app.hasCustomIcon -> null
        monochrome && app.monochromeIcon != null -> ColorFilter.tint(palette.accent, BlendMode.SrcIn)
        monochrome -> ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
        else -> null
    }

    val clickModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }
        )
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconSize * scale)
                .clip(frameShape)
                .background(palette.surface)
                .border(2.dp, palette.border, frameShape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (themedBitmap != null) {
                Image(
                    bitmap = themedBitmap,
                    contentDescription = app.label,
                    colorFilter = themedFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(frameShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Apps,
                    contentDescription = app.label,
                    tint = palette.accent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.label,
                color = palette.text,
                fontSize = (11 * scale).sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun KaruiAppContextDialog(
    app: AppEntry,
    shortcuts: List<ShortcutInfo>,
    isPinnedHome: Boolean,
    isPinnedDock: Boolean,
    isHidden: Boolean,
    isRootAvailable: Boolean,
    onDismiss: () -> Unit,
    onLaunchShortcut: (ShortcutInfo) -> Unit,
    onTogglePinHome: () -> Unit,
    onToggleDock: () -> Unit,
    onToggleHidden: () -> Unit,
    onOpenInfo: () -> Unit,
    onEditIconLabel: () -> Unit,
    onForceStop: () -> Unit,
    onFreeze: () -> Unit,
    onUninstall: (useRoot: Boolean) -> Unit
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(karuiShape())
                .background(palette.surface)
                .border(2.dp, palette.border, karuiShape())
                .padding(14.dp)
        ) {
            Text(
                text = app.label.uppercase(),
                color = palette.accent,
                fontWeight = FontWeight.Bold,
                fontSize = (15 * scale).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.packageName,
                color = palette.gray,
                fontFamily = FontFamily.Monospace,
                fontSize = (11 * scale).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))
            KaruiDashedDivider()
            Spacer(modifier = Modifier.height(10.dp))

            if (shortcuts.isNotEmpty()) {
                Text(
                    text = "SHORTCUTS",
                    color = palette.gray,
                    fontSize = (10 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                shortcuts.forEach { sc ->
                    MenuActionButton(
                        label = sc.shortLabel?.toString() ?: sc.id,
                        onClick = {
                            onDismiss()
                            onLaunchShortcut(sc)
                        }
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }
                KaruiDashedDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MenuActionButton(
                    label = "App Info",
                    onClick = { onDismiss(); onOpenInfo() },
                    modifier = Modifier.weight(1f)
                )
                if (!app.isSystem) {
                    MenuActionButton(
                        label = "Uninstall",
                        onClick = { onDismiss(); onUninstall(false) },
                        isDanger = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MenuActionButton(
                    label = if (isPinnedHome) "Remove" else "Pin Home",
                    onClick = { onTogglePinHome(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                MenuActionButton(
                    label = if (isPinnedDock) "Undock" else "Pin Dock",
                    onClick = { onToggleDock(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MenuActionButton(
                    label = if (isHidden) "Unhide" else "Hide App",
                    onClick = { onToggleHidden(); onDismiss() },
                    modifier = Modifier.weight(1f)
                )
                MenuActionButton(
                    label = "Edit Icon",
                    onClick = { onDismiss(); onEditIconLabel() },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isRootAvailable) {
                Spacer(modifier = Modifier.height(8.dp))
                KaruiDashedDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ROOT ACTIONS",
                    color = palette.accent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (10 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MenuActionButton(
                        label = "Force Stop",
                        onClick = { onForceStop(); onDismiss() },
                        modifier = Modifier.weight(1f)
                    )
                    MenuActionButton(
                        label = "Freeze",
                        onClick = { onFreeze(); onDismiss() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (isRootAvailable && app.isSystem) {
                Spacer(modifier = Modifier.height(6.dp))
                MenuActionButton(
                    label = "Root Uninstall",
                    onClick = { onDismiss(); onUninstall(true) },
                    isDanger = true
                )
            }
        }
    }
}

@Composable
private fun MenuActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(karuiShape())
            .background(palette.bg)
            .border(2.dp, palette.border, karuiShape())
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            color = if (isDanger) androidx.compose.ui.graphics.Color(0xFFEF5350) else palette.text,
            fontWeight = FontWeight.Bold,
            fontSize = (11 * scale).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
