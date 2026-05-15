package com.opclient.profile

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.library.domain.LibraryEntry
import com.opclient.library.domain.LibraryRepository
import com.opclient.library.domain.Shelf
import com.opclient.profile.domain.UserProfile
import com.opclient.profile.domain.UserProfileRepository
import com.opclient.profile.presentation.ProfileIntent
import com.opclient.profile.presentation.ProfileViewModel
import com.opclient.readinglog.domain.ReadingLogSyncService
import com.opclient.settings.domain.ReadingGoal
import com.opclient.settings.domain.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ProfileViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun fakeSettings(
        username: String? = null,
        goal: ReadingGoal? = null,
    ) = object : SettingsRepository {
        var storedUsername: String? = username
        var storedGoal: ReadingGoal? = goal
        override suspend fun getUsername() = storedUsername
        override suspend fun setUsername(u: String) { storedUsername = u }
        override suspend fun getReadingGoal(year: Int) = if (storedGoal?.year == year) storedGoal else null
        override suspend fun setReadingGoal(year: Int, target: Int) { storedGoal = ReadingGoal(year, target) }
        override suspend fun clearReadingGoal(year: Int) { storedGoal = null }
    }

    private fun fakeProfile(displayName: String = "Test User") = object : UserProfileRepository {
        override suspend fun getProfile(username: String): Result<UserProfile, ApiError> =
            Result.Success(UserProfile(username = username, displayName = displayName, bio = null, photoId = null))
    }

    private fun fakeSync(entriesResult: Int = 3) = object : ReadingLogSyncService {
        override suspend fun sync(username: String): Result<Int, ApiError> = Result.Success(entriesResult)
    }

    private fun fakeLibrary(initialReadEntries: List<LibraryEntry> = emptyList()) = object : LibraryRepository {
        val shelves = MutableStateFlow(initialReadEntries.associateBy { it.workKey })
        override fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>> =
            shelves.map { map -> map.values.filter { it.shelf == shelf }.toList() }
        override fun getCurrentShelf(workKey: String): Flow<Shelf?> =
            shelves.map { it[workKey]?.shelf }
        override suspend fun addToShelf(entry: LibraryEntry) { shelves.update { it + (entry.workKey to entry) } }
        override suspend fun removeFromShelf(workKey: String) { shelves.update { it - workKey } }
        override suspend fun moveToShelf(workKey: String, shelf: Shelf) {
            shelves.update { map ->
                map[workKey]?.let { e -> map + (workKey to e.copy(shelf = shelf)) } ?: map
            }
        }
    }

    @Test
    fun initialState_loadsUsernameAndGoal() = runTest {
        val year = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        val settings = fakeSettings(username = "mark", goal = ReadingGoal(year, 24))
        val vm = ProfileViewModel(settings, fakeProfile(), fakeSync(), fakeLibrary())
        advanceUntilIdle()
        assertEquals("mark", vm.uiState.value.username)
        assertEquals(24, vm.uiState.value.goalTarget)
    }

    @Test
    fun initialState_withUsername_fetchesProfileName() = runTest {
        val settings = fakeSettings(username = "mark")
        val vm = ProfileViewModel(settings, fakeProfile("Mark Reader"), fakeSync(), fakeLibrary())
        advanceUntilIdle()
        assertEquals("Mark Reader", vm.uiState.value.profileName)
    }

    @Test
    fun initialState_noUsername_profileNameEmpty() = runTest {
        val vm = ProfileViewModel(fakeSettings(), fakeProfile(), fakeSync(), fakeLibrary())
        advanceUntilIdle()
        assertEquals("", vm.uiState.value.profileName)
    }

    @Test
    fun setUsername_persistsAndFetchesProfile() = runTest {
        val settings = fakeSettings()
        val vm = ProfileViewModel(settings, fakeProfile("New User"), fakeSync(), fakeLibrary())
        vm.onIntent(ProfileIntent.SetUsername("newuser"))
        advanceUntilIdle()
        assertEquals("newuser", settings.storedUsername)
        assertEquals("newuser", vm.uiState.value.username)
        assertEquals("New User", vm.uiState.value.profileName)
    }

    @Test
    fun sync_withUsername_setsEntriesCount() = runTest {
        val settings = fakeSettings(username = "mark")
        val vm = ProfileViewModel(settings, fakeProfile(), fakeSync(entriesResult = 5), fakeLibrary())
        advanceUntilIdle()
        vm.onIntent(ProfileIntent.Sync)
        advanceUntilIdle()
        assertEquals(5, vm.uiState.value.lastSyncEntriesCount)
        assertFalse(vm.uiState.value.isSyncing)
    }

    @Test
    fun sync_withoutUsername_doesNothing() = runTest {
        val vm = ProfileViewModel(fakeSettings(), fakeProfile(), fakeSync(), fakeLibrary())
        advanceUntilIdle()
        vm.onIntent(ProfileIntent.Sync)
        advanceUntilIdle()
        assertNull(vm.uiState.value.lastSyncEntriesCount)
    }

    @Test
    fun setGoal_persistsAndUpdatesState() = runTest {
        val settings = fakeSettings()
        val vm = ProfileViewModel(settings, fakeProfile(), fakeSync(), fakeLibrary())
        vm.onIntent(ProfileIntent.SetGoal(12))
        advanceUntilIdle()
        assertEquals(12, vm.uiState.value.goalTarget)
        assertEquals(12, settings.storedGoal?.target)
    }

    @Test
    fun clearGoal_removesGoalFromStateAndStorage() = runTest {
        val year = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
        val settings = fakeSettings(goal = ReadingGoal(year, 24))
        val vm = ProfileViewModel(settings, fakeProfile(), fakeSync(), fakeLibrary())
        advanceUntilIdle()
        vm.onIntent(ProfileIntent.ClearGoal)
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.goalTarget)
        assertNull(settings.storedGoal)
    }

    @Test
    fun progressCount_countsOnlyBooksReadSinceYearStart() = runTest {
        val tz = TimeZone.currentSystemDefault()
        val year = Clock.System.now().toLocalDateTime(tz).year
        val yearStartMs = LocalDateTime(year, 1, 1, 0, 0).toInstant(tz).toEpochMilliseconds()
        val thisYear = LibraryEntry("/works/OL1W", "Book1", null, null, Shelf.READ, yearStartMs + 1_000)
        val lastYear = LibraryEntry("/works/OL2W", "Book2", null, null, Shelf.READ, yearStartMs - 1_000)
        val vm = ProfileViewModel(fakeSettings(), fakeProfile(), fakeSync(), fakeLibrary(listOf(thisYear, lastYear)))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.progressCount)
    }
}
