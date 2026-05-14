package com.opclient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.opclient.search.presentation.SearchScreen
import com.opclient.ui.AppShell
import com.opclient.ui.navigation.Destination

@Composable
fun App() {
    var selected by remember { mutableStateOf(Destination.SEARCH) }
    AppShell(
        selectedDestination = selected,
        onDestinationChange = { selected = it },
    ) {
        when (selected) {
            Destination.SEARCH -> SearchScreen(onBookClick = {})
            else -> Box(modifier = Modifier.fillMaxSize())
        }
    }
}
