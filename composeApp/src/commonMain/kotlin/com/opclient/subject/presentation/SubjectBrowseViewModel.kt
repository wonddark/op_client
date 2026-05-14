package com.opclient.subject.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FeaturedSubjects {
    val list = listOf(
        "Science Fiction", "Mystery", "Romance", "Fantasy",
        "History", "Biography", "Science", "Philosophy",
        "Children", "Poetry", "Travel", "Cooking",
    )
}

data class SubjectBrowseUiState(val subjects: List<String> = FeaturedSubjects.list)

class SubjectBrowseViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SubjectBrowseUiState())
    val uiState: StateFlow<SubjectBrowseUiState> = _uiState.asStateFlow()
}
