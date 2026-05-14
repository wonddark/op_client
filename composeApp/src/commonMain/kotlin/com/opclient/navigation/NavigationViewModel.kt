package com.opclient.navigation

import androidx.lifecycle.ViewModel
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

    fun navigateToTab() {
        _stack.update { listOf(Screen.Search) }
    }
}
