package com.opclient.ui.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = message.uppercase(),
            style = typography.sectionLabel.copy(color = colors.textSecondary),
        )
    }
}
