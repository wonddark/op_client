package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens
import java.util.Locale

@Composable
fun BookCard(
    title: String,
    author: String,
    coverContent: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(AppShapes.coverRadius))
                .background(colors.surface2),
            content = coverContent,
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = title,
            style = typography.bookTitle.copy(color = colors.textPrimary),
            maxLines = 2,
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
            text = author.uppercase(Locale.ROOT),
            style = typography.bookAuthor.copy(color = colors.textSecondary),
            maxLines = 1,
        )
    }
}
