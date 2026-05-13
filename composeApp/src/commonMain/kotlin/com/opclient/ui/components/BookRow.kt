package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun BookRow(
    title: String,
    author: String,
    subject: String? = null,
    coverWidth: Dp = 34.dp,
    coverHeight: Dp = 48.dp,
    coverContent: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) colors.surface2 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = coverWidth, height = coverHeight)
                .clip(RoundedCornerShape(AppShapes.coverRadius)),
            content = coverContent,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
            BasicText(text = title, style = typography.bookTitle.copy(color = colors.textPrimary))
            Spacer(Modifier.height(3.dp))
            BasicText(text = author.uppercase(), style = typography.bookAuthor.copy(color = colors.textSecondary))
            if (subject != null) {
                Spacer(Modifier.height(5.dp))
                SubjectTag(text = subject)
            }
        }
    }
}
