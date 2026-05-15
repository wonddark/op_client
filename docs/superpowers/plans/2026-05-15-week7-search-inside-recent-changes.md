# Week 7 — Search Inside & Recent Changes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add full-text passage search (toggle mode on SearchScreen) and a community activity feed (new top-level tab) using OpenLibrary's Search Inside and Recent Changes APIs.

**Architecture:** Two independent packages (`searchinside/`, `recentchanges/`) each following the DTO → mapper → domain → repository → ViewModel → Screen layering used throughout the project. `SearchInsideViewModel` is a second ViewModel hosted by the existing `SearchScreen` alongside the existing `SearchViewModel`. `RecentChangesScreen` is a new top-level screen added as a fourth nav tab (`Destination.CHANGES`). Both features use the existing `ApiClient` base class, Koin for DI, and `UnconfinedTestDispatcher` + MockEngine for tests.

**Tech Stack:** Ktor 3.x with MockEngine, kotlinx.serialization, kotlinx.coroutines, kotlinx.datetime, Koin 4.0.0 `viewModel { }` DSL, Compose Multiplatform, existing UI components (`FilterChip`, `SectionLabel`, `LoadingState`, `EmptyState`, `ErrorState`, `SecondaryButton`).

---

### Task 1: Search Inside DTOs, Mapper, and API Client

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/searchinside/domain/SearchInsideModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/searchinside/domain/SearchInsideRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/searchinside/data/SearchInsideDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/searchinside/data/SearchInsideMapper.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/searchinside/data/SearchInsideApiClient.kt`

- [ ] **Step 1: Create domain models**

  Create `composeApp/src/commonMain/kotlin/com/opclient/searchinside/domain/SearchInsideModels.kt`:
  ```kotlin
  package com.opclient.searchinside.domain

  data class SearchInsideResult(
      val workKey: String,
      val title: String,
      val authorName: String?,
      val coverUrl: String?,
      val passage: String,
  )
  ```

- [ ] **Step 2: Create repository interface**

  Create `composeApp/src/commonMain/kotlin/com/opclient/searchinside/domain/SearchInsideRepository.kt`:
  ```kotlin
  package com.opclient.searchinside.domain

  import com.opclient.core.ApiError
  import com.opclient.core.Result

  interface SearchInsideRepository {
      suspend fun search(query: String): Result<List<SearchInsideResult>, ApiError>
  }
  ```

- [ ] **Step 3: Create DTOs**

  Create `composeApp/src/commonMain/kotlin/com/opclient/searchinside/data/SearchInsideDto.kt`:
  ```kotlin
  package com.opclient.searchinside.data

  import kotlinx.serialization.SerialName
  import kotlinx.serialization.Serializable

  @Serializable
  data class SearchInsideResponseDto(
      @SerialName("numFound") val numFound: Int = 0,
      @SerialName("docs") val docs: List<SearchInsideDocDto> = emptyList(),
  )

  @Serializable
  data class SearchInsideDocDto(
      val key: String? = null,
      val title: String? = null,
      @SerialName("author_name") val authorName: List<String>? = null,
      @SerialName("cover_i") val coverId: Int? = null,
      val text: List<String>? = null,
  )
  ```

- [ ] **Step 4: Create mapper**

  Create `composeApp/src/commonMain/kotlin/com/opclient/searchinside/data/SearchInsideMapper.kt`:
  ```kotlin
  package com.opclient.searchinside.data

  import com.opclient.searchinside.domain.SearchInsideResult

  fun SearchInsideDocDto.toDomain(): SearchInsideResult =
      SearchInsideResult(
          workKey = key!!,
          title = title ?: "Unknown",
          authorName = authorName?.firstOrNull(),
          coverUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
          passage = text?.firstOrNull() ?: "",
      )
  ```

- [ ] **Step 5: Create API client**

  Create `composeApp/src/commonMain/kotlin/com/opclient/searchinside/data/SearchInsideApiClient.kt`:
  ```kotlin
  package com.opclient.searchinside.data

  import com.opclient.core.ApiClient
  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import io.ktor.client.HttpClient

  class SearchInsideApiClient(httpClient: HttpClient) :
      ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

      suspend fun search(query: String): Result<SearchInsideResponseDto, ApiError> =
          get(path = "/search/inside.json", queryParams = mapOf("q" to query))
  }
  ```

- [ ] **Step 6: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/searchinside/
  git commit -m "feat(searchinside): add domain models, DTOs, mapper, and API client"
  ```

---

### Task 2: Search Inside Repository (TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/searchinside/data/SearchInsideRepositoryImpl.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/searchinside/SearchInsideRepositoryTest.kt`

- [ ] **Step 1: Write failing tests**

  Create `composeApp/src/commonTest/kotlin/com/opclient/searchinside/SearchInsideRepositoryTest.kt`:
  ```kotlin
  package com.opclient.searchinside

  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.core.buildHttpClient
  import com.opclient.searchinside.data.SearchInsideApiClient
  import com.opclient.searchinside.data.SearchInsideRepositoryImpl
  import com.opclient.searchinside.domain.SearchInsideRepository
  import io.ktor.client.engine.mock.MockEngine
  import io.ktor.client.engine.mock.respond
  import io.ktor.http.HttpHeaders
  import io.ktor.http.HttpStatusCode
  import io.ktor.http.headersOf
  import kotlinx.coroutines.test.runTest
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertIs

  class SearchInsideRepositoryTest {

      private fun makeRepo(engine: MockEngine): SearchInsideRepository =
          SearchInsideRepositoryImpl(SearchInsideApiClient(buildHttpClient(engine)))

      @Test
      fun search_parsesResults() = runTest {
          val json = """
              {
                "numFound": 1,
                "docs": [
                  {
                    "key": "/works/OL82563W",
                    "title": "Dune",
                    "author_name": ["Frank Herbert"],
                    "cover_i": 8368541,
                    "text": ["The spice must flow."]
                  }
                ]
              }
          """.trimIndent()
          val engine = MockEngine {
              respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeRepo(engine).search("spice")
          assertIs<Result.Success<List<*>>>(result)
          assertEquals(1, result.value.size)
          val item = result.value[0]
          assertEquals("/works/OL82563W", item.workKey)
          assertEquals("Dune", item.title)
          assertEquals("Frank Herbert", item.authorName)
          assertEquals("The spice must flow.", item.passage)
          assertEquals("https://covers.openlibrary.org/b/id/8368541-M.jpg", item.coverUrl)
      }

      @Test
      fun search_emptyResponse_returnsEmptyList() = runTest {
          val json = """{"numFound": 0, "docs": []}"""
          val engine = MockEngine {
              respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeRepo(engine).search("xyz")
          assertIs<Result.Success<List<*>>>(result)
          assertEquals(0, result.value.size)
      }

      @Test
      fun search_404_returnsFailure() = runTest {
          val engine = MockEngine {
              respond("Not Found", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "text/plain"))
          }
          val result = makeRepo(engine).search("anything")
          assertIs<Result.Failure<ApiError>>(result)
      }

      @Test
      fun search_filtersDocsWithNullKey() = runTest {
          val json = """
              {
                "numFound": 2,
                "docs": [
                  {"key": "/works/OL1W", "title": "Book A", "text": ["passage A"]},
                  {"title": "No Key Book", "text": ["passage B"]}
                ]
              }
          """.trimIndent()
          val engine = MockEngine {
              respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeRepo(engine).search("query")
          assertIs<Result.Success<List<*>>>(result)
          assertEquals(1, result.value.size)
          assertEquals("/works/OL1W", result.value[0].workKey)
      }
  }
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.searchinside.SearchInsideRepositoryTest"`
  Expected: compilation error — `SearchInsideRepositoryImpl` not yet created

- [ ] **Step 3: Implement SearchInsideRepositoryImpl**

  Create `composeApp/src/commonMain/kotlin/com/opclient/searchinside/data/SearchInsideRepositoryImpl.kt`:
  ```kotlin
  package com.opclient.searchinside.data

  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.searchinside.domain.SearchInsideRepository
  import com.opclient.searchinside.domain.SearchInsideResult

  class SearchInsideRepositoryImpl(
      private val apiClient: SearchInsideApiClient,
  ) : SearchInsideRepository {

      override suspend fun search(query: String): Result<List<SearchInsideResult>, ApiError> =
          when (val result = apiClient.search(query)) {
              is Result.Success -> Result.Success(
                  result.value.docs
                      .filter { it.key != null }
                      .map { it.toDomain() },
              )
              is Result.Failure -> result
          }
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.searchinside.SearchInsideRepositoryTest"`
  Expected: 4 tests PASSED

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/searchinside/data/SearchInsideRepositoryImpl.kt \
          composeApp/src/commonTest/kotlin/com/opclient/searchinside/SearchInsideRepositoryTest.kt
  git commit -m "feat(searchinside): add SearchInsideRepositoryImpl with TDD"
  ```

---

### Task 3: Search Inside ViewModel (TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/searchinside/presentation/SearchInsideViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/searchinside/SearchInsideViewModelTest.kt`

- [ ] **Step 1: Write failing tests**

  Create `composeApp/src/commonTest/kotlin/com/opclient/searchinside/SearchInsideViewModelTest.kt`:
  ```kotlin
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
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.searchinside.SearchInsideViewModelTest"`
  Expected: compilation error — `SearchInsideViewModel` not yet created

- [ ] **Step 3: Implement SearchInsideViewModel**

  Create `composeApp/src/commonMain/kotlin/com/opclient/searchinside/presentation/SearchInsideViewModel.kt`:
  ```kotlin
  package com.opclient.searchinside.presentation

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.opclient.core.Result
  import com.opclient.searchinside.domain.SearchInsideRepository
  import com.opclient.searchinside.domain.SearchInsideResult
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch

  data class SearchInsideUiState(
      val results: List<SearchInsideResult> = emptyList(),
      val isLoading: Boolean = false,
      val error: String? = null,
      val hasSearched: Boolean = false,
  )

  sealed class SearchInsideIntent {
      data class Search(val query: String) : SearchInsideIntent()
      data object Clear : SearchInsideIntent()
  }

  class SearchInsideViewModel(
      private val repository: SearchInsideRepository,
  ) : ViewModel() {

      private val _uiState = MutableStateFlow(SearchInsideUiState())
      val uiState: StateFlow<SearchInsideUiState> = _uiState.asStateFlow()

      fun onIntent(intent: SearchInsideIntent) {
          when (intent) {
              is SearchInsideIntent.Search -> search(intent.query)
              SearchInsideIntent.Clear -> _uiState.value = SearchInsideUiState()
          }
      }

      private fun search(query: String) {
          if (query.isBlank()) return
          viewModelScope.launch {
              _uiState.update { it.copy(isLoading = true, error = null) }
              when (val result = repository.search(query)) {
                  is Result.Success -> _uiState.update {
                      it.copy(isLoading = false, results = result.value, hasSearched = true)
                  }
                  is Result.Failure -> _uiState.update {
                      it.copy(isLoading = false, error = "Search failed", hasSearched = true)
                  }
              }
          }
      }
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.searchinside.SearchInsideViewModelTest"`
  Expected: 4 tests PASSED

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/searchinside/presentation/SearchInsideViewModel.kt \
          composeApp/src/commonTest/kotlin/com/opclient/searchinside/SearchInsideViewModelTest.kt
  git commit -m "feat(searchinside): add SearchInsideViewModel with TDD"
  ```

---

### Task 4: Recent Changes DTOs, Mapper, and API Client

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/domain/RecentChangesModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/domain/RecentChangesRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/data/RecentChangesDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/data/RecentChangesMapper.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/data/RecentChangesApiClient.kt`

- [ ] **Step 1: Create domain models**

  Create `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/domain/RecentChangesModels.kt`:
  ```kotlin
  package com.opclient.recentchanges.domain

  data class RecentChange(
      val id: String,
      val label: String,
      val targetKey: String?,
      val addedAt: Long,
  )
  ```

- [ ] **Step 2: Create repository interface**

  Create `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/domain/RecentChangesRepository.kt`:
  ```kotlin
  package com.opclient.recentchanges.domain

  import com.opclient.core.ApiError
  import com.opclient.core.Result

  interface RecentChangesRepository {
      suspend fun getRecentChanges(): Result<List<RecentChange>, ApiError>
  }
  ```

- [ ] **Step 3: Create DTOs**

  Create `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/data/RecentChangesDto.kt`:
  ```kotlin
  package com.opclient.recentchanges.data

  import kotlinx.serialization.Serializable

  @Serializable
  data class RecentChangeDto(
      val id: String? = null,
      val kind: String? = null,
      val timestamp: String? = null,
      val author: RecentChangeAuthorDto? = null,
      val changes: List<RecentChangeRefDto> = emptyList(),
  )

  @Serializable
  data class RecentChangeAuthorDto(val key: String? = null)

  @Serializable
  data class RecentChangeRefDto(val key: String? = null)
  ```

- [ ] **Step 4: Create mapper**

  Create `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/data/RecentChangesMapper.kt`:
  ```kotlin
  package com.opclient.recentchanges.data

  import com.opclient.recentchanges.domain.RecentChange
  import kotlinx.datetime.Clock
  import kotlinx.datetime.Instant

  fun RecentChangeDto.toDomain(): RecentChange =
      RecentChange(
          id = id!!,
          label = when (kind) {
              "add-book" -> "Book added"
              "update-work" -> "Work updated"
              "update-author" -> "Author updated"
              "edit-book" -> "Book edited"
              else -> "Community edit"
          },
          targetKey = changes.firstOrNull()?.key,
          addedAt = timestamp?.toEpochMillis() ?: Clock.System.now().toEpochMilliseconds(),
      )

  private fun String.toEpochMillis(): Long =
      try {
          val normalized = if (endsWith("Z") || contains("+") || contains("-", startIndex = 10)) {
              this
          } else {
              "${this}Z"
          }
          Instant.parse(normalized).toEpochMilliseconds()
      } catch (_: Exception) {
          Clock.System.now().toEpochMilliseconds()
      }
  ```

- [ ] **Step 5: Create API client**

  Create `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/data/RecentChangesApiClient.kt`:
  ```kotlin
  package com.opclient.recentchanges.data

  import com.opclient.core.ApiClient
  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import io.ktor.client.HttpClient

  class RecentChangesApiClient(httpClient: HttpClient) :
      ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

      suspend fun getRecentChanges(): Result<List<RecentChangeDto>, ApiError> =
          get(path = "/recentchanges.json")
  }
  ```

- [ ] **Step 6: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/recentchanges/
  git commit -m "feat(recentchanges): add domain models, DTOs, mapper, and API client"
  ```

---

### Task 5: Recent Changes Repository (TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/data/RecentChangesRepositoryImpl.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/recentchanges/RecentChangesRepositoryTest.kt`

- [ ] **Step 1: Write failing tests**

  Create `composeApp/src/commonTest/kotlin/com/opclient/recentchanges/RecentChangesRepositoryTest.kt`:
  ```kotlin
  package com.opclient.recentchanges

  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.core.buildHttpClient
  import com.opclient.recentchanges.data.RecentChangesApiClient
  import com.opclient.recentchanges.data.RecentChangesRepositoryImpl
  import com.opclient.recentchanges.domain.RecentChangesRepository
  import io.ktor.client.engine.mock.MockEngine
  import io.ktor.client.engine.mock.respond
  import io.ktor.http.HttpHeaders
  import io.ktor.http.HttpStatusCode
  import io.ktor.http.headersOf
  import kotlinx.coroutines.test.runTest
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertIs

  class RecentChangesRepositoryTest {

      private fun makeRepo(engine: MockEngine): RecentChangesRepository =
          RecentChangesRepositoryImpl(RecentChangesApiClient(buildHttpClient(engine)))

      @Test
      fun getRecentChanges_parsesItems() = runTest {
          val json = """
              [
                {
                  "id": "abc123",
                  "kind": "add-book",
                  "timestamp": "2024-01-15T10:00:00Z",
                  "author": {"key": "/people/mark"},
                  "changes": [{"key": "/works/OL82563W"}]
                }
              ]
          """.trimIndent()
          val engine = MockEngine {
              respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeRepo(engine).getRecentChanges()
          assertIs<Result.Success<List<*>>>(result)
          assertEquals(1, result.value.size)
          val item = result.value[0]
          assertEquals("abc123", item.id)
          assertEquals("Book added", item.label)
          assertEquals("/works/OL82563W", item.targetKey)
      }

      @Test
      fun getRecentChanges_emptyList_returnsEmptyList() = runTest {
          val engine = MockEngine {
              respond("[]", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeRepo(engine).getRecentChanges()
          assertIs<Result.Success<List<*>>>(result)
          assertEquals(0, result.value.size)
      }

      @Test
      fun getRecentChanges_500_returnsFailure() = runTest {
          val engine = MockEngine {
              respond("Server Error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
          }
          val result = makeRepo(engine).getRecentChanges()
          assertIs<Result.Failure<ApiError>>(result)
      }
  }
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.recentchanges.RecentChangesRepositoryTest"`
  Expected: compilation error — `RecentChangesRepositoryImpl` not yet created

- [ ] **Step 3: Implement RecentChangesRepositoryImpl**

  Create `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/data/RecentChangesRepositoryImpl.kt`:
  ```kotlin
  package com.opclient.recentchanges.data

  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.recentchanges.domain.RecentChange
  import com.opclient.recentchanges.domain.RecentChangesRepository

  class RecentChangesRepositoryImpl(
      private val apiClient: RecentChangesApiClient,
  ) : RecentChangesRepository {

      override suspend fun getRecentChanges(): Result<List<RecentChange>, ApiError> =
          when (val result = apiClient.getRecentChanges()) {
              is Result.Success -> Result.Success(
                  result.value
                      .filter { it.id != null }
                      .map { it.toDomain() },
              )
              is Result.Failure -> result
          }
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.recentchanges.RecentChangesRepositoryTest"`
  Expected: 3 tests PASSED

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/recentchanges/data/RecentChangesRepositoryImpl.kt \
          composeApp/src/commonTest/kotlin/com/opclient/recentchanges/RecentChangesRepositoryTest.kt
  git commit -m "feat(recentchanges): add RecentChangesRepositoryImpl with TDD"
  ```

---

### Task 6: Recent Changes ViewModel (TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/presentation/RecentChangesViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/recentchanges/RecentChangesViewModelTest.kt`

- [ ] **Step 1: Write failing tests**

  Create `composeApp/src/commonTest/kotlin/com/opclient/recentchanges/RecentChangesViewModelTest.kt`:
  ```kotlin
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
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.recentchanges.RecentChangesViewModelTest"`
  Expected: compilation error — `RecentChangesViewModel` not yet created

- [ ] **Step 3: Implement RecentChangesViewModel**

  Create `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/presentation/RecentChangesViewModel.kt`:
  ```kotlin
  package com.opclient.recentchanges.presentation

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.opclient.core.Result
  import com.opclient.recentchanges.domain.RecentChange
  import com.opclient.recentchanges.domain.RecentChangesRepository
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch

  data class RecentChangesUiState(
      val changes: List<RecentChange> = emptyList(),
      val isLoading: Boolean = false,
      val error: String? = null,
  )

  sealed class RecentChangesIntent {
      data object Load : RecentChangesIntent()
      data object Refresh : RecentChangesIntent()
  }

  class RecentChangesViewModel(
      private val repository: RecentChangesRepository,
  ) : ViewModel() {

      private val _uiState = MutableStateFlow(RecentChangesUiState())
      val uiState: StateFlow<RecentChangesUiState> = _uiState.asStateFlow()

      init {
          onIntent(RecentChangesIntent.Load)
      }

      fun onIntent(intent: RecentChangesIntent) {
          when (intent) {
              RecentChangesIntent.Load -> load()
              RecentChangesIntent.Refresh -> refresh()
          }
      }

      private fun load() {
          if (_uiState.value.changes.isNotEmpty()) return
          fetch()
      }

      private fun refresh() {
          _uiState.update { it.copy(changes = emptyList()) }
          fetch()
      }

      private fun fetch() {
          viewModelScope.launch {
              _uiState.update { it.copy(isLoading = true, error = null) }
              when (val result = repository.getRecentChanges()) {
                  is Result.Success -> _uiState.update {
                      it.copy(isLoading = false, changes = result.value)
                  }
                  is Result.Failure -> _uiState.update {
                      it.copy(isLoading = false, error = "Failed to load changes")
                  }
              }
          }
      }
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.recentchanges.RecentChangesViewModelTest"`
  Expected: 3 tests PASSED

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/recentchanges/presentation/RecentChangesViewModel.kt \
          composeApp/src/commonTest/kotlin/com/opclient/recentchanges/RecentChangesViewModelTest.kt
  git commit -m "feat(recentchanges): add RecentChangesViewModel with TDD"
  ```

---

### Task 7: Navigation — Add CHANGES Tab

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/ui/navigation/Destination.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/ui/navigation/DestinationIcon.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt`

- [ ] **Step 1: Add Screen.RecentChanges**

  Replace `composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt` with:
  ```kotlin
  package com.opclient.navigation

  sealed class Screen {
      data object Search : Screen()
      data class BookDetail(val workKey: String) : Screen()
      data class AuthorDetail(val authorKey: String) : Screen()
      data object SubjectList : Screen()
      data class SubjectDetail(val subjectName: String) : Screen()
      data object Library : Screen()
      data object Profile : Screen()
      data object RecentChanges : Screen()
  }
  ```

- [ ] **Step 2: Add CHANGES destination**

  Replace `composeApp/src/commonMain/kotlin/com/opclient/ui/navigation/Destination.kt` with:
  ```kotlin
  package com.opclient.ui.navigation

  enum class Destination(val label: String) {
      SEARCH("Search"),
      BROWSE("Browse"),
      LIBRARY("Library"),
      CHANGES("Changes"),
  }
  ```

- [ ] **Step 3: Add CHANGES icon to DestinationIcon**

  In `composeApp/src/commonMain/kotlin/com/opclient/ui/navigation/DestinationIcon.kt`, add the `CHANGES` branch inside the `when (destination)` block. Add it after the `Destination.LIBRARY` branch, before the closing `}` of the `when`:

  ```kotlin
  Destination.CHANGES -> {
      drawCircle(
          color = tint,
          radius = s * 0.35f,
          center = Offset(s * 0.5f, s * 0.5f),
          style = Stroke(width = strokeWidth),
      )
      drawLine(
          color = tint,
          start = Offset(s * 0.5f, s * 0.5f),
          end = Offset(s * 0.5f, s * 0.26f),
          strokeWidth = strokeWidth,
          cap = StrokeCap.Round,
      )
      drawLine(
          color = tint,
          start = Offset(s * 0.5f, s * 0.5f),
          end = Offset(s * 0.68f, s * 0.5f),
          strokeWidth = strokeWidth,
          cap = StrokeCap.Round,
      )
  }
  ```

- [ ] **Step 4: Update NavigationViewModel.navigateToTab**

  In `composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt`, add the `CHANGES` branch to `navigateToTab`. Replace the `navigateToTab` function with:

  ```kotlin
  fun navigateToTab(destination: Destination) {
      _stack.update {
          when (destination) {
              Destination.SEARCH -> listOf(Screen.Search)
              Destination.BROWSE -> listOf(Screen.SubjectList)
              Destination.LIBRARY -> listOf(Screen.Library)
              Destination.CHANGES -> listOf(Screen.RecentChanges)
          }
      }
  }
  ```

- [ ] **Step 5: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt \
          composeApp/src/commonMain/kotlin/com/opclient/ui/navigation/Destination.kt \
          composeApp/src/commonMain/kotlin/com/opclient/ui/navigation/DestinationIcon.kt \
          composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt
  git commit -m "feat(recentchanges): add Screen.RecentChanges and CHANGES navigation tab"
  ```

---

### Task 8: Recent Changes Screen and App Wiring

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/presentation/RecentChangesScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/App.kt`

- [ ] **Step 1: Create RecentChangesScreen**

  Create `composeApp/src/commonMain/kotlin/com/opclient/recentchanges/presentation/RecentChangesScreen.kt`:
  ```kotlin
  package com.opclient.recentchanges.presentation

  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.Arrangement
  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.Row
  import androidx.compose.foundation.layout.fillMaxSize
  import androidx.compose.foundation.layout.fillMaxWidth
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.foundation.lazy.items
  import androidx.compose.foundation.text.BasicText
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.collectAsState
  import androidx.compose.runtime.getValue
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.unit.dp
  import com.opclient.recentchanges.domain.RecentChange
  import com.opclient.ui.components.SecondaryButton
  import com.opclient.ui.components.SectionLabel
  import com.opclient.ui.components.feedback.EmptyState
  import com.opclient.ui.components.feedback.ErrorState
  import com.opclient.ui.components.feedback.LoadingState
  import com.opclient.ui.theme.AppThemeTokens
  import kotlinx.datetime.Instant
  import kotlinx.datetime.TimeZone
  import kotlinx.datetime.toLocalDateTime
  import org.koin.compose.viewmodel.koinViewModel

  @Composable
  fun RecentChangesScreen(
      onBookClick: (String) -> Unit,
      onAuthorClick: (String) -> Unit,
      viewModel: RecentChangesViewModel = koinViewModel(),
  ) {
      val uiState by viewModel.uiState.collectAsState()

      Column(modifier = Modifier.fillMaxSize()) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
          ) {
              SectionLabel(text = "RECENT CHANGES")
              SecondaryButton(
                  text = "REFRESH",
                  onClick = { viewModel.onIntent(RecentChangesIntent.Refresh) },
              )
          }

          when {
              uiState.isLoading -> LoadingState()
              uiState.error != null -> ErrorState(
                  message = uiState.error ?: "",
                  onRetry = { viewModel.onIntent(RecentChangesIntent.Refresh) },
              )
              uiState.changes.isEmpty() -> EmptyState(message = "No recent changes")
              else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                  items(uiState.changes, key = { it.id }) { change ->
                      ChangeRow(
                          change = change,
                          onClick = {
                              val key = change.targetKey ?: return@ChangeRow
                              when {
                                  key.startsWith("/works/") || key.startsWith("/books/") ->
                                      onBookClick(key)
                                  key.startsWith("/authors/") ->
                                      onAuthorClick(key)
                              }
                          },
                      )
                  }
              }
          }
      }
  }

  @Composable
  private fun ChangeRow(change: RecentChange, onClick: () -> Unit) {
      val colors = AppThemeTokens.colors
      val typography = AppThemeTokens.typography
      Row(
          modifier = Modifier
              .fillMaxWidth()
              .clickable(onClick = onClick)
              .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
      ) {
          BasicText(
              text = change.label,
              style = typography.bookTitle.copy(color = colors.textPrimary),
              modifier = Modifier.weight(1f),
          )
          BasicText(
              text = formatTimestamp(change.addedAt),
              style = typography.bookAuthor.copy(color = colors.textSecondary),
          )
      }
  }

  private fun formatTimestamp(epochMillis: Long): String {
      val instant = Instant.fromEpochMilliseconds(epochMillis)
      val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
      return "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')}"
  }
  ```

- [ ] **Step 2: Wire RecentChangesScreen in App.kt**

  In `composeApp/src/commonMain/kotlin/com/opclient/App.kt`, add the import and the `Screen.RecentChanges` branch.

  Add import after the existing imports:
  ```kotlin
  import com.opclient.recentchanges.presentation.RecentChangesScreen
  ```

  Add branch to the `when (screen)` block (after `Screen.Profile`):
  ```kotlin
  Screen.RecentChanges -> RecentChangesScreen(
      onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
      onAuthorClick = { key -> navVm.navigateTo(Screen.AuthorDetail(key)) },
  )
  ```

- [ ] **Step 3: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/recentchanges/presentation/RecentChangesScreen.kt \
          composeApp/src/commonMain/kotlin/com/opclient/App.kt
  git commit -m "feat(recentchanges): add RecentChangesScreen and wire in App.kt"
  ```

---

### Task 9: SearchScreen — Full Text Mode Toggle

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchScreen.kt`

- [ ] **Step 1: Replace SearchScreen.kt with the new version**

  Replace the entire contents of `composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchScreen.kt` with:
  ```kotlin
  package com.opclient.search.presentation

  import androidx.compose.foundation.clickable
  import androidx.compose.foundation.layout.Arrangement
  import androidx.compose.foundation.layout.Box
  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.Row
  import androidx.compose.foundation.layout.fillMaxSize
  import androidx.compose.foundation.layout.fillMaxWidth
  import androidx.compose.foundation.layout.height
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.foundation.lazy.items
  import androidx.compose.foundation.text.BasicText
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.LaunchedEffect
  import androidx.compose.runtime.collectAsState
  import androidx.compose.runtime.getValue
  import androidx.compose.runtime.mutableStateOf
  import androidx.compose.runtime.remember
  import androidx.compose.runtime.setValue
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.graphics.painter.ColorPainter
  import androidx.compose.ui.layout.ContentScale
  import androidx.compose.ui.text.style.TextOverflow
  import androidx.compose.ui.unit.dp
  import coil3.compose.AsyncImage
  import com.opclient.core.ApiError
  import com.opclient.searchinside.presentation.SearchInsideIntent
  import com.opclient.searchinside.presentation.SearchInsideViewModel
  import com.opclient.ui.components.BookRow
  import com.opclient.ui.components.FilterChip
  import com.opclient.ui.components.PrimaryButton
  import com.opclient.ui.components.SearchInput
  import com.opclient.ui.components.SectionLabel
  import com.opclient.ui.components.feedback.EmptyState
  import com.opclient.ui.components.feedback.ErrorState
  import com.opclient.ui.components.feedback.LoadingState
  import com.opclient.ui.theme.AppThemeTokens
  import org.koin.compose.viewmodel.koinViewModel

  private enum class SearchMode { BOOKS, FULL_TEXT }

  @Composable
  fun SearchScreen(
      onBookClick: (String) -> Unit,
      viewModel: SearchViewModel = koinViewModel(),
      insideVm: SearchInsideViewModel = koinViewModel(),
  ) {
      val uiState by viewModel.uiState.collectAsState()
      val insideUiState by insideVm.uiState.collectAsState()
      var searchMode by remember { mutableStateOf(SearchMode.BOOKS) }
      var errorMessage by remember { mutableStateOf<String?>(null) }
      val colors = AppThemeTokens.colors
      val typography = AppThemeTokens.typography
      val placeholderPainter = remember(colors.surface2) { ColorPainter(colors.surface2) }

      LaunchedEffect(viewModel) {
          viewModel.effects.collect { effect ->
              when (effect) {
                  is SearchEffect.SearchError -> {
                      errorMessage = when (val err = effect.error) {
                          is ApiError.NetworkError -> "Network error. Check your connection."
                          is ApiError.HttpError -> "Server error (${err.code})."
                          is ApiError.ParseError -> "Failed to parse response."
                          ApiError.Unknown -> "Unknown error occurred."
                      }
                  }
              }
          }
      }

      Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
          SearchInput(
              value = uiState.query,
              onValueChange = { viewModel.onIntent(SearchIntent.QueryChanged(it)) },
              onSearch = {
                  errorMessage = null
                  when (searchMode) {
                      SearchMode.BOOKS -> viewModel.onIntent(SearchIntent.Search)
                      SearchMode.FULL_TEXT -> insideVm.onIntent(SearchInsideIntent.Search(uiState.query))
                  }
              },
              modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
          )

          Row(
              modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
              FilterChip(
                  label = "Books",
                  selected = searchMode == SearchMode.BOOKS,
                  onToggle = { searchMode = SearchMode.BOOKS },
              )
              FilterChip(
                  label = "Full Text",
                  selected = searchMode == SearchMode.FULL_TEXT,
                  onToggle = { searchMode = SearchMode.FULL_TEXT },
              )
          }

          when (searchMode) {
              SearchMode.BOOKS -> {
                  when (uiState.status) {
                      SearchStatus.Idle -> {
                          SectionLabel(text = "RECENT", modifier = Modifier.padding(bottom = 8.dp))
                      }
                      SearchStatus.Loading -> {
                          LoadingState()
                      }
                      SearchStatus.Success, SearchStatus.LoadingMore -> {
                          SectionLabel(
                              text = "RESULTS · ${uiState.totalFound} BOOKS",
                              modifier = Modifier.padding(bottom = 8.dp),
                          )
                          LazyColumn(modifier = Modifier.fillMaxSize()) {
                              items(uiState.books, key = { it.key }) { book ->
                                  BookRow(
                                      title = book.title,
                                      author = book.author,
                                      subject = book.primarySubject,
                                      coverContent = {
                                          if (book.coverUrl != null) {
                                              AsyncImage(
                                                  model = book.coverUrl,
                                                  contentDescription = null,
                                                  contentScale = ContentScale.Crop,
                                                  placeholder = placeholderPainter,
                                                  error = placeholderPainter,
                                                  modifier = Modifier.fillMaxSize(),
                                              )
                                          } else {
                                              Box(modifier = Modifier.fillMaxSize())
                                          }
                                      },
                                      onClick = { onBookClick(book.key) },
                                  )
                              }
                              if (uiState.canLoadMore && uiState.status != SearchStatus.LoadingMore) {
                                  item {
                                      Box(
                                          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                          contentAlignment = Alignment.Center,
                                      ) {
                                          PrimaryButton(
                                              text = "LOAD MORE",
                                              onClick = { viewModel.onIntent(SearchIntent.LoadMore) },
                                          )
                                      }
                                  }
                              }
                              if (uiState.status == SearchStatus.LoadingMore) {
                                  item {
                                      Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                                          LoadingState(modifier = Modifier.fillMaxSize())
                                      }
                                  }
                              }
                          }
                      }
                      SearchStatus.Empty -> {
                          EmptyState(message = "No books found for \"${uiState.query}\"")
                      }
                      SearchStatus.Error -> {
                          ErrorState(
                              message = errorMessage ?: "Something went wrong.",
                              onRetry = { viewModel.onIntent(SearchIntent.Search) },
                          )
                      }
                  }
              }
              SearchMode.FULL_TEXT -> {
                  when {
                      insideUiState.isLoading -> LoadingState()
                      insideUiState.error != null -> ErrorState(
                          message = insideUiState.error ?: "",
                          onRetry = { insideVm.onIntent(SearchInsideIntent.Search(uiState.query)) },
                      )
                      !insideUiState.hasSearched -> SectionLabel(
                          text = "SEARCH INSIDE BOOKS",
                          modifier = Modifier.padding(bottom = 8.dp),
                      )
                      insideUiState.results.isEmpty() -> EmptyState(
                          message = "No passages found for \"${uiState.query}\"",
                      )
                      else -> {
                          SectionLabel(
                              text = "PASSAGES · ${insideUiState.results.size}",
                              modifier = Modifier.padding(bottom = 8.dp),
                          )
                          LazyColumn(modifier = Modifier.fillMaxSize()) {
                              items(insideUiState.results, key = { it.workKey }) { result ->
                                  Column(
                                      modifier = Modifier
                                          .fillMaxWidth()
                                          .clickable { onBookClick(result.workKey) }
                                          .padding(vertical = 12.dp),
                                  ) {
                                      BasicText(
                                          text = result.title,
                                          style = typography.bookTitle.copy(color = colors.textPrimary),
                                      )
                                      if (result.authorName != null) {
                                          BasicText(
                                              text = result.authorName,
                                              style = typography.bookAuthor.copy(color = colors.textSecondary),
                                          )
                                      }
                                      BasicText(
                                          text = "\"${result.passage}\"",
                                          style = typography.body.copy(color = colors.textSecondary),
                                          maxLines = 3,
                                          overflow = TextOverflow.Ellipsis,
                                      )
                                  }
                              }
                          }
                      }
                  }
              }
          }
      }
  }
  ```

- [ ] **Step 2: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchScreen.kt
  git commit -m "feat(searchinside): add full text mode toggle to SearchScreen"
  ```

---

### Task 10: Koin Wiring + Full Test Suite

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/di/SearchInsideModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/di/RecentChangesModule.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`

- [ ] **Step 1: Create SearchInsideModule**

  Create `composeApp/src/commonMain/kotlin/com/opclient/di/SearchInsideModule.kt`:
  ```kotlin
  package com.opclient.di

  import com.opclient.searchinside.data.SearchInsideApiClient
  import com.opclient.searchinside.data.SearchInsideRepositoryImpl
  import com.opclient.searchinside.domain.SearchInsideRepository
  import com.opclient.searchinside.presentation.SearchInsideViewModel
  import org.koin.core.module.Module
  import org.koin.core.module.dsl.viewModel
  import org.koin.dsl.module

  val searchInsideModule: Module = module {
      factory { SearchInsideApiClient(get()) }
      single<SearchInsideRepository> { SearchInsideRepositoryImpl(get()) }
      viewModel { SearchInsideViewModel(get()) }
  }
  ```

- [ ] **Step 2: Create RecentChangesModule**

  Create `composeApp/src/commonMain/kotlin/com/opclient/di/RecentChangesModule.kt`:
  ```kotlin
  package com.opclient.di

  import com.opclient.recentchanges.data.RecentChangesApiClient
  import com.opclient.recentchanges.data.RecentChangesRepositoryImpl
  import com.opclient.recentchanges.domain.RecentChangesRepository
  import com.opclient.recentchanges.presentation.RecentChangesViewModel
  import org.koin.core.module.Module
  import org.koin.core.module.dsl.viewModel
  import org.koin.dsl.module

  val recentChangesModule: Module = module {
      factory { RecentChangesApiClient(get()) }
      single<RecentChangesRepository> { RecentChangesRepositoryImpl(get()) }
      viewModel { RecentChangesViewModel(get()) }
  }
  ```

- [ ] **Step 3: Update OpClientApplication.kt**

  In `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`, add the two new imports:
  ```kotlin
  import com.opclient.di.recentChangesModule
  import com.opclient.di.searchInsideModule
  ```

  Change the `modules(...)` call to:
  ```kotlin
  modules(commonModule, androidModule, settingsModule, searchModule, searchInsideModule, authorModule, subjectModule, libraryModule, bookModule, profileModule, recentChangesModule)
  ```

- [ ] **Step 4: Update Main.kt**

  In `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`, add the two new imports:
  ```kotlin
  import com.opclient.di.recentChangesModule
  import com.opclient.di.searchInsideModule
  ```

  Change the `modules(...)` call to:
  ```kotlin
  modules(commonModule, desktopModule, settingsModule, searchModule, searchInsideModule, authorModule, subjectModule, libraryModule, bookModule, profileModule, recentChangesModule)
  ```

- [ ] **Step 5: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Run full test suite**

  Run: `./gradlew :composeApp:jvmTest`
  Expected: all tests PASSED (includes all 14 new tests + all prior tests)

  If any test fails, fix it before continuing.

- [ ] **Step 7: Launch desktop app and verify**

  Run: `./gradlew :composeApp:run &`

  Verify these golden paths:

  1. Four tabs visible in nav bar: Search, Browse, Library, Changes
  2. CHANGES tab navigates to Recent Changes screen with "RECENT CHANGES" header and REFRESH button
  3. Recent changes list loads automatically (may show spinner then items)
  4. REFRESH button re-fetches the list
  5. Tapping a "Book added" item navigates to BookDetailScreen
  6. Tapping a "Author updated" item navigates to AuthorDetailScreen
  7. On SEARCH tab: two FilterChips visible — "BOOKS" (selected by default) and "FULL TEXT"
  8. BOOKS mode: existing search works unchanged
  9. FULL TEXT mode: type a query, press search → shows passages with book title, author, excerpt
  10. Tapping a passage result navigates to BookDetailScreen
  11. Switching from BOOKS to FULL TEXT preserves the query and keeps BOOKS results hidden (not cleared)
  12. Switching back to BOOKS restores previous BOOKS results

- [ ] **Step 8: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/di/SearchInsideModule.kt \
          composeApp/src/commonMain/kotlin/com/opclient/di/RecentChangesModule.kt \
          composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt \
          composeApp/src/jvmMain/kotlin/com/opclient/Main.kt
  git commit -m "feat: add Koin wiring for SearchInsideModule and RecentChangesModule"
  ```
