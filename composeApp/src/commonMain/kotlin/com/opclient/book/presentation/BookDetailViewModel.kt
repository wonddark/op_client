package com.opclient.book.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.book.domain.BookDetail
import com.opclient.book.domain.BookRepository
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.presentation.DetailStatus
import com.opclient.subject.domain.SubjectRepository
import com.opclient.subject.domain.SubjectWork
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val book: BookDetail? = null,
    val relatedWorks: List<SubjectWork> = emptyList(),
    val relatedSubjectName: String = "",
)

sealed class BookDetailIntent {
    data class Load(val workKey: String) : BookDetailIntent()
    data object Retry : BookDetailIntent()
}

sealed class BookDetailEffect {
    data class LoadError(val error: ApiError) : BookDetailEffect()
}

class BookDetailViewModel(
    private val repository: BookRepository,
    private val subjectRepository: SubjectRepository,
) : ViewModel() {
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
                is Result.Success -> {
                    val book = result.value
                    _uiState.update { it.copy(status = DetailStatus.Success, book = book) }
                    if (book.subjects.isNotEmpty()) {
                        loadRelatedWorks(book.subjects.first(), book.key)
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = DetailStatus.Error) }
                    _effects.tryEmit(BookDetailEffect.LoadError(result.error))
                }
            }
        }
    }

    private fun loadRelatedWorks(subjectName: String, currentBookKey: String) {
        viewModelScope.launch {
            when (val result = subjectRepository.getSubjectPage(subjectName, limit = 6, offset = 0)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        relatedWorks = result.value.works.filter { w -> w.key != currentBookKey },
                        relatedSubjectName = subjectName,
                    )
                }
                is Result.Failure -> { /* silent — main book detail stays Success */ }
            }
        }
    }
}
