package com.opclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.opclient.ui.navigation.BottomNavBar
import com.opclient.ui.navigation.Destination
import com.opclient.ui.theme.AppThemeTokens

@Composable
actual fun AppShell(
    selectedDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = AppThemeTokens.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Box(modifier = Modifier.weight(1f).statusBarsPadding()) { content() }
        BottomNavBar(
            selected = selectedDestination,
            onSelect = onDestinationChange,
            modifier = Modifier.navigationBarsPadding(),
        )
    }
}
