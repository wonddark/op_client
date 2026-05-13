package com.opclient.ui.components

import androidx.compose.foundation.background
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
fun SubjectTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    Box(
        modifier = modifier
            .background(color = colors.accentLight, shape = RoundedCornerShape(AppShapes.radius))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        BasicText(
            text = text.uppercase(Locale.ROOT),
            style = typography.tag.copy(color = colors.accent),
        )
    }
}
