package com.opclient.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.auth.domain.AuthRepository
import com.opclient.core.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val username: String? = null,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val usernameInput: String = "",
    val passwordInput: String = "",
)

sealed class AuthIntent {
    data class UsernameChanged(val value: String) : AuthIntent()
    data class PasswordChanged(val value: String) : AuthIntent()
    data object Login : AuthIntent()
    data object Logout : AuthIntent()
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val loggedIn = repository.isLoggedIn()
            val username = if (loggedIn) repository.getLoggedInUsername() else null
            _uiState.update { it.copy(isLoggedIn = loggedIn, username = username) }
        }
    }

    fun onIntent(intent: AuthIntent) {
        when (intent) {
            is AuthIntent.UsernameChanged -> _uiState.update { it.copy(usernameInput = intent.value, error = null) }
            is AuthIntent.PasswordChanged -> _uiState.update { it.copy(passwordInput = intent.value, error = null) }
            AuthIntent.Login -> login()
            AuthIntent.Logout -> logout()
        }
    }

    private fun login() {
        val state = _uiState.value
        if (state.usernameInput.isBlank() || state.passwordInput.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (repository.login(state.usernameInput.trim(), state.passwordInput)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoggedIn = true, username = it.usernameInput, isLoading = false, passwordInput = "")
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = "Login failed. Check your credentials.")
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update { AuthUiState() }
        }
    }
}
