package com.opclient.subject

import app.cash.turbine.test
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.presentation.DetailStatus
import com.opclient.subject.domain.SubjectPage
import com.opclient.subject.domain.SubjectRepository
import com.opclient.subject.domain.SubjectWork
import com.opclient.subject.presentation.SubjectDetailEffect
import com.opclient.subject.presentation.SubjectDetailIntent
import com.opclient.subject.presentation.SubjectDetailViewModel
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

class SubjectDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun work(key: String = "/works/OL1W") =
        SubjectWork(key, "Dune", "Frank Herbert", null)

    private fun page(works: List<SubjectWork> = listOf(work()), workCount: Int = 20) =
        SubjectPage("Science Fiction", workCount, works)

    private fun successRepo(p: SubjectPage = page()) = object : SubjectRepository {
        override suspend fun getSubjectPage(subjectName: String, limit: Int, offset: Int) =
            Result.Success(p)
    }

    private fun failingRepo(error: ApiError = ApiError.HttpError(500, "Error")) = object : SubjectRepository {
        override suspend fun getSubjectPage(subjectName: String, limit: Int, offset: Int) =
            Result.Failure(error)
    }

    @Test
    fun load_setsSuccessWithWorksAndWorkCount() = runTest {
        val vm = SubjectDetailViewModel(successRepo())
        vm.onIntent(SubjectDetailIntent.Load("Science Fiction"))
        advanceUntilIdle()

        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertEquals("Science Fiction", vm.uiState.value.subjectName)
        assertEquals(1, vm.uiState.value.works.size)
        assertEquals(20, vm.uiState.value.workCount)
    }

    @Test
    fun load_canLoadMore_trueWhenWorksBelowCount() = runTest {
        val vm = SubjectDetailViewModel(successRepo(page(works = listOf(work()), workCount = 20)))
        vm.onIntent(SubjectDetailIntent.Load("Science Fiction"))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.canLoadMore)
    }

    @Test
    fun load_canLoadMore_falseWhenAllLoaded() = runTest {
        val vm = SubjectDetailViewModel(successRepo(page(works = listOf(work()), workCount = 1)))
        vm.onIntent(SubjectDetailIntent.Load("Science Fiction"))
        advanceUntilIdle()

        assertFalse(vm.uiState.value.canLoadMore)
    }

    @Test
    fun load_onFailure_setsErrorAndEmitsEffect() = runTest {
        val error = ApiError.HttpError(500, "Server Error")
        val vm = SubjectDetailViewModel(failingRepo(error))

        vm.effects.test {
            vm.onIntent(SubjectDetailIntent.Load("Science Fiction"))
            advanceUntilIdle()

            assertEquals(DetailStatus.Error, vm.uiState.value.status)
            val effect = awaitItem()
            assertIs<SubjectDetailEffect.LoadError>(effect)
            assertEquals(error, effect.error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun loadMore_appendsWorksAndUpdatesCanLoadMore() = runTest {
        val page1 = page(works = List(12) { work("/works/OL${it}W") }, workCount = 25)
        val page2 = page(works = List(12) { work("/works/OL${it + 100}W") }, workCount = 25)
        var callOffset = -1
        val repo = object : SubjectRepository {
            override suspend fun getSubjectPage(subjectName: String, limit: Int, offset: Int): Result<SubjectPage, ApiError> {
                callOffset = offset
                return if (offset == 0) Result.Success(page1) else Result.Success(page2)
            }
        }
        val vm = SubjectDetailViewModel(repo)
        vm.onIntent(SubjectDetailIntent.Load("Science Fiction"))
        advanceUntilIdle()
        assertEquals(12, vm.uiState.value.works.size)
        assertTrue(vm.uiState.value.canLoadMore)

        vm.onIntent(SubjectDetailIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(24, vm.uiState.value.works.size)
        assertEquals(12, callOffset)
        assertTrue(vm.uiState.value.canLoadMore)
    }

    @Test
    fun loadMore_noopWhenCanLoadMoreIsFalse() = runTest {
        var callCount = 0
        val repo = object : SubjectRepository {
            override suspend fun getSubjectPage(subjectName: String, limit: Int, offset: Int): Result<SubjectPage, ApiError> {
                callCount++
                return Result.Success(page(works = listOf(work()), workCount = 1))
            }
        }
        val vm = SubjectDetailViewModel(repo)
        vm.onIntent(SubjectDetailIntent.Load("Science Fiction"))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canLoadMore)

        vm.onIntent(SubjectDetailIntent.LoadMore)
        advanceUntilIdle()

        assertEquals(1, callCount)
    }

    @Test
    fun loadMore_onFailure_emitsEffectAndCanLoadMoreFalse() = runTest {
        val page1 = page(works = List(12) { work("/works/OL${it}W") }, workCount = 25)
        var callCount = 0
        val repo = object : SubjectRepository {
            override suspend fun getSubjectPage(subjectName: String, limit: Int, offset: Int): Result<SubjectPage, ApiError> {
                callCount++
                return if (offset == 0) Result.Success(page1)
                else Result.Failure(ApiError.HttpError(500, "Error"))
            }
        }
        val vm = SubjectDetailViewModel(repo)
        vm.onIntent(SubjectDetailIntent.Load("Science Fiction"))
        advanceUntilIdle()

        vm.effects.test {
            vm.onIntent(SubjectDetailIntent.LoadMore)
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isLoadingMore)
            assertFalse(vm.uiState.value.canLoadMore)
            assertIs<SubjectDetailEffect.LoadError>(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun retry_reloadsWithLastSubjectName() = runTest {
        var callCount = 0
        val repo = object : SubjectRepository {
            override suspend fun getSubjectPage(subjectName: String, limit: Int, offset: Int): Result<SubjectPage, ApiError> {
                callCount++
                return if (callCount == 1) Result.Failure(ApiError.HttpError(503, "Unavailable"))
                else Result.Success(page())
            }
        }
        val vm = SubjectDetailViewModel(repo)
        vm.onIntent(SubjectDetailIntent.Load("Science Fiction"))
        advanceUntilIdle()
        assertEquals(DetailStatus.Error, vm.uiState.value.status)

        vm.onIntent(SubjectDetailIntent.Retry)
        advanceUntilIdle()

        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertEquals(2, callCount)
    }
}
