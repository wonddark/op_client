package com.opclient.subject.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.core.Result
import com.opclient.subject.domain.SubjectRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object FeaturedSubjects {
    val list = listOf(
        "Science Fiction", "Mystery", "Romance", "Fantasy",
        "History", "Biography", "Science", "Philosophy",
        "Children", "Poetry", "Travel", "Cooking",
    )
}

enum class SubjectSearchStatus { Idle, Loading, Success, Error }

data class SubjectBrowseUiState(
    val subjects: List<String> = FeaturedSubjects.list,
    val searchQuery: String = "",
    val searchResults: List<String> = emptyList(),
    val searchStatus: SubjectSearchStatus = SubjectSearchStatus.Idle,
)

@OptIn(FlowPreview::class)
class SubjectBrowseViewModel(private val repository: SubjectRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SubjectBrowseUiState())
    val uiState: StateFlow<SubjectBrowseUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(searchStatus = SubjectSearchStatus.Idle, searchResults = emptyList()) }
                        return@collectLatest
                    }
                    _uiState.update { it.copy(searchStatus = SubjectSearchStatus.Loading) }
                    when (val result = repository.searchSubjects(query)) {
                        is Result.Success -> _uiState.update {
                            it.copy(searchResults = result.value, searchStatus = SubjectSearchStatus.Success)
                        }
                        is Result.Failure -> _uiState.update { it.copy(searchStatus = SubjectSearchStatus.Error) }
                    }
                }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _uiState.update { it.copy(searchStatus = SubjectSearchStatus.Idle, searchResults = emptyList()) }
        }
        _searchQuery.value = query
    }
}
