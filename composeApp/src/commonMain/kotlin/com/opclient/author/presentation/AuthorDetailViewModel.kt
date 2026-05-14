// composeApp/src/commonMain/kotlin/com/opclient/author/presentation/AuthorDetailViewModel.kt
package com.opclient.author.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.author.domain.AuthorDetail
import com.opclient.author.domain.AuthorRepository
import com.opclient.presentation.DetailStatus
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

data class AuthorDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val author: AuthorDetail? = null,
)

sealed class AuthorDetailIntent {
    data class Load(val authorKey: String) : AuthorDetailIntent()
    data object Retry : AuthorDetailIntent()
}

sealed class AuthorDetailEffect {
    data class LoadError(val error: ApiError) : AuthorDetailEffect()
}

class AuthorDetailViewModel(private val repository: AuthorRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthorDetailUiState())
    val uiState: StateFlow<AuthorDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AuthorDetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<AuthorDetailEffect> = _effects.asSharedFlow()

    private var lastKey: String = ""

    fun onIntent(intent: AuthorDetailIntent) {
        when (intent) {
            is AuthorDetailIntent.Load -> load(intent.authorKey)
            AuthorDetailIntent.Retry -> if (lastKey.isNotEmpty()) load(lastKey)
        }
    }

    private fun load(authorKey: String) {
        lastKey = authorKey
        viewModelScope.launch {
            _uiState.update { it.copy(status = DetailStatus.Loading) }
            when (val result = repository.getAuthor(authorKey)) {
                is Result.Success -> _uiState.update {
                    it.copy(status = DetailStatus.Success, author = result.value)
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = DetailStatus.Error) }
                    _effects.tryEmit(AuthorDetailEffect.LoadError(result.error))
                }
            }
        }
    }
}
