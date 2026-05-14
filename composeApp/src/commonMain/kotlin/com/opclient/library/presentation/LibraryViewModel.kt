package com.opclient.library.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.library.domain.LibraryEntry
import com.opclient.library.domain.LibraryRepository
import com.opclient.library.domain.Shelf
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val selectedShelf: Shelf = Shelf.WANT_TO_READ,
    val entries: List<LibraryEntry> = emptyList(),
)

sealed class LibraryIntent {
    data class SelectShelf(val shelf: Shelf) : LibraryIntent()
}

class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private var shelfJob: Job? = null

    init {
        collectShelf(Shelf.WANT_TO_READ)
    }

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.SelectShelf -> {
                _uiState.update { it.copy(selectedShelf = intent.shelf) }
                collectShelf(intent.shelf)
            }
        }
    }

    private fun collectShelf(shelf: Shelf) {
        shelfJob?.cancel()
        shelfJob = viewModelScope.launch {
            repository.getShelf(shelf).collect { entries ->
                _uiState.update { it.copy(entries = entries) }
            }
        }
    }
}
