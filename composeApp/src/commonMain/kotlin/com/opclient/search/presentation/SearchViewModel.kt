package com.opclient.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.search.domain.Book
import com.opclient.search.domain.SearchRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_LIMIT = 20

data class SearchUiState(
    val query: String = "",
    val books: List<Book> = emptyList(),
    val totalFound: Int = 0,
    val offset: Int = 0,
    val status: SearchStatus = SearchStatus.Idle,
    val canLoadMore: Boolean = false,
)

enum class SearchStatus { Idle, Loading, LoadingMore, Success, Empty, Error }

sealed class SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent()
    data object Search : SearchIntent()
    data object LoadMore : SearchIntent()
    data object ClearSearch : SearchIntent()
}

sealed class SearchEffect {
    data class SearchError(val error: ApiError) : SearchEffect()
}

class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SearchEffect>(replay = 0, extraBufferCapacity = 1)
    val effects: SharedFlow<SearchEffect> = _effects.asSharedFlow()

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> _uiState.update { it.copy(query = intent.query) }
            SearchIntent.Search -> search()
            SearchIntent.LoadMore -> loadMore()
            SearchIntent.ClearSearch -> _uiState.value = SearchUiState()
        }
    }

    private fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(status = SearchStatus.Loading) }
            when (val result = repository.search(query, offset = 0, limit = SEARCH_LIMIT)) {
                is Result.Success -> {
                    val data = result.value
                    _uiState.update {
                        it.copy(
                            books = data.books,
                            totalFound = data.totalFound,
                            offset = data.offset,
                            status = if (data.books.isEmpty()) SearchStatus.Empty else SearchStatus.Success,
                            canLoadMore = data.books.size < data.totalFound,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = SearchStatus.Error) }
                    _effects.tryEmit(SearchEffect.SearchError(result.error))
                }
            }
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        if (!state.canLoadMore || state.status == SearchStatus.LoadingMore) return
        val nextOffset = state.offset + SEARCH_LIMIT
        viewModelScope.launch {
            _uiState.update { it.copy(status = SearchStatus.LoadingMore) }
            when (val result = repository.search(state.query, offset = nextOffset, limit = SEARCH_LIMIT)) {
                is Result.Success -> {
                    val data = result.value
                    _uiState.update {
                        val newBooks = it.books + data.books
                        it.copy(
                            books = newBooks,
                            totalFound = data.totalFound,
                            offset = data.offset,
                            status = SearchStatus.Success,
                            canLoadMore = newBooks.size < data.totalFound,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = SearchStatus.Success, canLoadMore = false) }
                    _effects.tryEmit(SearchEffect.SearchError(result.error))
                }
            }
        }
    }
}
