package com.opclient.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.opclient.ui.theme.AppThemeTokens
import java.util.Locale

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    BasicText(
        text = text.uppercase(Locale.ROOT),
        style = typography.sectionLabel.copy(color = colors.textSecondary),
        modifier = modifier,
    )
}
