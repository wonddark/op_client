package com.opclient.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun IconSidebar(
    selected: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val borderColor = colors.border

    Column(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(colors.surface)
            .drawBehind {
                drawLine(
                    color = borderColor,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Destination.entries.forEach { destination ->
            val isSelected = destination == selected
            val bgColor = if (isSelected) colors.accentLight else colors.surface2
            val tint = if (isSelected) colors.accent else colors.textSecondary
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color = bgColor, shape = RoundedCornerShape(AppShapes.radius))
                    .semantics { contentDescription = destination.label }
                    .clickable(role = Role.Tab) { onSelect(destination) },
                contentAlignment = Alignment.Center,
            ) {
                DestinationIcon(destination = destination, tint = tint, size = 16.dp)
            }
        }
    }
}
