package com.opclient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.opclient.author.presentation.AuthorDetailScreen
import com.opclient.book.presentation.BookDetailScreen
import com.opclient.library.presentation.LibraryScreen
import com.opclient.navigation.NavigationViewModel
import com.opclient.profile.presentation.ProfileScreen
import com.opclient.navigation.Screen
import com.opclient.recentchanges.presentation.RecentChangesScreen
import com.opclient.search.presentation.SearchScreen
import com.opclient.subject.presentation.SubjectBrowseScreen
import com.opclient.subject.presentation.SubjectDetailScreen
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
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
                onBack = { navVm.navigateBack() },
            )
            is Screen.AuthorDetail -> AuthorDetailScreen(
                authorKey = screen.authorKey,
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
                onBack = { navVm.navigateBack() },
            )
            Screen.SubjectList -> SubjectBrowseScreen(
                onSubjectClick = { name -> navVm.navigateTo(Screen.SubjectDetail(name)) },
            )
            is Screen.SubjectDetail -> SubjectDetailScreen(
                subjectName = screen.subjectName,
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
                onBack = { navVm.navigateBack() },
            )
            Screen.Library -> LibraryScreen(
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
                onProfileClick = { navVm.navigateTo(Screen.Profile) },
            )
            Screen.Profile -> ProfileScreen(
                onBack = { navVm.navigateBack() },
            )
            Screen.RecentChanges -> RecentChangesScreen(
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
                onAuthorClick = { key -> navVm.navigateTo(Screen.AuthorDetail(key)) },
            )
        }
    }
}
