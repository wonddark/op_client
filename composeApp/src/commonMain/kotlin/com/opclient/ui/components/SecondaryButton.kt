package com.opclient.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens
import java.util.Locale

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    Box(
        modifier = modifier
            .border(width = 1.dp, color = colors.border, shape = RoundedCornerShape(AppShapes.radius))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text.uppercase(Locale.ROOT),
            style = typography.button.copy(color = colors.textSecondary),
        )
    }
}
