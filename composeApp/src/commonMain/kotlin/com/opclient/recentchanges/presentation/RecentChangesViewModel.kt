package com.opclient.recentchanges.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.core.Result
import com.opclient.recentchanges.domain.RecentChange
import com.opclient.recentchanges.domain.RecentChangesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecentChangesUiState(
    val changes: List<RecentChange> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class RecentChangesIntent {
    data object Load : RecentChangesIntent()
    data object Refresh : RecentChangesIntent()
}

class RecentChangesViewModel(
    private val repository: RecentChangesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentChangesUiState())
    val uiState: StateFlow<RecentChangesUiState> = _uiState.asStateFlow()

    init {
        onIntent(RecentChangesIntent.Load)
    }

    fun onIntent(intent: RecentChangesIntent) {
        when (intent) {
            RecentChangesIntent.Load -> load()
            RecentChangesIntent.Refresh -> refresh()
        }
    }

    private fun load() {
        if (_uiState.value.changes.isNotEmpty()) return
        fetch()
    }

    private fun refresh() {
        _uiState.update { it.copy(changes = emptyList()) }
        fetch()
    }

    private fun fetch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getRecentChanges()) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, changes = result.value)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load changes")
                }
            }
        }
    }
}
