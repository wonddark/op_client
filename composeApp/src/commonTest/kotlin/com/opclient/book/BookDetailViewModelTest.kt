package com.opclient.book

import app.cash.turbine.test
import com.opclient.book.domain.AuthorRef
import com.opclient.book.domain.BookDetail
import com.opclient.book.domain.BookRepository
import com.opclient.book.presentation.BookDetailEffect
import com.opclient.book.presentation.BookDetailIntent
import com.opclient.book.presentation.BookDetailViewModel
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.library.domain.LibraryEntry
import com.opclient.library.domain.LibraryRepository
import com.opclient.library.domain.Shelf
import com.opclient.presentation.DetailStatus
import com.opclient.subject.domain.SubjectPage
import com.opclient.subject.domain.SubjectRepository
import com.opclient.subject.domain.SubjectWork
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
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BookDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private fun detail(key: String = "/works/OL1W", subjects: List<String> = listOf("Science Fiction")) =
        BookDetail(
            key = key, title = "Dune", description = "desc",
            authors = listOf(AuthorRef("/authors/OL1A", "Frank Herbert")),
            subjects = subjects, firstPublishDate = "1965",
            coverUrl = "https://example.com/cover.jpg",
        )

    private fun successRepo(d: BookDetail = detail()) = object : BookRepository {
        override suspend fun getBook(workKey: String): Result<BookDetail, ApiError> = Result.Success(d)
    }

    private fun failingRepo(error: ApiError = ApiError.HttpError(404, "Not Found")) = object : BookRepository {
        override suspend fun getBook(workKey: String): Result<BookDetail, ApiError> = Result.Failure(error)
    }

    private fun subjectRepo(
        result: Result<SubjectPage, ApiError> = Result.Success(
            SubjectPage(
                "Science Fiction", 50,
                listOf(
                    SubjectWork("/works/OL2W", "Foundation", "Asimov", null),
                    SubjectWork("/works/OL1W", "Dune", "Herbert", null),
                ),
            ),
        ),
    ) = object : SubjectRepository {
        override suspend fun getSubjectPage(subjectName: String, limit: Int, offset: Int) = result
    }

    private fun emptySubjectRepo() = subjectRepo(
        Result.Success(SubjectPage("Science Fiction", 0, emptyList())),
    )

    private fun fakeLibraryRepo() = object : LibraryRepository {
        val shelves = MutableStateFlow<Map<String, Shelf>>(emptyMap())

        override fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>> =
            shelves.map { map ->
                map.entries.filter { it.value == shelf }.map { (key, s) ->
                    LibraryEntry(workKey = key, title = "Book", authorName = null, coverUrl = null, shelf = s, addedAt = 0L)
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

    // --- Existing tests (updated constructor) ---

    @Test
    fun load_setsLoadingThenSuccess() = runTest {
        val vm = BookDetailViewModel(successRepo(), emptySubjectRepo(), fakeLibraryRepo())
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()
        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertEquals(detail(), vm.uiState.value.book)
    }

    @Test
    fun load_onFailure_setsErrorAndEmitsEffect() = runTest {
        val error = ApiError.HttpError(500, "Server Error")
        val vm = BookDetailViewModel(failingRepo(error), emptySubjectRepo(), fakeLibraryRepo())

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
        val vm = BookDetailViewModel(repo, emptySubjectRepo(), fakeLibraryRepo())
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
        val vm = BookDetailViewModel(repo, emptySubjectRepo(), fakeLibraryRepo())
        vm.onIntent(BookDetailIntent.Retry)
        advanceUntilIdle()
        assertEquals(0, callCount)
    }

    @Test
    fun load_populatesRelatedWorksAfterSuccess() = runTest {
        val vm = BookDetailViewModel(successRepo(), subjectRepo(), fakeLibraryRepo())
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()

        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertEquals("Science Fiction", vm.uiState.value.relatedSubjectName)
        assertTrue(vm.uiState.value.relatedWorks.isNotEmpty())
    }

    @Test
    fun load_relatedWorks_excludesCurrentBook() = runTest {
        val vm = BookDetailViewModel(successRepo(), subjectRepo(), fakeLibraryRepo())
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()

        val relatedKeys = vm.uiState.value.relatedWorks.map { it.key }
        assertTrue(!relatedKeys.contains("/works/OL1W"), "Related works should not include current book")
    }

    @Test
    fun load_subjectApiFailure_statusRemainsSuccess() = runTest {
        val vm = BookDetailViewModel(
            successRepo(),
            subjectRepo(Result.Failure(ApiError.HttpError(500, "Subject Error"))),
            fakeLibraryRepo(),
        )
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()

        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertTrue(vm.uiState.value.relatedWorks.isEmpty())
    }

    @Test
    fun load_emptySubjects_skipsSubjectFetch() = runTest {
        var subjectCallCount = 0
        val repo = object : SubjectRepository {
            override suspend fun getSubjectPage(subjectName: String, limit: Int, offset: Int): Result<SubjectPage, ApiError> {
                subjectCallCount++
                return Result.Success(SubjectPage("", 0, emptyList()))
            }
        }
        val vm = BookDetailViewModel(successRepo(detail(subjects = emptyList())), repo, fakeLibraryRepo())
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()

        assertEquals(0, subjectCallCount)
    }

    // --- New library tests ---

    @Test
    fun currentShelf_nullInitially() = runTest {
        val vm = BookDetailViewModel(successRepo(), emptySubjectRepo(), fakeLibraryRepo())
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()
        assertNull(vm.uiState.value.currentShelf)
    }

    @Test
    fun currentShelf_updatesFromLibraryFlow() = runTest {
        val libraryRepo = fakeLibraryRepo()
        val vm = BookDetailViewModel(successRepo(), emptySubjectRepo(), libraryRepo)
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()

        libraryRepo.shelves.value = mapOf("/works/OL1W" to Shelf.READING)
        advanceUntilIdle()

        assertEquals(Shelf.READING, vm.uiState.value.currentShelf)
    }

    @Test
    fun setShelf_callsAddToShelf_whenCurrentShelfNull() = runTest {
        val libraryRepo = fakeLibraryRepo()
        val vm = BookDetailViewModel(successRepo(), emptySubjectRepo(), libraryRepo)
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()

        vm.onIntent(BookDetailIntent.SetShelf(Shelf.WANT_TO_READ))
        advanceUntilIdle()

        assertEquals(Shelf.WANT_TO_READ, libraryRepo.shelves.value["/works/OL1W"])
    }

    @Test
    fun setShelf_callsMoveToShelf_whenAlreadyOnShelf() = runTest {
        val libraryRepo = fakeLibraryRepo()
        libraryRepo.shelves.value = mapOf("/works/OL1W" to Shelf.WANT_TO_READ)
        val vm = BookDetailViewModel(successRepo(), emptySubjectRepo(), libraryRepo)
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()

        vm.onIntent(BookDetailIntent.SetShelf(Shelf.READ))
        advanceUntilIdle()

        assertEquals(Shelf.READ, libraryRepo.shelves.value["/works/OL1W"])
    }

    @Test
    fun removeFromLibrary_callsRemoveFromShelf() = runTest {
        val libraryRepo = fakeLibraryRepo()
        libraryRepo.shelves.value = mapOf("/works/OL1W" to Shelf.READING)
        val vm = BookDetailViewModel(successRepo(), emptySubjectRepo(), libraryRepo)
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()

        vm.onIntent(BookDetailIntent.RemoveFromLibrary)
        advanceUntilIdle()

        assertTrue(libraryRepo.shelves.value.isEmpty())
    }

    @Test
    fun setShelf_noOp_whenBookNull() = runTest {
        val libraryRepo = fakeLibraryRepo()
        val vm = BookDetailViewModel(successRepo(), emptySubjectRepo(), libraryRepo)
        // No Load intent — book is null
        vm.onIntent(BookDetailIntent.SetShelf(Shelf.READ))
        advanceUntilIdle()
        assertTrue(libraryRepo.shelves.value.isEmpty())
    }

    @Test
    fun removeFromLibrary_noOp_whenBookNull() = runTest {
        val libraryRepo = fakeLibraryRepo()
        val vm = BookDetailViewModel(successRepo(), emptySubjectRepo(), libraryRepo)
        // No Load intent — book is null
        vm.onIntent(BookDetailIntent.RemoveFromLibrary)
        advanceUntilIdle()
        assertTrue(libraryRepo.shelves.value.isEmpty())
    }
}
