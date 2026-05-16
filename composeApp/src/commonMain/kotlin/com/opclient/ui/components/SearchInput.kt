package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit = {},
    placeholder: String = "FIND BOOKS, AUTHORS…",
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    val shape = RoundedCornerShape(AppShapes.radius)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                onSearch()
                true
            } else {
                false
            }
        },
        singleLine = true,
        textStyle = typography.body.copy(color = colors.textPrimary),
        cursorBrush = SolidColor(colors.textPrimary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = colors.surface, shape = shape)
                    .border(width = 1.dp, color = colors.border, shape = shape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = typography.body.copy(color = colors.textSecondary),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}
