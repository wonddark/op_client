package com.opclient.searchinside

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.searchinside.domain.SearchInsideRepository
import com.opclient.searchinside.domain.SearchInsideResult
import com.opclient.searchinside.presentation.SearchInsideIntent
import com.opclient.searchinside.presentation.SearchInsideViewModel
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
import kotlin.test.assertNull

class SearchInsideViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun fakeRepo(result: Result<List<SearchInsideResult>, ApiError>) =
        object : SearchInsideRepository {
            override suspend fun search(query: String) = result
        }

    private fun result1() = SearchInsideResult("/works/OL1W", "Dune", "Herbert", null, "spice passage")
    private fun result2() = SearchInsideResult("/works/OL2W", "Foundation", "Asimov", null, "robot passage")

    @Test
    fun initialState_isEmpty() = runTest {
        val vm = SearchInsideViewModel(fakeRepo(Result.Success(emptyList())))
        assertFalse(vm.uiState.value.hasSearched)
        assertEquals(emptyList(), vm.uiState.value.results)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun search_loadsResults() = runTest {
        val expected = listOf(result1())
        val vm = SearchInsideViewModel(fakeRepo(Result.Success(expected)))
        vm.onIntent(SearchInsideIntent.Search("spice"))
        advanceUntilIdle()
        assertEquals(expected, vm.uiState.value.results)
        assertEquals(true, vm.uiState.value.hasSearched)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun search_replacesExistingResults() = runTest {
        var callCount = 0
        val repo = object : SearchInsideRepository {
            override suspend fun search(query: String) =
                if (callCount++ == 0) Result.Success(listOf(result1()))
                else Result.Success(listOf(result2()))
        }
        val vm = SearchInsideViewModel(repo)
        vm.onIntent(SearchInsideIntent.Search("spice"))
        advanceUntilIdle()
        vm.onIntent(SearchInsideIntent.Search("robot"))
        advanceUntilIdle()
        assertEquals(listOf(result2()), vm.uiState.value.results)
    }

    @Test
    fun clear_resetsState() = runTest {
        val vm = SearchInsideViewModel(fakeRepo(Result.Success(listOf(result1()))))
        vm.onIntent(SearchInsideIntent.Search("spice"))
        advanceUntilIdle()
        vm.onIntent(SearchInsideIntent.Clear)
        assertEquals(emptyList(), vm.uiState.value.results)
        assertFalse(vm.uiState.value.hasSearched)
        assertFalse(vm.uiState.value.isLoading)
    }
}
