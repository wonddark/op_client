// composeApp/src/commonTest/kotlin/com/opclient/author/AuthorDetailViewModelTest.kt
package com.opclient.author

import app.cash.turbine.test
import com.opclient.author.domain.AuthorDetail
import com.opclient.author.domain.AuthorRepository
import com.opclient.author.presentation.AuthorDetailEffect
import com.opclient.author.presentation.AuthorDetailIntent
import com.opclient.author.presentation.AuthorDetailViewModel
import com.opclient.book.presentation.DetailStatus
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

class AuthorDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun detail(key: String = "/authors/OL1A") = AuthorDetail(
        key = key, name = "Frank Herbert", bio = "Author",
        birthDate = "1920", deathDate = "1986", photoUrl = null, works = emptyList(),
    )

    private fun successRepo(d: AuthorDetail = detail()) = object : AuthorRepository {
        override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> = Result.Success(d)
    }

    private fun failingRepo(error: ApiError = ApiError.HttpError(404, "Not Found")) = object : AuthorRepository {
        override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> = Result.Failure(error)
    }

    @Test
    fun load_setsLoadingThenSuccess() = runTest {
        val vm = AuthorDetailViewModel(successRepo())
        vm.onIntent(AuthorDetailIntent.Load("/authors/OL1A"))
        advanceUntilIdle()
        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertEquals(detail(), vm.uiState.value.author)
    }

    @Test
    fun load_onFailure_setsErrorAndEmitsEffect() = runTest {
        val error = ApiError.HttpError(500, "Server Error")
        val vm = AuthorDetailViewModel(failingRepo(error))

        vm.effects.test {
            vm.onIntent(AuthorDetailIntent.Load("/authors/OL1A"))
            advanceUntilIdle()

            assertEquals(DetailStatus.Error, vm.uiState.value.status)
            val effect = awaitItem()
            assertIs<AuthorDetailEffect.LoadError>(effect)
            assertEquals(error, effect.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retry_reloadsWithLastKey() = runTest {
        var callCount = 0
        val repo = object : AuthorRepository {
            override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> {
                callCount++
                return if (callCount == 1) Result.Failure(ApiError.HttpError(503, "Unavailable"))
                else Result.Success(detail())
            }
        }
        val vm = AuthorDetailViewModel(repo)
        vm.onIntent(AuthorDetailIntent.Load("/authors/OL1A"))
        advanceUntilIdle()
        assertEquals(DetailStatus.Error, vm.uiState.value.status)

        vm.onIntent(AuthorDetailIntent.Retry)
        advanceUntilIdle()

        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertEquals(2, callCount)
    }
}
