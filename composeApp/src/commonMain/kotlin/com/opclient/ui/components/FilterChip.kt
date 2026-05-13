package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens
import java.util.Locale

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    val shape = RoundedCornerShape(AppShapes.radius)
    val bgColor = if (selected) colors.accentLight else colors.surface
    val textColor = if (selected) colors.accent else colors.textSecondary
    val borderColor = if (selected) colors.accent else colors.border

    Box(
        modifier = modifier
            .background(color = bgColor, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        BasicText(
            text = label.uppercase(Locale.ROOT),
            style = typography.tag.copy(color = textColor),
        )
    }
}
