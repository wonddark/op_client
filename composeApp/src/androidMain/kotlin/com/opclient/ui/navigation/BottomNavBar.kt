package com.opclient.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun BottomNavBar(
    selected: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    val borderColor = colors.border

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colors.surface)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            },
    ) {
        Destination.entries.forEach { destination ->
            val isSelected = destination == selected
            val tint = if (isSelected) colors.accent else colors.textSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(destination) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                DestinationIcon(destination = destination, tint = tint, size = 20.dp)
                Spacer(Modifier.height(3.dp))
                BasicText(
                    text = destination.label.uppercase(),
                    style = typography.navLabel.copy(
                        color = tint,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light,
                    ),
                )
            }
        }
    }
}
