# Week 3 — Books & Authors Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Navigation stack + Book Detail + Author Detail screens wired end-to-end: tap search result → full book detail → tap author → author profile and bibliography.

**Architecture:** Simple state-based navigation (`List<Screen>`) in `NavigationViewModel`. Data layers follow SearchModule pattern (cache-first with Mutex + TTL, injectable timeSource). `BookRepositoryImpl` delegates author name resolution to `AuthorRepository` to share `AuthorCache`. MVI ViewModels with `DetailStatus { Loading, Success, Error }`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform 1.7.1, Koin 4.0.0, Ktor 3.0.3 + MockEngine, kotlinx-serialization, Turbine 1.1.0, kotlin.test.

---

### Task 1: Navigation — Screen sealed class + NavigationViewModel + tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/navigation/NavigationViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/navigation/NavigationViewModelTest.kt
package com.opclient.navigation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    fun navigateToTab_resetsStack() = runTest {
        val vm = NavigationViewModel()
        vm.navigateTo(Screen.BookDetail("/works/OL1W"))
        vm.navigateTo(Screen.AuthorDetail("/authors/OL1A"))
        vm.navigateToTab()
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

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.navigation.*" 2>&1 | tail -20`
Expected: FAIL with "unresolved reference: Screen" or similar compile error

- [ ] **Step 3: Create Screen.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt
package com.opclient.navigation

sealed class Screen {
    data object Search : Screen()
    data class BookDetail(val workKey: String) : Screen()
    data class AuthorDetail(val authorKey: String) : Screen()
}
```

- [ ] **Step 4: Create NavigationViewModel.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt
package com.opclient.navigation

import androidx.lifecycle.ViewModel
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

    fun navigateToTab() {
        _stack.value = listOf(Screen.Search)
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.navigation.*" 2>&1 | tail -20`
Expected: 6 tests pass

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt \
        composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt \
        composeApp/src/commonTest/kotlin/com/opclient/navigation/NavigationViewModelTest.kt
git commit -m "feat(navigation): add Screen sealed class and NavigationViewModel with stack-based nav"
```

---

### Task 2: DTOs + Domain Models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/book/data/BookDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/book/domain/BookModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/author/domain/AuthorModels.kt`

No tests for pure data class definitions — tested via mapper tests.

- [ ] **Step 1: Create BookDto.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/book/data/BookDto.kt
package com.opclient.book.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WorkDto(
    val key: String,
    val title: String? = null,
    val description: JsonElement? = null,
    @SerialName("first_publish_date") val firstPublishDate: String? = null,
    val subjects: List<String>? = null,
    val covers: List<Int>? = null,
    val authors: List<WorkAuthorEntryDto>? = null,
)

@Serializable
data class WorkAuthorEntryDto(val author: WorkAuthorKeyDto)

@Serializable
data class WorkAuthorKeyDto(val key: String)
```

- [ ] **Step 2: Create BookModels.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/book/domain/BookModels.kt
package com.opclient.book.domain

data class BookDetail(
    val key: String,
    val title: String,
    val description: String?,
    val authors: List<AuthorRef>,
    val subjects: List<String>,
    val firstPublishDate: String?,
    val coverUrl: String?,
)

data class AuthorRef(val key: String, val name: String)
```

- [ ] **Step 3: Create AuthorDto.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorDto.kt
package com.opclient.author.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AuthorDto(
    val key: String,
    val name: String? = null,
    val bio: JsonElement? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("death_date") val deathDate: String? = null,
    val photos: List<Int>? = null,
)

@Serializable
data class AuthorWorksDto(
    val entries: List<AuthorWorkEntryDto>? = null,
    val size: Int = 0,
)

@Serializable
data class AuthorWorkEntryDto(
    val key: String,
    val title: String? = null,
    val covers: List<Int>? = null,
)
```

- [ ] **Step 4: Create AuthorModels.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/author/domain/AuthorModels.kt
package com.opclient.author.domain

data class AuthorDetail(
    val key: String,
    val name: String,
    val bio: String?,
    val birthDate: String?,
    val deathDate: String?,
    val photoUrl: String?,
    val works: List<AuthorWork>,
)

data class AuthorWork(
    val key: String,
    val title: String,
    val coverUrl: String?,
)
```

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/book/data/BookDto.kt \
        composeApp/src/commonMain/kotlin/com/opclient/book/domain/BookModels.kt \
        composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorDto.kt \
        composeApp/src/commonMain/kotlin/com/opclient/author/domain/AuthorModels.kt
git commit -m "feat(book,author): add DTOs and domain models"
```

---

### Task 3: BookMapper + BookMapperTest

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/book/data/BookMapper.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/book/BookMapperTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/book/BookMapperTest.kt
package com.opclient.book

import com.opclient.book.data.WorkAuthorEntryDto
import com.opclient.book.data.WorkAuthorKeyDto
import com.opclient.book.data.WorkDto
import com.opclient.book.data.toText
import com.opclient.book.data.toDomain
import com.opclient.book.data.toBookCoverUrl
import com.opclient.book.domain.AuthorRef
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookMapperTest {

    @Test
    fun toText_null_returnsNull() {
        assertNull(null.toText())
    }

    @Test
    fun toText_jsonNull_returnsNull() {
        assertNull(JsonNull.toText())
    }

    @Test
    fun toText_string_returnsContent() {
        assertEquals("Some description", JsonPrimitive("Some description").toText())
    }

    @Test
    fun toText_object_returnsValue() {
        val obj = JsonObject(mapOf(
            "type" to JsonPrimitive("/type/text"),
            "value" to JsonPrimitive("Rich text"),
        ))
        assertEquals("Rich text", obj.toText())
    }

    @Test
    fun toText_objectMissingValue_returnsNull() {
        val obj = JsonObject(mapOf("type" to JsonPrimitive("/type/text")))
        assertNull(obj.toText())
    }

    @Test
    fun toBookCoverUrl_null_returnsNull() {
        assertNull(null.toBookCoverUrl())
    }

    @Test
    fun toBookCoverUrl_emptyList_returnsNull() {
        assertNull(emptyList<Int>().toBookCoverUrl())
    }

    @Test
    fun toBookCoverUrl_returnsLargeCover() {
        assertEquals(
            "https://covers.openlibrary.org/b/id/12345-L.jpg",
            listOf(12345).toBookCoverUrl(size = "L"),
        )
    }

    @Test
    fun toBookCoverUrl_defaultSize_isM() {
        assertEquals(
            "https://covers.openlibrary.org/b/id/99-M.jpg",
            listOf(99).toBookCoverUrl(),
        )
    }

    @Test
    fun toDomain_mapsAllFields() {
        val dto = WorkDto(
            key = "/works/OL82563W",
            title = "Dune",
            description = JsonPrimitive("A great book"),
            firstPublishDate = "1965",
            subjects = listOf("Science Fiction", "Adventure"),
            covers = listOf(12345),
            authors = listOf(
                WorkAuthorEntryDto(author = WorkAuthorKeyDto(key = "/authors/OL222A")),
            ),
        )
        val authorRefs = listOf(AuthorRef(key = "/authors/OL222A", name = "Frank Herbert"))

        val domain = dto.toDomain(authorRefs)

        assertEquals("/works/OL82563W", domain.key)
        assertEquals("Dune", domain.title)
        assertEquals("A great book", domain.description)
        assertEquals("1965", domain.firstPublishDate)
        assertEquals(listOf("Science Fiction", "Adventure"), domain.subjects)
        assertEquals("https://covers.openlibrary.org/b/id/12345-L.jpg", domain.coverUrl)
        assertEquals(authorRefs, domain.authors)
    }

    @Test
    fun toDomain_nullTitle_fallsBackToUnknown() {
        val dto = WorkDto(key = "/works/OL1W", title = null)
        val domain = dto.toDomain(emptyList())
        assertEquals("Unknown Title", domain.title)
    }

    @Test
    fun toDomain_noCovers_coverUrlIsNull() {
        val dto = WorkDto(key = "/works/OL1W", covers = null)
        val domain = dto.toDomain(emptyList())
        assertNull(domain.coverUrl)
    }

    @Test
    fun extractOlid_fromKeyPath() {
        assertEquals("OL82563W", "/works/OL82563W".substringAfterLast("/"))
        assertEquals("OL26320A", "/authors/OL26320A".substringAfterLast("/"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.book.BookMapperTest" 2>&1 | tail -20`
Expected: FAIL with unresolved reference errors

- [ ] **Step 3: Create BookMapper.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/book/data/BookMapper.kt
package com.opclient.book.data

import com.opclient.book.domain.AuthorRef
import com.opclient.book.domain.BookDetail
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

private const val COVER_BASE = "https://covers.openlibrary.org/b/id"

internal fun JsonElement?.toText(): String? = when {
    this == null || this is JsonNull -> null
    this is JsonPrimitive && isString -> contentOrNull
    this is JsonObject -> this["value"]?.jsonPrimitive?.contentOrNull
    else -> null
}

internal fun List<Int>?.toBookCoverUrl(size: String = "M"): String? =
    this?.firstOrNull()?.let { "$COVER_BASE/$it-$size.jpg" }

internal fun WorkDto.toDomain(authorRefs: List<AuthorRef>): BookDetail =
    BookDetail(
        key = key,
        title = title ?: "Unknown Title",
        description = description.toText(),
        authors = authorRefs,
        subjects = subjects ?: emptyList(),
        firstPublishDate = firstPublishDate,
        coverUrl = covers.toBookCoverUrl(size = "L"),
    )
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.book.BookMapperTest" 2>&1 | tail -20`
Expected: 12 tests pass

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/book/data/BookMapper.kt \
        composeApp/src/commonTest/kotlin/com/opclient/book/BookMapperTest.kt
git commit -m "feat(book): add BookMapper with toText, cover URL helpers, and WorkDto.toDomain"
```

---

### Task 4: AuthorMapper + AuthorMapperTest

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorMapper.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/author/AuthorMapperTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/author/AuthorMapperTest.kt
package com.opclient.author

import com.opclient.author.data.AuthorDto
import com.opclient.author.data.AuthorWorkEntryDto
import com.opclient.author.data.AuthorWorksDto
import com.opclient.author.data.toAuthorPhotoUrl
import com.opclient.author.data.toDomain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthorMapperTest {

    @Test
    fun toAuthorPhotoUrl_null_returnsNull() {
        assertNull(null.toAuthorPhotoUrl())
    }

    @Test
    fun toAuthorPhotoUrl_emptyList_returnsNull() {
        assertNull(emptyList<Int>().toAuthorPhotoUrl())
    }

    @Test
    fun toAuthorPhotoUrl_returnsMediumUrl() {
        assertEquals(
            "https://covers.openlibrary.org/a/id/7777-M.jpg",
            listOf(7777).toAuthorPhotoUrl(),
        )
    }

    @Test
    fun toDomain_mapsAllFields() {
        val dto = AuthorDto(
            key = "/authors/OL26320A",
            name = "Frank Herbert",
            bio = JsonPrimitive("American author"),
            birthDate = "1920",
            deathDate = "1986",
            photos = listOf(7777),
        )
        val works = listOf(
            AuthorWorkEntryDto(key = "/works/OL82563W", title = "Dune", covers = listOf(12345)),
        )

        val domain = dto.toDomain(works)

        assertEquals("/authors/OL26320A", domain.key)
        assertEquals("Frank Herbert", domain.name)
        assertEquals("American author", domain.bio)
        assertEquals("1920", domain.birthDate)
        assertEquals("1986", domain.deathDate)
        assertEquals("https://covers.openlibrary.org/a/id/7777-M.jpg", domain.photoUrl)
        assertEquals(1, domain.works.size)
        assertEquals("/works/OL82563W", domain.works[0].key)
        assertEquals("Dune", domain.works[0].title)
        assertEquals("https://covers.openlibrary.org/b/id/12345-M.jpg", domain.works[0].coverUrl)
    }

    @Test
    fun toDomain_nullName_fallsBackToUnknown() {
        val dto = AuthorDto(key = "/authors/OL1A", name = null)
        assertEquals("Unknown Author", dto.toDomain(emptyList()).name)
    }

    @Test
    fun toDomain_bioAsObject_extractsValue() {
        val dto = AuthorDto(
            key = "/authors/OL1A",
            bio = JsonObject(mapOf(
                "type" to JsonPrimitive("/type/text"),
                "value" to JsonPrimitive("Object bio"),
            )),
        )
        assertEquals("Object bio", dto.toDomain(emptyList()).bio)
    }

    @Test
    fun toDomain_noPhotos_photoUrlIsNull() {
        val dto = AuthorDto(key = "/authors/OL1A", photos = null)
        assertNull(dto.toDomain(emptyList()).photoUrl)
    }

    @Test
    fun toDomain_workWithNoCovers_coverUrlIsNull() {
        val dto = AuthorDto(key = "/authors/OL1A")
        val works = listOf(AuthorWorkEntryDto(key = "/works/OL1W", title = "Book", covers = null))
        assertNull(dto.toDomain(works).works[0].coverUrl)
    }

    @Test
    fun toDomain_workWithNullTitle_fallsBackToUnknown() {
        val dto = AuthorDto(key = "/authors/OL1A")
        val works = listOf(AuthorWorkEntryDto(key = "/works/OL1W", title = null))
        assertEquals("Unknown Title", dto.toDomain(works).works[0].title)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.author.AuthorMapperTest" 2>&1 | tail -20`
Expected: FAIL with unresolved reference errors

- [ ] **Step 3: Create AuthorMapper.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorMapper.kt
package com.opclient.author.data

import com.opclient.author.domain.AuthorDetail
import com.opclient.author.domain.AuthorWork
import com.opclient.book.data.toText
import com.opclient.book.data.toBookCoverUrl
import kotlinx.serialization.json.JsonElement

private const val AUTHOR_COVER_BASE = "https://covers.openlibrary.org/a/id"

internal fun List<Int>?.toAuthorPhotoUrl(): String? =
    this?.firstOrNull()?.let { "$AUTHOR_COVER_BASE/$it-M.jpg" }

internal fun AuthorDto.toDomain(works: List<AuthorWorkEntryDto>): AuthorDetail =
    AuthorDetail(
        key = key,
        name = name ?: "Unknown Author",
        bio = bio.toText(),
        birthDate = birthDate,
        deathDate = deathDate,
        photoUrl = photos.toAuthorPhotoUrl(),
        works = works.map { entry ->
            AuthorWork(
                key = entry.key,
                title = entry.title ?: "Unknown Title",
                coverUrl = entry.covers.toBookCoverUrl(size = "M"),
            )
        },
    )
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.author.AuthorMapperTest" 2>&1 | tail -20`
Expected: 9 tests pass

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorMapper.kt \
        composeApp/src/commonTest/kotlin/com/opclient/author/AuthorMapperTest.kt
git commit -m "feat(author): add AuthorMapper with photo URL helper and AuthorDto.toDomain"
```

---

### Task 5: BookCache + AuthorCache + tests

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/book/data/BookCache.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorCache.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/book/BookCacheTest.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/author/AuthorCacheTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/book/BookCacheTest.kt
package com.opclient.book

import com.opclient.book.data.BookCache
import com.opclient.book.domain.BookDetail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookCacheTest {

    private fun detail(key: String = "/works/OL1W") = BookDetail(
        key = key, title = "T", description = null, authors = emptyList(),
        subjects = emptyList(), firstPublishDate = null, coverUrl = null,
    )

    @Test
    fun get_onMiss_returnsNull() = runTest {
        assertNull(BookCache().get("work:OL1W"))
    }

    @Test
    fun get_afterPut_returnsEntry() = runTest {
        val cache = BookCache()
        val d = detail()
        cache.put("work:OL1W", d)
        assertEquals(d, cache.get("work:OL1W"))
    }

    @Test
    fun get_expiredEntry_returnsNull() = runTest {
        var fakeTime = 0L
        val cache = BookCache(ttlMs = 100L, timeSource = { fakeTime })
        cache.put("work:OL1W", detail())
        fakeTime = 200L
        assertNull(cache.get("work:OL1W"))
    }

    @Test
    fun put_overwritesExistingEntry() = runTest {
        val cache = BookCache()
        cache.put("work:OL1W", detail("old"))
        val updated = detail("new")
        cache.put("work:OL1W", updated)
        assertEquals(updated, cache.get("work:OL1W"))
    }
}
```

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/author/AuthorCacheTest.kt
package com.opclient.author

import com.opclient.author.data.AuthorCache
import com.opclient.author.domain.AuthorDetail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthorCacheTest {

    private fun detail(key: String = "/authors/OL1A") = AuthorDetail(
        key = key, name = "N", bio = null, birthDate = null,
        deathDate = null, photoUrl = null, works = emptyList(),
    )

    @Test
    fun get_onMiss_returnsNull() = runTest {
        assertNull(AuthorCache().get("author:OL1A"))
    }

    @Test
    fun get_afterPut_returnsEntry() = runTest {
        val cache = AuthorCache()
        val d = detail()
        cache.put("author:OL1A", d)
        assertEquals(d, cache.get("author:OL1A"))
    }

    @Test
    fun get_expiredEntry_returnsNull() = runTest {
        var fakeTime = 0L
        val cache = AuthorCache(ttlMs = 100L, timeSource = { fakeTime })
        cache.put("author:OL1A", detail())
        fakeTime = 200L
        assertNull(cache.get("author:OL1A"))
    }

    @Test
    fun put_overwritesExistingEntry() = runTest {
        val cache = AuthorCache()
        cache.put("author:OL1A", detail("old"))
        val updated = detail("new")
        cache.put("author:OL1A", updated)
        assertEquals(updated, cache.get("author:OL1A"))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.book.BookCacheTest" --tests "com.opclient.author.AuthorCacheTest" 2>&1 | tail -20`
Expected: FAIL with unresolved references

- [ ] **Step 3: Create BookCache.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/book/data/BookCache.kt
package com.opclient.book.data

import com.opclient.book.domain.BookDetail
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

private const val DEFAULT_TTL_MS = 30 * 60 * 1_000L

class BookCache(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val timeSource: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()
    private val store = mutableMapOf<String, Pair<BookDetail, Long>>()

    suspend fun get(key: String): BookDetail? = mutex.withLock {
        store[key]?.takeIf { (_, ts) -> now() - ts < ttlMs }?.first
    }

    suspend fun put(key: String, value: BookDetail): Unit = mutex.withLock {
        store[key] = value to now()
    }

    private fun now(): Long = timeSource()
}
```

- [ ] **Step 4: Create AuthorCache.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorCache.kt
package com.opclient.author.data

import com.opclient.author.domain.AuthorDetail
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

private const val DEFAULT_TTL_MS = 30 * 60 * 1_000L

class AuthorCache(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val timeSource: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()
    private val store = mutableMapOf<String, Pair<AuthorDetail, Long>>()

    suspend fun get(key: String): AuthorDetail? = mutex.withLock {
        store[key]?.takeIf { (_, ts) -> now() - ts < ttlMs }?.first
    }

    suspend fun put(key: String, value: AuthorDetail): Unit = mutex.withLock {
        store[key] = value to now()
    }

    private fun now(): Long = timeSource()
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.book.BookCacheTest" --tests "com.opclient.author.AuthorCacheTest" 2>&1 | tail -20`
Expected: 8 tests pass

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/book/data/BookCache.kt \
        composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorCache.kt \
        composeApp/src/commonTest/kotlin/com/opclient/book/BookCacheTest.kt \
        composeApp/src/commonTest/kotlin/com/opclient/author/AuthorCacheTest.kt
git commit -m "feat(book,author): add BookCache and AuthorCache with 30-min TTL and injectable timeSource"
```

---

### Task 6: BookApiClient + AuthorApiClient

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/book/data/BookApiClient.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorApiClient.kt`

No isolated API client tests — covered by repository tests with MockEngine.

- [ ] **Step 1: Create BookApiClient.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/book/data/BookApiClient.kt
package com.opclient.book.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class BookApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun getWork(workOlid: String): Result<WorkDto, ApiError> =
        get("/works/$workOlid.json")
}
```

- [ ] **Step 2: Create AuthorApiClient.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorApiClient.kt
package com.opclient.author.data

import com.opclient.core.ApiClient
import com.opclient.core.ApiError
import com.opclient.core.Result
import io.ktor.client.HttpClient

class AuthorApiClient(httpClient: HttpClient) :
    ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

    suspend fun getAuthor(authorOlid: String): Result<AuthorDto, ApiError> =
        get("/authors/$authorOlid.json")

    suspend fun getAuthorWorks(authorOlid: String): Result<AuthorWorksDto, ApiError> =
        get("/authors/$authorOlid/works.json")
}
```

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/book/data/BookApiClient.kt \
        composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorApiClient.kt
git commit -m "feat(book,author): add BookApiClient and AuthorApiClient"
```

---

### Task 7: AuthorRepository interface + AuthorRepositoryImpl + AuthorRepositoryTest

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/author/domain/AuthorRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorRepositoryImpl.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/author/AuthorRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/author/AuthorRepositoryTest.kt
package com.opclient.author

import com.opclient.author.data.AuthorApiClient
import com.opclient.author.data.AuthorCache
import com.opclient.author.data.AuthorRepositoryImpl
import com.opclient.author.domain.AuthorDetail
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AuthorRepositoryTest {

    private val profileJson = """
        {
          "key": "/authors/OL26320A",
          "name": "Frank Herbert",
          "bio": "American author",
          "birth_date": "1920",
          "death_date": "1986",
          "photos": [7777]
        }
    """.trimIndent()

    private val worksJson = """
        {
          "entries": [
            { "key": "/works/OL82563W", "title": "Dune", "covers": [12345] }
          ],
          "size": 1
        }
    """.trimIndent()

    private fun makeRepo(
        engine: MockEngine,
        cache: AuthorCache = AuthorCache(),
    ): AuthorRepositoryImpl {
        val apiClient = AuthorApiClient(buildHttpClient(engine))
        return AuthorRepositoryImpl(apiClient = apiClient, cache = cache)
    }

    @Test
    fun getAuthor_cacheMiss_fetchesProfileAndWorks() = runTest {
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            val body = if (request.url.encodedPath.endsWith("/works.json")) worksJson else profileJson
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val result = makeRepo(engine).getAuthor("/authors/OL26320A")

        assertIs<Result.Success<AuthorDetail>>(result)
        assertEquals("Frank Herbert", result.value.name)
        assertEquals("American author", result.value.bio)
        assertEquals(1, result.value.works.size)
        assertEquals("Dune", result.value.works[0].title)
        assertEquals(2, callCount)
    }

    @Test
    fun getAuthor_cacheHit_skipsApi() = runTest {
        var callCount = 0
        val engine = MockEngine { request ->
            callCount++
            val body = if (request.url.encodedPath.endsWith("/works.json")) worksJson else profileJson
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = AuthorCache()
        val repo = makeRepo(engine, cache)

        repo.getAuthor("/authors/OL26320A")
        repo.getAuthor("/authors/OL26320A")

        assertEquals(2, callCount)
    }

    @Test
    fun getAuthor_profileApiError_propagatesFailure() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/works.json")) {
                respond(worksJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                respond("error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
            }
        }

        val result = makeRepo(engine).getAuthor("/authors/OL26320A")

        assertIs<Result.Failure<ApiError>>(result)
    }

    @Test
    fun getAuthor_worksApiError_returnsAuthorWithEmptyWorks() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/works.json")) {
                respond("error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
            } else {
                respond(profileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }

        val result = makeRepo(engine).getAuthor("/authors/OL26320A")

        assertIs<Result.Success<AuthorDetail>>(result)
        assertEquals(emptyList(), result.value.works)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.author.AuthorRepositoryTest" 2>&1 | tail -20`
Expected: FAIL with unresolved reference errors

- [ ] **Step 3: Create AuthorRepository.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/author/domain/AuthorRepository.kt
package com.opclient.author.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface AuthorRepository {
    suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError>
}
```

- [ ] **Step 4: Create AuthorRepositoryImpl.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorRepositoryImpl.kt
package com.opclient.author.data

import com.opclient.author.domain.AuthorDetail
import com.opclient.author.domain.AuthorRepository
import com.opclient.core.ApiError
import com.opclient.core.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AuthorRepositoryImpl(
    private val apiClient: AuthorApiClient,
    private val cache: AuthorCache,
) : AuthorRepository {

    override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> {
        val olid = authorKey.substringAfterLast("/")
        val cacheKey = "author:$olid"

        cache.get(cacheKey)?.let { return Result.Success(it) }

        return coroutineScope {
            val profileDeferred = async { apiClient.getAuthor(olid) }
            val worksDeferred = async { apiClient.getAuthorWorks(olid) }

            val profileResult = profileDeferred.await()
            if (profileResult is Result.Failure) return@coroutineScope profileResult

            val worksEntries = (worksDeferred.await() as? Result.Success)
                ?.value?.entries ?: emptyList()

            val domain = (profileResult as Result.Success).value.toDomain(worksEntries)
            cache.put(cacheKey, domain)
            Result.Success(domain)
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.author.AuthorRepositoryTest" 2>&1 | tail -20`
Expected: 4 tests pass

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/author/domain/AuthorRepository.kt \
        composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorRepositoryImpl.kt \
        composeApp/src/commonTest/kotlin/com/opclient/author/AuthorRepositoryTest.kt
git commit -m "feat(author): add AuthorRepository interface and impl with parallel fetch and cache"
```

---

### Task 8: BookRepository interface + BookRepositoryImpl + BookRepositoryTest

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/book/domain/BookRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/book/data/BookRepositoryImpl.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/book/BookRepositoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/book/BookRepositoryTest.kt
package com.opclient.book

import com.opclient.author.domain.AuthorDetail
import com.opclient.author.domain.AuthorRepository
import com.opclient.book.data.BookApiClient
import com.opclient.book.data.BookCache
import com.opclient.book.data.BookRepositoryImpl
import com.opclient.book.domain.BookDetail
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.core.buildHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BookRepositoryTest {

    private val workJson = """
        {
          "key": "/works/OL82563W",
          "title": "Dune",
          "description": "A great novel",
          "first_publish_date": "1965",
          "subjects": ["Science Fiction"],
          "covers": [12345],
          "authors": [
            { "author": { "key": "/authors/OL26320A" } }
          ]
        }
    """.trimIndent()

    private fun fakeAuthorRepo(
        authorDetail: AuthorDetail = AuthorDetail(
            key = "/authors/OL26320A", name = "Frank Herbert", bio = null,
            birthDate = null, deathDate = null, photoUrl = null, works = emptyList(),
        ),
    ): AuthorRepository = object : AuthorRepository {
        override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> =
            Result.Success(authorDetail)
    }

    private fun makeRepo(
        engine: MockEngine,
        cache: BookCache = BookCache(),
        authorRepository: AuthorRepository = fakeAuthorRepo(),
    ): BookRepositoryImpl {
        val apiClient = BookApiClient(buildHttpClient(engine))
        return BookRepositoryImpl(apiClient = apiClient, cache = cache, authorRepository = authorRepository)
    }

    @Test
    fun getBook_cacheMiss_callsApiAndReturnsDetail() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(workJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }

        val result = makeRepo(engine).getBook("/works/OL82563W")

        assertIs<Result.Success<BookDetail>>(result)
        assertEquals("Dune", result.value.title)
        assertEquals("A great novel", result.value.description)
        assertEquals("Frank Herbert", result.value.authors[0].name)
        assertEquals(1, callCount)
    }

    @Test
    fun getBook_cacheHit_skipsApi() = runTest {
        var callCount = 0
        val engine = MockEngine {
            callCount++
            respond(workJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val cache = BookCache()
        val repo = makeRepo(engine, cache)

        repo.getBook("/works/OL82563W")
        repo.getBook("/works/OL82563W")

        assertEquals(1, callCount)
    }

    @Test
    fun getBook_apiError_propagatesFailure() = runTest {
        val engine = MockEngine {
            respond("error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
        }

        val result = makeRepo(engine).getBook("/works/OL82563W")

        assertIs<Result.Failure<ApiError>>(result)
        assertIs<ApiError.HttpError>(result.error)
        assertEquals(500, (result.error as ApiError.HttpError).code)
    }

    @Test
    fun getBook_authorFetchFails_authorRefExcluded() = runTest {
        val engine = MockEngine {
            respond(workJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val failingAuthorRepo = object : AuthorRepository {
            override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> =
                Result.Failure(ApiError.HttpError(404, "Not Found"))
        }

        val result = makeRepo(engine, authorRepository = failingAuthorRepo).getBook("/works/OL82563W")

        assertIs<Result.Success<BookDetail>>(result)
        assertEquals(emptyList(), result.value.authors)
    }

    @Test
    fun getBook_multipleAuthors_fetchedInParallel() = runTest {
        val multiAuthorJson = """
            {
              "key": "/works/OL1W",
              "title": "Co-written",
              "authors": [
                { "author": { "key": "/authors/OL1A" } },
                { "author": { "key": "/authors/OL2A" } }
              ]
            }
        """.trimIndent()
        val engine = MockEngine {
            respond(multiAuthorJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        var authorCallCount = 0
        val countingAuthorRepo = object : AuthorRepository {
            override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> {
                authorCallCount++
                return Result.Success(AuthorDetail(key = authorKey, name = "A$authorCallCount",
                    bio = null, birthDate = null, deathDate = null, photoUrl = null, works = emptyList()))
            }
        }

        val result = makeRepo(engine, authorRepository = countingAuthorRepo).getBook("/works/OL1W")

        assertIs<Result.Success<BookDetail>>(result)
        assertEquals(2, result.value.authors.size)
        assertEquals(2, authorCallCount)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.book.BookRepositoryTest" 2>&1 | tail -20`
Expected: FAIL with unresolved reference errors

- [ ] **Step 3: Create BookRepository.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/book/domain/BookRepository.kt
package com.opclient.book.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface BookRepository {
    suspend fun getBook(workKey: String): Result<BookDetail, ApiError>
}
```

- [ ] **Step 4: Create BookRepositoryImpl.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/book/data/BookRepositoryImpl.kt
package com.opclient.book.data

import com.opclient.author.domain.AuthorRepository
import com.opclient.book.domain.AuthorRef
import com.opclient.book.domain.BookDetail
import com.opclient.book.domain.BookRepository
import com.opclient.core.ApiError
import com.opclient.core.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class BookRepositoryImpl(
    private val apiClient: BookApiClient,
    private val cache: BookCache,
    private val authorRepository: AuthorRepository,
) : BookRepository {

    override suspend fun getBook(workKey: String): Result<BookDetail, ApiError> {
        val olid = workKey.substringAfterLast("/")
        val cacheKey = "work:$olid"

        cache.get(cacheKey)?.let { return Result.Success(it) }

        return when (val result = apiClient.getWork(olid)) {
            is Result.Failure -> result
            is Result.Success -> {
                val dto = result.value
                val authorKeys = dto.authors?.map { it.author.key } ?: emptyList()

                val authors = coroutineScope {
                    authorKeys.map { key -> async { authorRepository.getAuthor(key) } }
                        .mapNotNull { deferred ->
                            val r = deferred.await()
                            if (r is Result.Success) {
                                AuthorRef(key = r.value.key, name = r.value.name)
                            } else null
                        }
                }

                val domain = dto.toDomain(authors)
                cache.put(cacheKey, domain)
                Result.Success(domain)
            }
        }
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.book.BookRepositoryTest" 2>&1 | tail -20`
Expected: 5 tests pass

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/book/domain/BookRepository.kt \
        composeApp/src/commonMain/kotlin/com/opclient/book/data/BookRepositoryImpl.kt \
        composeApp/src/commonTest/kotlin/com/opclient/book/BookRepositoryTest.kt
git commit -m "feat(book): add BookRepository interface and impl with parallel author resolution"
```

---

### Task 9: Koin modules (BookModule + AuthorModule + CommonModule update)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/di/BookModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/di/AuthorModule.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/di/CommonModule.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`

- [ ] **Step 1: Create BookModule.kt**

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
    viewModel { BookDetailViewModel(get()) }
}
```

- [ ] **Step 2: Create AuthorModule.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/di/AuthorModule.kt
package com.opclient.di

import com.opclient.author.data.AuthorApiClient
import com.opclient.author.data.AuthorCache
import com.opclient.author.data.AuthorRepositoryImpl
import com.opclient.author.domain.AuthorRepository
import com.opclient.author.presentation.AuthorDetailViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authorModule: Module = module {
    single { AuthorCache() }
    factory { AuthorApiClient(get()) }
    single<AuthorRepository> { AuthorRepositoryImpl(apiClient = get(), cache = get()) }
    viewModel { AuthorDetailViewModel(get()) }
}
```

- [ ] **Step 3: Update CommonModule.kt to add NavigationViewModel**

Replace the current content of `composeApp/src/commonMain/kotlin/com/opclient/di/CommonModule.kt`:

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/di/CommonModule.kt
package com.opclient.di

import com.opclient.core.buildHttpClient
import com.opclient.core.createHttpEngine
import com.opclient.navigation.NavigationViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule: Module = module {
    single { buildHttpClient(createHttpEngine()) }
    viewModel { NavigationViewModel() }
}
```

- [ ] **Step 4: Update OpClientApplication.kt**

Replace modules list in `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`:

```kotlin
// composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt
package com.opclient

import android.app.Application
import com.opclient.di.androidModule
import com.opclient.di.authorModule
import com.opclient.di.bookModule
import com.opclient.di.commonModule
import com.opclient.di.searchModule
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
            modules(commonModule, androidModule, searchModule, authorModule, bookModule)
        }
    }
}
```

- [ ] **Step 5: Update Main.kt**

Replace modules list in `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`:

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
import com.opclient.ui.theme.AppTheme
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin

fun main() {
    Napier.base(DebugAntilog())
    startKoin {
        modules(commonModule, desktopModule, searchModule, authorModule, bookModule)
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

Note: `BookModule` and `AuthorModule` reference ViewModels not yet created. The modules compile once `BookDetailViewModel` and `AuthorDetailViewModel` exist (Tasks 10–11). Run a build after Tasks 10–11 to verify.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/di/BookModule.kt \
        composeApp/src/commonMain/kotlin/com/opclient/di/AuthorModule.kt \
        composeApp/src/commonMain/kotlin/com/opclient/di/CommonModule.kt \
        composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt \
        composeApp/src/jvmMain/kotlin/com/opclient/Main.kt
git commit -m "feat(di): add BookModule, AuthorModule, wire NavigationViewModel into CommonModule"
```

---

### Task 10: BookDetailViewModel + BookDetailViewModelTest

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/book/BookDetailViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.book.BookDetailViewModelTest" 2>&1 | tail -20`
Expected: FAIL with unresolved reference errors

- [ ] **Step 3: Create BookDetailViewModel.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailViewModel.kt
package com.opclient.book.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.book.domain.BookDetail
import com.opclient.book.domain.BookRepository
import com.opclient.core.ApiError
import com.opclient.core.Result
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailStatus { Loading, Success, Error }

data class BookDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val book: BookDetail? = null,
)

sealed class BookDetailIntent {
    data class Load(val workKey: String) : BookDetailIntent()
    data object Retry : BookDetailIntent()
}

sealed class BookDetailEffect {
    data class LoadError(val error: ApiError) : BookDetailEffect()
}

class BookDetailViewModel(private val repository: BookRepository) : ViewModel() {
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
                is Result.Success -> _uiState.update {
                    it.copy(status = DetailStatus.Success, book = result.value)
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = DetailStatus.Error) }
                    _effects.tryEmit(BookDetailEffect.LoadError(result.error))
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.book.BookDetailViewModelTest" 2>&1 | tail -20`
Expected: 4 tests pass

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailViewModel.kt \
        composeApp/src/commonTest/kotlin/com/opclient/book/BookDetailViewModelTest.kt
git commit -m "feat(book): add BookDetailViewModel MVI with Load/Retry intents and DetailStatus"
```

---

### Task 11: AuthorDetailViewModel + AuthorDetailViewModelTest

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/author/presentation/AuthorDetailViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/author/AuthorDetailViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.author.AuthorDetailViewModelTest" 2>&1 | tail -20`
Expected: FAIL with unresolved reference errors

- [ ] **Step 3: Create AuthorDetailViewModel.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/author/presentation/AuthorDetailViewModel.kt
package com.opclient.author.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.opclient.author.domain.AuthorDetail
import com.opclient.author.domain.AuthorRepository
import com.opclient.book.presentation.DetailStatus
import com.opclient.core.ApiError
import com.opclient.core.Result
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthorDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val author: AuthorDetail? = null,
)

sealed class AuthorDetailIntent {
    data class Load(val authorKey: String) : AuthorDetailIntent()
    data object Retry : AuthorDetailIntent()
}

sealed class AuthorDetailEffect {
    data class LoadError(val error: ApiError) : AuthorDetailEffect()
}

class AuthorDetailViewModel(private val repository: AuthorRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthorDetailUiState())
    val uiState: StateFlow<AuthorDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<AuthorDetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<AuthorDetailEffect> = _effects.asSharedFlow()

    private var lastKey: String = ""

    fun onIntent(intent: AuthorDetailIntent) {
        when (intent) {
            is AuthorDetailIntent.Load -> load(intent.authorKey)
            AuthorDetailIntent.Retry -> if (lastKey.isNotEmpty()) load(lastKey)
        }
    }

    private fun load(authorKey: String) {
        lastKey = authorKey
        viewModelScope.launch {
            _uiState.update { it.copy(status = DetailStatus.Loading) }
            when (val result = repository.getAuthor(authorKey)) {
                is Result.Success -> _uiState.update {
                    it.copy(status = DetailStatus.Success, author = result.value)
                }
                is Result.Failure -> {
                    _uiState.update { it.copy(status = DetailStatus.Error) }
                    _effects.tryEmit(AuthorDetailEffect.LoadError(result.error))
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :composeApp:desktopTest --tests "com.opclient.author.AuthorDetailViewModelTest" 2>&1 | tail -20`
Expected: 3 tests pass

- [ ] **Step 5: Run full test suite to verify nothing broken**

Run: `./gradlew :composeApp:desktopTest 2>&1 | tail -30`
Expected: All tests pass

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/author/presentation/AuthorDetailViewModel.kt \
        composeApp/src/commonTest/kotlin/com/opclient/author/AuthorDetailViewModelTest.kt
git commit -m "feat(author): add AuthorDetailViewModel MVI with Load/Retry intents"
```

---

### Task 12: BookDetailScreen composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailScreen.kt`

- [ ] **Step 1: Create BookDetailScreen.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailScreen.kt
package com.opclient.book.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.SubjectTag
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailScreen(
    workKey: String,
    onAuthorClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: BookDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    LaunchedEffect(Unit) {
        viewModel.onIntent(BookDetailIntent.Load(workKey))
    }

    when (uiState.status) {
        DetailStatus.Loading -> LoadingState()
        DetailStatus.Error -> ErrorState(
            message = "Failed to load book.",
            onRetry = { viewModel.onIntent(BookDetailIntent.Retry) },
        )
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
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailScreen.kt
git commit -m "feat(book): add BookDetailScreen composable"
```

---

### Task 13: AuthorDetailScreen composable

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/author/presentation/AuthorDetailScreen.kt`

- [ ] **Step 1: Create AuthorDetailScreen.kt**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/author/presentation/AuthorDetailScreen.kt
package com.opclient.author.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.opclient.book.presentation.DetailStatus
import com.opclient.ui.components.BookRow
import com.opclient.ui.components.SecondaryButton
import com.opclient.ui.components.SectionLabel
import com.opclient.ui.components.feedback.ErrorState
import com.opclient.ui.components.feedback.LoadingState
import com.opclient.ui.theme.AppThemeTokens
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthorDetailScreen(
    authorKey: String,
    onBookClick: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AuthorDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    LaunchedEffect(Unit) {
        viewModel.onIntent(AuthorDetailIntent.Load(authorKey))
    }

    when (uiState.status) {
        DetailStatus.Loading -> LoadingState()
        DetailStatus.Error -> ErrorState(
            message = "Failed to load author.",
            onRetry = { viewModel.onIntent(AuthorDetailIntent.Retry) },
        )
        DetailStatus.Success -> {
            val author = uiState.author ?: return
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    SecondaryButton(
                        text = "← BACK",
                        onClick = onBack,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
                item {
                    if (author.photoUrl != null) {
                        AsyncImage(
                            model = author.photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            placeholder = ColorPainter(colors.surface2),
                            error = ColorPainter(colors.surface2),
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .size(80.dp)
                                .clip(CircleShape),
                        )
                    }
                }
                item {
                    SectionLabel(
                        text = author.name,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (author.birthDate != null || author.deathDate != null) {
                    item {
                        val dates = listOfNotNull(author.birthDate, author.deathDate).joinToString(" – ")
                        BasicText(
                            text = dates,
                            style = typography.bookAuthor.copy(color = colors.textSecondary),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
                if (author.bio != null) {
                    item {
                        BasicText(
                            text = author.bio,
                            style = typography.bookAuthor.copy(color = colors.textPrimary),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                item {
                    SectionLabel(
                        text = "WORKS",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(author.works, key = { it.key }) { work ->
                    BookRow(
                        title = work.title,
                        author = "",
                        coverContent = {
                            if (work.coverUrl != null) {
                                AsyncImage(
                                    model = work.coverUrl,
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
                        onClick = { onBookClick(work.key) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew :composeApp:compileCommonMainKotlinMetadata 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/author/presentation/AuthorDetailScreen.kt
git commit -m "feat(author): add AuthorDetailScreen composable with lazy works list"
```

---

### Task 14: App.kt navigation wiring + full build verification

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/App.kt`

- [ ] **Step 1: Update App.kt to use NavigationViewModel and route all three screens**

Replace the entire content of `composeApp/src/commonMain/kotlin/com/opclient/App.kt`:

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/App.kt
package com.opclient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.opclient.author.presentation.AuthorDetailScreen
import com.opclient.book.presentation.BookDetailScreen
import com.opclient.navigation.NavigationViewModel
import com.opclient.navigation.Screen
import com.opclient.search.presentation.SearchScreen
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
            navVm.navigateToTab()
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
        }
    }
}
```

- [ ] **Step 2: Run full test suite**

Run: `./gradlew :composeApp:desktopTest 2>&1 | tail -30`
Expected: All tests pass

- [ ] **Step 3: Full desktop build**

Run: `./gradlew :composeApp:compileKotlinDesktop 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Full Android build**

Run: `./gradlew :composeApp:compileDebugKotlin 2>&1 | tail -20`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/App.kt
git commit -m "feat(app): wire NavigationViewModel into App.kt, route Book and Author detail screens"
```

---

### Task 15: Live API verification

No code changes — manual verification that the full feature works end-to-end on Desktop.

- [ ] **Step 1: Run desktop app**

Run: `./gradlew :composeApp:run`

- [ ] **Step 2: Test search → book detail flow**

1. Search for "Dune"
2. Click a result
3. Verify: BookDetailScreen shows cover hero (220dp), title, author names, subjects, description, first publish date
4. Verify: Back button returns to Search results

- [ ] **Step 3: Test book detail → author detail flow**

1. From BookDetailScreen, click an author name
2. Verify: AuthorDetailScreen shows circular photo (80dp), name, dates, bio, WORKS section with book list
3. Click a book in the works list
4. Verify: navigates to that book's BookDetailScreen

- [ ] **Step 4: Test back navigation chain**

1. Search → Book → Author → Book
2. Back → Author
3. Back → Book
4. Back → Search

Verify each step shows correct screen.

- [ ] **Step 5: Test error handling**

1. Disable network
2. Navigate to a book detail
3. Verify ErrorState with RETRY button appears
4. Re-enable network, click RETRY
5. Verify book loads successfully

- [ ] **Step 6: Commit final state**

```bash
git add -A
git status  # verify only expected changes
git commit -m "feat(week3): complete Books & Authors detail with navigation stack"
```
