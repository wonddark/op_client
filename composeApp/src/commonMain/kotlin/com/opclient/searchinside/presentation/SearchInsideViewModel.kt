package com.opclient.searchinside.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.core.Result
import com.opclient.searchinside.domain.SearchInsideRepository
import com.opclient.searchinside.domain.SearchInsideResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchInsideUiState(
    val results: List<SearchInsideResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false,
)

sealed class SearchInsideIntent {
    data class Search(val query: String) : SearchInsideIntent()
    data object Clear : SearchInsideIntent()
}

class SearchInsideViewModel(
    private val repository: SearchInsideRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchInsideUiState())
    val uiState: StateFlow<SearchInsideUiState> = _uiState.asStateFlow()

    fun onIntent(intent: SearchInsideIntent) {
        when (intent) {
            is SearchInsideIntent.Search -> search(intent.query)
            SearchInsideIntent.Clear -> _uiState.value = SearchInsideUiState()
        }
    }

    private fun search(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.search(query)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, results = result.value, hasSearched = true)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = "Search failed", hasSearched = true)
                }
            }
        }
    }
}
