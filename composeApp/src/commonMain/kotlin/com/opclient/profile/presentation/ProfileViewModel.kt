package com.opclient.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.core.Result
import com.opclient.library.domain.LibraryRepository
import com.opclient.library.domain.Shelf
import com.opclient.profile.domain.UserProfileRepository
import com.opclient.readinglog.domain.ReadingLogSyncService
import com.opclient.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

data class ProfileUiState(
    val username: String = "",
    val profileName: String = "",
    val profilePhotoUrl: String? = null,
    val goalTarget: Int = 0,
    val progressCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val lastSyncEntriesCount: Int? = null,
)

sealed class ProfileIntent {
    data class SetUsername(val username: String) : ProfileIntent()
    data object Sync : ProfileIntent()
    data class SetGoal(val target: Int) : ProfileIntent()
    data object ClearGoal : ProfileIntent()
}

class ProfileViewModel(
    private val settingsRepository: SettingsRepository,
    private val profileRepository: UserProfileRepository,
    private val syncService: ReadingLogSyncService,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedUsername = settingsRepository.getUsername() ?: ""
            val year = currentYear()
            val goal = settingsRepository.getReadingGoal(year)
            _uiState.update { it.copy(username = savedUsername, goalTarget = goal?.target ?: 0) }
            if (savedUsername.isNotEmpty()) fetchProfile(savedUsername)
        }
        viewModelScope.launch {
            libraryRepository.getShelf(Shelf.READ).collect { entries ->
                val yearStart = yearStartEpoch(currentYear())
                _uiState.update { it.copy(progressCount = entries.count { it.addedAt >= yearStart }) }
            }
        }
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.SetUsername -> handleSetUsername(intent.username)
            ProfileIntent.Sync -> handleSync()
            is ProfileIntent.SetGoal -> handleSetGoal(intent.target)
            ProfileIntent.ClearGoal -> handleClearGoal()
        }
    }

    private fun handleSetUsername(username: String) {
        viewModelScope.launch {
            settingsRepository.setUsername(username)
            _uiState.update { it.copy(username = username, profileName = "") }
            if (username.isNotEmpty()) fetchProfile(username)
        }
    }

    private fun handleSync() {
        val username = _uiState.value.username
        if (username.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncError = null) }
            when (val result = syncService.sync(username)) {
                is Result.Success -> _uiState.update {
                    it.copy(isSyncing = false, lastSyncEntriesCount = result.value)
                }
                is Result.Failure -> _uiState.update {
                    it.copy(isSyncing = false, syncError = "Sync failed")
                }
            }
        }
    }

    private fun handleSetGoal(target: Int) {
        viewModelScope.launch {
            settingsRepository.setReadingGoal(currentYear(), target)
            _uiState.update { it.copy(goalTarget = target) }
        }
    }

    private fun handleClearGoal() {
        viewModelScope.launch {
            settingsRepository.clearReadingGoal(currentYear())
            _uiState.update { it.copy(goalTarget = 0) }
        }
    }

    private fun fetchProfile(username: String) {
        viewModelScope.launch {
            when (val result = profileRepository.getProfile(username)) {
                is Result.Success -> _uiState.update {
                    it.copy(profileName = result.value.displayName, profilePhotoUrl = result.value.photoUrl)
                }
                is Result.Failure -> { /* silent — profile name stays empty */ }
            }
        }
    }

    private fun currentYear(): Int =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year

    private fun yearStartEpoch(year: Int): Long =
        LocalDateTime(year, 1, 1, 0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}
