package com.opclient.ui

import androidx.compose.runtime.Composable
import com.opclient.ui.navigation.Destination

@Composable
expect fun AppShell(
    selectedDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    content: @Composable () -> Unit,
)
