package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    val bgColor = if (enabled) colors.accent else colors.border

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(AppShapes.radius))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text.uppercase(),
            style = typography.button.copy(color = Color.White),
        )
    }
}
