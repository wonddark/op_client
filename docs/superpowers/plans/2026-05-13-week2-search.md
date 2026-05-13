# Week 2 — Search API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement OpenLibrary full-text search end-to-end — API client, TTL cache, repository, MVI ViewModel, and Search screen — satisfying Milestone 1.

**Architecture:** Repository-first TDD. Each layer is tested before the layer above it. `SearchCache` is a separate Koin `single`. `SearchViewModel` extends `androidx.lifecycle.ViewModel` and uses `viewModelScope` for coroutine lifecycle management.

**Tech Stack:** Ktor 3.0.3 (MockEngine for tests), kotlinx.serialization, kotlinx.coroutines 1.9.0 + Turbine 1.1.0, Koin 4.0.0 `koin-compose-viewmodel`, Coil 3.x `AsyncImage`

---

## File Map

**Create:**
- `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchDto.kt`
- `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchMapper.kt`
- `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchCache.kt`
- `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchApiClient.kt`
- `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchRepositoryImpl.kt`
- `composeApp/src/commonMain/kotlin/com/opclient/search/domain/SearchModels.kt`
- `composeApp/src/commonMain/kotlin/com/opclient/search/domain/SearchRepository.kt`
- `composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchViewModel.kt`
- `composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchScreen.kt`
- `composeApp/src/commonMain/kotlin/com/opclient/di/SearchModule.kt`
- `composeApp/src/commonTest/kotlin/com/opclient/search/SearchCacheTest.kt`
- `composeApp/src/commonTest/kotlin/com/opclient/search/SearchMapperTest.kt`
- `composeApp/src/commonTest/kotlin/com/opclient/search/SearchRepositoryTest.kt`
- `composeApp/src/commonTest/kotlin/com/opclient/search/SearchViewModelTest.kt`

**Modify:**
- `gradle/libs.versions.toml` — add `turbine`, `koin-compose-viewmodel`
- `composeApp/build.gradle.kts` — wire new deps into source sets
- `composeApp/src/commonMain/kotlin/com/opclient/ui/components/SearchInput.kt` — add `onSearch` param
- `composeApp/src/androidUnitTest/kotlin/com/opclient/ui/components/SearchInputTest.kt` — add `onSearch` test
- `composeApp/src/commonMain/kotlin/com/opclient/App.kt` — wire AppShell + SearchScreen
- `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt` — add `searchModule`
- `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt` — add `searchModule`

---

### Task 1: Add missing dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Add entries to the version catalog**

In `gradle/libs.versions.toml`, under `[libraries]`, add:

```toml
turbine = { module = "app.cash.turbine:turbine", version = "1.1.0" }
koin-compose-viewmodel = { module = "io.insert-koin:koin-compose-viewmodel", version.ref = "koin" }
```

- [ ] **Step 2: Wire deps in build.gradle.kts**

In `composeApp/build.gradle.kts`, inside `commonMain.dependencies { }`, add:

```kotlin
implementation(libs.koin.compose.viewmodel)
```

Inside `commonTest.dependencies { }`, add:

```kotlin
implementation(libs.turbine)
```

- [ ] **Step 3: Sync and verify compilation**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`. No unresolved reference errors.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml composeApp/build.gradle.kts
git commit -m "build: add turbine and koin-compose-viewmodel dependencies"
```

---

### Task 2: Extend SearchInput with onSearch callback

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/ui/components/SearchInput.kt`
- Modify: `composeApp/src/androidUnitTest/kotlin/com/opclient/ui/components/SearchInputTest.kt`

- [ ] **Step 1: Write the failing test**

In `SearchInputTest.kt`, add this test inside the class (add import `androidx.compose.ui.test.performImeAction`):

```kotlin
@Test
fun searchInput_callsOnSearch_whenImeActionPerformed() {
    var searchTriggered = false
    composeTestRule.setContent {
        AppTheme {
            SearchInput(
                value = "Dune",
                onValueChange = {},
                onSearch = { searchTriggered = true },
            )
        }
    }
    composeTestRule.onNode(hasSetTextAction()).performImeAction()
    assertEquals(true, searchTriggered)
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :composeApp:testDebugUnitTest \
    --tests "com.opclient.ui.components.SearchInputTest.searchInput_callsOnSearch_whenImeActionPerformed"
```

Expected: FAIL — compilation error, `SearchInput` has no `onSearch` parameter.

- [ ] **Step 3: Add onSearch param to SearchInput**

Replace the full content of `SearchInput.kt`:

```kotlin
package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit = {},
    placeholder: String = "FIND BOOKS, AUTHORS…",
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    val shape = RoundedCornerShape(AppShapes.radius)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = typography.body.copy(color = colors.textPrimary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = colors.surface, shape = shape)
                    .border(width = 1.dp, color = colors.border, shape = shape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        BasicText(
                            text = placeholder,
                            style = typography.body.copy(color = colors.textSecondary),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}
```

- [ ] **Step 4: Run all SearchInput tests**

```bash
./gradlew :composeApp:testDebugUnitTest --tests "com.opclient.ui.components.SearchInputTest"
```

Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/ui/components/SearchInput.kt \
        composeApp/src/androidUnitTest/kotlin/com/opclient/ui/components/SearchInputTest.kt
git commit -m "feat(ui): add onSearch callback to SearchInput"
```

---

### Task 3: Create DTOs and domain models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/search/domain/SearchModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/search/domain/SearchRepository.kt`

Pure data definitions — no TDD step needed.

- [ ] **Step 1: Create DTOs**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchDto.kt
package com.opclient.search.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    val numFound: Int,
    val start: Int,
    val docs: List<SearchDocDto>,
)

@Serializable
data class SearchDocDto(
    val key: String,
    val title: String? = null,
    @SerialName("author_name") val authorName: List<String>? = null,
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("cover_i") val coverId: Int? = null,
    val subject: List<String>? = null,
)
```

- [ ] **Step 2: Create domain models**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/search/domain/SearchModels.kt
package com.opclient.search.domain

data class Book(
    val key: String,
    val title: String,
    val author: String,
    val firstPublishYear: Int?,
    val coverUrl: String?,
    val primarySubject: String?,
)

data class SearchResults(
    val query: String,
    val totalFound: Int,
    val books: List<Book>,
    val offset: Int,
)
```

- [ ] **Step 3: Create SearchRepository interface**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/search/domain/SearchRepository.kt
package com.opclient.search.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface SearchRepository {
    suspend fun search(query: String, offset: Int = 0, limit: Int = 20): Result<SearchResults, ApiError>
}
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/search/
git commit -m "feat(search): add DTOs, domain models, and SearchRepository interface"
```

---

### Task 4: SearchMapper TDD

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchMapper.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/search/SearchMapperTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/search/SearchMapperTest.kt
package com.opclient.search

import com.opclient.search.data.SearchDocDto
import com.opclient.search.data.SearchResponseDto
import com.opclient.search.data.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchMapperTest {

    @Test
    fun docDto_mapsTitle() {
        val dto = SearchDocDto(key = "/works/OL1W", title = "Dune")
        assertEquals("Dune", dto.toDomain().title)
    }

    @Test
    fun docDto_nullTitle_usesUnknownTitle() {
        val dto = SearchDocDto(key = "/works/OL1W", title = null)
        assertEquals("Unknown Title", dto.toDomain().title)
    }

    @Test
    fun docDto_mapsFirstAuthor() {
        val dto = SearchDocDto(key = "/works/OL1W", authorName = listOf("Frank Herbert", "Other"))
        assertEquals("Frank Herbert", dto.toDomain().author)
    }

    @Test
    fun docDto_nullAuthorName_usesUnknown() {
        val dto = SearchDocDto(key = "/works/OL1W", authorName = null)
        assertEquals("Unknown", dto.toDomain().author)
    }

    @Test
    fun docDto_emptyAuthorList_usesUnknown() {
        val dto = SearchDocDto(key = "/works/OL1W", authorName = emptyList())
        assertEquals("Unknown", dto.toDomain().author)
    }

    @Test
    fun docDto_coverId_buildsCoverUrl() {
        val dto = SearchDocDto(key = "/works/OL1W", coverId = 12345)
        assertEquals("https://covers.openlibrary.org/b/id/12345-M.jpg", dto.toDomain().coverUrl)
    }

    @Test
    fun docDto_nullCoverId_nullCoverUrl() {
        val dto = SearchDocDto(key = "/works/OL1W", coverId = null)
        assertNull(dto.toDomain().coverUrl)
    }

    @Test
    fun docDto_mapsFirstSubject() {
        val dto = SearchDocDto(key = "/works/OL1W", subject = listOf("Science fiction", "Adventure"))
        assertEquals("Science fiction", dto.toDomain().primarySubject)
    }

    @Test
    fun docDto_nullSubject_nullPrimarySubject() {
        val dto = SearchDocDto(key = "/works/OL1W", subject = null)
        assertNull(dto.toDomain().primarySubject)
    }

    @Test
    fun responseDto_mapsNumFoundAndStart() {
        val dto = SearchResponseDto(numFound = 500, start = 20, docs = emptyList())
        val result = dto.toDomain("dune")
        assertEquals(500, result.totalFound)
        assertEquals(20, result.offset)
        assertEquals("dune", result.query)
    }

    @Test
    fun responseDto_mapsAllDocs() {
        val dto = SearchResponseDto(
            numFound = 2,
            start = 0,
            docs = listOf(
                SearchDocDto(key = "/works/OL1W", title = "Dune"),
                SearchDocDto(key = "/works/OL2W", title = "Foundation"),
            ),
        )
        assertEquals(2, dto.toDomain("sci-fi").books.size)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.search.SearchMapperTest"
```

Expected: FAIL — compilation error: `toDomain` not defined.

- [ ] **Step 3: Implement mapper**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchMapper.kt
package com.opclient.search.data

import com.opclient.search.domain.Book
import com.opclient.search.domain.SearchResults

private const val COVER_BASE_URL = "https://covers.openlibrary.org/b/id"

internal fun SearchDocDto.toDomain(): Book =
    Book(
        key = key,
        title = title ?: "Unknown Title",
        author = authorName?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Unknown",
        firstPublishYear = firstPublishYear,
        coverUrl = coverId?.let { "$COVER_BASE_URL/$it-M.jpg" },
        primarySubject = subject?.firstOrNull(),
    )

internal fun SearchResponseDto.toDomain(query: String): SearchResults =
    SearchResults(
        query = query,
        totalFound = numFound,
        books = docs.map { it.toDomain() },
        offset = start,
    )
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.search.SearchMapperTest"
```

Expected: 11 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchMapper.kt \
        composeApp/src/commonTest/kotlin/com/opclient/search/SearchMapperTest.kt
git commit -m "feat(search): add SearchMapper with TDD"
```

---

### Task 5: SearchCache TDD

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchCache.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/search/SearchCacheTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/search/SearchCacheTest.kt
package com.opclient.search

import com.opclient.search.data.SearchCache
import com.opclient.search.domain.SearchResults
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchCacheTest {

    private fun results(query: String = "dune") =
        SearchResults(query = query, totalFound = 1, books = emptyList(), offset = 0)

    @Test
    fun get_onMiss_returnsNull() = runTest {
        val cache = SearchCache()
        assertNull(cache.get("dune:0"))
    }

    @Test
    fun get_afterPut_returnsEntry() = runTest {
        val cache = SearchCache()
        val data = results()
        cache.put("dune:0", data)
        assertEquals(data, cache.get("dune:0"))
    }

    @Test
    fun get_expiredEntry_returnsNull() = runTest {
        val cache = SearchCache(ttlMs = 1L)
        cache.put("dune:0", results())
        delay(10)
        assertNull(cache.get("dune:0"))
    }

    @Test
    fun get_differentKeys_independent() = runTest {
        val cache = SearchCache()
        val r1 = results("dune")
        val r2 = results("foundation")
        cache.put("dune:0", r1)
        cache.put("foundation:0", r2)
        assertEquals(r1, cache.get("dune:0"))
        assertEquals(r2, cache.get("foundation:0"))
    }

    @Test
    fun put_overwritesExistingEntry() = runTest {
        val cache = SearchCache()
        cache.put("dune:0", results("old"))
        val updated = results("updated")
        cache.put("dune:0", updated)
        assertEquals(updated, cache.get("dune:0"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.search.SearchCacheTest"
```

Expected: FAIL — compilation error: `SearchCache` not defined.

- [ ] **Step 3: Implement SearchCache**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchCache.kt
package com.opclient.search.data

import com.opclient.search.domain.SearchResults
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

private const val DEFAULT_TTL_MS = 5 * 60 * 1_000L

class SearchCache(private val ttlMs: Long = DEFAULT_TTL_MS) {
    private val mutex = Mutex()
    private val store = mutableMapOf<String, Pair<SearchResults, Long>>()

    suspend fun get(key: String): SearchResults? = mutex.withLock {
        store[key]?.takeIf { (_, ts) -> now() - ts < ttlMs }?.first
    }

    suspend fun put(key: String, results: SearchResults): Unit = mutex.withLock {
        store[key] = results to now()
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.search.SearchCacheTest"
```

Expected: 5 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchCache.kt \
        composeApp/src/commonTest/kotlin/com/opclient/search/SearchCacheTest.kt
git commit -m "feat(search): add SearchCache with TTL and TDD"
```

---

### Task 6: SearchApiClient

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchApiClient.kt`

No dedicated unit test — covered via MockEngine in Task 7's repository tests.

- [ ] **Step 1: Implement SearchApiClient**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchApiClient.kt
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
            queryParams = mapOf("q" to query, "offset" to "$offset", "limit" to "$limit"),
        )
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :composeApp:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchApiClient.kt
git commit -m "feat(search): add SearchApiClient"
```

---

### Task 7: SearchRepositoryImpl TDD

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchRepositoryImpl.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/search/SearchRepositoryTest.kt`

- [ ] **Step 1: Write failing tests**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/search/SearchRepositoryTest.kt
package com.opclient.search

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import com.opclient.search.data.SearchApiClient
import com.opclient.search.data.SearchCache
import com.opclient.search.data.SearchRepositoryImpl
import com.opclient.search.domain.SearchResults
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SearchRepositoryTest {

    private val successJson = """
        {
          "numFound": 100,
          "start": 0,
          "docs": [
            {
              "key": "/works/OL82563W",
              "title": "Dune",
              "author_name": ["Frank Herbert"],
              "first_publish_year": 1965,
              "cover_i": 8368541
            }
          ]
        }
    """.trimIndent()

    private fun makeRepo(
        engine: MockEngine,
        cache: SearchCache = SearchCache(),
    ): SearchRepositoryImpl {
        val apiClient = SearchApiClient(buildHttpClient(engine))
        return SearchRepositoryImpl(apiClient = apiClient, cache = cache)
    }

    @Test
    fun search_cacheMiss_callsApiAndReturnsResults() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = successJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = makeRepo(engine).search("dune")

        assertIs<Result.Success<SearchResults>>(result)
        assertEquals(100, result.value.totalFound)
        assertEquals(1, result.value.books.size)
        assertEquals("Dune", result.value.books[0].title)
        assertEquals("Frank Herbert", result.value.books[0].author)
        assertEquals(1, callCount)
    }

    @Test
    fun search_cacheHit_skipsApi() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = successJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val cache = SearchCache()
        val repo = makeRepo(engine, cache)

        repo.search("dune")  // miss — populates cache
        repo.search("dune")  // hit — must not call API

        assertEquals(1, callCount)
    }

    @Test
    fun search_differentOffsets_cachedIndependently() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(
                content = successJson,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val repo = makeRepo(engine)

        repo.search("dune", offset = 0)
        repo.search("dune", offset = 20)

        assertEquals(2, callCount)
    }

    @Test
    fun search_apiError_propagatesFailure() = runTest {
        val engine = MockEngine {
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }

        val result = makeRepo(engine).search("dune")

        assertIs<Result.Failure<ApiError>>(result)
        assertIs<ApiError.HttpError>(result.error)
        assertEquals(500, (result.error as ApiError.HttpError).code)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.search.SearchRepositoryTest"
```

Expected: FAIL — compilation error: `SearchRepositoryImpl` not defined.

- [ ] **Step 3: Implement SearchRepositoryImpl**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchRepositoryImpl.kt
package com.opclient.search.data

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.search.domain.SearchRepository
import com.opclient.search.domain.SearchResults

class SearchRepositoryImpl(
    private val apiClient: SearchApiClient,
    private val cache: SearchCache,
) : SearchRepository {

    override suspend fun search(
        query: String,
        offset: Int,
        limit: Int,
    ): Result<SearchResults, ApiError> {
        val key = "$query:$offset"
        val cached = cache.get(key)
        if (cached != null) return Result.Success(cached)

        return when (val result = apiClient.search(query, offset, limit)) {
            is Result.Success -> {
                val domain = result.value.toDomain(query)
                cache.put(key, domain)
                Result.Success(domain)
            }
            is Result.Failure -> result
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.search.SearchRepositoryTest"
```

Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/search/data/SearchRepositoryImpl.kt \
        composeApp/src/commonTest/kotlin/com/opclient/search/SearchRepositoryTest.kt
git commit -m "feat(search): add SearchRepositoryImpl with cache-first strategy and TDD"
```

---

### Task 8: SearchModule + Koin wiring

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/di/SearchModule.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`

- [ ] **Step 1: Create SearchModule**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/di/SearchModule.kt
package com.opclient.di

import com.opclient.search.data.SearchApiClient
import com.opclient.search.data.SearchCache
import com.opclient.search.data.SearchRepositoryImpl
import com.opclient.search.domain.SearchRepository
import com.opclient.search.presentation.SearchViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchModule = module {
    single { SearchCache() }
    factory { SearchApiClient(get()) }
    single<SearchRepository> { SearchRepositoryImpl(apiClient = get(), cache = get()) }
    viewModel { SearchViewModel(get()) }
}
```

- [ ] **Step 2: Register searchModule in OpClientApplication.kt**

Change the `modules(...)` call to:

```kotlin
modules(commonModule, androidModule, searchModule)
```

Add import at top of file:

```kotlin
import com.opclient.di.searchModule
```

- [ ] **Step 3: Register searchModule in Main.kt**

Change the `modules(...)` call to:

```kotlin
modules(commonModule, desktopModule, searchModule)
```

Add import at top of file:

```kotlin
import com.opclient.di.searchModule
```

- [ ] **Step 4: Verify compilation on both targets**

```bash
./gradlew :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid
```

Expected: `BUILD SUCCESSFUL` on both targets.

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/di/SearchModule.kt \
        composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt \
        composeApp/src/jvmMain/kotlin/com/opclient/Main.kt
git commit -m "feat(search): wire SearchModule into Koin on both platforms"
```

---

### Task 9: SearchViewModel TDD

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/search/SearchViewModelTest.kt`

> **Note on viewModelScope:** `SearchViewModel` extends `androidx.lifecycle.ViewModel` which is available in `commonMain` via `koin-compose-viewmodel`'s transitive lifecycle dependency. `viewModelScope` is an extension property on `ViewModel` from the same library. If the compiler cannot resolve `viewModelScope`, replace it with a manual scope:
> ```kotlin
> private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
> override fun onCleared() { super.onCleared(); scope.cancel() }
> ```
> and replace all `viewModelScope.launch` with `scope.launch`.

- [ ] **Step 1: Write failing tests**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/search/SearchViewModelTest.kt
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
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.search.SearchViewModelTest"
```

Expected: FAIL — compilation error: `SearchViewModel`, `SearchUiState`, `SearchStatus`, `SearchIntent`, `SearchEffect` not defined.

- [ ] **Step 3: Implement SearchViewModel**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchViewModel.kt
package com.opclient.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.search.domain.Book
import com.opclient.search.domain.SearchRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_LIMIT = 20

data class SearchUiState(
    val query: String = "",
    val books: List<Book> = emptyList(),
    val totalFound: Int = 0,
    val offset: Int = 0,
    val status: SearchStatus = SearchStatus.Idle,
    val canLoadMore: Boolean = false,
)

enum class SearchStatus { Idle, Loading, LoadingMore, Success, Empty, Error }

sealed class SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent()
    data object Search : SearchIntent()
    data object LoadMore : SearchIntent()
    data object ClearSearch : SearchIntent()
}

sealed class SearchEffect {
    data class SearchError(val error: ApiError) : SearchEffect()
}

class SearchViewModel(private val repository: SearchRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SearchEffect>(replay = 0)
    val effects: SharedFlow<SearchEffect> = _effects.asSharedFlow()

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> _uiState.update { it.copy(query = intent.query) }
            SearchIntent.Search -> search()
            SearchIntent.LoadMore -> loadMore()
            SearchIntent.ClearSearch -> _uiState.value = SearchUiState()
        }
    }

    private fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(status = SearchStatus.Loading) }
            when (val result = repository.search(query, offset = 0, limit = SEARCH_LIMIT)) {
                is Result.Success -> {
                    val data = result.value
                    _uiState.update {
                        it.copy(
                            books = data.books,
                            totalFound = data.totalFound,
                            offset = data.offset,
                            status = if (data.books.isEmpty()) SearchStatus.Empty else SearchStatus.Success,
                            canLoadMore = data.books.size < data.totalFound,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = SearchStatus.Error) }
                    _effects.emit(SearchEffect.SearchError(result.error))
                }
            }
        }
    }

    private fun loadMore() {
        val state = _uiState.value
        if (!state.canLoadMore || state.status == SearchStatus.LoadingMore) return
        val nextOffset = state.offset + SEARCH_LIMIT
        viewModelScope.launch {
            _uiState.update { it.copy(status = SearchStatus.LoadingMore) }
            when (val result = repository.search(state.query, offset = nextOffset, limit = SEARCH_LIMIT)) {
                is Result.Success -> {
                    val data = result.value
                    _uiState.update {
                        val newBooks = it.books + data.books
                        it.copy(
                            books = newBooks,
                            totalFound = data.totalFound,
                            offset = nextOffset,
                            status = SearchStatus.Success,
                            canLoadMore = newBooks.size < data.totalFound,
                        )
                    }
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = SearchStatus.Success) }
                    _effects.emit(SearchEffect.SearchError(result.error))
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.search.SearchViewModelTest"
```

Expected: 10 tests PASS.

- [ ] **Step 5: Run all common tests to check for regressions**

```bash
./gradlew :composeApp:jvmTest
```

Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchViewModel.kt \
        composeApp/src/commonTest/kotlin/com/opclient/search/SearchViewModelTest.kt
git commit -m "feat(search): add SearchViewModel MVI with TDD"
```

---

### Task 10: SearchScreen composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchScreen.kt`

No unit test — UI composable verified via live integration in Task 12.

- [ ] **Step 1: Implement SearchScreen**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchScreen.kt
package com.opclient.search.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opclient.core.ApiError
import com.opclient.ui.components.BookRow
import com.opclient.ui.components.PrimaryButton
import com.opclient.ui.components.SearchInput
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.feedback.EmptyState
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchScreen(
    onBookClick: (String) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val colors = AppThemeTokens.colors

    LaunchedEffect(viewModel.effects) {
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
            onSearch = { viewModel.onIntent(SearchIntent.Search) },
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )

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
                    items(uiState.books) { book ->
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
                                        placeholder = ColorPainter(colors.surface2),
                                        error = ColorPainter(colors.surface2),
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                            },
                            onClick = { onBookClick(book.key) },
                        )
                    }
                    if (uiState.canLoadMore) {
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
}
```

- [ ] **Step 2: Verify compilation on both targets**

```bash
./gradlew :composeApp:compileKotlinJvm :composeApp:compileDebugKotlinAndroid
```

Expected: `BUILD SUCCESSFUL` on both targets.

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/search/presentation/SearchScreen.kt
git commit -m "feat(search): add SearchScreen composable"
```

---

### Task 11: Wire App.kt

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/App.kt`

- [ ] **Step 1: Update App.kt**

Replace the full content of `App.kt`:

```kotlin
package com.opclient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.opclient.search.presentation.SearchScreen
import com.opclient.ui.AppShell
import com.opclient.ui.navigation.Destination

@Composable
fun App() {
    var selected by remember { mutableStateOf(Destination.SEARCH) }
    AppShell(
        selectedDestination = selected,
        onDestinationChange = { selected = it },
    ) {
        when (selected) {
            Destination.SEARCH -> SearchScreen(onBookClick = {})
            else -> Box(modifier = Modifier.fillMaxSize())
        }
    }
}
```

- [ ] **Step 2: Full build verification**

```bash
./gradlew :composeApp:assembleDebug :composeApp:jvmJar
```

Expected: `BUILD SUCCESSFUL`. Both targets compile and package.

- [ ] **Step 3: Run all tests**

```bash
./gradlew :composeApp:allTests
```

Expected: All tests PASS. No regressions.

- [ ] **Step 4: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/App.kt
git commit -m "feat(search): wire SearchScreen into App via AppShell"
```

---

### Task 12: Live API verification

No code changes. Manually verify the running app against the live OpenLibrary API.

- [ ] **Step 1: Run the Desktop app**

```bash
./gradlew :composeApp:run
```

- [ ] **Step 2: Verify the search flow**

1. App opens — Search screen visible, "RECENT" label shown
2. Type "dune" in the search input and press Enter
3. Loading spinner appears briefly, then book list populates
4. Verify first result shows "Dune" as title and "Frank Herbert" as author
5. Verify cover image renders (or `surface2` placeholder if no cover)
6. Verify "RESULTS · N BOOKS" section label shows non-zero count
7. If more than 20 results: scroll to bottom, "LOAD MORE" button visible
8. Click "LOAD MORE" — additional results append below existing list
9. Search for "xzxzxzxzxz" — Empty state appears with "No books found" message
10. Clear the field and press Enter — nothing happens (blank query guard)
11. Navigate to Browse/Library tabs — placeholder empty screen, no crash
12. Navigate back to Search — state preserved

- [ ] **Step 3: Run Android if emulator is available**

```bash
./gradlew :composeApp:installDebug
```

Repeat the same verification steps on the Android emulator.

- [ ] **Step 4: Commit any fixes found during verification**

If bugs surface, fix them and commit:

```bash
git add -p
git commit -m "fix(search): <describe what was wrong>"
```

Skip this step if no fixes are needed.
