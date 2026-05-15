package com.opclient.lists

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.lists.domain.ListSeed
import com.opclient.lists.domain.ListsRepository
import com.opclient.lists.domain.ReadingList
import com.opclient.lists.presentation.ListsIntent
import com.opclient.lists.presentation.ListsViewModel
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
import kotlin.test.assertTrue

class ListsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun list(key: String = "/people/user/lists/OL1L", name: String = "My List") =
        ReadingList(key = key, name = name, description = "", seedCount = 0)

    private fun seed(workKey: String = "/works/OL1W") =
        ListSeed(workKey = workKey, title = "Book", authorName = null, coverUrl = null)

    private fun fakeRepo(
        lists: List<ReadingList> = emptyList(),
        seeds: List<ListSeed> = emptyList(),
    ) = object : ListsRepository {
        override suspend fun getLists() = Result.Success(lists)
        override suspend fun createList(name: String, desc: String) = Result.Success(list(name = name))
        override suspend fun deleteList(key: String) = Result.Success(Unit)
        override suspend fun getSeeds(key: String) = Result.Success(seeds)
        override suspend fun addSeed(key: String, work: String) = Result.Success(Unit)
        override suspend fun removeSeed(key: String, work: String) = Result.Success(Unit)
    }

    @Test
    fun init_loadsLists() = runTest {
        val vm = ListsViewModel(fakeRepo(lists = listOf(list())))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.lists.size)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun selectList_loadsSeeds() = runTest {
        val theList = list()
        val vm = ListsViewModel(fakeRepo(lists = listOf(theList), seeds = listOf(seed())))
        advanceUntilIdle()
        vm.onIntent(ListsIntent.SelectList(theList))
        advanceUntilIdle()
        assertEquals(theList, vm.uiState.value.selectedList)
        assertEquals(1, vm.uiState.value.seeds.size)
    }

    @Test
    fun createList_addsToList() = runTest {
        val vm = ListsViewModel(fakeRepo())
        advanceUntilIdle()
        vm.onIntent(ListsIntent.CreateList("New List", ""))
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.lists.size)
        assertEquals("New List", vm.uiState.value.lists[0].name)
    }

    @Test
    fun deleteList_removesFromList() = runTest {
        val theList = list()
        val vm = ListsViewModel(fakeRepo(lists = listOf(theList)))
        advanceUntilIdle()
        vm.onIntent(ListsIntent.DeleteList(theList))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.lists.isEmpty())
    }

    @Test
    fun error_setsLoading_false() = runTest {
        val failRepo = object : ListsRepository {
            override suspend fun getLists() = Result.Failure(ApiError.HttpError(500, "Server Error"))
            override suspend fun createList(n: String, d: String) = Result.Failure(ApiError.HttpError(500, ""))
            override suspend fun deleteList(k: String) = Result.Failure(ApiError.HttpError(500, ""))
            override suspend fun getSeeds(k: String) = Result.Failure(ApiError.HttpError(500, ""))
            override suspend fun addSeed(k: String, w: String) = Result.Failure(ApiError.HttpError(500, ""))
            override suspend fun removeSeed(k: String, w: String) = Result.Failure(ApiError.HttpError(500, ""))
        }
        val vm = ListsViewModel(failRepo)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.lists.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }
}
