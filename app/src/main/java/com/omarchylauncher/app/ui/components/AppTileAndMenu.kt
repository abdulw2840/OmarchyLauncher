package com.omarchylauncher.app.ui.components

import android.content.pm.ShortcutInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.zIndex
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

enum class IconGestureState {
    IDLE,
    PRESSED,
    LONG_PRESSED,
    DRAGGING,
    MENU_OPEN
}

fun Modifier.karuiAppHoldOrDrag(
    key: Any?,
    haptic: HapticFeedback?,
    onInteractionActiveChange: (Boolean) -> Unit = {},
    onTap: () -> Unit,
    onShowMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: () -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
): Modifier = this.pointerInput(key) {
    val touchSlop = viewConfiguration.touchSlop
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    awaitEachGesture {
        var gestureState = IconGestureState.IDLE
        val down = awaitFirstDown(requireUnconsumed = false)
        val downId = down.id
        val downPos = down.position
        gestureState = IconGestureState.PRESSED
        onInteractionActiveChange(true)

        var movedBeforeLongPress = false
        var releasedBeforeLongPress = false
        var wasConsumed = false

        try {
            val timedOut = withTimeoutOrNull(longPressTimeout) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val ch = event.changes.firstOrNull { it.id == downId }
                    if (ch == null || !ch.pressed) {
                        releasedBeforeLongPress = true
                        if (ch?.isConsumed == true) wasConsumed = true
                        break
                    }
                    if (ch.isConsumed) {
                        wasConsumed = true
                        break
                    }
                    if ((ch.position - downPos).getDistance() > touchSlop) {
                        movedBeforeLongPress = true
                        break
                    }
                }
            }

            if (releasedBeforeLongPress) {
                gestureState = IconGestureState.IDLE
                onInteractionActiveChange(false)
                if (!movedBeforeLongPress && !wasConsumed) {
                    onTap()
                }
                return@awaitEachGesture
            }
            if (movedBeforeLongPress || wasConsumed || timedOut != null) {
                gestureState = IconGestureState.IDLE
                onInteractionActiveChange(false)
                return@awaitEachGesture
            }

            // PRESSED -> LONG_PRESSED (haptic). Wait a short grace window before opening menu
            // so users intending to drag don't get a menu flash.
            gestureState = IconGestureState.LONG_PRESSED
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)

            var releasedDuringGrace = false
            withTimeoutOrNull(300L) {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Main)
                    val ch = event.changes.firstOrNull { it.id == downId }
                    if (ch == null || !ch.pressed) {
                        ch?.consume()
                        releasedDuringGrace = true
                        break
                    }
                    if ((ch.position - downPos).getDistance() > touchSlop) {
                        gestureState = IconGestureState.DRAGGING
                        onDragStart()
                        ch.consume()
                        break
                    }
                }
            }

            if (releasedDuringGrace) {
                gestureState = IconGestureState.MENU_OPEN
                onShowMenu()
                onInteractionActiveChange(false)
                return@awaitEachGesture
            }

            if (gestureState == IconGestureState.LONG_PRESSED) {
                gestureState = IconGestureState.MENU_OPEN
                onShowMenu()
            }

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val ch = event.changes.firstOrNull { it.id == downId }
                if (ch == null || !ch.pressed) {
                    ch?.consume()
                    if (gestureState == IconGestureState.DRAGGING) {
                        onDragEnd()
                    }
                    gestureState = IconGestureState.IDLE
                    onInteractionActiveChange(false)
                    break
                }
                if (gestureState != IconGestureState.DRAGGING) {
                    if ((ch.position - downPos).getDistance() > touchSlop) {
                        gestureState = IconGestureState.DRAGGING
                        onDismissMenu()
                        onDragStart()
                        ch.consume()
                    }
                } else {
                    ch.consume()
                    onDrag()
                }
            }
        } catch (cancelled: CancellationException) {
            if (gestureState == IconGestureState.DRAGGING) {
                onDragCancel()
            }
            gestureState = IconGestureState.IDLE
            onInteractionActiveChange(false)
            throw cancelled
        } catch (failure: Exception) {
            // Recover from gesture/input failures without swallowing fatal JVM errors.
            if (gestureState == IconGestureState.DRAGGING) {
                onDragCancel()
            }
            gestureState = IconGestureState.IDLE
            onInteractionActiveChange(false)
        }
    }
}

@Composable
fun KaruiAppTile(
    app: AppEntry,
    showLabel: Boolean,
    monochrome: Boolean,
    hapticsEnabled: Boolean,
    iconShape: IconShape = IconShape.SQUARE,
    iconSize: Dp = 44.dp,
    onClick: (() -> Unit)? = null,
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

    val clickModifier = when {
        onLongClick != null && onClick != null -> Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = {
                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }
        )
        onClick != null -> Modifier.clickable(onClick = onClick)
        else -> Modifier
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

    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .clip(karuiShape())
                .background(palette.surface)
                .border(2.dp, palette.border, karuiShape())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
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
