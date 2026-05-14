package com.opclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.opclient.ui.navigation.Destination
import com.opclient.ui.navigation.IconSidebar
import com.opclient.ui.theme.AppThemeTokens

@Composable
actual fun AppShell(
    selectedDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = AppThemeTokens.colors
    Row(modifier = Modifier.fillMaxSize().background(colors.background)) {
        IconSidebar(selected = selectedDestination, onSelect = onDestinationChange)
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}
