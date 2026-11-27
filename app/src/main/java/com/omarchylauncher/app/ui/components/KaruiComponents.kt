package com.omarchylauncher.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omarchylauncher.app.ui.theme.LocalKaruiPalette
import com.omarchylauncher.app.ui.theme.LocalUiScale

@Composable
fun KaruiBox(
    title: String,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 15.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.bg)
                .border(2.dp, palette.border, RectangleShape)
                .padding(
                    top = (contentPadding + 4.dp) * scale,
                    start = contentPadding * scale,
                    end = contentPadding * scale,
                    bottom = contentPadding * scale
                ),
            content = content
        )

        Text(
            text = title.uppercase(),
            color = palette.accent,
            fontSize = (12 * scale).sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (12 * scale).sp,
            modifier = Modifier
                .offset(x = 10.dp, y = (-6).dp)
                .background(palette.bg)
                .padding(horizontal = 6.dp)
        )
    }
}

@Composable
fun KaruiPivotHeader(
    title: String,
    tabs: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    topInset: Dp,
    modifier: Modifier = Modifier
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.0f to palette.bg,
                    0.82f to palette.bg,
                    1.0f to palette.bg.copy(alpha = 0f)
                )
            )
            .padding(
                top = topInset + (14 * scale).dp,
                start = (20 * scale).dp,
                end = (20 * scale).dp,
                bottom = (14 * scale).dp
            )
    ) {
        Text(
            text = title.lowercase(),
            color = palette.text,
            fontSize = (36 * scale).sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = (-1).sp,
            lineHeight = (38 * scale).sp
        )

        Spacer(modifier = Modifier.height((6 * scale).dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy((24 * scale).dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val active = index == selectedIndex
                Box(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(index) }
                        .then(
                            if (active) {
                                Modifier.drawBehind {
                                    drawLine(
                                        color = palette.accent,
                                        start = Offset(0f, size.height),
                                        end = Offset(size.width, size.height),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                            } else Modifier
                        )
                        .padding(bottom = 4.dp)
                ) {
                    Text(
                        text = label.lowercase(),
                        color = if (active) palette.accent else palette.text.copy(alpha = 0.35f),
                        fontSize = (22 * scale).sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun KaruiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor ?: palette.accent,
            contentColor = contentColor ?: palette.accentTxt,
            disabledContainerColor = (containerColor ?: palette.accent).copy(alpha = 0.4f),
            disabledContentColor = (contentColor ?: palette.accentTxt).copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(
            horizontal = (14 * scale).dp,
            vertical = (11 * scale).dp
        ),
        modifier = modifier
    ) {
        Text(
            text = text.uppercase(),
            fontWeight = FontWeight.Bold,
            fontSize = (13 * scale).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun KaruiSubtleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Box(
        modifier = modifier
            .background(palette.surface)
            .border(2.dp, palette.border, RectangleShape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = (12 * scale).dp, vertical = (9 * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = if (enabled) palette.text else palette.gray,
            fontWeight = FontWeight.Bold,
            fontSize = (12 * scale).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun KaruiChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalKaruiPalette.current
    val scale = LocalUiScale.current

    Box(
        modifier = modifier
            .background(if (selected) palette.accent else palette.bg)
            .border(2.dp, palette.border, RectangleShape)
            .clickable { onClick() }
            .padding(horizontal = (12 * scale).dp, vertical = (7 * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.uppercase(),
            color = if (selected) palette.accentTxt else palette.gray,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = (12 * scale).sp,
            maxLines = 1
        )
    }
}

@Composable
fun KaruiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalKaruiPalette.current
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        animationSpec = tween(150),
        label = "karuiSwitchThumb"
    )

    Box(
        modifier = modifier
            .size(width = 44.dp, height = 24.dp)
            .background(if (checked) palette.accent else palette.surface)
            .border(2.dp, palette.border, RectangleShape)
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset, y = 5.dp)
                .size(14.dp)
                .background(if (checked) palette.accentTxt else palette.gray)
        )
    }
}

@Composable
fun KaruiDashedDivider(modifier: Modifier = Modifier) {
    val palette = LocalKaruiPalette.current
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        drawLine(
            color = palette.border,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
    }
}
