package com.opclient.navigation

import androidx.lifecycle.ViewModel
import com.opclient.ui.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NavigationViewModel : ViewModel() {
    private val _stack = MutableStateFlow<List<Screen>>(listOf(Screen.Search))
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()

    fun navigateTo(screen: Screen) {
        _stack.update { it + screen }
    }

    fun navigateBack() {
        _stack.update { if (it.size > 1) it.dropLast(1) else it }
    }

    fun navigateToTab(destination: Destination) {
        _stack.update {
            when (destination) {
                Destination.SEARCH -> listOf(Screen.Search)
                Destination.BROWSE -> listOf(Screen.SubjectList)
                Destination.LIBRARY -> listOf(Screen.Library)
            }
        }
    }
}
