package com.opclient.search

import app.cash.turbine.test
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.search.domain.Book
import com.opclient.search.domain.SearchRepository
import com.opclient.search.domain.SearchResults
import com.opclient.search.presentation.SearchEffect
import com.opclient.search.presentation.SearchIntent
import com.opclient.search.presentation.SearchStatus
import com.opclient.search.presentation.SearchViewModel
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
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SearchViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun book(n: Int) = Book(
        key = "/works/OL${n}W",
        title = "Book $n",
        author = "Author",
        firstPublishYear = null,
        coverUrl = null,
        primarySubject = null,
    )

    @Test
    fun queryChanged_updatesQueryWithoutTriggering() = runTest {
        val vm = SearchViewModel(FakeRepo())
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        assertEquals("dune", vm.uiState.value.query)
        assertEquals(SearchStatus.Idle, vm.uiState.value.status)
    }

    @Test
    fun search_blankQuery_doesNothing() = runTest {
        val repo = FakeRepo()
        val vm = SearchViewModel(repo)
        vm.onIntent(SearchIntent.Search)
        advanceUntilIdle()
        assertEquals(SearchStatus.Idle, vm.uiState.value.status)
        assertEquals(0, repo.callCount)
    }

    @Test
    fun search_success_setsSuccessStatusAndBooks() = runTest {
        val books = (1..5).map { book(it) }
        val repo = FakeRepo(Result.Success(SearchResults("dune", 100, books, 0)))
        val vm = SearchViewModel(repo)
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        vm.onIntent(SearchIntent.Search)
        advanceUntilIdle()
        assertEquals(SearchStatus.Success, vm.uiState.value.status)
        assertEquals(books, vm.uiState.value.books)
        assertEquals(100, vm.uiState.value.totalFound)
    }

    @Test
    fun search_success_canLoadMoreWhenBooksLessThanTotal() = runTest {
        val repo = FakeRepo(Result.Success(SearchResults("dune", 100, (1..20).map { book(it) }, 0)))
        val vm = SearchViewModel(repo)
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        vm.onIntent(SearchIntent.Search)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.canLoadMore)
    }

    @Test
    fun search_success_cannotLoadMoreWhenBooksEqualTotal() = runTest {
        val books = (1..5).map { book(it) }
        val repo = FakeRepo(Result.Success(SearchResults("dune", 5, books, 0)))
        val vm = SearchViewModel(repo)
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        vm.onIntent(SearchIntent.Search)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canLoadMore)
    }

    @Test
    fun search_emptyResults_setsEmptyStatus() = runTest {
        val repo = FakeRepo(Result.Success(SearchResults("xyz", 0, emptyList(), 0)))
        val vm = SearchViewModel(repo)
        vm.onIntent(SearchIntent.QueryChanged("xyz"))
        vm.onIntent(SearchIntent.Search)
        advanceUntilIdle()
        assertEquals(SearchStatus.Empty, vm.uiState.value.status)
    }

    @Test
    fun search_failure_setsErrorStatusAndEmitsEffect() = runTest {
        val error = ApiError.HttpError(500, "Server Error")
        val repo = FakeRepo(Result.Failure(error))
        val vm = SearchViewModel(repo)

        vm.effects.test {
            vm.onIntent(SearchIntent.QueryChanged("dune"))
            vm.onIntent(SearchIntent.Search)
            advanceUntilIdle()

            assertEquals(SearchStatus.Error, vm.uiState.value.status)
            val effect = awaitItem()
            assertIs<SearchEffect.SearchError>(effect)
            assertEquals(error, effect.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadMore_appendsBooksAndAdvancesOffset() = runTest {
        val page1 = (1..20).map { book(it) }
        val page2 = (21..40).map { book(it) }
        val repo = object : SearchRepository {
            override suspend fun search(query: String, offset: Int, limit: Int): Result<SearchResults, ApiError> =
                if (offset == 0)
                    Result.Success(SearchResults(query, 100, page1, 0))
                else
                    Result.Success(SearchResults(query, 100, page2, 20))
        }
        val vm = SearchViewModel(repo)
        vm.onIntent(SearchIntent.QueryChanged("kotlin"))
        vm.onIntent(SearchIntent.Search)
        advanceUntilIdle()
        assertEquals(20, vm.uiState.value.books.size)

        vm.onIntent(SearchIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(40, vm.uiState.value.books.size)
        assertEquals(20, vm.uiState.value.offset)
        assertEquals(SearchStatus.Success, vm.uiState.value.status)
        assertTrue(vm.uiState.value.canLoadMore)  // 40 < 100
    }

    @Test
    fun loadMore_ignored_whenCannotLoadMore() = runTest {
        val books = (1..5).map { book(it) }
        val repo = FakeRepo(Result.Success(SearchResults("dune", 5, books, 0)))
        val vm = SearchViewModel(repo)
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        vm.onIntent(SearchIntent.Search)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canLoadMore)
        val callsBefore = repo.callCount

        vm.onIntent(SearchIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(callsBefore, repo.callCount)
    }

    @Test
    fun loadMore_failure_revertsToSuccessAndEmitsEffect() = runTest {
        val page1 = (1..20).map { book(it) }
        val error = ApiError.HttpError(503, "Service Unavailable")
        val repo = object : SearchRepository {
            override suspend fun search(query: String, offset: Int, limit: Int): Result<SearchResults, ApiError> =
                if (offset == 0)
                    Result.Success(SearchResults(query, 100, page1, 0))
                else
                    Result.Failure(error)
        }
        val vm = SearchViewModel(repo)
        vm.onIntent(SearchIntent.QueryChanged("kotlin"))
        vm.onIntent(SearchIntent.Search)
        advanceUntilIdle()
        assertEquals(20, vm.uiState.value.books.size)

        vm.effects.test {
            vm.onIntent(SearchIntent.LoadMore)
            advanceUntilIdle()

            assertEquals(SearchStatus.Success, vm.uiState.value.status)
            assertFalse(vm.uiState.value.canLoadMore)
            val effect = awaitItem()
            assertIs<SearchEffect.SearchError>(effect)
            assertEquals(error, effect.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearSearch_resetsState() = runTest {
        val books = (1..5).map { book(it) }
        val repo = FakeRepo(Result.Success(SearchResults("dune", 100, books, 0)))
        val vm = SearchViewModel(repo)
        vm.onIntent(SearchIntent.QueryChanged("dune"))
        vm.onIntent(SearchIntent.Search)
        advanceUntilIdle()
        assertEquals(SearchStatus.Success, vm.uiState.value.status)

        vm.onIntent(SearchIntent.ClearSearch)

        assertEquals("", vm.uiState.value.query)
        assertEquals(emptyList(), vm.uiState.value.books)
        assertEquals(SearchStatus.Idle, vm.uiState.value.status)
        assertFalse(vm.uiState.value.canLoadMore)
    }
}

private class FakeRepo(
    private val result: Result<SearchResults, ApiError> = Result.Success(
        SearchResults("test", 0, emptyList(), 0),
    ),
) : SearchRepository {
    var callCount = 0

    override suspend fun search(query: String, offset: Int, limit: Int): Result<SearchResults, ApiError> {
        callCount++
        return result
    }
}
