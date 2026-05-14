# Week 4 — Subjects & Categories Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the BROWSE tab (subject browsing with pagination), "More in {subject}" related-content section on BookDetailScreen, and `fields` param optimization in SearchApiClient.

**Architecture:** Three subsystems built in dependency order: (1) subject data layer + ViewModels + screens wired to BROWSE tab; (2) BookDetailViewModel updated to load related works via SubjectRepository; (3) SearchApiClient updated with `fields` param. Platform-adaptive pagination uses `expect object PlatformConfig` with JVM `useLazyPagination=false` (load-more button) and Android `useLazyPagination=true` (infinite scroll).

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.7.1, Ktor 3.0.3 + MockEngine, kotlinx.serialization, kotlinx.coroutines + Turbine 1.1.0, Koin 4.0.0, kotlin.test, `buildHttpClient` from `com.opclient.core`.

---

### Task 1: PlatformConfig expect/actual

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/platform/PlatformConfig.kt`
- Create: `composeApp/src/androidMain/kotlin/com/opclient/platform/PlatformConfig.kt`
- Create: `composeApp/src/jvmMain/kotlin/com/opclient/platform/PlatformConfig.kt`

No test needed — compile-time constant verified by the build.

- [ ] **Step 1: Create commonMain expect**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/platform/PlatformConfig.kt
package com.opclient.platform

expect object PlatformConfig {
    val useLazyPagination: Boolean
}
```

- [ ] **Step 2: Create androidMain actual**

```kotlin
// composeApp/src/androidMain/kotlin/com/opclient/platform/PlatformConfig.kt
package com.opclient.platform

actual object PlatformConfig {
    actual val useLazyPagination = true
}
```

- [ ] **Step 3: Create jvmMain actual**

```kotlin
// composeApp/src/jvmMain/kotlin/com/opclient/platform/PlatformConfig.kt
package com.opclient.platform

actual object PlatformConfig {
    actual val useLazyPagination = false
}
```

- [ ] **Step 4: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinJvm :composeApp:compileKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/platform/PlatformConfig.kt \
        composeApp/src/androidMain/kotlin/com/opclient/platform/PlatformConfig.kt \
        composeApp/src/jvmMain/kotlin/com/opclient/platform/PlatformConfig.kt
git commit -m "feat(platform): add PlatformConfig expect/actual for pagination style"
```

---

### Task 2: Screen + NavigationViewModel + NavigationViewModelTest updates

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/opclient/navigation/NavigationViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Open `composeApp/src/commonTest/kotlin/com/opclient/navigation/NavigationViewModelTest.kt`.

Replace the file with the full updated content (adds `Destination` import, fixes existing `navigateToTab` call, adds three new tests):

```kotlin
package com.opclient.navigation

import com.opclient.ui.navigation.Destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigationViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun initialStack_isSearch() = runTest {
        val vm = NavigationViewModel()
        assertEquals(listOf(Screen.Search), vm.stack.value)
    }

    @Test
    fun navigateTo_pushesScreen() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        assertEquals(listOf(Screen.Search, Screen.BookDetail("/works/OL1W")), vm.stack.value)
    }

    @Test
    fun navigateBack_popsScreen() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateBack()
        assertEquals(listOf(Screen.Search), vm.stack.value)
    }

    @Test
    fun navigateBack_onRoot_isNoOp() = runTest {
        val vm = NavigationViewModel()
        vm.navigateBack()
        assertEquals(listOf(Screen.Search), vm.stack.value)
    }

    @Test
    fun navigateToTab_search_resetsToSearch() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateToTab(Destination.SEARCH)
        assertEquals(listOf(Screen.Search), vm.stack.value)
    }

    @Test
    fun navigateToTab_browse_resetsToSubjectList() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateToTab(Destination.BROWSE)
        assertEquals(listOf(Screen.SubjectList), vm.stack.value)
    }

    @Test
    fun navigateToTab_library_resetsToSearch() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateToTab(Destination.LIBRARY)
        assertEquals(listOf(Screen.Search), vm.stack.value)
    }

    @Test
    fun deepStack_multipleBack() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateTo(Screen.AuthorDetail("/authors/OL1A"))
        vm.navigateBack()
        assertEquals(listOf(Screen.Search, Screen.BookDetail("/works/OL1W")), vm.stack.value)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.navigation.NavigationViewModelTest"`
Expected: FAIL — `navigateToTab` has wrong signature, `Screen.SubjectList` not found.

- [ ] **Step 3: Add SubjectList and SubjectDetail to Screen.kt**

Replace file content:

```kotlin
package com.opclient.navigation

sealed class Screen {
    data object Search : Screen()
    data class BookDetail(val workKey: String) : Screen()
    data class AuthorDetail(val authorKey: String) : Screen()
    data object SubjectList : Screen()
    data class SubjectDetail(val subjectName: String) : Screen()
}
```

- [ ] **Step 4: Update NavigationViewModel to accept Destination**

Replace file content:

```kotlin
package com.opclient.navigation

import androidx.lifecycle.ViewModel
import com.opclient.ui.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class NavigationViewModel : ViewModel() {
    private val _stack = MutableStateFlow<List<Screen>>(listOf(Screen.Search))
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()

    fun navigateTo(screen: Screen) {
        _stack.update { it + screen }
    }

    fun navigateBack() {
        _stack.update { if (it.size > 1) it.dropLast(1) else it }
    }

    fun navigateToTab(destination: Destination) {
        _stack.update {
            when (destination) {
                Destination.SEARCH -> listOf(Screen.Search)
                Destination.BROWSE -> listOf(Screen.SubjectList)
                Destination.LIBRARY -> listOf(Screen.Search)
            }
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.navigation.NavigationViewModelTest"`
Expected: 8 tests, all PASS

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt \
        composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt \
        composeApp/src/commonTest/kotlin/com/opclient/navigation/NavigationViewModelTest.kt
git commit -m "feat(navigation): add SubjectList/SubjectDetail screens, navigateToTab(Destination)"
```

---

### Task 3: SubjectDto + SubjectModels + SubjectMapper + SubjectMapperTest

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/domain/SubjectModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectMapper.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectMapperTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectMapperTest.kt`:

```kotlin
package com.opclient.subject

import com.opclient.subject.data.SubjectDto
import com.opclient.subject.data.SubjectWorkAuthorDto
import com.opclient.subject.data.SubjectWorkDto
import com.opclient.subject.data.toCoverUrl
import com.opclient.subject.data.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubjectMapperTest {

    @Test
    fun toCoverUrl_withCoverId_returnsUrl() {
        assertEquals(
            "https://covers.openlibrary.org/b/id/99-M.jpg",
            99.toCoverUrl(),
        )
    }

    @Test
    fun toCoverUrl_null_returnsNull() {
        assertNull(null.toCoverUrl())
    }

    @Test
    fun subjectWorkDto_toDomain_mapsAllFields() {
        val dto = SubjectWorkDto(
            key = "/works/OL1W",
            title = "Dune",
            authors = listOf(SubjectWorkAuthorDto("Frank Herbert")),
            coverId = 123,
        )
        val domain = dto.toDomain()
        assertEquals("/works/OL1W", domain.key)
        assertEquals("Dune", domain.title)
        assertEquals("Frank Herbert", domain.authorName)
        assertEquals("https://covers.openlibrary.org/b/id/123-M.jpg", domain.coverUrl)
    }

    @Test
    fun subjectWorkDto_nullTitle_defaultsToUnknown() {
        val dto = SubjectWorkDto(key = "/works/OL1W", title = null, authors = null, coverId = null)
        val domain = dto.toDomain()
        assertEquals("Unknown Title", domain.title)
        assertNull(domain.authorName)
        assertNull(domain.coverUrl)
    }

    @Test
    fun subjectWorkDto_emptyAuthors_nullAuthorName() {
        val dto = SubjectWorkDto(key = "/works/OL1W", title = "T", authors = emptyList(), coverId = null)
        assertNull(dto.toDomain().authorName)
    }

    @Test
    fun subjectDto_toDomain_mapsNameAndWorkCount() {
        val dto = SubjectDto(name = "Science Fiction", workCount = 100, works = null)
        val page = dto.toDomain()
        assertEquals("Science Fiction", page.subjectName)
        assertEquals(100, page.workCount)
        assertEquals(emptyList(), page.works)
    }

    @Test
    fun subjectDto_toDomain_mapsWorks() {
        val dto = SubjectDto(
            name = "SF",
            workCount = 1,
            works = listOf(SubjectWorkDto("/works/OL1W", "Dune", listOf(SubjectWorkAuthorDto("FH")), 5)),
        )
        val page = dto.toDomain()
        assertEquals(1, page.works.size)
        assertEquals("Dune", page.works[0].title)
        assertEquals("FH", page.works[0].authorName)
    }

    @Test
    fun subjectDto_nullWorks_returnsEmptyList() {
        val dto = SubjectDto(name = "SF", workCount = 0, works = null)
        assertEquals(emptyList(), dto.toDomain().works)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.subject.SubjectMapperTest"`
Expected: FAIL — types not defined yet.

- [ ] **Step 3: Create SubjectDto.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectDto.kt
package com.opclient.subject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubjectDto(
    val name: String,
    @SerialName("work_count") val workCount: Int = 0,
    val works: List<SubjectWorkDto>? = null,
)

@Serializable
data class SubjectWorkDto(
    val key: String,
    val title: String? = null,
    val authors: List<SubjectWorkAuthorDto>? = null,
    @SerialName("cover_id") val coverId: Int? = null,
)

@Serializable
data class SubjectWorkAuthorDto(val name: String? = null)
```

- [ ] **Step 4: Create SubjectModels.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/domain/SubjectModels.kt
package com.opclient.subject.domain

data class SubjectPage(
    val subjectName: String,
    val workCount: Int,
    val works: List<SubjectWork>,
)

data class SubjectWork(
    val key: String,
    val title: String,
    val authorName: String?,
    val coverUrl: String?,
)
```

- [ ] **Step 5: Create SubjectMapper.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectMapper.kt
package com.opclient.subject.data

import com.opclient.subject.domain.SubjectPage
import com.opclient.subject.domain.SubjectWork

private const val COVER_BASE = "https://covers.openlibrary.org/b/id"

internal fun Int?.toCoverUrl(): String? = this?.let { "$COVER_BASE/$it-M.jpg" }

internal fun SubjectWorkDto.toDomain(): SubjectWork = SubjectWork(
    key = key,
    title = title ?: "Unknown Title",
    authorName = authors?.firstOrNull()?.name,
    coverUrl = coverId.toCoverUrl(),
)

internal fun SubjectDto.toDomain(): SubjectPage = SubjectPage(
    subjectName = name,
    workCount = workCount,
    works = works?.map { it.toDomain() } ?: emptyList(),
)
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.subject.SubjectMapperTest"`
Expected: 8 tests, all PASS

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectDto.kt \
        composeApp/src/commonMain/kotlin/com/opclient/subject/domain/SubjectModels.kt \
        composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectMapper.kt \
        composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectMapperTest.kt
git commit -m "feat(subject): add SubjectDto, SubjectModels, SubjectMapper with tests"
```

---

### Task 4: SubjectCache + SubjectCacheTest

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectCache.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectCacheTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectCacheTest.kt`:

```kotlin
package com.opclient.subject

import com.opclient.subject.data.SubjectCache
import com.opclient.subject.domain.SubjectPage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubjectCacheTest {

    private fun page(name: String = "SF") = SubjectPage(name, 10, emptyList())

    @Test
    fun get_onMiss_returnsNull() = runTest {
        assertNull(SubjectCache().get("subject:sf"))
    }

    @Test
    fun get_afterPut_returnsEntry() = runTest {
        val cache = SubjectCache()
        val p = page()
        cache.put("subject:sf", p)
        assertEquals(p, cache.get("subject:sf"))
    }

    @Test
    fun get_expiredEntry_returnsNull() = runTest {
        var fakeTime = 0L
        val cache = SubjectCache(ttlMs = 100L, timeSource = { fakeTime })
        cache.put("subject:sf", page())
        fakeTime = 200L
        assertNull(cache.get("subject:sf"))
    }

    @Test
    fun put_overwritesExistingEntry() = runTest {
        val cache = SubjectCache()
        cache.put("subject:sf", page("old"))
        val updated = page("new")
        cache.put("subject:sf", updated)
        assertEquals(updated, cache.get("subject:sf"))
    }

    @Test
    fun get_differentKeys_independent() = runTest {
        val cache = SubjectCache()
        val p1 = page("Mystery")
        val p2 = page("Fantasy")
        cache.put("subject:mystery", p1)
        cache.put("subject:fantasy", p2)
        assertEquals(p1, cache.get("subject:mystery"))
        assertEquals(p2, cache.get("subject:fantasy"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.subject.SubjectCacheTest"`
Expected: FAIL — `SubjectCache` not defined.

- [ ] **Step 3: Create SubjectCache.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectCache.kt
package com.opclient.subject.data

import com.opclient.subject.domain.SubjectPage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

private const val DEFAULT_TTL_MS = 30 * 60 * 1_000L

class SubjectCache(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val timeSource: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()
    private val store = mutableMapOf<String, Pair<SubjectPage, Long>>()

    suspend fun get(key: String): SubjectPage? = mutex.withLock {
        store[key]?.takeIf { (_, ts) -> now() - ts < ttlMs }?.first
    }

    suspend fun put(key: String, value: SubjectPage): Unit = mutex.withLock {
        store[key] = value to now()
    }

    private fun now(): Long = timeSource()
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.subject.SubjectCacheTest"`
Expected: 5 tests, all PASS

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectCache.kt \
        composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectCacheTest.kt
git commit -m "feat(subject): add SubjectCache with TTL and injectable timeSource"
```

---

### Task 5: SubjectApiClient

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectApiClient.kt`

The API client is tested indirectly through `SubjectRepositoryTest` in Task 6.

- [ ] **Step 1: Create SubjectApiClient.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectApiClient.kt
package com.opclient.subject.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class SubjectApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun getSubject(subjectName: String, limit: Int, offset: Int): Result<SubjectDto, ApiError> {
        val normalized = subjectName.replace(" ", "_").lowercase()
        return get(
            path = "/subjects/$normalized.json",
            queryParams = mapOf("limit" to "$limit", "offset" to "$offset"),
        )
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectApiClient.kt
git commit -m "feat(subject): add SubjectApiClient"
```

---

### Task 6: SubjectRepository interface + impl + tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/domain/SubjectRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectRepositoryImpl.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectRepositoryTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectRepositoryTest.kt`:

```kotlin
package com.opclient.subject

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import com.opclient.subject.data.SubjectApiClient
import com.opclient.subject.data.SubjectCache
import com.opclient.subject.data.SubjectRepositoryImpl
import com.opclient.subject.domain.SubjectPage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SubjectRepositoryTest {

    private val subjectJson = """
        {
          "name": "Science Fiction",
          "work_count": 100,
          "works": [
            {
              "key": "/works/OL1W",
              "title": "Dune",
              "authors": [{"name": "Frank Herbert"}],
              "cover_id": 123
            }
          ]
        }
    """.trimIndent()

    private fun makeRepo(
        engine: MockEngine,
        cache: SubjectCache = SubjectCache(),
    ): SubjectRepositoryImpl {
        val apiClient = SubjectApiClient(buildHttpClient(engine))
        return SubjectRepositoryImpl(apiClient = apiClient, cache = cache)
    }

    @Test
    fun getSubjectPage_cacheMiss_callsApiAndReturns() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(subjectJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val result = makeRepo(engine).getSubjectPage("Science Fiction", 12, 0)

        assertIs<Result.Success<SubjectPage>>(result)
        assertEquals("Science Fiction", result.value.subjectName)
        assertEquals(100, result.value.workCount)
        assertEquals(1, result.value.works.size)
        assertEquals("Dune", result.value.works[0].title)
        assertEquals(1, callCount)
    }

    @Test
    fun getSubjectPage_cacheHit_skipsApi() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(subjectJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = SubjectCache()
        val repo = makeRepo(engine, cache)

        repo.getSubjectPage("Science Fiction", 12, 0)
        assertEquals(1, callCount)

        repo.getSubjectPage("Science Fiction", 12, 0)
        assertEquals(1, callCount)
    }

    @Test
    fun getSubjectPage_offset0_storesInCache() = runTest {
        val engine = MockEngine {
            respond(subjectJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = SubjectCache()
        val repo = makeRepo(engine, cache)

        repo.getSubjectPage("Science Fiction", 12, 0)

        val cached = cache.get("subject:science_fiction")
        assertEquals("Science Fiction", cached?.subjectName)
    }

    @Test
    fun getSubjectPage_offsetNonZero_bypassesCache() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(subjectJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = SubjectCache()
        val repo = makeRepo(engine, cache)

        repo.getSubjectPage("Science Fiction", 12, 0)
        assertEquals(1, callCount)

        repo.getSubjectPage("Science Fiction", 12, 12)
        assertEquals(2, callCount)

        repo.getSubjectPage("Science Fiction", 12, 12)
        assertEquals(3, callCount)
    }

    @Test
    fun getSubjectPage_normalizesSubjectName() = runTest {
        var capturedPath = ""
        val engine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(subjectJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        makeRepo(engine).getSubjectPage("Science Fiction", 12, 0)

        assertTrue(capturedPath.contains("science_fiction"), "Expected 'science_fiction' in $capturedPath")
    }

    @Test
    fun getSubjectPage_apiError_propagatesFailure() = runTest {
        val engine = MockEngine {
            respond("error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
        }

        val result = makeRepo(engine).getSubjectPage("Science Fiction", 12, 0)

        assertIs<Result.Failure<ApiError>>(result)
        assertIs<ApiError.HttpError>(result.error)
        assertEquals(500, (result.error as ApiError.HttpError).code)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.subject.SubjectRepositoryTest"`
Expected: FAIL — `SubjectRepositoryImpl` and `SubjectRepository` not found.

- [ ] **Step 3: Create SubjectRepository.kt interface**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/domain/SubjectRepository.kt
package com.opclient.subject.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface SubjectRepository {
    suspend fun getSubjectPage(
        subjectName: String,
        limit: Int = 12,
        offset: Int = 0,
    ): Result<SubjectPage, ApiError>
}
```

- [ ] **Step 4: Create SubjectRepositoryImpl.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectRepositoryImpl.kt
package com.opclient.subject.data

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.subject.domain.SubjectPage
import com.opclient.subject.domain.SubjectRepository

class SubjectRepositoryImpl(
    private val apiClient: SubjectApiClient,
    private val cache: SubjectCache,
) : SubjectRepository {

    override suspend fun getSubjectPage(
        subjectName: String,
        limit: Int,
        offset: Int,
    ): Result<SubjectPage, ApiError> {
        val normalized = subjectName.replace(" ", "_").lowercase()

        if (offset == 0) {
            val cacheKey = "subject:$normalized"
            cache.get(cacheKey)?.let { return Result.Success(it) }
            return when (val result = apiClient.getSubject(subjectName, limit, offset)) {
                is Result.Success -> {
                    val page = result.value.toDomain()
                    cache.put(cacheKey, page)
                    Result.Success(page)
                }
                is Result.Failure -> result
            }
        }

        return when (val result = apiClient.getSubject(subjectName, limit, offset)) {
            is Result.Success -> Result.Success(result.value.toDomain())
            is Result.Failure -> result
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.subject.SubjectRepositoryTest"`
Expected: 6 tests, all PASS

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/subject/domain/SubjectRepository.kt \
        composeApp/src/commonMain/kotlin/com/opclient/subject/data/SubjectRepositoryImpl.kt \
        composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectRepositoryTest.kt
git commit -m "feat(subject): add SubjectRepository interface and impl with cache-first strategy and tests"
```

---

### Task 7: SubjectBrowseViewModel + SubjectBrowseViewModelTest

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectBrowseViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectBrowseViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectBrowseViewModelTest.kt`:

```kotlin
package com.opclient.subject

import com.opclient.subject.presentation.FeaturedSubjects
import com.opclient.subject.presentation.SubjectBrowseViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubjectBrowseViewModelTest {

    @Test
    fun initialState_containsAll12Subjects() {
        val vm = SubjectBrowseViewModel()
        assertEquals(12, vm.uiState.value.subjects.size)
    }

    @Test
    fun initialState_matchesFeaturedSubjectsList() {
        val vm = SubjectBrowseViewModel()
        assertEquals(FeaturedSubjects.list, vm.uiState.value.subjects)
    }

    @Test
    fun initialState_containsExpectedSubjects() {
        val vm = SubjectBrowseViewModel()
        val subjects = vm.uiState.value.subjects
        assertTrue(subjects.contains("Science Fiction"))
        assertTrue(subjects.contains("Mystery"))
        assertTrue(subjects.contains("History"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.subject.SubjectBrowseViewModelTest"`
Expected: FAIL — `SubjectBrowseViewModel` not found.

- [ ] **Step 3: Create SubjectBrowseViewModel.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectBrowseViewModel.kt
package com.opclient.subject.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FeaturedSubjects {
    val list = listOf(
        "Science Fiction", "Mystery", "Romance", "Fantasy",
        "History", "Biography", "Science", "Philosophy",
        "Children", "Poetry", "Travel", "Cooking",
    )
}

data class SubjectBrowseUiState(val subjects: List<String> = FeaturedSubjects.list)

class SubjectBrowseViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SubjectBrowseUiState())
    val uiState: StateFlow<SubjectBrowseUiState> = _uiState.asStateFlow()
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.subject.SubjectBrowseViewModelTest"`
Expected: 3 tests, all PASS

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectBrowseViewModel.kt \
        composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectBrowseViewModelTest.kt
git commit -m "feat(subject): add SubjectBrowseViewModel with hardcoded featured subjects list"
```

---

### Task 8: SubjectDetailViewModel + SubjectDetailViewModelTest

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectDetailViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectDetailViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectDetailViewModelTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.subject.SubjectDetailViewModelTest"`
Expected: FAIL — `SubjectDetailViewModel` not found.

- [ ] **Step 3: Create SubjectDetailViewModel.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectDetailViewModel.kt
package com.opclient.subject.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.presentation.DetailStatus
import com.opclient.subject.domain.SubjectRepository
import com.opclient.subject.domain.SubjectWork
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubjectDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val subjectName: String = "",
    val works: List<SubjectWork> = emptyList(),
    val workCount: Int = 0,
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
)

sealed class SubjectDetailIntent {
    data class Load(val subjectName: String) : SubjectDetailIntent()
    data object LoadMore : SubjectDetailIntent()
    data object Retry : SubjectDetailIntent()
}

sealed class SubjectDetailEffect {
    data class LoadError(val error: ApiError) : SubjectDetailEffect()
}

class SubjectDetailViewModel(private val repository: SubjectRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SubjectDetailUiState())
    val uiState: StateFlow<SubjectDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SubjectDetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<SubjectDetailEffect> = _effects.asSharedFlow()

    private val pageSize = 12
    private var lastSubjectName: String = ""

    fun onIntent(intent: SubjectDetailIntent) {
        when (intent) {
            is SubjectDetailIntent.Load -> load(intent.subjectName)
            SubjectDetailIntent.LoadMore -> loadMore()
            SubjectDetailIntent.Retry -> if (lastSubjectName.isNotEmpty()) load(lastSubjectName)
        }
    }

    private fun load(subjectName: String) {
        lastSubjectName = subjectName
        viewModelScope.launch {
            _uiState.update { it.copy(status = DetailStatus.Loading, subjectName = subjectName, works = emptyList()) }
            when (val result = repository.getSubjectPage(subjectName, pageSize, 0)) {
                is Result.Success -> _uiState.update {
                    val works = result.value.works
                    it.copy(
                        status = DetailStatus.Success,
                        works = works,
                        workCount = result.value.workCount,
                        canLoadMore = works.size < result.value.workCount,
                    )
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = DetailStatus.Error) }
                    _effects.tryEmit(SubjectDetailEffect.LoadError(result.error))
                }
            }
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.canLoadMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            when (val result = repository.getSubjectPage(lastSubjectName, pageSize, state.works.size)) {
                is Result.Success -> _uiState.update {
                    val newWorks = it.works + result.value.works
                    it.copy(
                        isLoadingMore = false,
                        works = newWorks,
                        canLoadMore = newWorks.size < it.workCount,
                    )
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(isLoadingMore = false, canLoadMore = false) }
                    _effects.tryEmit(SubjectDetailEffect.LoadError(result.error))
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.subject.SubjectDetailViewModelTest"`
Expected: 8 tests, all PASS

- [ ] **Step 5: Run all tests to ensure no regressions**

Run: `./gradlew :composeApp:allTests`
Expected: all tests PASS

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectDetailViewModel.kt \
        composeApp/src/commonTest/kotlin/com/opclient/subject/SubjectDetailViewModelTest.kt
git commit -m "feat(subject): add SubjectDetailViewModel MVI with pagination and tests"
```

---

### Task 9: SubjectModule Koin wiring

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/di/SubjectModule.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`

- [ ] **Step 1: Create SubjectModule.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/di/SubjectModule.kt
package com.opclient.di

import com.opclient.subject.data.SubjectApiClient
import com.opclient.subject.data.SubjectCache
import com.opclient.subject.data.SubjectRepositoryImpl
import com.opclient.subject.domain.SubjectRepository
import com.opclient.subject.presentation.SubjectBrowseViewModel
import com.opclient.subject.presentation.SubjectDetailViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val subjectModule: Module = module {
    single { SubjectCache() }
    factory { SubjectApiClient(get()) }
    single<SubjectRepository> { SubjectRepositoryImpl(apiClient = get(), cache = get()) }
    viewModel { SubjectBrowseViewModel() }
    viewModel { SubjectDetailViewModel(get()) }
}
```

- [ ] **Step 2: Update OpClientApplication.kt — add subjectModule before bookModule**

```kotlin
// composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt
package com.opclient

import android.app.Application
import com.opclient.di.androidModule
import com.opclient.di.authorModule
import com.opclient.di.bookModule
import com.opclient.di.commonModule
import com.opclient.di.searchModule
import com.opclient.di.subjectModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class OpClientApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        startKoin {
            androidContext(this@OpClientApplication)
            modules(commonModule, androidModule, searchModule, authorModule, subjectModule, bookModule)
        }
    }
}
```

- [ ] **Step 3: Update Main.kt — add subjectModule before bookModule**

```kotlin
// composeApp/src/jvmMain/kotlin/com/opclient/Main.kt
package com.opclient

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.opclient.di.authorModule
import com.opclient.di.bookModule
import com.opclient.di.commonModule
import com.opclient.di.desktopModule
import com.opclient.di.searchModule
import com.opclient.di.subjectModule
import com.opclient.ui.theme.AppTheme
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin

fun main() {
    Napier.base(DebugAntilog())
    startKoin {
        modules(commonModule, desktopModule, searchModule, authorModule, subjectModule, bookModule)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "op_client",
        ) {
            AppTheme {
                App()
            }
        }
    }
}
```

- [ ] **Step 4: Verify full build**

Run: `./gradlew :composeApp:compileKotlinJvm :composeApp:allTests`
Expected: BUILD SUCCESSFUL, all tests PASS

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/di/SubjectModule.kt \
        composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt \
        composeApp/src/jvmMain/kotlin/com/opclient/Main.kt
git commit -m "feat(di): add SubjectModule, wire into Android and Desktop DI startup"
```

---

### Task 10: SubjectBrowseScreen composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectBrowseScreen.kt`

No unit test — composable behaviour verified visually in Task 16.

- [ ] **Step 1: Create SubjectBrowseScreen.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectBrowseScreen.kt
package com.opclient.subject.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.components.FilterChip
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubjectBrowseScreen(
    onSubjectClick: (String) -> Unit,
    viewModel: SubjectBrowseViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(uiState.subjects) { subject ->
            FilterChip(
                label = subject,
                selected = false,
                onToggle = { onSubjectClick(subject) },
            )
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectBrowseScreen.kt
git commit -m "feat(subject): add SubjectBrowseScreen with featured subjects grid"
```

---

### Task 11: SubjectDetailScreen composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectDetailScreen.kt`

- [ ] **Step 1: Create SubjectDetailScreen.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectDetailScreen.kt
package com.opclient.subject.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opclient.platform.PlatformConfig
import com.opclient.presentation.DetailStatus
import com.opclient.subject.domain.SubjectWork
import com.opclient.ui.components.BookRow
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SubjectDetailScreen(
    subjectName: String,
    onBookClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SubjectDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(subjectName) {
        viewModel.onIntent(SubjectDetailIntent.Load(subjectName))
    }

    when (uiState.status) {
        DetailStatus.Loading -> LoadingState()
        DetailStatus.Error -> ErrorState(
            message = "Failed to load subject.",
            onRetry = { viewModel.onIntent(SubjectDetailIntent.Retry) },
        )
        DetailStatus.Success -> {
            if (PlatformConfig.useLazyPagination) {
                SubjectDetailLazyContent(uiState, onBack, onBookClick, viewModel)
            } else {
                SubjectDetailScrollContent(uiState, onBack, onBookClick, viewModel)
            }
        }
    }
}

@Composable
private fun SubjectDetailScrollContent(
    uiState: SubjectDetailUiState,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: SubjectDetailViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SecondaryButton(
            text = "← BACK",
            onClick = onBack,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        SectionLabel(
            text = uiState.subjectName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        uiState.works.forEach { work ->
            SubjectWorkRow(work = work, onBookClick = onBookClick)
        }
        if (uiState.canLoadMore) {
            SecondaryButton(
                text = "Load more",
                onClick = { viewModel.onIntent(SubjectDetailIntent.LoadMore) },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
            )
        }
        if (uiState.isLoadingMore) {
            LoadingState()
        }
    }
}

@Composable
private fun SubjectDetailLazyContent(
    uiState: SubjectDetailUiState,
    onBack: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: SubjectDetailViewModel,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.works.size - 3 && uiState.canLoadMore && !uiState.isLoadingMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.onIntent(SubjectDetailIntent.LoadMore)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            SecondaryButton(
                text = "← BACK",
                onClick = onBack,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        item {
            SectionLabel(
                text = uiState.subjectName,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        items(uiState.works, key = { it.key }) { work ->
            SubjectWorkRow(work = work, onBookClick = onBookClick)
        }
        if (uiState.isLoadingMore) {
            item { LoadingState() }
        }
    }
}

@Composable
private fun SubjectWorkRow(work: SubjectWork, onBookClick: (String) -> Unit) {
    val colors = AppThemeTokens.colors
    BookRow(
        title = work.title,
        author = work.authorName ?: "",
        coverContent = {
            AsyncImage(
                model = work.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholder = ColorPainter(colors.surface2),
                error = ColorPainter(colors.surface2),
                modifier = Modifier.fillMaxSize(),
            )
        },
        onClick = { onBookClick(work.key) },
    )
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinJvm`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/subject/presentation/SubjectDetailScreen.kt
git commit -m "feat(subject): add SubjectDetailScreen with platform-adaptive pagination"
```

---

### Task 12: App.kt routing update

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/App.kt`

- [ ] **Step 1: Update App.kt**

Replace the file content with:

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/App.kt
package com.opclient

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.opclient.author.presentation.AuthorDetailScreen
import com.opclient.book.presentation.BookDetailScreen
import com.opclient.navigation.NavigationViewModel
import com.opclient.navigation.Screen
import com.opclient.search.presentation.SearchScreen
import com.opclient.subject.presentation.SubjectBrowseScreen
import com.opclient.subject.presentation.SubjectDetailScreen
import com.opclient.ui.AppShell
import com.opclient.ui.navigation.Destination
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val navVm: NavigationViewModel = koinViewModel()
    val stack by navVm.stack.collectAsState()
    var selectedTab by remember { mutableStateOf(Destination.SEARCH) }

    AppShell(
        selectedDestination = selectedTab,
        onDestinationChange = { tab ->
            selectedTab = tab
            navVm.navigateToTab(tab)
        },
    ) {
        when (val screen = stack.last()) {
            Screen.Search -> SearchScreen(
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
            )
            is Screen.BookDetail -> BookDetailScreen(
                workKey = screen.workKey,
                onAuthorClick = { key -> navVm.navigateTo(Screen.AuthorDetail(key)) },
                onBack = { navVm.navigateBack() },
            )
            is Screen.AuthorDetail -> AuthorDetailScreen(
                authorKey = screen.authorKey,
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
                onBack = { navVm.navigateBack() },
            )
            Screen.SubjectList -> SubjectBrowseScreen(
                onSubjectClick = { name -> navVm.navigateTo(Screen.SubjectDetail(name)) },
            )
            is Screen.SubjectDetail -> SubjectDetailScreen(
                subjectName = screen.subjectName,
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
                onBack = { navVm.navigateBack() },
            )
        }
    }
}
```

- [ ] **Step 2: Verify full build and all tests pass**

Run: `./gradlew :composeApp:compileKotlinJvm :composeApp:allTests`
Expected: BUILD SUCCESSFUL, all tests PASS

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/App.kt
git commit -m "feat(app): route SubjectList and SubjectDetail screens in App.kt"
```

---

### Task 13: BookDetailViewModel update — related works + BookDetailViewModelTest + BookModule

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/opclient/book/BookDetailViewModelTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/di/BookModule.kt`

- [ ] **Step 1: Write the new/updated failing tests**

Replace the content of `composeApp/src/commonTest/kotlin/com/opclient/book/BookDetailViewModelTest.kt`:

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/book/BookDetailViewModelTest.kt
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
import com.opclient.presentation.DetailStatus
import com.opclient.subject.domain.SubjectPage
import com.opclient.subject.domain.SubjectRepository
import com.opclient.subject.domain.SubjectWork
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

    @Test
    fun load_setsLoadingThenSuccess() = runTest {
        val vm = BookDetailViewModel(successRepo(), emptySubjectRepo())
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()
        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertEquals(detail(), vm.uiState.value.book)
    }

    @Test
    fun load_onFailure_setsErrorAndEmitsEffect() = runTest {
        val error = ApiError.HttpError(500, "Server Error")
        val vm = BookDetailViewModel(failingRepo(error), emptySubjectRepo())

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
        val vm = BookDetailViewModel(repo, emptySubjectRepo())
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
        val vm = BookDetailViewModel(repo, emptySubjectRepo())
        vm.onIntent(BookDetailIntent.Retry)
        advanceUntilIdle()
        assertEquals(0, callCount)
    }

    @Test
    fun load_populatesRelatedWorksAfterSuccess() = runTest {
        val vm = BookDetailViewModel(successRepo(), subjectRepo())
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()

        assertEquals(DetailStatus.Success, vm.uiState.value.status)
        assertEquals("Science Fiction", vm.uiState.value.relatedSubjectName)
        assertTrue(vm.uiState.value.relatedWorks.isNotEmpty())
    }

    @Test
    fun load_relatedWorks_excludesCurrentBook() = runTest {
        val vm = BookDetailViewModel(successRepo(), subjectRepo())
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
        val vm = BookDetailViewModel(successRepo(detail(subjects = emptyList())), repo)
        vm.onIntent(BookDetailIntent.Load("/works/OL1W"))
        advanceUntilIdle()

        assertEquals(0, subjectCallCount)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.book.BookDetailViewModelTest"`
Expected: FAIL — `BookDetailViewModel` constructor doesn't accept `SubjectRepository` yet, `relatedWorks` and `relatedSubjectName` not on `BookDetailUiState`.

- [ ] **Step 3: Update BookDetailViewModel.kt**

Replace file content:

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailViewModel.kt
package com.opclient.book.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.book.domain.BookDetail
import com.opclient.book.domain.BookRepository
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.presentation.DetailStatus
import com.opclient.subject.domain.SubjectRepository
import com.opclient.subject.domain.SubjectWork
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BookDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val book: BookDetail? = null,
    val relatedWorks: List<SubjectWork> = emptyList(),
    val relatedSubjectName: String = "",
)

sealed class BookDetailIntent {
    data class Load(val workKey: String) : BookDetailIntent()
    data object Retry : BookDetailIntent()
}

sealed class BookDetailEffect {
    data class LoadError(val error: ApiError) : BookDetailEffect()
}

class BookDetailViewModel(
    private val repository: BookRepository,
    private val subjectRepository: SubjectRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(BookDetailUiState())
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<BookDetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<BookDetailEffect> = _effects.asSharedFlow()

    private var lastKey: String = ""

    fun onIntent(intent: BookDetailIntent) {
        when (intent) {
            is BookDetailIntent.Load -> load(intent.workKey)
            BookDetailIntent.Retry -> if (lastKey.isNotEmpty()) load(lastKey)
        }
    }

    private fun load(workKey: String) {
        lastKey = workKey
        viewModelScope.launch {
            _uiState.update { it.copy(status = DetailStatus.Loading) }
            when (val result = repository.getBook(workKey)) {
                is Result.Success -> {
                    val book = result.value
                    _uiState.update { it.copy(status = DetailStatus.Success, book = book) }
                    if (book.subjects.isNotEmpty()) {
                        loadRelatedWorks(book.subjects.first(), book.key)
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = DetailStatus.Error) }
                    _effects.tryEmit(BookDetailEffect.LoadError(result.error))
                }
            }
        }
    }

    private fun loadRelatedWorks(subjectName: String, currentBookKey: String) {
        viewModelScope.launch {
            when (val result = subjectRepository.getSubjectPage(subjectName, limit = 6, offset = 0)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        relatedWorks = result.value.works.filter { w -> w.key != currentBookKey },
                        relatedSubjectName = subjectName,
                    )
                }
                is Result.Failure -> { /* silent — main book detail stays Success */ }
            }
        }
    }
}
```

- [ ] **Step 4: Update BookModule.kt — inject SubjectRepository**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/di/BookModule.kt
package com.opclient.di

import com.opclient.book.data.BookApiClient
import com.opclient.book.data.BookCache
import com.opclient.book.data.BookRepositoryImpl
import com.opclient.book.domain.BookRepository
import com.opclient.book.presentation.BookDetailViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val bookModule: Module = module {
    single { BookCache() }
    factory { BookApiClient(get()) }
    single<BookRepository> { BookRepositoryImpl(apiClient = get(), cache = get(), authorRepository = get()) }
    viewModel { BookDetailViewModel(get(), get()) }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :composeApp:allTests --tests "com.opclient.book.BookDetailViewModelTest"`
Expected: all tests PASS

- [ ] **Step 6: Run all tests**

Run: `./gradlew :composeApp:allTests`
Expected: all tests PASS

- [ ] **Step 7: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailViewModel.kt \
        composeApp/src/commonTest/kotlin/com/opclient/book/BookDetailViewModelTest.kt \
        composeApp/src/commonMain/kotlin/com/opclient/di/BookModule.kt
git commit -m "feat(book): add related works to BookDetailViewModel via SubjectRepository"
```

---

### Task 14: BookDetailScreen — "More in {subject}" section

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailScreen.kt`

- [ ] **Step 1: Update BookDetailScreen.kt**

Add these imports at the top of the import block (after the existing imports):

```kotlin
import com.opclient.subject.domain.SubjectWork
```

Then add the "More in" section after the existing description block (after the `if (book.description != null)` block, still inside the `Column`):

```kotlin
                if (uiState.relatedWorks.isNotEmpty()) {
                    SectionLabel(
                        text = "More in ${uiState.relatedSubjectName}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    uiState.relatedWorks.forEach { work ->
                        BookRow(
                            title = work.title,
                            author = work.authorName ?: "",
                            coverContent = {
                                AsyncImage(
                                    model = work.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    placeholder = ColorPainter(colors.surface2),
                                    error = ColorPainter(colors.surface2),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                            onClick = { onBookClick(work.key) },
                        )
                    }
                }
```

The full updated `BookDetailScreen.kt` `Success` branch (inside `DetailStatus.Success ->`) should now look like this in the Column:

```kotlin
        DetailStatus.Success -> {
            val book = uiState.book ?: return
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                SecondaryButton(
                    text = "← BACK",
                    onClick = onBack,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                if (book.coverUrl != null) {
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(colors.surface2),
                        error = ColorPainter(colors.surface2),
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                    )
                }
                SectionLabel(
                    text = book.title,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                book.authors.forEach { author ->
                    BasicText(
                        text = author.name,
                        style = typography.bookAuthor.copy(color = colors.textPrimary),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                            .clickable { onAuthorClick(author.key) },
                    )
                }
                if (book.firstPublishDate != null) {
                    BasicText(
                        text = book.firstPublishDate,
                        style = typography.bookAuthor.copy(color = colors.textSecondary),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                if (book.subjects.isNotEmpty()) {
                    FlowRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        book.subjects.take(8).forEach { subject ->
                            SubjectTag(text = subject, modifier = Modifier.padding(end = 4.dp, bottom = 4.dp))
                        }
                    }
                }
                if (book.description != null) {
                    BasicText(
                        text = book.description,
                        style = typography.bookAuthor.copy(color = colors.textPrimary),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (uiState.relatedWorks.isNotEmpty()) {
                    SectionLabel(
                        text = "More in ${uiState.relatedSubjectName}",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    uiState.relatedWorks.forEach { work ->
                        BookRow(
                            title = work.title,
                            author = work.authorName ?: "",
                            coverContent = {
                                AsyncImage(
                                    model = work.coverUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    placeholder = ColorPainter(colors.surface2),
                                    error = ColorPainter(colors.surface2),
                                    modifier = Modifier.fillMaxSize(),
                                )
                            },
                            onClick = { onBookClick(work.key) },
                        )
                    }
                }
            }
        }
```

You also need to add `onBookClick: (String) -> Unit` to the `BookDetailScreen` parameters and pass it through from `App.kt`. Looking at the existing `BookDetailScreen`, it already has `onAuthorClick`. Add `onBookClick` the same way:

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    workKey: String,
    onAuthorClick: (String) -> Unit,
    onBookClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BookDetailViewModel = koinViewModel(),
) {
```

Then update `App.kt` to pass `onBookClick` to `BookDetailScreen`:

```kotlin
is Screen.BookDetail -> BookDetailScreen(
    workKey = screen.workKey,
    onAuthorClick = { key -> navVm.navigateTo(Screen.AuthorDetail(key)) },
    onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
    onBack = { navVm.navigateBack() },
)
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew :composeApp:compileKotlinJvm :composeApp:allTests`
Expected: BUILD SUCCESSFUL, all tests PASS

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailScreen.kt \
        composeApp/src/commonMain/kotlin/com/opclient/App.kt
git commit -m "feat(book): add 'More in subject' related works section to BookDetailScreen"
```

---

### Task 15: SearchApiClient fields param

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchApiClient.kt`

- [ ] **Step 1: Update SearchApiClient.kt — add fields param**

Replace file content:

```kotlin
package com.opclient.search.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class SearchApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun search(
        query: String,
        offset: Int,
        limit: Int,
    ): Result<SearchResponseDto, ApiError> =
        get(
            path = "/search.json",
            queryParams = mapOf(
                "q" to query,
                "offset" to "$offset",
                "limit" to "$limit",
                "fields" to "key,title,author_name,cover_i,first_publish_year",
            ),
        )
}
```

- [ ] **Step 2: Run all tests to verify no regressions**

Run: `./gradlew :composeApp:allTests`
Expected: all tests PASS (MockEngine ignores query params in assertions)

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchApiClient.kt
git commit -m "perf(search): add fields param to SearchApiClient to reduce response payload"
```

---

### Task 16: Live API verification

**Files:** None — runtime verification only.

- [ ] **Step 1: Run full build and all tests**

Run: `./gradlew :composeApp:allTests`
Expected: all tests PASS with BUILD SUCCESSFUL

- [ ] **Step 2: Launch the Desktop app**

Run: `./gradlew :composeApp:run`

- [ ] **Step 3: Verify BROWSE tab loads featured subjects**

1. Click the BROWSE tab in the sidebar.
2. Expected: 12 subject chips displayed in a 2-column grid (Science Fiction, Mystery, Romance, etc.).

- [ ] **Step 4: Verify subject detail with load-more (Desktop)**

1. Click "Science Fiction".
2. Expected: subject detail screen loads, shows first 12 books with cover images.
3. Expected: "← BACK" button visible at top; subject name shown as section label.
4. Expected: "Load more" button visible at bottom (Desktop path).
5. Click "Load more" — expected: 12 more books append below.

- [ ] **Step 5: Verify book navigation from subject detail**

1. Click any book row in the subject detail.
2. Expected: BookDetailScreen loads for that book.
3. Expected: Logs show `REQUEST: https://openlibrary.org/works/OL...json` and `RESPONSE: 200 OK`.

- [ ] **Step 6: Verify "More in {subject}" section on book detail**

1. From a book detail screen (navigate there via search), scroll to the bottom.
2. Expected: "More in {subject name}" section appears below description with 1–5 related book rows.
3. Expected: Logs show subject API request: `REQUEST: https://openlibrary.org/subjects/....json`.
4. Click a related book row — expected: navigates to that book's detail screen.

- [ ] **Step 7: Verify BACK navigation**

1. From book detail (reached via subject), press "← BACK".
2. Expected: returns to subject detail screen.
3. Press "← BACK" again — expected: returns to subject list (BROWSE tab).

- [ ] **Step 8: Verify search still works with fields param**

1. Switch to SEARCH tab, search for "tolkien".
2. Expected: results load correctly; logs show `fields=key,title,author_name,cover_i,first_publish_year` in request URL.
