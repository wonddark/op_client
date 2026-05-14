package com.opclient.subject.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class SubjectDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val subjectName: String = "",
    val works: List<SubjectWork> = emptyList(),
    val workCount: Int = 0,
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
)

sealed class SubjectDetailIntent {
    data class Load(val subjectName: String) : SubjectDetailIntent()
    data object LoadMore : SubjectDetailIntent()
    data object Retry : SubjectDetailIntent()
}

sealed class SubjectDetailEffect {
    data class LoadError(val error: ApiError) : SubjectDetailEffect()
}

class SubjectDetailViewModel(private val repository: SubjectRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SubjectDetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<SubjectDetailEffect> = _effects.asSharedFlow()

    private val pageSize = 12
    private var lastSubjectName: String = ""

    fun onIntent(intent: SubjectDetailIntent) {
        when (intent) {
            is SubjectDetailIntent.Load -> load(intent.subjectName)
            SubjectDetailIntent.LoadMore -> loadMore()
            SubjectDetailIntent.Retry -> if (lastSubjectName.isNotEmpty()) load(lastSubjectName)
        }
    }

    private fun load(subjectName: String) {
        lastSubjectName = subjectName
        viewModelScope.launch {
            _uiState.update { it.copy(status = DetailStatus.Loading, subjectName = subjectName, works = emptyList()) }
            when (val result = repository.getSubjectPage(subjectName, pageSize, 0)) {
                is Result.Success -> _uiState.update {
                    val works = result.value.works
                    it.copy(
                        status = DetailStatus.Success,
                        works = works,
                        workCount = result.value.workCount,
                        canLoadMore = works.size < result.value.workCount,
                    )
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = DetailStatus.Error) }
                    _effects.tryEmit(SubjectDetailEffect.LoadError(result.error))
                }
            }
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.canLoadMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = repository.getSubjectPage(lastSubjectName, pageSize, state.works.size)) {
                is Result.Success -> _uiState.update {
                    val newWorks = it.works + result.value.works
                    it.copy(
                        isLoadingMore = false,
                        works = newWorks,
                        canLoadMore = newWorks.size < it.workCount,
                    )
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoadingMore = false, canLoadMore = false) }
                    _effects.tryEmit(SubjectDetailEffect.LoadError(result.error))
                }
            }
        }
    }
}
