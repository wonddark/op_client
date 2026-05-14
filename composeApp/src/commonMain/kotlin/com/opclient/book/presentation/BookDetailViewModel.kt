// composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailViewModel.kt
package com.opclient.book.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.book.domain.BookDetail
import com.opclient.book.domain.BookRepository
import com.opclient.core.ApiError
import com.opclient.core.Result
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailStatus { Loading, Success, Error }

data class BookDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val book: BookDetail? = null,
)

sealed class BookDetailIntent {
    data class Load(val workKey: String) : BookDetailIntent()
    data object Retry : BookDetailIntent()
}

sealed class BookDetailEffect {
    data class LoadError(val error: ApiError) : BookDetailEffect()
}

class BookDetailViewModel(private val repository: BookRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<BookDetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<BookDetailEffect> = _effects.asSharedFlow()

    private var lastKey: String = ""

    fun onIntent(intent: BookDetailIntent) {
        when (intent) {
            is BookDetailIntent.Load -> load(intent.workKey)
            BookDetailIntent.Retry -> if (lastKey.isNotEmpty()) load(lastKey)
        }
    }

    private fun load(workKey: String) {
        lastKey = workKey
        viewModelScope.launch {
            _uiState.update { it.copy(status = DetailStatus.Loading) }
            when (val result = repository.getBook(workKey)) {
                is Result.Success -> _uiState.update {
                    it.copy(status = DetailStatus.Success, book = result.value)
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = DetailStatus.Error) }
                    _effects.tryEmit(BookDetailEffect.LoadError(result.error))
                }
            }
        }
    }
}
