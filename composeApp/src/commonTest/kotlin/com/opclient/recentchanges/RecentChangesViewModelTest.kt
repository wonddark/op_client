package com.opclient.recentchanges

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.recentchanges.domain.RecentChange
import com.opclient.recentchanges.domain.RecentChangesRepository
import com.opclient.recentchanges.presentation.RecentChangesIntent
import com.opclient.recentchanges.presentation.RecentChangesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class RecentChangesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun fakeRepo(result: Result<List<RecentChange>, ApiError>) =
        object : RecentChangesRepository {
            override suspend fun getRecentChanges() = result
        }

    private fun change(id: String = "1") =
        RecentChange(id = id, label = "Book added", targetKey = "/works/OL1W", addedAt = 1000L)

    @Test
    fun init_loadsChanges() = runTest {
        val changes = listOf(change("1"), change("2"))
        val vm = RecentChangesViewModel(fakeRepo(Result.Success(changes)))
        advanceUntilIdle()
        assertEquals(changes, vm.uiState.value.changes)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun refresh_reFetches() = runTest {
        var callCount = 0
        val batch1 = listOf(change("1"))
        val batch2 = listOf(change("2"))
        val repo = object : RecentChangesRepository {
            override suspend fun getRecentChanges() =
                if (callCount++ == 0) Result.Success(batch1) else Result.Success(batch2)
        }
        val vm = RecentChangesViewModel(repo)
        advanceUntilIdle()
        assertEquals(batch1, vm.uiState.value.changes)
        vm.onIntent(RecentChangesIntent.Refresh)
        advanceUntilIdle()
        assertEquals(batch2, vm.uiState.value.changes)
    }

    @Test
    fun load_whenAlreadyLoaded_doesNotReFetch() = runTest {
        var callCount = 0
        val repo = object : RecentChangesRepository {
            override suspend fun getRecentChanges(): Result<List<RecentChange>, ApiError> {
                callCount++
                return Result.Success(listOf(change("1")))
            }
        }
        val vm = RecentChangesViewModel(repo)
        advanceUntilIdle()
        vm.onIntent(RecentChangesIntent.Load)
        advanceUntilIdle()
        assertEquals(1, callCount)
    }

    @Test
    fun error_setsErrorState() = runTest {
        val vm = RecentChangesViewModel(
            fakeRepo(Result.Failure(ApiError.HttpError(500, "Server Error")))
        )
        advanceUntilIdle()
        assertNotNull(vm.uiState.value.error)
        assertEquals(emptyList(), vm.uiState.value.changes)
        assertFalse(vm.uiState.value.isLoading)
    }
}
