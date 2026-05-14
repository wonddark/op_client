// composeApp/src/commonTest/kotlin/com/opclient/book/BookDetailViewModelTest.kt
package com.opclient.book

import app.cash.turbine.test
import com.opclient.book.domain.AuthorRef
import com.opclient.book.domain.BookDetail
import com.opclient.book.domain.BookRepository
import com.opclient.book.presentation.BookDetailEffect
import com.opclient.book.presentation.BookDetailIntent
import com.opclient.book.presentation.BookDetailViewModel
import com.opclient.presentation.DetailStatus
import com.opclient.core.ApiError
import com.opclient.core.Result
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
import kotlin.test.assertIs

class BookDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun detail(key: String = "/works/OL1W") = BookDetail(
        key = key, title = "Dune", description = "desc",
        authors = listOf(AuthorRef("/authors/OL1A", "Frank Herbert")),
        subjects = listOf("SF"), firstPublishDate = "1965",
        coverUrl = "https://example.com/cover.jpg",
    )

    private fun successRepo(d: BookDetail = detail()) = object : BookRepository {
        override suspend fun getBook(workKey: String): Result<BookDetail, ApiError> = Result.Success(d)
    }

    private fun failingRepo(error: ApiError = ApiError.HttpError(404, "Not Found")) = object : BookRepository {
        override suspend fun getBook(workKey: String): Result<BookDetail, ApiError> = Result.Failure(error)
    }

    @Test
    fun load_setsLoadingThenSuccess() = runTest {
        val vm = BookDetailViewModel(successRepo())
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()
        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertEquals(detail(), vm.uiState.value.book)
    }

    @Test
    fun load_onFailure_setsErrorAndEmitsEffect() = runTest {
        val error = ApiError.HttpError(500, "Server Error")
        val vm = BookDetailViewModel(failingRepo(error))

        vm.effects.test {
            vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
            advanceUntilIdle()

            assertEquals(DetailStatus.Error, vm.uiState.value.status)
            val effect = awaitItem()
            assertIs<BookDetailEffect.LoadError>(effect)
            assertEquals(error, effect.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retry_reloadsWithLastKey() = runTest {
        var callCount = 0
        val repo = object : BookRepository {
            override suspend fun getBook(workKey: String): Result<BookDetail, ApiError> {
                callCount++
                return if (callCount == 1) Result.Failure(ApiError.HttpError(503, "Unavailable"))
                else Result.Success(detail())
            }
        }
        val vm = BookDetailViewModel(repo)
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()
        assertEquals(DetailStatus.Error, vm.uiState.value.status)

        vm.onIntent(BookDetailIntent.Retry)
        advanceUntilIdle()

        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertEquals(2, callCount)
    }

    @Test
    fun retry_beforeLoad_doesNothing() = runTest {
        var callCount = 0
        val repo = object : BookRepository {
            override suspend fun getBook(workKey: String): Result<BookDetail, ApiError> {
                callCount++
                return Result.Success(detail())
            }
        }
        val vm = BookDetailViewModel(repo)
        vm.onIntent(BookDetailIntent.Retry)
        advanceUntilIdle()
        assertEquals(0, callCount)
    }
}
