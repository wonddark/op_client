// composeApp/src/commonMain/kotlin/com/opclient/App.kt
package com.opclient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.opclient.author.presentation.AuthorDetailScreen
import com.opclient.book.presentation.BookDetailScreen
import com.opclient.navigation.NavigationViewModel
import com.opclient.navigation.Screen
import com.opclient.search.presentation.SearchScreen
import com.opclient.ui.AppShell
import com.opclient.ui.navigation.Destination
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val navVm: NavigationViewModel = koinViewModel()
    val stack by navVm.stack.collectAsState()
    var selectedTab by remember { mutableStateOf(Destination.SEARCH) }

    AppShell(
        selectedDestination = selectedTab,
        onDestinationChange = { tab ->
            selectedTab = tab
            navVm.navigateToTab(tab)
        },
    ) {
        when (val screen = stack.last()) {
            Screen.Search -> SearchScreen(
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
            )
            is Screen.BookDetail -> BookDetailScreen(
                workKey = screen.workKey,
                onAuthorClick = { key -> navVm.navigateTo(Screen.AuthorDetail(key)) },
                onBack = { navVm.navigateBack() },
            )
            is Screen.AuthorDetail -> AuthorDetailScreen(
                authorKey = screen.authorKey,
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
                onBack = { navVm.navigateBack() },
            )
            Screen.SubjectList -> Box(Modifier.fillMaxSize())
            is Screen.SubjectDetail -> Box(Modifier.fillMaxSize())
        }
    }
}
