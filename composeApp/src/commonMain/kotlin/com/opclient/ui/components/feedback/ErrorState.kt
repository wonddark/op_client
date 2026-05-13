package com.opclient.ui.components.feedback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(text = message, style = typography.body.copy(color = colors.textSecondary))
        if (onRetry != null) {
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = "RETRY",
                style = typography.button.copy(color = colors.accent),
                modifier = Modifier.clickable(onClick = onRetry),
            )
        }
    }
}
