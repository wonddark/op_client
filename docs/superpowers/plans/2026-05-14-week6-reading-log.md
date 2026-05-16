# Week 6 — My Books and Reading Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add OpenLibrary reading log sync, user profile display, and local reading goal tracking to the existing library.

**Architecture:** A new `SettingsDatabase` (second SQLDelight database) persists username and reading goals. `ReadingLogSyncService` fetches all three shelves from the OpenLibrary reading log API and upserts into the existing `LibraryRepository`. `ProfileApiClient` retrieves the user's public OpenLibrary profile. `ProfileViewModel` drives a new `ProfileScreen` (sub-screen of Library) that shows profile info, a reading goal, year-to-date progress, and a sync button. Navigation: `LibraryScreen` gains an `onProfileClick` callback that pushes `Screen.Profile` onto the stack.

**Tech Stack:** SQLDelight 2.0.2 (`SettingsDatabase`), Ktor MockEngine (tests), kotlinx.datetime, kotlinx.serialization `JsonElement` for polymorphic bio field, Koin named qualifier `named("settings")`.

---

### Task 1: SettingsDatabase Gradle Setup

**Files:**
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Add SettingsDatabase to sqldelight block**

  In `composeApp/build.gradle.kts`, change the `sqldelight {}` block from:
  ```kotlin
  sqldelight {
      databases {
          create("LibraryDatabase") {
              packageName.set("com.opclient.library")
          }
      }
  }
  ```
  To:
  ```kotlin
  sqldelight {
      databases {
          create("LibraryDatabase") {
              packageName.set("com.opclient.library")
          }
          create("SettingsDatabase") {
              packageName.set("com.opclient.settings")
          }
      }
  }
  ```

- [ ] **Step 2: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

  ```bash
  git add composeApp/build.gradle.kts
  git commit -m "build: add SettingsDatabase to SQLDelight configuration"
  ```

---

### Task 2: Settings SQLDelight Schema

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/settings/com/opclient/settings/Settings.sq`

- [ ] **Step 1: Create Settings.sq**

  Create the file `composeApp/src/commonMain/sqldelight/settings/com/opclient/settings/Settings.sq`:
  ```sql
  CREATE TABLE UserPreference (
      key   TEXT NOT NULL PRIMARY KEY,
      value TEXT NOT NULL
  );

  CREATE TABLE GoalRow (
      year   INTEGER NOT NULL PRIMARY KEY,
      target INTEGER NOT NULL
  );

  getPreference:
  SELECT value FROM UserPreference WHERE key = ?;

  upsertPreference:
  INSERT OR REPLACE INTO UserPreference VALUES (?, ?);

  getGoal:
  SELECT * FROM GoalRow WHERE year = ?;

  upsertGoal:
  INSERT OR REPLACE INTO GoalRow VALUES (?, ?);

  deleteGoal:
  DELETE FROM GoalRow WHERE year = ?;
  ```

- [ ] **Step 2: Trigger code generation and verify**

  Run: `./gradlew :composeApp:generateCommonMainSettingsDatabaseInterface`
  Expected: `BUILD SUCCESSFUL`

  Run: `find composeApp/build -name "SettingsDatabase.kt" 2>/dev/null | head -3`
  Expected: at least one path printed

- [ ] **Step 3: Commit**

  ```bash
  git add composeApp/src/commonMain/sqldelight/settings/
  git commit -m "feat(settings): add SettingsDatabase SQLDelight schema"
  ```

---

### Task 3: Settings Repository (TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/settings/domain/SettingsModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/settings/domain/SettingsRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/settings/data/SettingsRepositoryImpl.kt`
- Create: `composeApp/src/jvmTest/kotlin/com/opclient/settings/SettingsRepositoryTest.kt`

- [ ] **Step 1: Create domain models**

  Create `composeApp/src/commonMain/kotlin/com/opclient/settings/domain/SettingsModels.kt`:
  ```kotlin
  package com.opclient.settings.domain

  data class ReadingGoal(val year: Int, val target: Int)
  ```

- [ ] **Step 2: Create repository interface**

  Create `composeApp/src/commonMain/kotlin/com/opclient/settings/domain/SettingsRepository.kt`:
  ```kotlin
  package com.opclient.settings.domain

  interface SettingsRepository {
      suspend fun getUsername(): String?
      suspend fun setUsername(username: String)
      suspend fun getReadingGoal(year: Int): ReadingGoal?
      suspend fun setReadingGoal(year: Int, target: Int)
      suspend fun clearReadingGoal(year: Int)
  }
  ```

- [ ] **Step 3: Write failing tests**

  Create `composeApp/src/jvmTest/kotlin/com/opclient/settings/SettingsRepositoryTest.kt`:
  ```kotlin
  package com.opclient.settings

  import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
  import com.opclient.settings.data.SettingsRepositoryImpl
  import com.opclient.settings.domain.SettingsRepository
  import kotlinx.coroutines.test.runTest
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertNull

  class SettingsRepositoryTest {

      private fun createRepo(): SettingsRepository {
          val driver = JdbcSqliteDriver("jdbc:sqlite::memory:")
          SettingsDatabase.Schema.create(driver)
          return SettingsRepositoryImpl(SettingsDatabase(driver))
      }

      @Test
      fun getUsername_returnsNullWhenNotSet() = runTest {
          assertNull(createRepo().getUsername())
      }

      @Test
      fun setUsername_thenGet_returnsSavedUsername() = runTest {
          val repo = createRepo()
          repo.setUsername("mark")
          assertEquals("mark", repo.getUsername())
      }

      @Test
      fun setUsername_overwritesPreviousValue() = runTest {
          val repo = createRepo()
          repo.setUsername("mark")
          repo.setUsername("newuser")
          assertEquals("newuser", repo.getUsername())
      }

      @Test
      fun getReadingGoal_returnsNullWhenNotSet() = runTest {
          assertNull(createRepo().getReadingGoal(2026))
      }

      @Test
      fun setReadingGoal_thenGet_returnsSavedGoal() = runTest {
          val repo = createRepo()
          repo.setReadingGoal(2026, 24)
          val goal = repo.getReadingGoal(2026)
          assertEquals(2026, goal?.year)
          assertEquals(24, goal?.target)
      }

      @Test
      fun clearReadingGoal_removesGoal() = runTest {
          val repo = createRepo()
          repo.setReadingGoal(2026, 24)
          repo.clearReadingGoal(2026)
          assertNull(repo.getReadingGoal(2026))
      }

      @Test
      fun readingGoal_isolatedByYear() = runTest {
          val repo = createRepo()
          repo.setReadingGoal(2025, 20)
          repo.setReadingGoal(2026, 24)
          assertEquals(20, repo.getReadingGoal(2025)?.target)
          assertEquals(24, repo.getReadingGoal(2026)?.target)
      }
  }
  ```

- [ ] **Step 4: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.settings.SettingsRepositoryTest"`
  Expected: compilation error — `SettingsRepositoryImpl` not yet created

- [ ] **Step 5: Implement SettingsRepositoryImpl**

  Create `composeApp/src/commonMain/kotlin/com/opclient/settings/data/SettingsRepositoryImpl.kt`:
  ```kotlin
  package com.opclient.settings.data

  import com.opclient.settings.SettingsDatabase
  import com.opclient.settings.domain.ReadingGoal
  import com.opclient.settings.domain.SettingsRepository
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.withContext

  class SettingsRepositoryImpl(private val db: SettingsDatabase) : SettingsRepository {

      private companion object {
          const val KEY_USERNAME = "username"
      }

      override suspend fun getUsername(): String? = withContext(Dispatchers.IO) {
          db.settingsQueries.getPreference(KEY_USERNAME).executeAsOneOrNull()
      }

      override suspend fun setUsername(username: String) = withContext(Dispatchers.IO) {
          db.settingsQueries.upsertPreference(KEY_USERNAME, username)
      }

      override suspend fun getReadingGoal(year: Int): ReadingGoal? = withContext(Dispatchers.IO) {
          db.settingsQueries.getGoal(year.toLong()).executeAsOneOrNull()
              ?.let { ReadingGoal(it.year.toInt(), it.target.toInt()) }
      }

      override suspend fun setReadingGoal(year: Int, target: Int) = withContext(Dispatchers.IO) {
          db.settingsQueries.upsertGoal(year.toLong(), target.toLong())
      }

      override suspend fun clearReadingGoal(year: Int) = withContext(Dispatchers.IO) {
          db.settingsQueries.deleteGoal(year.toLong())
      }
  }
  ```

- [ ] **Step 6: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.settings.SettingsRepositoryTest"`
  Expected: 7 tests PASSED

- [ ] **Step 7: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/settings/ \
          composeApp/src/jvmTest/kotlin/com/opclient/settings/SettingsRepositoryTest.kt
  git commit -m "feat(settings): add SettingsRepository with TDD"
  ```

---

### Task 4: User Profile DTOs and API Client

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/profile/domain/ProfileModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/profile/domain/ProfileRepository.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/profile/data/ProfileDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/profile/data/ProfileMapper.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/profile/data/ProfileApiClient.kt`

- [ ] **Step 1: Create domain models**

  Create `composeApp/src/commonMain/kotlin/com/opclient/profile/domain/ProfileModels.kt`:
  ```kotlin
  package com.opclient.profile.domain

  data class UserProfile(
      val username: String,
      val displayName: String,
      val bio: String?,
      val photoId: Int?,
  ) {
      val photoUrl: String? = photoId?.let { "https://covers.openlibrary.org/p/id/$it-M.jpg" }
  }
  ```

- [ ] **Step 2: Create repository interface**

  Create `composeApp/src/commonMain/kotlin/com/opclient/profile/domain/ProfileRepository.kt`:
  ```kotlin
  package com.opclient.profile.domain

  import com.opclient.core.ApiError
  import com.opclient.core.Result

  interface UserProfileRepository {
      suspend fun getProfile(username: String): Result<UserProfile, ApiError>
  }
  ```

- [ ] **Step 3: Create DTOs**

  The OpenLibrary profile `bio` field may be a plain string or a JSON object `{"type":"/type/text","value":"..."}`. Using `JsonElement?` handles both cases.

  Create `composeApp/src/commonMain/kotlin/com/opclient/profile/data/ProfileDto.kt`:
  ```kotlin
  package com.opclient.profile.data

  import kotlinx.serialization.Serializable
  import kotlinx.serialization.json.JsonElement

  @Serializable
  data class UserProfileDto(
      val key: String? = null,
      val name: String? = null,
      val bio: JsonElement? = null,
      val photos: List<Int>? = null,
      val location: String? = null,
  )
  ```

- [ ] **Step 4: Create mapper**

  Create `composeApp/src/commonMain/kotlin/com/opclient/profile/data/ProfileMapper.kt`:
  ```kotlin
  package com.opclient.profile.data

  import com.opclient.profile.domain.UserProfile
  import kotlinx.serialization.json.JsonObject
  import kotlinx.serialization.json.JsonPrimitive
  import kotlinx.serialization.json.contentOrNull
  import kotlinx.serialization.json.jsonPrimitive

  fun UserProfileDto.toDomain(): UserProfile {
      val bioText = when (val b = bio) {
          is JsonPrimitive -> b.contentOrNull
          is JsonObject -> b["value"]?.jsonPrimitive?.contentOrNull
          else -> null
      }
      return UserProfile(
          username = key?.removePrefix("/people/") ?: "",
          displayName = name ?: "",
          bio = bioText,
          photoId = photos?.firstOrNull(),
      )
  }
  ```

- [ ] **Step 5: Create API client**

  Create `composeApp/src/commonMain/kotlin/com/opclient/profile/data/ProfileApiClient.kt`:
  ```kotlin
  package com.opclient.profile.data

  import com.opclient.core.ApiClient
  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import io.ktor.client.HttpClient

  class ProfileApiClient(httpClient: HttpClient) :
      ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

      suspend fun getProfile(username: String): Result<UserProfileDto, ApiError> =
          get(path = "/people/$username.json")
  }
  ```

- [ ] **Step 6: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/profile/domain/ \
          composeApp/src/commonMain/kotlin/com/opclient/profile/data/
  git commit -m "feat(profile): add UserProfile DTOs, mapper, and ProfileApiClient"
  ```

---

### Task 5: User Profile Repository (TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/profile/data/ProfileRepositoryImpl.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/profile/ProfileRepositoryTest.kt`

- [ ] **Step 1: Write failing tests**

  Create `composeApp/src/commonTest/kotlin/com/opclient/profile/ProfileRepositoryTest.kt`:
  ```kotlin
  package com.opclient.profile

  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.core.buildHttpClient
  import com.opclient.profile.data.ProfileApiClient
  import com.opclient.profile.data.UserProfileRepositoryImpl
  import com.opclient.profile.domain.UserProfile
  import com.opclient.profile.domain.UserProfileRepository
  import io.ktor.client.engine.mock.MockEngine
  import io.ktor.client.engine.mock.respond
  import io.ktor.http.HttpHeaders
  import io.ktor.http.HttpStatusCode
  import io.ktor.http.headersOf
  import kotlinx.coroutines.test.runTest
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertIs

  class ProfileRepositoryTest {

      private val profileJson = """
          {"key":"/people/mark","name":"Mark Reader","photos":[12345]}
      """.trimIndent()

      private fun makeRepo(engine: MockEngine): UserProfileRepository {
          val apiClient = ProfileApiClient(buildHttpClient(engine))
          return UserProfileRepositoryImpl(apiClient)
      }

      @Test
      fun getProfile_parsesDisplayName() = runTest {
          val engine = MockEngine {
              respond(profileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeRepo(engine).getProfile("mark")
          assertIs<Result.Success<UserProfile>>(result)
          assertEquals("Mark Reader", result.value.displayName)
      }

      @Test
      fun getProfile_parsesUsernameFromKey() = runTest {
          val engine = MockEngine {
              respond(profileJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeRepo(engine).getProfile("mark")
          assertIs<Result.Success<UserProfile>>(result)
          assertEquals("mark", result.value.username)
      }

      @Test
      fun getProfile_404_returnsFailure() = runTest {
          val engine = MockEngine {
              respond("Not Found", HttpStatusCode.NotFound, headersOf(HttpHeaders.ContentType, "text/plain"))
          }
          val result = makeRepo(engine).getProfile("nonexistent")
          assertIs<Result.Failure<ApiError>>(result)
      }

      @Test
      fun getProfile_withBioObject_parsesBioText() = runTest {
          val json = """{"key":"/people/mark","name":"Mark","bio":{"type":"/type/text","value":"Book lover"}}"""
          val engine = MockEngine {
              respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeRepo(engine).getProfile("mark")
          assertIs<Result.Success<UserProfile>>(result)
          assertEquals("Book lover", result.value.bio)
      }

      @Test
      fun getProfile_withBioString_parsesBioText() = runTest {
          val json = """{"key":"/people/mark","name":"Mark","bio":"Plain bio text"}"""
          val engine = MockEngine {
              respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeRepo(engine).getProfile("mark")
          assertIs<Result.Success<UserProfile>>(result)
          assertEquals("Plain bio text", result.value.bio)
      }

      @Test
      fun getProfile_missingPhotoId_returnsNullPhotoUrl() = runTest {
          val json = """{"key":"/people/mark","name":"Mark"}"""
          val engine = MockEngine {
              respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeRepo(engine).getProfile("mark")
          assertIs<Result.Success<UserProfile>>(result)
          assertEquals(null, result.value.photoUrl)
      }
  }
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.profile.ProfileRepositoryTest"`
  Expected: compilation error — `UserProfileRepositoryImpl` not yet created

- [ ] **Step 3: Implement UserProfileRepositoryImpl**

  Create `composeApp/src/commonMain/kotlin/com/opclient/profile/data/ProfileRepositoryImpl.kt`:
  ```kotlin
  package com.opclient.profile.data

  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.profile.domain.UserProfile
  import com.opclient.profile.domain.UserProfileRepository

  class UserProfileRepositoryImpl(
      private val apiClient: ProfileApiClient,
  ) : UserProfileRepository {

      override suspend fun getProfile(username: String): Result<UserProfile, ApiError> =
          when (val result = apiClient.getProfile(username)) {
              is Result.Success -> Result.Success(result.value.toDomain())
              is Result.Failure -> result
          }
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.profile.ProfileRepositoryTest"`
  Expected: 6 tests PASSED

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/profile/data/ProfileRepositoryImpl.kt \
          composeApp/src/commonTest/kotlin/com/opclient/profile/ProfileRepositoryTest.kt
  git commit -m "feat(profile): add UserProfileRepositoryImpl with TDD"
  ```

---

### Task 6: Reading Log DTOs and API Client

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/readinglog/data/ReadingLogDto.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/readinglog/data/ReadingLogMapper.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/readinglog/data/ReadingLogApiClient.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/readinglog/domain/ReadingLogSyncService.kt`

- [ ] **Step 1: Create DTOs**

  Create `composeApp/src/commonMain/kotlin/com/opclient/readinglog/data/ReadingLogDto.kt`:
  ```kotlin
  package com.opclient.readinglog.data

  import kotlinx.serialization.SerialName
  import kotlinx.serialization.Serializable

  @Serializable
  data class ReadingLogResponseDto(
      @SerialName("reading_log_entries") val entries: List<ReadingLogEntryDto> = emptyList(),
  )

  @Serializable
  data class ReadingLogEntryDto(
      val work: ReadingLogWorkDto,
      @SerialName("logged_date") val loggedDate: String? = null,
  )

  @Serializable
  data class ReadingLogWorkDto(
      val key: String,
      val title: String? = null,
      @SerialName("author_names") val authorNames: List<String>? = null,
      @SerialName("cover_id") val coverId: Int? = null,
  )
  ```

- [ ] **Step 2: Create mapper**

  The OpenLibrary `logged_date` uses ISO 8601 format with timezone offset, e.g. `"2024-01-15T10:00:00+00:00"`. `Instant.parse` from `kotlinx-datetime` handles this format.

  Create `composeApp/src/commonMain/kotlin/com/opclient/readinglog/data/ReadingLogMapper.kt`:
  ```kotlin
  package com.opclient.readinglog.data

  import com.opclient.library.domain.LibraryEntry
  import com.opclient.library.domain.Shelf
  import kotlinx.datetime.Clock
  import kotlinx.datetime.Instant

  fun ReadingLogEntryDto.toLibraryEntry(shelf: Shelf): LibraryEntry =
      LibraryEntry(
          workKey = work.key,
          title = work.title ?: "Unknown",
          authorName = work.authorNames?.firstOrNull(),
          coverUrl = work.coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
          shelf = shelf,
          addedAt = loggedDate?.toEpochMillis() ?: Clock.System.now().toEpochMilliseconds(),
      )

  private fun String.toEpochMillis(): Long =
      try {
          Instant.parse(this).toEpochMilliseconds()
      } catch (_: Exception) {
          Clock.System.now().toEpochMilliseconds()
      }
  ```

- [ ] **Step 3: Create sync service interface**

  Create `composeApp/src/commonMain/kotlin/com/opclient/readinglog/domain/ReadingLogSyncService.kt`:
  ```kotlin
  package com.opclient.readinglog.domain

  import com.opclient.core.ApiError
  import com.opclient.core.Result

  interface ReadingLogSyncService {
      suspend fun sync(username: String): Result<Int, ApiError>
  }
  ```

- [ ] **Step 4: Create API client**

  The OpenLibrary reading log shelf keys differ from the local `Shelf` enum names.

  Create `composeApp/src/commonMain/kotlin/com/opclient/readinglog/data/ReadingLogApiClient.kt`:
  ```kotlin
  package com.opclient.readinglog.data

  import com.opclient.core.ApiClient
  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.library.domain.Shelf
  import io.ktor.client.HttpClient

  class ReadingLogApiClient(httpClient: HttpClient) :
      ApiClient(baseUrl = "https://openlibrary.org", httpClient = httpClient) {

      suspend fun getShelf(username: String, shelf: Shelf): Result<ReadingLogResponseDto, ApiError> =
          get(path = "/people/$username/books/${shelf.toRemoteKey()}.json")
  }

  private fun Shelf.toRemoteKey(): String = when (this) {
      Shelf.WANT_TO_READ -> "want-to-read"
      Shelf.READING -> "currently-reading"
      Shelf.READ -> "already-read"
  }
  ```

- [ ] **Step 5: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/readinglog/
  git commit -m "feat(readinglog): add reading log DTOs, mapper, API client, and sync interface"
  ```

---

### Task 7: Reading Log Sync Service (TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/readinglog/data/ReadingLogSyncServiceImpl.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/readinglog/ReadingLogSyncServiceTest.kt`

- [ ] **Step 1: Write failing tests**

  Create `composeApp/src/commonTest/kotlin/com/opclient/readinglog/ReadingLogSyncServiceTest.kt`:
  ```kotlin
  package com.opclient.readinglog

  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.core.buildHttpClient
  import com.opclient.library.domain.LibraryEntry
  import com.opclient.library.domain.LibraryRepository
  import com.opclient.library.domain.Shelf
  import com.opclient.readinglog.data.ReadingLogApiClient
  import com.opclient.readinglog.data.ReadingLogSyncServiceImpl
  import io.ktor.client.engine.mock.MockEngine
  import io.ktor.client.engine.mock.respond
  import io.ktor.http.HttpHeaders
  import io.ktor.http.HttpStatusCode
  import io.ktor.http.headersOf
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.flowOf
  import kotlinx.coroutines.test.runTest
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertIs

  class ReadingLogSyncServiceTest {

      private val wantToReadJson = """
          {
            "reading_log_entries": [
              {
                "work": {
                  "key": "/works/OL82563W",
                  "title": "Dune",
                  "author_names": ["Frank Herbert"],
                  "cover_id": 8368541
                },
                "logged_date": "2024-01-15T10:00:00+00:00"
              }
            ]
          }
      """.trimIndent()

      private val emptyJson = """{"reading_log_entries":[]}"""

      private fun makeFakeLibrary(addedEntries: MutableList<LibraryEntry>) = object : LibraryRepository {
          override fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>> = flowOf(emptyList())
          override fun getCurrentShelf(workKey: String): Flow<Shelf?> = flowOf(null)
          override suspend fun addToShelf(entry: LibraryEntry) { addedEntries += entry }
          override suspend fun removeFromShelf(workKey: String) {}
          override suspend fun moveToShelf(workKey: String, shelf: Shelf) {}
      }

      private fun makeService(engine: MockEngine, added: MutableList<LibraryEntry>): ReadingLogSyncServiceImpl {
          val apiClient = ReadingLogApiClient(buildHttpClient(engine))
          return ReadingLogSyncServiceImpl(apiClient, makeFakeLibrary(added))
      }

      @Test
      fun sync_importsEntryFromWantToRead() = runTest {
          val added = mutableListOf<LibraryEntry>()
          val engine = MockEngine { request ->
              when {
                  request.url.encodedPath.contains("want-to-read") ->
                      respond(wantToReadJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                  else ->
                      respond(emptyJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
              }
          }
          val result = makeService(engine, added).sync("mark")
          assertIs<Result.Success<Int>>(result)
          assertEquals(1, result.value)
          assertEquals(1, added.size)
          assertEquals("/works/OL82563W", added[0].workKey)
          assertEquals(Shelf.WANT_TO_READ, added[0].shelf)
      }

      @Test
      fun sync_mapsCoverUrl() = runTest {
          val added = mutableListOf<LibraryEntry>()
          val engine = MockEngine { _ ->
              respond(wantToReadJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          makeService(engine, added).sync("mark")
          assertEquals("https://covers.openlibrary.org/b/id/8368541-M.jpg", added[0].coverUrl)
      }

      @Test
      fun sync_mapsAuthorName() = runTest {
          val added = mutableListOf<LibraryEntry>()
          val engine = MockEngine { _ ->
              respond(wantToReadJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          makeService(engine, added).sync("mark")
          assertEquals("Frank Herbert", added[0].authorName)
      }

      @Test
      fun sync_emptyLog_returnsZero() = runTest {
          val added = mutableListOf<LibraryEntry>()
          val engine = MockEngine { _ ->
              respond(emptyJson, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
          }
          val result = makeService(engine, added).sync("mark")
          assertIs<Result.Success<Int>>(result)
          assertEquals(0, result.value)
      }

      @Test
      fun sync_apiError_returnsFailureEarly() = runTest {
          val added = mutableListOf<LibraryEntry>()
          val engine = MockEngine { _ ->
              respond("Server Error", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "text/plain"))
          }
          val result = makeService(engine, added).sync("mark")
          assertIs<Result.Failure<ApiError>>(result)
      }
  }
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.readinglog.ReadingLogSyncServiceTest"`
  Expected: compilation error — `ReadingLogSyncServiceImpl` not yet created

- [ ] **Step 3: Implement ReadingLogSyncServiceImpl**

  Create `composeApp/src/commonMain/kotlin/com/opclient/readinglog/data/ReadingLogSyncServiceImpl.kt`:
  ```kotlin
  package com.opclient.readinglog.data

  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.library.domain.LibraryRepository
  import com.opclient.library.domain.Shelf
  import com.opclient.readinglog.domain.ReadingLogSyncService

  class ReadingLogSyncServiceImpl(
      private val apiClient: ReadingLogApiClient,
      private val libraryRepository: LibraryRepository,
  ) : ReadingLogSyncService {

      override suspend fun sync(username: String): Result<Int, ApiError> {
          var entriesSynced = 0
          for (shelf in Shelf.entries) {
              when (val result = apiClient.getShelf(username, shelf)) {
                  is Result.Success -> {
                      result.value.entries.forEach { entry ->
                          libraryRepository.addToShelf(entry.toLibraryEntry(shelf))
                          entriesSynced++
                      }
                  }
                  is Result.Failure -> return result
              }
          }
          return Result.Success(entriesSynced)
      }
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.readinglog.ReadingLogSyncServiceTest"`
  Expected: 5 tests PASSED

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/readinglog/data/ReadingLogSyncServiceImpl.kt \
          composeApp/src/commonTest/kotlin/com/opclient/readinglog/ReadingLogSyncServiceTest.kt
  git commit -m "feat(readinglog): add ReadingLogSyncServiceImpl with TDD"
  ```

---

### Task 8: ProfileViewModel (TDD)

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/profile/presentation/ProfileViewModel.kt`
- Create: `composeApp/src/commonTest/kotlin/com/opclient/profile/ProfileViewModelTest.kt`

- [ ] **Step 1: Write failing tests**

  Create `composeApp/src/commonTest/kotlin/com/opclient/profile/ProfileViewModelTest.kt`:
  ```kotlin
  package com.opclient.profile

  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.library.domain.LibraryEntry
  import com.opclient.library.domain.LibraryRepository
  import com.opclient.library.domain.Shelf
  import com.opclient.profile.domain.UserProfile
  import com.opclient.profile.domain.UserProfileRepository
  import com.opclient.profile.presentation.ProfileIntent
  import com.opclient.profile.presentation.ProfileViewModel
  import com.opclient.readinglog.domain.ReadingLogSyncService
  import com.opclient.settings.domain.ReadingGoal
  import com.opclient.settings.domain.SettingsRepository
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
  import kotlinx.datetime.Clock
  import kotlinx.datetime.LocalDateTime
  import kotlinx.datetime.TimeZone
  import kotlinx.datetime.toInstant
  import kotlinx.datetime.toLocalDateTime
  import kotlin.test.AfterTest
  import kotlin.test.BeforeTest
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertFalse
  import kotlin.test.assertNull

  class ProfileViewModelTest {

      private val testDispatcher = UnconfinedTestDispatcher()

      @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
      @AfterTest fun tearDown() { Dispatchers.resetMain() }

      private fun fakeSettings(
          username: String? = null,
          goal: ReadingGoal? = null,
      ) = object : SettingsRepository {
          var storedUsername: String? = username
          var storedGoal: ReadingGoal? = goal
          override suspend fun getUsername() = storedUsername
          override suspend fun setUsername(u: String) { storedUsername = u }
          override suspend fun getReadingGoal(year: Int) = if (storedGoal?.year == year) storedGoal else null
          override suspend fun setReadingGoal(year: Int, target: Int) { storedGoal = ReadingGoal(year, target) }
          override suspend fun clearReadingGoal(year: Int) { storedGoal = null }
      }

      private fun fakeProfile(displayName: String = "Test User") = object : UserProfileRepository {
          override suspend fun getProfile(username: String): Result<UserProfile, ApiError> =
              Result.Success(UserProfile(username = username, displayName = displayName, bio = null, photoId = null))
      }

      private fun fakeSync(entriesResult: Int = 3) = object : ReadingLogSyncService {
          override suspend fun sync(username: String): Result<Int, ApiError> = Result.Success(entriesResult)
      }

      private fun fakeLibrary(initialReadEntries: List<LibraryEntry> = emptyList()) = object : LibraryRepository {
          val shelves = MutableStateFlow(initialReadEntries.associateBy { it.workKey })
          override fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>> =
              shelves.map { map -> map.values.filter { it.shelf == shelf }.toList() }
          override fun getCurrentShelf(workKey: String): Flow<Shelf?> =
              shelves.map { it[workKey]?.shelf }
          override suspend fun addToShelf(entry: LibraryEntry) { shelves.update { it + (entry.workKey to entry) } }
          override suspend fun removeFromShelf(workKey: String) { shelves.update { it - workKey } }
          override suspend fun moveToShelf(workKey: String, shelf: Shelf) {
              shelves.update { map ->
                  map[workKey]?.let { e -> map + (workKey to e.copy(shelf = shelf)) } ?: map
              }
          }
      }

      @Test
      fun initialState_loadsUsernameAndGoal() = runTest {
          val year = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
          val settings = fakeSettings(username = "mark", goal = ReadingGoal(year, 24))
          val vm = ProfileViewModel(settings, fakeProfile(), fakeSync(), fakeLibrary())
          advanceUntilIdle()
          assertEquals("mark", vm.uiState.value.username)
          assertEquals(24, vm.uiState.value.goalTarget)
      }

      @Test
      fun initialState_withUsername_fetchesProfileName() = runTest {
          val settings = fakeSettings(username = "mark")
          val vm = ProfileViewModel(settings, fakeProfile("Mark Reader"), fakeSync(), fakeLibrary())
          advanceUntilIdle()
          assertEquals("Mark Reader", vm.uiState.value.profileName)
      }

      @Test
      fun initialState_noUsername_profileNameEmpty() = runTest {
          val vm = ProfileViewModel(fakeSettings(), fakeProfile(), fakeSync(), fakeLibrary())
          advanceUntilIdle()
          assertEquals("", vm.uiState.value.profileName)
      }

      @Test
      fun setUsername_persistsAndFetchesProfile() = runTest {
          val settings = fakeSettings()
          val vm = ProfileViewModel(settings, fakeProfile("New User"), fakeSync(), fakeLibrary())
          vm.onIntent(ProfileIntent.SetUsername("newuser"))
          advanceUntilIdle()
          assertEquals("newuser", settings.storedUsername)
          assertEquals("newuser", vm.uiState.value.username)
          assertEquals("New User", vm.uiState.value.profileName)
      }

      @Test
      fun sync_withUsername_setsEntriesCount() = runTest {
          val settings = fakeSettings(username = "mark")
          val vm = ProfileViewModel(settings, fakeProfile(), fakeSync(entriesResult = 5), fakeLibrary())
          advanceUntilIdle()
          vm.onIntent(ProfileIntent.Sync)
          advanceUntilIdle()
          assertEquals(5, vm.uiState.value.lastSyncEntriesCount)
          assertFalse(vm.uiState.value.isSyncing)
      }

      @Test
      fun sync_withoutUsername_doesNothing() = runTest {
          val vm = ProfileViewModel(fakeSettings(), fakeProfile(), fakeSync(), fakeLibrary())
          advanceUntilIdle()
          vm.onIntent(ProfileIntent.Sync)
          advanceUntilIdle()
          assertNull(vm.uiState.value.lastSyncEntriesCount)
      }

      @Test
      fun setGoal_persistsAndUpdatesState() = runTest {
          val settings = fakeSettings()
          val vm = ProfileViewModel(settings, fakeProfile(), fakeSync(), fakeLibrary())
          vm.onIntent(ProfileIntent.SetGoal(12))
          advanceUntilIdle()
          assertEquals(12, vm.uiState.value.goalTarget)
          assertEquals(12, settings.storedGoal?.target)
      }

      @Test
      fun clearGoal_removesGoalFromStateAndStorage() = runTest {
          val year = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
          val settings = fakeSettings(goal = ReadingGoal(year, 24))
          val vm = ProfileViewModel(settings, fakeProfile(), fakeSync(), fakeLibrary())
          advanceUntilIdle()
          vm.onIntent(ProfileIntent.ClearGoal)
          advanceUntilIdle()
          assertEquals(0, vm.uiState.value.goalTarget)
          assertNull(settings.storedGoal)
      }

      @Test
      fun progressCount_countsOnlyBooksReadSinceYearStart() = runTest {
          val tz = TimeZone.currentSystemDefault()
          val year = Clock.System.now().toLocalDateTime(tz).year
          val yearStartMs = LocalDateTime(year, 1, 1, 0, 0).toInstant(tz).toEpochMilliseconds()
          val thisYear = LibraryEntry("/works/OL1W", "Book1", null, null, Shelf.READ, yearStartMs + 1_000)
          val lastYear = LibraryEntry("/works/OL2W", "Book2", null, null, Shelf.READ, yearStartMs - 1_000)
          val vm = ProfileViewModel(fakeSettings(), fakeProfile(), fakeSync(), fakeLibrary(listOf(thisYear, lastYear)))
          advanceUntilIdle()
          assertEquals(1, vm.uiState.value.progressCount)
      }
  }
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.profile.ProfileViewModelTest"`
  Expected: compilation error — `ProfileViewModel` and `ProfileIntent` not yet created

- [ ] **Step 3: Implement ProfileViewModel**

  Create `composeApp/src/commonMain/kotlin/com/opclient/profile/presentation/ProfileViewModel.kt`:
  ```kotlin
  package com.opclient.profile.presentation

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.opclient.core.Result
  import com.opclient.library.domain.LibraryRepository
  import com.opclient.library.domain.Shelf
  import com.opclient.profile.domain.UserProfileRepository
  import com.opclient.readinglog.domain.ReadingLogSyncService
  import com.opclient.settings.domain.SettingsRepository
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch
  import kotlinx.datetime.Clock
  import kotlinx.datetime.LocalDateTime
  import kotlinx.datetime.TimeZone
  import kotlinx.datetime.toInstant
  import kotlinx.datetime.toLocalDateTime

  data class ProfileUiState(
      val username: String = "",
      val profileName: String = "",
      val profilePhotoUrl: String? = null,
      val goalTarget: Int = 0,
      val progressCount: Int = 0,
      val isSyncing: Boolean = false,
      val syncError: String? = null,
      val lastSyncEntriesCount: Int? = null,
  )

  sealed class ProfileIntent {
      data class SetUsername(val username: String) : ProfileIntent()
      data object Sync : ProfileIntent()
      data class SetGoal(val target: Int) : ProfileIntent()
      data object ClearGoal : ProfileIntent()
  }

  class ProfileViewModel(
      private val settingsRepository: SettingsRepository,
      private val profileRepository: UserProfileRepository,
      private val syncService: ReadingLogSyncService,
      private val libraryRepository: LibraryRepository,
  ) : ViewModel() {

      private val _uiState = MutableStateFlow(ProfileUiState())
      val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

      init {
          viewModelScope.launch {
              val savedUsername = settingsRepository.getUsername() ?: ""
              val year = currentYear()
              val goal = settingsRepository.getReadingGoal(year)
              _uiState.update { it.copy(username = savedUsername, goalTarget = goal?.target ?: 0) }
              if (savedUsername.isNotEmpty()) fetchProfile(savedUsername)
          }
          viewModelScope.launch {
              libraryRepository.getShelf(Shelf.READ).collect { entries ->
                  val yearStart = yearStartEpoch(currentYear())
                  _uiState.update { it.copy(progressCount = entries.count { it.addedAt >= yearStart }) }
              }
          }
      }

      fun onIntent(intent: ProfileIntent) {
          when (intent) {
              is ProfileIntent.SetUsername -> handleSetUsername(intent.username)
              ProfileIntent.Sync -> handleSync()
              is ProfileIntent.SetGoal -> handleSetGoal(intent.target)
              ProfileIntent.ClearGoal -> handleClearGoal()
          }
      }

      private fun handleSetUsername(username: String) {
          viewModelScope.launch {
              settingsRepository.setUsername(username)
              _uiState.update { it.copy(username = username, profileName = "") }
              if (username.isNotEmpty()) fetchProfile(username)
          }
      }

      private fun handleSync() {
          val username = _uiState.value.username
          if (username.isEmpty()) return
          viewModelScope.launch {
              _uiState.update { it.copy(isSyncing = true, syncError = null) }
              when (val result = syncService.sync(username)) {
                  is Result.Success -> _uiState.update {
                      it.copy(isSyncing = false, lastSyncEntriesCount = result.value)
                  }
                  is Result.Failure -> _uiState.update {
                      it.copy(isSyncing = false, syncError = "Sync failed")
                  }
              }
          }
      }

      private fun handleSetGoal(target: Int) {
          viewModelScope.launch {
              settingsRepository.setReadingGoal(currentYear(), target)
              _uiState.update { it.copy(goalTarget = target) }
          }
      }

      private fun handleClearGoal() {
          viewModelScope.launch {
              settingsRepository.clearReadingGoal(currentYear())
              _uiState.update { it.copy(goalTarget = 0) }
          }
      }

      private fun fetchProfile(username: String) {
          viewModelScope.launch {
              when (val result = profileRepository.getProfile(username)) {
                  is Result.Success -> _uiState.update {
                      it.copy(profileName = result.value.displayName, profilePhotoUrl = result.value.photoUrl)
                  }
                  is Result.Failure -> { /* silent — profile name stays empty */ }
              }
          }
      }

      private fun currentYear(): Int =
          Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year

      private fun yearStartEpoch(year: Int): Long =
          LocalDateTime(year, 1, 1, 0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.profile.ProfileViewModelTest"`
  Expected: 9 tests PASSED

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/profile/presentation/ProfileViewModel.kt \
          composeApp/src/commonTest/kotlin/com/opclient/profile/ProfileViewModelTest.kt
  git commit -m "feat(profile): add ProfileViewModel MVI with TDD"
  ```

---

### Task 9: ProfileScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/profile/presentation/ProfileScreen.kt`

- [ ] **Step 1: Implement ProfileScreen**

  Use `SearchInput` for the username field (already styled with border/background). For the goal input, use `BasicTextField` with the same decoration pattern as `SearchInput`.

  Create `composeApp/src/commonMain/kotlin/com/opclient/profile/presentation/ProfileScreen.kt`:
  ```kotlin
  package com.opclient.profile.presentation

  import androidx.compose.foundation.background
  import androidx.compose.foundation.border
  import androidx.compose.foundation.layout.Arrangement
  import androidx.compose.foundation.layout.Box
  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.Row
  import androidx.compose.foundation.layout.fillMaxSize
  import androidx.compose.foundation.layout.fillMaxWidth
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.layout.size
  import androidx.compose.foundation.rememberScrollState
  import androidx.compose.foundation.shape.CircleShape
  import androidx.compose.foundation.shape.RoundedCornerShape
  import androidx.compose.foundation.text.BasicText
  import androidx.compose.foundation.text.BasicTextField
  import androidx.compose.foundation.text.KeyboardOptions
  import androidx.compose.foundation.verticalScroll
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.collectAsState
  import androidx.compose.runtime.getValue
  import androidx.compose.runtime.mutableStateOf
  import androidx.compose.runtime.remember
  import androidx.compose.runtime.setValue
  import androidx.compose.ui.Alignment
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.draw.clip
  import androidx.compose.ui.graphics.painter.ColorPainter
  import androidx.compose.ui.layout.ContentScale
  import androidx.compose.ui.text.input.KeyboardType
  import androidx.compose.ui.unit.dp
  import coil3.compose.AsyncImage
  import com.opclient.ui.components.PrimaryButton
  import com.opclient.ui.components.SearchInput
  import com.opclient.ui.components.SecondaryButton
  import com.opclient.ui.components.SectionLabel
  import com.opclient.ui.theme.AppShapes
  import com.opclient.ui.theme.AppThemeTokens
  import org.koin.compose.viewmodel.koinViewModel

  @Composable
  fun ProfileScreen(
      onBack: () -> Unit,
      viewModel: ProfileViewModel = koinViewModel(),
  ) {
      val uiState by viewModel.uiState.collectAsState()
      val colors = AppThemeTokens.colors
      val typography = AppThemeTokens.typography

      var usernameInput by remember(uiState.username) { mutableStateOf(uiState.username) }
      var goalInput by remember(uiState.goalTarget) {
          mutableStateOf(if (uiState.goalTarget > 0) "${uiState.goalTarget}" else "")
      }

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
              text = "OPENLIBRARY PROFILE",
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
              SearchInput(
                  value = usernameInput,
                  onValueChange = { usernameInput = it },
                  onSearch = { viewModel.onIntent(ProfileIntent.SetUsername(usernameInput)) },
                  placeholder = "USERNAME",
                  modifier = Modifier.weight(1f),
              )
              SecondaryButton(
                  text = "SAVE",
                  onClick = { viewModel.onIntent(ProfileIntent.SetUsername(usernameInput)) },
              )
          }
          if (uiState.profilePhotoUrl != null || uiState.profileName.isNotEmpty()) {
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 4.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.CenterVertically,
              ) {
                  AsyncImage(
                      model = uiState.profilePhotoUrl,
                      contentDescription = null,
                      contentScale = ContentScale.Crop,
                      placeholder = ColorPainter(colors.surface2),
                      error = ColorPainter(colors.surface2),
                      modifier = Modifier
                          .size(48.dp)
                          .clip(CircleShape),
                  )
                  if (uiState.profileName.isNotEmpty()) {
                      BasicText(
                          text = uiState.profileName,
                          style = typography.bookTitle.copy(color = colors.textPrimary),
                      )
                  }
              }
          }

          SectionLabel(
              text = "SYNC READING LOG",
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
          PrimaryButton(
              text = if (uiState.isSyncing) "SYNCING…" else "SYNC NOW",
              enabled = !uiState.isSyncing && uiState.username.isNotEmpty(),
              onClick = { viewModel.onIntent(ProfileIntent.Sync) },
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
          )
          if (uiState.lastSyncEntriesCount != null) {
              BasicText(
                  text = "Last sync: ${uiState.lastSyncEntriesCount} entries imported",
                  style = typography.bookAuthor.copy(color = colors.textSecondary),
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
              )
          }
          if (uiState.syncError != null) {
              BasicText(
                  text = uiState.syncError,
                  style = typography.bookAuthor.copy(color = colors.textSecondary),
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
              )
          }

          SectionLabel(
              text = "READING GOAL",
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          )
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
          ) {
              val shape = RoundedCornerShape(AppShapes.radius)
              BasicTextField(
                  value = goalInput,
                  onValueChange = { goalInput = it.filter { c -> c.isDigit() } },
                  modifier = Modifier.weight(1f),
                  singleLine = true,
                  textStyle = typography.body.copy(color = colors.textPrimary),
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                  decorationBox = { innerTextField ->
                      Box(
                          modifier = Modifier
                              .background(color = colors.surface, shape = shape)
                              .border(width = 1.dp, color = colors.border, shape = shape)
                              .padding(horizontal = 12.dp, vertical = 8.dp),
                          contentAlignment = Alignment.CenterStart,
                      ) {
                          if (goalInput.isEmpty()) {
                              BasicText(
                                  text = "BOOKS THIS YEAR",
                                  style = typography.body.copy(color = colors.textSecondary),
                              )
                          }
                          innerTextField()
                      }
                  },
              )
              SecondaryButton(
                  text = "SET",
                  onClick = {
                      val target = goalInput.toIntOrNull() ?: 0
                      if (target > 0) viewModel.onIntent(ProfileIntent.SetGoal(target))
                  },
              )
              SecondaryButton(
                  text = "CLEAR",
                  onClick = { viewModel.onIntent(ProfileIntent.ClearGoal) },
              )
          }
          if (uiState.goalTarget > 0) {
              BasicText(
                  text = "${uiState.progressCount} / ${uiState.goalTarget} books read this year",
                  style = typography.bookAuthor.copy(color = colors.textPrimary),
                  modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
              )
          }
      }
  }
  ```

- [ ] **Step 2: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/profile/presentation/ProfileScreen.kt
  git commit -m "feat(profile): add ProfileScreen with username, sync, and reading goal UI"
  ```

---

### Task 10: Navigation

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/library/presentation/LibraryScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/App.kt`

- [ ] **Step 1: Add Screen.Profile**

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
  }
  ```

- [ ] **Step 2: Add profile button to LibraryScreen**

  In `composeApp/src/commonMain/kotlin/com/opclient/library/presentation/LibraryScreen.kt`, add `onProfileClick` parameter and a profile button.

  Change the composable signature from:
  ```kotlin
  @Composable
  fun LibraryScreen(
      onBookClick: (String) -> Unit,
      viewModel: LibraryViewModel = koinViewModel(),
  ) {
  ```
  To:
  ```kotlin
  @Composable
  fun LibraryScreen(
      onBookClick: (String) -> Unit,
      onProfileClick: () -> Unit,
      viewModel: LibraryViewModel = koinViewModel(),
  ) {
  ```

  Add imports at the top of the file:
  ```kotlin
  import androidx.compose.foundation.layout.fillMaxWidth
  import com.opclient.ui.components.SecondaryButton
  ```

  After the opening `Column(modifier = Modifier.fillMaxSize()) {`, add a profile row before the shelf filter chips row:
  ```kotlin
  Row(
      modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.End,
  ) {
      SecondaryButton(text = "PROFILE & SYNC", onClick = onProfileClick)
  }
  ```

- [ ] **Step 3: Update App.kt to wire Screen.Profile and pass onProfileClick**

  In `composeApp/src/commonMain/kotlin/com/opclient/App.kt`, add the import:
  ```kotlin
  import com.opclient.profile.presentation.ProfileScreen
  ```

  Change the `Screen.Library` branch from:
  ```kotlin
  Screen.Library -> LibraryScreen(
      onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
  )
  ```
  To:
  ```kotlin
  Screen.Library -> LibraryScreen(
      onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
      onProfileClick = { navVm.navigateTo(Screen.Profile) },
  )
  ```

  Add the `Screen.Profile` branch to the `when` block:
  ```kotlin
  Screen.Profile -> ProfileScreen(
      onBack = { navVm.navigateBack() },
  )
  ```

- [ ] **Step 4: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt \
          composeApp/src/commonMain/kotlin/com/opclient/library/presentation/LibraryScreen.kt \
          composeApp/src/commonMain/kotlin/com/opclient/App.kt
  git commit -m "feat(profile): add Screen.Profile and wire navigation from LibraryScreen"
  ```

---

### Task 11: Koin Wiring

**Files:**
- Modify: `composeApp/src/androidMain/kotlin/com/opclient/di/AndroidModule.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/opclient/di/DesktopModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/di/SettingsModule.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/di/ProfileModule.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`

- [ ] **Step 1: Add settings SqlDriver to AndroidModule.kt**

  In `composeApp/src/androidMain/kotlin/com/opclient/di/AndroidModule.kt`, the file currently binds an unnamed `SqlDriver` for the library database. Add a second binding for settings using Koin's `named("settings")` qualifier.

  Full replacement of `AndroidModule.kt`:
  ```kotlin
  package com.opclient.di

  import app.cash.sqldelight.db.SqlDriver
  import app.cash.sqldelight.driver.android.AndroidSqliteDriver
  import com.opclient.library.LibraryDatabase
  import com.opclient.settings.SettingsDatabase
  import org.koin.android.ext.koin.androidContext
  import org.koin.core.module.Module
  import org.koin.core.qualifier.named
  import org.koin.dsl.module

  val androidModule: Module = module {
      single<SqlDriver> {
          AndroidSqliteDriver(LibraryDatabase.Schema, androidContext(), "library.db")
      }
      single<SqlDriver>(named("settings")) {
          AndroidSqliteDriver(SettingsDatabase.Schema, androidContext(), "settings.db")
      }
  }
  ```

- [ ] **Step 2: Add settings SqlDriver to DesktopModule.kt**

  Full replacement of `composeApp/src/jvmMain/kotlin/com/opclient/di/DesktopModule.kt`:
  ```kotlin
  package com.opclient.di

  import app.cash.sqldelight.db.SqlDriver
  import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
  import com.opclient.library.LibraryDatabase
  import com.opclient.settings.SettingsDatabase
  import org.koin.core.module.Module
  import org.koin.core.qualifier.named
  import org.koin.dsl.module

  val desktopModule: Module = module {
      single<SqlDriver> {
          JdbcSqliteDriver("jdbc:sqlite:library.db").also { LibraryDatabase.Schema.create(it) }
      }
      single<SqlDriver>(named("settings")) {
          JdbcSqliteDriver("jdbc:sqlite:settings.db").also { SettingsDatabase.Schema.create(it) }
      }
  }
  ```

- [ ] **Step 3: Create SettingsModule.kt**

  Create `composeApp/src/commonMain/kotlin/com/opclient/di/SettingsModule.kt`:
  ```kotlin
  package com.opclient.di

  import com.opclient.settings.SettingsDatabase
  import com.opclient.settings.data.SettingsRepositoryImpl
  import com.opclient.settings.domain.SettingsRepository
  import org.koin.core.module.Module
  import org.koin.core.qualifier.named
  import org.koin.dsl.module

  val settingsModule: Module = module {
      single { SettingsDatabase(get(named("settings"))) }
      single<SettingsRepository> { SettingsRepositoryImpl(get()) }
  }
  ```

- [ ] **Step 4: Create ProfileModule.kt**

  `profileModule` must load after `libraryModule` in the module list because it depends on `LibraryRepository`. Koin resolves lazily so order in the list doesn't matter for initialization, but the ordering convention makes intent clear.

  Create `composeApp/src/commonMain/kotlin/com/opclient/di/ProfileModule.kt`:
  ```kotlin
  package com.opclient.di

  import com.opclient.profile.data.ProfileApiClient
  import com.opclient.profile.data.UserProfileRepositoryImpl
  import com.opclient.profile.domain.UserProfileRepository
  import com.opclient.profile.presentation.ProfileViewModel
  import com.opclient.readinglog.data.ReadingLogApiClient
  import com.opclient.readinglog.data.ReadingLogSyncServiceImpl
  import com.opclient.readinglog.domain.ReadingLogSyncService
  import org.koin.core.module.Module
  import org.koin.core.module.dsl.viewModel
  import org.koin.dsl.module

  val profileModule: Module = module {
      factory { ProfileApiClient(get()) }
      single<UserProfileRepository> { UserProfileRepositoryImpl(apiClient = get()) }
      factory { ReadingLogApiClient(get()) }
      single<ReadingLogSyncService> { ReadingLogSyncServiceImpl(apiClient = get(), libraryRepository = get()) }
      viewModel { ProfileViewModel(settingsRepository = get(), profileRepository = get(), syncService = get(), libraryRepository = get()) }
  }
  ```

- [ ] **Step 5: Update OpClientApplication.kt**

  In `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`, add the new imports and modules:
  ```kotlin
  import com.opclient.di.profileModule
  import com.opclient.di.settingsModule
  ```

  Change the `modules(...)` call to:
  ```kotlin
  modules(commonModule, androidModule, settingsModule, searchModule, authorModule, subjectModule, libraryModule, bookModule, profileModule)
  ```

- [ ] **Step 6: Update Main.kt**

  In `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`, add the new imports and modules:
  ```kotlin
  import com.opclient.di.profileModule
  import com.opclient.di.settingsModule
  ```

  Change the `modules(...)` call to:
  ```kotlin
  modules(commonModule, desktopModule, settingsModule, searchModule, authorModule, subjectModule, libraryModule, bookModule, profileModule)
  ```

- [ ] **Step 7: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

  ```bash
  git add composeApp/src/androidMain/kotlin/com/opclient/di/AndroidModule.kt \
          composeApp/src/jvmMain/kotlin/com/opclient/di/DesktopModule.kt \
          composeApp/src/commonMain/kotlin/com/opclient/di/SettingsModule.kt \
          composeApp/src/commonMain/kotlin/com/opclient/di/ProfileModule.kt \
          composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt \
          composeApp/src/jvmMain/kotlin/com/opclient/Main.kt
  git commit -m "feat(profile): add SettingsModule and ProfileModule Koin wiring"
  ```

---

### Task 12: Full Test Suite + Live Verification

**Files:** none

- [ ] **Step 1: Run full test suite**

  Run: `./gradlew :composeApp:jvmTest`
  Expected: all tests PASSED

  If any test fails, fix it before continuing.

- [ ] **Step 2: Launch desktop app**

  Run: `./gradlew :composeApp:run &`

  Verify the following golden paths:

  1. LIBRARY tab shows a "PROFILE & SYNC" button in the top-right area
  2. Tap "PROFILE & SYNC" → navigates to ProfileScreen
  3. Back button returns to LibraryScreen
  4. Enter a valid OpenLibrary username (e.g. `mark`) and tap SAVE → profile name appears below the input field
  5. Tap "SYNC NOW" → button shows "SYNCING…" briefly, then "X entries imported" appears
  6. After sync, LibraryScreen shelves contain the synced books from OpenLibrary
  7. Enter a number in the reading goal input and tap SET → goal is saved; "X / Y books read this year" appears
  8. Tap CLEAR → goal text disappears
  9. Navigate to a synced book → BookDetailScreen shows the book's current shelf highlighted as PrimaryButton
  10. Username and goal persist after closing and reopening the app (SettingsDatabase is file-based)

- [ ] **Step 3: Final commit (if any fixes needed after live test)**

  ```bash
  git add -p  # stage only intentional fixes
  git commit -m "fix(profile): address issues found during live verification"
  ```
