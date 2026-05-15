package com.opclient.lists.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.core.Result
import com.opclient.lists.domain.ListSeed
import com.opclient.lists.domain.ListsRepository
import com.opclient.lists.domain.ReadingList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ListsUiState(
    val lists: List<ReadingList> = emptyList(),
    val selectedList: ReadingList? = null,
    val seeds: List<ListSeed> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingSeeds: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
)

sealed class ListsIntent {
    data object Load : ListsIntent()
    data class SelectList(val list: ReadingList) : ListsIntent()
    data object DeselectList : ListsIntent()
    data class CreateList(val name: String, val description: String) : ListsIntent()
    data class DeleteList(val list: ReadingList) : ListsIntent()
    data class RemoveSeed(val workKey: String) : ListsIntent()
    data object ShowCreateDialog : ListsIntent()
    data object HideCreateDialog : ListsIntent()
}

class ListsViewModel(private val repository: ListsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ListsUiState())
    val uiState: StateFlow<ListsUiState> = _uiState.asStateFlow()

    init {
        loadLists()
    }

    fun onIntent(intent: ListsIntent) {
        when (intent) {
            ListsIntent.Load -> loadLists()
            is ListsIntent.SelectList -> selectList(intent.list)
            ListsIntent.DeselectList -> _uiState.update { it.copy(selectedList = null, seeds = emptyList()) }
            is ListsIntent.CreateList -> createList(intent.name, intent.description)
            is ListsIntent.DeleteList -> deleteList(intent.list)
            is ListsIntent.RemoveSeed -> removeSeed(intent.workKey)
            ListsIntent.ShowCreateDialog -> _uiState.update { it.copy(showCreateDialog = true) }
            ListsIntent.HideCreateDialog -> _uiState.update { it.copy(showCreateDialog = false) }
        }
    }

    private fun loadLists() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = repository.getLists()) {
                is Result.Success -> _uiState.update { it.copy(lists = result.value, isLoading = false) }
                is Result.Failure -> _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun selectList(list: ReadingList) {
        _uiState.update { it.copy(selectedList = list, seeds = emptyList(), isLoadingSeeds = true) }
        viewModelScope.launch {
            when (val result = repository.getSeeds(list.key)) {
                is Result.Success -> _uiState.update { it.copy(seeds = result.value, isLoadingSeeds = false) }
                is Result.Failure -> _uiState.update { it.copy(isLoadingSeeds = false) }
            }
        }
    }

    private fun createList(name: String, description: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(showCreateDialog = false) }
            when (val result = repository.createList(name, description)) {
                is Result.Success -> _uiState.update { it.copy(lists = it.lists + result.value) }
                is Result.Failure -> _uiState.update { it.copy(error = "Failed to create list") }
            }
        }
    }

    private fun deleteList(list: ReadingList) {
        viewModelScope.launch {
            when (repository.deleteList(list.key)) {
                is Result.Success -> _uiState.update {
                    val newLists = it.lists - list
                    val newSelected = if (it.selectedList?.key == list.key) null else it.selectedList
                    it.copy(
                        lists = newLists,
                        selectedList = newSelected,
                        seeds = if (newSelected == null) emptyList() else it.seeds,
                    )
                }
                is Result.Failure -> _uiState.update { it.copy(error = "Failed to delete list") }
            }
        }
    }

    private fun removeSeed(workKey: String) {
        val list = _uiState.value.selectedList ?: return
        viewModelScope.launch {
            when (repository.removeSeed(list.key, workKey)) {
                is Result.Success -> _uiState.update { it.copy(seeds = it.seeds.filter { s -> s.workKey != workKey }) }
                is Result.Failure -> _uiState.update { it.copy(error = "Failed to remove seed") }
            }
        }
    }
}
