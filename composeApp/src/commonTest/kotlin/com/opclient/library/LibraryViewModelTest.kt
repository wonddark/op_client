package com.opclient.library

import app.cash.turbine.test
import com.opclient.library.domain.LibraryEntry
import com.opclient.library.domain.LibraryRepository
import com.opclient.library.domain.Shelf
import com.opclient.library.presentation.LibraryIntent
import com.opclient.library.presentation.LibraryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LibraryViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun fakeRepo() = object : LibraryRepository {
        val shelves = MutableStateFlow<Map<String, Shelf>>(emptyMap())

        override fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>> =
            shelves.map { map ->
                map.entries
                    .filter { it.value == shelf }
                    .map { (key, shelfValue) ->
                        LibraryEntry(
                            workKey = key, title = "Book $key",
                            authorName = null, coverUrl = null,
                            shelf = shelfValue, addedAt = 0L,
                        )
                    }
            }

        override fun getCurrentShelf(workKey: String): Flow<Shelf?> =
            shelves.map { it[workKey] }

        override suspend fun addToShelf(entry: LibraryEntry) {
            shelves.update { it + (entry.workKey to entry.shelf) }
        }

        override suspend fun removeFromShelf(workKey: String) {
            shelves.update { it - workKey }
        }

        override suspend fun moveToShelf(workKey: String, shelf: Shelf) {
            shelves.update { it + (workKey to shelf) }
        }
    }

    @Test
    fun initialState_isWantToRead_emptyEntries() = runTest {
        val vm = LibraryViewModel(fakeRepo())
        assertEquals(Shelf.WANT_TO_READ, vm.uiState.value.selectedShelf)
        assertTrue(vm.uiState.value.entries.isEmpty())
    }

    @Test
    fun selectShelf_switchesSubscription() = runTest {
        val repo = fakeRepo()
        repo.shelves.value = mapOf("/works/OL1W" to Shelf.READING)
        val vm = LibraryViewModel(repo)

        vm.onIntent(LibraryIntent.SelectShelf(Shelf.READING))

        vm.uiState.test {
            val state = awaitItem()
            assertEquals(Shelf.READING, state.selectedShelf)
            assertEquals(1, state.entries.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun entries_reflectFakeRepoFlow() = runTest {
        val repo = fakeRepo()
        val vm = LibraryViewModel(repo)

        vm.uiState.test {
            awaitItem() // initial empty state

            repo.shelves.value = mapOf("/works/OL1W" to Shelf.WANT_TO_READ)
            val updated = awaitItem()
            assertEquals(1, updated.entries.size)
            assertEquals("/works/OL1W", updated.entries.first().workKey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun selectShelf_cancelsPreviousJob() = runTest {
        val repo = fakeRepo()
        repo.shelves.value = mapOf("/works/OL1W" to Shelf.WANT_TO_READ)
        val vm = LibraryViewModel(repo)

        vm.onIntent(LibraryIntent.SelectShelf(Shelf.READ))

        // Switched to READ shelf — previous WANT_TO_READ job cancelled
        assertEquals(Shelf.READ, vm.uiState.value.selectedShelf)
        assertTrue(vm.uiState.value.entries.isEmpty())
    }
}
