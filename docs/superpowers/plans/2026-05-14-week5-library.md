# Week 5 — Library Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a personal reading library with WANT_TO_READ / READING / READ shelves, persisted via SQLDelight, accessible from BookDetailScreen shelf buttons, and browsable in a dedicated LIBRARY tab.

**Architecture:** SQLDelight 2.0.2 reactive Flow queries power `LibraryRepositoryImpl`; `LibraryViewModel` subscribes to shelf flows; `BookDetailViewModel` gains a third constructor param (`LibraryRepository`) and exposes `currentShelf: Shelf?` state plus two new intents (`SetShelf`, `RemoveFromLibrary`). Platform-specific `SqlDriver` bindings live in `androidModule` and `desktopModule`. `libraryModule` must load before `bookModule`.

**Tech Stack:** SQLDelight 2.0.2, `coroutines-extensions` (Flow queries), `AndroidSqliteDriver` (Android), `JdbcSqliteDriver` (Desktop + JVM tests), `kotlinx-datetime` (`Clock.System`), Koin, Turbine.

---

### Task 1: Gradle Setup

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `composeApp/build.gradle.kts`

- [ ] **Step 1: Add SQLDelight entries to version catalog**

  In `gradle/libs.versions.toml`, add to `[versions]`:
  ```toml
  sqldelight = "2.0.2"
  ```

  Add to `[libraries]`:
  ```toml
  sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
  sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
  sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
  ```

  Add to `[plugins]`:
  ```toml
  sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
  ```

- [ ] **Step 2: Apply plugin, add dependencies, add sqldelight block**

  In `composeApp/build.gradle.kts`:

  Add to `plugins` block (after `alias(libs.plugins.ktlint)`):
  ```kotlin
  alias(libs.plugins.sqldelight)
  ```

  Add to `commonMain.dependencies`:
  ```kotlin
  implementation(libs.sqldelight.coroutines)
  ```

  Add to `androidMain.dependencies`:
  ```kotlin
  implementation(libs.sqldelight.android.driver)
  ```

  Add to `jvmMain.dependencies`:
  ```kotlin
  implementation(libs.sqldelight.sqlite.driver)
  ```

  Add `jvmTest.dependencies` block (inside the `sourceSets {}` block, after `androidUnitTest.dependencies`):
  ```kotlin
  jvmTest.dependencies {
      implementation(libs.sqldelight.sqlite.driver)
  }
  ```

  Add `sqldelight {}` configuration block at the top level of the file (after the closing `}` of the `kotlin {}` block):
  ```kotlin
  sqldelight {
      databases {
          create("LibraryDatabase") {
              packageName.set("com.opclient.library")
          }
      }
  }
  ```

- [ ] **Step 3: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

  ```bash
  git add gradle/libs.versions.toml composeApp/build.gradle.kts
  git commit -m "build: add SQLDelight 2.0.2 to Gradle configuration"
  ```

---

### Task 2: SQLDelight Schema

**Files:**
- Create: `composeApp/src/commonMain/sqldelight/com/opclient/library/Library.sq`

- [ ] **Step 1: Create schema file**

  Create directory and file `composeApp/src/commonMain/sqldelight/com/opclient/library/Library.sq`:

  ```sql
  CREATE TABLE LibraryEntry (
      work_key    TEXT    NOT NULL PRIMARY KEY,
      title       TEXT    NOT NULL,
      author_name TEXT,
      cover_url   TEXT,
      shelf       TEXT    NOT NULL,
      added_at    INTEGER NOT NULL
  );

  getShelf:
  SELECT * FROM LibraryEntry WHERE shelf = ? ORDER BY added_at DESC;

  getShelfForBook:
  SELECT shelf FROM LibraryEntry WHERE work_key = ?;

  upsertEntry:
  INSERT OR REPLACE INTO LibraryEntry VALUES (?, ?, ?, ?, ?, ?);

  removeEntry:
  DELETE FROM LibraryEntry WHERE work_key = ?;

  updateShelf:
  UPDATE LibraryEntry SET shelf = ? WHERE work_key = ?;
  ```

- [ ] **Step 2: Trigger code generation and verify**

  Run: `./gradlew :composeApp:generateCommonMainLibraryDatabaseInterface`
  Expected: `BUILD SUCCESSFUL`

  Run: `find composeApp/build -name "LibraryDatabase.kt" 2>/dev/null | head -3`
  Expected: at least one path printed (the generated `LibraryDatabase.kt`)

- [ ] **Step 3: Commit**

  ```bash
  git add composeApp/src/commonMain/sqldelight/
  git commit -m "feat(library): add SQLDelight LibraryEntry schema"
  ```

---

### Task 3: Domain Models

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/library/domain/LibraryModels.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/library/domain/LibraryRepository.kt`

- [ ] **Step 1: Create LibraryModels.kt**

  ```kotlin
  package com.opclient.library.domain

  enum class Shelf(val label: String) {
      WANT_TO_READ("Want to Read"),
      READING("Reading"),
      READ("Read"),
  }

  data class LibraryEntry(
      val workKey: String,
      val title: String,
      val authorName: String?,
      val coverUrl: String?,
      val shelf: Shelf,
      val addedAt: Long,
  )
  ```

- [ ] **Step 2: Create LibraryRepository.kt**

  ```kotlin
  package com.opclient.library.domain

  import kotlinx.coroutines.flow.Flow

  interface LibraryRepository {
      fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>>
      fun getCurrentShelf(workKey: String): Flow<Shelf?>
      suspend fun addToShelf(entry: LibraryEntry)
      suspend fun removeFromShelf(workKey: String)
      suspend fun moveToShelf(workKey: String, shelf: Shelf)
  }
  ```

- [ ] **Step 3: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/library/domain/
  git commit -m "feat(library): add domain models Shelf, LibraryEntry, LibraryRepository"
  ```

---

### Task 4: LibraryRepositoryImpl (TDD)

**Files:**
- Create: `composeApp/src/jvmTest/kotlin/com/opclient/library/LibraryRepositoryTest.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/library/data/LibraryRepositoryImpl.kt`

Note: `LibraryRepositoryTest` lives in `jvmTest` (not `commonTest`) because it imports `JdbcSqliteDriver`, which is JVM-only.

- [ ] **Step 1: Write failing tests**

  Create `composeApp/src/jvmTest/kotlin/com/opclient/library/LibraryRepositoryTest.kt`:

  ```kotlin
  package com.opclient.library

  import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
  import app.cash.turbine.test
  import com.opclient.library.data.LibraryRepositoryImpl
  import com.opclient.library.domain.LibraryEntry
  import com.opclient.library.domain.LibraryRepository
  import com.opclient.library.domain.Shelf
  import kotlinx.coroutines.test.runTest
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertNull

  class LibraryRepositoryTest {

      private fun createRepo(): LibraryRepository {
          val driver = JdbcSqliteDriver("jdbc:sqlite::memory:")
          LibraryDatabase.Schema.create(driver)
          val db = LibraryDatabase(driver)
          return LibraryRepositoryImpl(db)
      }

      private fun entry(
          workKey: String = "/works/OL1W",
          title: String = "Test Book",
          shelf: Shelf = Shelf.WANT_TO_READ,
          addedAt: Long = 1000L,
      ) = LibraryEntry(
          workKey = workKey,
          title = title,
          authorName = "Test Author",
          coverUrl = null,
          shelf = shelf,
          addedAt = addedAt,
      )

      @Test
      fun addToShelf_persistsEntry() = runTest {
          val repo = createRepo()
          repo.addToShelf(entry())
          repo.getShelf(Shelf.WANT_TO_READ).test {
              val list = awaitItem()
              assertEquals(1, list.size)
              assertEquals("/works/OL1W", list.first().workKey)
              cancelAndIgnoreRemainingEvents()
          }
      }

      @Test
      fun getShelf_emitsOnChange() = runTest {
          val repo = createRepo()
          repo.getShelf(Shelf.WANT_TO_READ).test {
              assertEquals(emptyList(), awaitItem())
              repo.addToShelf(entry())
              val list = awaitItem()
              assertEquals(1, list.size)
              cancelAndIgnoreRemainingEvents()
          }
      }

      @Test
      fun moveToShelf_updatesShelf() = runTest {
          val repo = createRepo()
          repo.addToShelf(entry(shelf = Shelf.WANT_TO_READ))
          repo.moveToShelf("/works/OL1W", Shelf.READING)
          repo.getShelf(Shelf.READING).test {
              val list = awaitItem()
              assertEquals(1, list.size)
              assertEquals(Shelf.READING, list.first().shelf)
              cancelAndIgnoreRemainingEvents()
          }
      }

      @Test
      fun removeFromShelf_deletesEntry() = runTest {
          val repo = createRepo()
          repo.addToShelf(entry())
          repo.removeFromShelf("/works/OL1W")
          repo.getShelf(Shelf.WANT_TO_READ).test {
              assertEquals(emptyList(), awaitItem())
              cancelAndIgnoreRemainingEvents()
          }
      }

      @Test
      fun getCurrentShelf_nullForUnknownKey() = runTest {
          val repo = createRepo()
          repo.getCurrentShelf("/works/UNKNOWN").test {
              assertNull(awaitItem())
              cancelAndIgnoreRemainingEvents()
          }
      }

      @Test
      fun getShelf_isolatesEntriesByShelf() = runTest {
          val repo = createRepo()
          repo.addToShelf(entry(workKey = "/works/OL1W", shelf = Shelf.READING))
          repo.getShelf(Shelf.WANT_TO_READ).test {
              assertEquals(emptyList(), awaitItem())
              cancelAndIgnoreRemainingEvents()
          }
      }
  }
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.library.LibraryRepositoryTest"`
  Expected: compilation error — `LibraryRepositoryImpl` not yet created

- [ ] **Step 3: Implement LibraryRepositoryImpl**

  Create `composeApp/src/commonMain/kotlin/com/opclient/library/data/LibraryRepositoryImpl.kt`:

  ```kotlin
  package com.opclient.library.data

  import app.cash.sqldelight.coroutines.asFlow
  import app.cash.sqldelight.coroutines.mapToList
  import app.cash.sqldelight.coroutines.mapToOneOrNull
  import com.opclient.library.LibraryDatabase
  import com.opclient.library.domain.LibraryEntry
  import com.opclient.library.domain.LibraryRepository
  import com.opclient.library.domain.Shelf
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.map
  import kotlinx.coroutines.withContext

  class LibraryRepositoryImpl(private val db: LibraryDatabase) : LibraryRepository {

      override fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>> =
          db.libraryQueries.getShelf(shelf.name)
              .asFlow()
              .mapToList(Dispatchers.IO)
              .map { rows -> rows.map { it.toDomain() } }

      override fun getCurrentShelf(workKey: String): Flow<Shelf?> =
          db.libraryQueries.getShelfForBook(workKey)
              .asFlow()
              .mapToOneOrNull(Dispatchers.IO)
              .map { row -> row?.shelf?.let { Shelf.valueOf(it) } }

      override suspend fun addToShelf(entry: LibraryEntry) = withContext(Dispatchers.IO) {
          db.libraryQueries.upsertEntry(
              entry.workKey, entry.title, entry.authorName, entry.coverUrl,
              entry.shelf.name, entry.addedAt,
          )
      }

      override suspend fun removeFromShelf(workKey: String) = withContext(Dispatchers.IO) {
          db.libraryQueries.removeEntry(workKey)
      }

      override suspend fun moveToShelf(workKey: String, shelf: Shelf) = withContext(Dispatchers.IO) {
          db.libraryQueries.updateShelf(shelf.name, workKey)
      }
  }

  private fun com.opclient.library.LibraryEntry.toDomain() = LibraryEntry(
      workKey = work_key,
      title = title,
      authorName = author_name,
      coverUrl = cover_url,
      shelf = Shelf.valueOf(shelf),
      addedAt = added_at,
  )
  ```

- [ ] **Step 4: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.library.LibraryRepositoryTest"`
  Expected: 6 tests PASSED

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/jvmTest/kotlin/com/opclient/library/LibraryRepositoryTest.kt \
          composeApp/src/commonMain/kotlin/com/opclient/library/data/LibraryRepositoryImpl.kt
  git commit -m "feat(library): add LibraryRepositoryImpl with TDD"
  ```

---

### Task 5: LibraryViewModel (TDD)

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/opclient/library/LibraryViewModelTest.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/library/presentation/LibraryViewModel.kt`

- [ ] **Step 1: Write failing tests**

  Create `composeApp/src/commonTest/kotlin/com/opclient/library/LibraryViewModelTest.kt`:

  ```kotlin
  package com.opclient.library

  import app.cash.turbine.test
  import com.opclient.library.domain.LibraryEntry
  import com.opclient.library.domain.LibraryRepository
  import com.opclient.library.domain.Shelf
  import com.opclient.library.presentation.LibraryIntent
  import com.opclient.library.presentation.LibraryViewModel
  import kotlinx.coroutines.Dispatchers
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.map
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.test.UnconfinedTestDispatcher
  import kotlinx.coroutines.test.resetMain
  import kotlinx.coroutines.test.runTest
  import kotlinx.coroutines.test.setMain
  import kotlin.test.AfterTest
  import kotlin.test.BeforeTest
  import kotlin.test.Test
  import kotlin.test.assertEquals
  import kotlin.test.assertTrue

  class LibraryViewModelTest {

      private val testDispatcher = UnconfinedTestDispatcher()

      @BeforeTest fun setUp() { Dispatchers.setMain(testDispatcher) }
      @AfterTest fun tearDown() { Dispatchers.resetMain() }

      private fun fakeRepo() = object : LibraryRepository {
          val shelves = MutableStateFlow<Map<String, Shelf>>(emptyMap())

          override fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>> =
              shelves.map { map ->
                  map.entries
                      .filter { it.value == shelf }
                      .map { (key, shelfValue) ->
                          LibraryEntry(
                              workKey = key, title = "Book $key",
                              authorName = null, coverUrl = null,
                              shelf = shelfValue, addedAt = 0L,
                          )
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

      @Test
      fun initialState_isWantToRead_emptyEntries() = runTest {
          val vm = LibraryViewModel(fakeRepo())
          assertEquals(Shelf.WANT_TO_READ, vm.uiState.value.selectedShelf)
          assertTrue(vm.uiState.value.entries.isEmpty())
      }

      @Test
      fun selectShelf_switchesSubscription() = runTest {
          val repo = fakeRepo()
          repo.shelves.value = mapOf("/works/OL1W" to Shelf.READING)
          val vm = LibraryViewModel(repo)

          vm.onIntent(LibraryIntent.SelectShelf(Shelf.READING))

          vm.uiState.test {
              val state = awaitItem()
              assertEquals(Shelf.READING, state.selectedShelf)
              assertEquals(1, state.entries.size)
              cancelAndIgnoreRemainingEvents()
          }
      }

      @Test
      fun entries_reflectFakeRepoFlow() = runTest {
          val repo = fakeRepo()
          val vm = LibraryViewModel(repo)

          vm.uiState.test {
              awaitItem() // initial empty state

              repo.shelves.value = mapOf("/works/OL1W" to Shelf.WANT_TO_READ)
              val updated = awaitItem()
              assertEquals(1, updated.entries.size)
              assertEquals("/works/OL1W", updated.entries.first().workKey)
              cancelAndIgnoreRemainingEvents()
          }
      }

      @Test
      fun selectShelf_cancelsPreviousJob() = runTest {
          val repo = fakeRepo()
          repo.shelves.value = mapOf("/works/OL1W" to Shelf.WANT_TO_READ)
          val vm = LibraryViewModel(repo)

          vm.onIntent(LibraryIntent.SelectShelf(Shelf.READ))

          // Switched to READ shelf — previous WANT_TO_READ job cancelled
          assertEquals(Shelf.READ, vm.uiState.value.selectedShelf)
          assertTrue(vm.uiState.value.entries.isEmpty())
      }
  }
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.library.LibraryViewModelTest"`
  Expected: compilation error — `LibraryViewModel` and `LibraryIntent` not yet created

- [ ] **Step 3: Implement LibraryViewModel**

  Create `composeApp/src/commonMain/kotlin/com/opclient/library/presentation/LibraryViewModel.kt`:

  ```kotlin
  package com.opclient.library.presentation

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.opclient.library.domain.LibraryEntry
  import com.opclient.library.domain.LibraryRepository
  import com.opclient.library.domain.Shelf
  import kotlinx.coroutines.Job
  import kotlinx.coroutines.flow.MutableStateFlow
  import kotlinx.coroutines.flow.StateFlow
  import kotlinx.coroutines.flow.asStateFlow
  import kotlinx.coroutines.flow.update
  import kotlinx.coroutines.launch

  data class LibraryUiState(
      val selectedShelf: Shelf = Shelf.WANT_TO_READ,
      val entries: List<LibraryEntry> = emptyList(),
  )

  sealed class LibraryIntent {
      data class SelectShelf(val shelf: Shelf) : LibraryIntent()
  }

  class LibraryViewModel(private val repository: LibraryRepository) : ViewModel() {
      private val _uiState = MutableStateFlow(LibraryUiState())
      val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

      private var shelfJob: Job? = null

      init {
          collectShelf(Shelf.WANT_TO_READ)
      }

      fun onIntent(intent: LibraryIntent) {
          when (intent) {
              is LibraryIntent.SelectShelf -> {
                  _uiState.update { it.copy(selectedShelf = intent.shelf) }
                  collectShelf(intent.shelf)
              }
          }
      }

      private fun collectShelf(shelf: Shelf) {
          shelfJob?.cancel()
          shelfJob = viewModelScope.launch {
              repository.getShelf(shelf).collect { entries ->
                  _uiState.update { it.copy(entries = entries) }
              }
          }
      }
  }
  ```

- [ ] **Step 4: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.library.LibraryViewModelTest"`
  Expected: 4 tests PASSED

- [ ] **Step 5: Commit**

  ```bash
  git add composeApp/src/commonTest/kotlin/com/opclient/library/LibraryViewModelTest.kt \
          composeApp/src/commonMain/kotlin/com/opclient/library/presentation/LibraryViewModel.kt
  git commit -m "feat(library): add LibraryViewModel MVI with TDD"
  ```

---

### Task 6: Screen.Library + Navigation Update

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/opclient/navigation/NavigationViewModelTest.kt`

- [ ] **Step 1: Update NavigationViewModelTest first (write failing test)**

  In `NavigationViewModelTest.kt`, rename `navigateToTab_library_resetsToSearch` to `navigateToTab_library_resetsToLibrary` and change its expected value:

  ```kotlin
  @Test
  fun navigateToTab_library_resetsToLibrary() = runTest {
      val vm = NavigationViewModel()
      vm.navigateTo(Screen.BookDetail("/works/OL1W"))
      vm.navigateToTab(Destination.LIBRARY)
      assertEquals(listOf(Screen.Library), vm.stack.value)
  }
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.navigation.NavigationViewModelTest.navigateToTab_library_resetsToLibrary"`
  Expected: compilation error — `Screen.Library` not defined

- [ ] **Step 3: Add Screen.Library to Screen.kt**

  Full file `composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt`:

  ```kotlin
  package com.opclient.navigation

  sealed class Screen {
      data object Search : Screen()
      data class BookDetail(val workKey: String) : Screen()
      data class AuthorDetail(val authorKey: String) : Screen()
      data object SubjectList : Screen()
      data class SubjectDetail(val subjectName: String) : Screen()
      data object Library : Screen()
  }
  ```

- [ ] **Step 4: Update NavigationViewModel LIBRARY branch**

  In `composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt`, change the LIBRARY case:

  ```kotlin
  fun navigateToTab(destination: Destination) {
      _stack.update {
          when (destination) {
              Destination.SEARCH -> listOf(Screen.Search)
              Destination.BROWSE -> listOf(Screen.SubjectList)
              Destination.LIBRARY -> listOf(Screen.Library)
          }
      }
  }
  ```

- [ ] **Step 5: Run all navigation tests**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.navigation.NavigationViewModelTest"`
  Expected: all 8 tests PASSED

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/navigation/Screen.kt \
          composeApp/src/commonMain/kotlin/com/opclient/navigation/NavigationViewModel.kt \
          composeApp/src/commonTest/kotlin/com/opclient/navigation/NavigationViewModelTest.kt
  git commit -m "feat(library): add Screen.Library and wire LIBRARY tab navigation"
  ```

---

### Task 7: Koin Wiring

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/di/LibraryModule.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/opclient/di/AndroidModule.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/opclient/di/DesktopModule.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`

Note: `BookModule.kt` change (`get(), get(), get()`) is deferred to Task 10, when `BookDetailViewModel` actually gains its third param.

- [ ] **Step 1: Create LibraryModule.kt**

  ```kotlin
  package com.opclient.di

  import com.opclient.library.LibraryDatabase
  import com.opclient.library.data.LibraryRepositoryImpl
  import com.opclient.library.domain.LibraryRepository
  import com.opclient.library.presentation.LibraryViewModel
  import org.koin.core.module.Module
  import org.koin.core.module.dsl.viewModel
  import org.koin.dsl.module

  val libraryModule: Module = module {
      single { LibraryDatabase(get()) }
      single<LibraryRepository> { LibraryRepositoryImpl(get()) }
      viewModel { LibraryViewModel(get()) }
  }
  ```

- [ ] **Step 2: Add SqlDriver to AndroidModule.kt**

  ```kotlin
  package com.opclient.di

  import app.cash.sqldelight.db.SqlDriver
  import app.cash.sqldelight.driver.android.AndroidSqliteDriver
  import com.opclient.library.LibraryDatabase
  import org.koin.android.ext.koin.androidContext
  import org.koin.core.module.Module
  import org.koin.dsl.module

  val androidModule: Module = module {
      single<SqlDriver> {
          AndroidSqliteDriver(LibraryDatabase.Schema, androidContext(), "library.db")
      }
  }
  ```

- [ ] **Step 3: Add SqlDriver to DesktopModule.kt**

  ```kotlin
  package com.opclient.di

  import app.cash.sqldelight.db.SqlDriver
  import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
  import com.opclient.library.LibraryDatabase
  import org.koin.core.module.Module
  import org.koin.dsl.module

  val desktopModule: Module = module {
      single<SqlDriver> {
          JdbcSqliteDriver("jdbc:sqlite:library.db").also {
              LibraryDatabase.Schema.create(it)
          }
      }
  }
  ```

- [ ] **Step 4: Update OpClientApplication.kt module order**

  In `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`, change:

  ```kotlin
  modules(commonModule, androidModule, searchModule, authorModule, subjectModule, libraryModule, bookModule)
  ```

  Add the missing import:
  ```kotlin
  import com.opclient.di.libraryModule
  ```

- [ ] **Step 5: Update Main.kt module order**

  In `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`, change:

  ```kotlin
  modules(commonModule, desktopModule, searchModule, authorModule, subjectModule, libraryModule, bookModule)
  ```

  Add the missing import:
  ```kotlin
  import com.opclient.di.libraryModule
  ```

- [ ] **Step 6: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/di/LibraryModule.kt \
          composeApp/src/androidMain/kotlin/com/opclient/di/AndroidModule.kt \
          composeApp/src/jvmMain/kotlin/com/opclient/di/DesktopModule.kt \
          composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt \
          composeApp/src/jvmMain/kotlin/com/opclient/Main.kt
  git commit -m "feat(library): add LibraryModule and platform SqlDriver bindings"
  ```

---

### Task 8: LibraryScreen

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/library/presentation/LibraryScreen.kt`

- [ ] **Step 1: Implement LibraryScreen**

  ```kotlin
  package com.opclient.library.presentation

  import androidx.compose.foundation.layout.Arrangement
  import androidx.compose.foundation.layout.Column
  import androidx.compose.foundation.layout.Row
  import androidx.compose.foundation.layout.fillMaxSize
  import androidx.compose.foundation.layout.fillMaxWidth
  import androidx.compose.foundation.layout.padding
  import androidx.compose.foundation.lazy.LazyColumn
  import androidx.compose.foundation.lazy.items
  import androidx.compose.runtime.Composable
  import androidx.compose.runtime.collectAsState
  import androidx.compose.runtime.getValue
  import androidx.compose.ui.Modifier
  import androidx.compose.ui.unit.dp
  import com.opclient.library.domain.Shelf
  import com.opclient.ui.components.BookRow
  import com.opclient.ui.components.FilterChip
  import com.opclient.ui.components.feedback.EmptyState
  import org.koin.compose.viewmodel.koinViewModel

  @Composable
  fun LibraryScreen(
      onBookClick: (String) -> Unit,
      viewModel: LibraryViewModel = koinViewModel(),
  ) {
      val uiState by viewModel.uiState.collectAsState()

      Column(modifier = Modifier.fillMaxSize()) {
          Row(
              modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
              Shelf.entries.forEach { shelf ->
                  FilterChip(
                      label = shelf.label,
                      selected = uiState.selectedShelf == shelf,
                      onClick = { viewModel.onIntent(LibraryIntent.SelectShelf(shelf)) },
                  )
              }
          }

          if (uiState.entries.isEmpty()) {
              EmptyState(message = "No books here yet")
          } else {
              LazyColumn {
                  items(uiState.entries, key = { it.workKey }) { entry ->
                      BookRow(
                          title = entry.title,
                          author = entry.authorName ?: "",
                          onClick = { onBookClick(entry.workKey) },
                      )
                  }
              }
          }
      }
  }
  ```

  Note: `BookRow` may require a `coverContent` lambda — check the existing `BookRow` signature in `composeApp/src/commonMain/kotlin/com/opclient/ui/components/BookRow.kt`. If `coverContent` is required, provide a default empty placeholder:
  ```kotlin
  coverContent = {},
  ```

- [ ] **Step 2: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/library/presentation/LibraryScreen.kt
  git commit -m "feat(library): add LibraryScreen with shelf tabs and book list"
  ```

---

### Task 9: App.kt Routing

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/App.kt`

- [ ] **Step 1: Add Screen.Library import and route**

  In `composeApp/src/commonMain/kotlin/com/opclient/App.kt`, add the `LibraryScreen` import and add the `Screen.Library` branch to the `when` expression:

  ```kotlin
  import com.opclient.library.presentation.LibraryScreen
  ```

  Add to the `when (val screen = stack.last())` block:
  ```kotlin
  Screen.Library -> LibraryScreen(
      onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
  )
  ```

  The complete `when` block becomes:
  ```kotlin
  when (val screen = stack.last()) {
      Screen.Search -> SearchScreen(
          onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
      )
      is Screen.BookDetail -> BookDetailScreen(
          workKey = screen.workKey,
          onAuthorClick = { key -> navVm.navigateTo(Screen.AuthorDetail(key)) },
          onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
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
      Screen.Library -> LibraryScreen(
          onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
      )
  }
  ```

- [ ] **Step 2: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/App.kt
  git commit -m "feat(library): route Screen.Library in App.kt"
  ```

---

### Task 10: BookDetailViewModel Update (TDD)

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/com/opclient/book/BookDetailViewModelTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/di/BookModule.kt`

- [ ] **Step 1: Write new failing tests in BookDetailViewModelTest**

  The existing tests use `BookDetailViewModel(successRepo(), emptySubjectRepo())` — these will all break once the constructor gains a third param. Update `BookDetailViewModelTest.kt` to the full new version:

  ```kotlin
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
  ```

- [ ] **Step 2: Run to verify failure**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.book.BookDetailViewModelTest"`
  Expected: compilation error — `BookDetailViewModel` still takes 2 params; new intents not defined

- [ ] **Step 3: Update BookDetailViewModel**

  Full replacement of `composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailViewModel.kt`:

  ```kotlin
  package com.opclient.book.presentation

  import androidx.lifecycle.ViewModel
  import androidx.lifecycle.viewModelScope
  import com.opclient.book.domain.BookDetail
  import com.opclient.book.domain.BookRepository
  import com.opclient.core.ApiError
  import com.opclient.core.Result
  import com.opclient.library.domain.LibraryEntry
  import com.opclient.library.domain.LibraryRepository
  import com.opclient.library.domain.Shelf
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
  import kotlinx.datetime.Clock

  data class BookDetailUiState(
      val status: DetailStatus = DetailStatus.Loading,
      val book: BookDetail? = null,
      val relatedWorks: List<SubjectWork> = emptyList(),
      val relatedSubjectName: String = "",
      val currentShelf: Shelf? = null,
  )

  sealed class BookDetailIntent {
      data class Load(val workKey: String) : BookDetailIntent()
      data object Retry : BookDetailIntent()
      data class SetShelf(val shelf: Shelf) : BookDetailIntent()
      data object RemoveFromLibrary : BookDetailIntent()
  }

  sealed class BookDetailEffect {
      data class LoadError(val error: ApiError) : BookDetailEffect()
  }

  class BookDetailViewModel(
      private val repository: BookRepository,
      private val subjectRepository: SubjectRepository,
      private val libraryRepository: LibraryRepository,
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
              is BookDetailIntent.SetShelf -> setShelf(intent.shelf)
              BookDetailIntent.RemoveFromLibrary -> removeFromLibrary()
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
                      observeShelf(book.key)
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

      private fun observeShelf(workKey: String) {
          viewModelScope.launch {
              libraryRepository.getCurrentShelf(workKey).collect { shelf ->
                  _uiState.update { it.copy(currentShelf = shelf) }
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

      private fun setShelf(shelf: Shelf) {
          val book = _uiState.value.book ?: return
          viewModelScope.launch {
              if (_uiState.value.currentShelf == null) {
                  libraryRepository.addToShelf(
                      LibraryEntry(
                          workKey = book.key,
                          title = book.title,
                          authorName = book.authors.firstOrNull()?.name,
                          coverUrl = book.coverUrl,
                          shelf = shelf,
                          addedAt = Clock.System.now().toEpochMilliseconds(),
                      )
                  )
              } else {
                  libraryRepository.moveToShelf(book.key, shelf)
              }
          }
      }

      private fun removeFromLibrary() {
          val book = _uiState.value.book ?: return
          viewModelScope.launch {
              libraryRepository.removeFromShelf(book.key)
          }
      }
  }
  ```

- [ ] **Step 4: Update BookModule.kt to pass 3 args**

  In `composeApp/src/commonMain/kotlin/com/opclient/di/BookModule.kt`, change the viewModel binding:

  ```kotlin
  viewModel { BookDetailViewModel(get(), get(), get()) }
  ```

  Add the missing import:
  ```kotlin
  import com.opclient.book.presentation.BookDetailViewModel
  ```

- [ ] **Step 5: Run tests to verify they pass**

  Run: `./gradlew :composeApp:jvmTest --tests "com.opclient.book.BookDetailViewModelTest"`
  Expected: all 15 tests PASSED (7 existing + 8 new)

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailViewModel.kt \
          composeApp/src/commonTest/kotlin/com/opclient/book/BookDetailViewModelTest.kt \
          composeApp/src/commonMain/kotlin/com/opclient/di/BookModule.kt
  git commit -m "feat(library): extend BookDetailViewModel with shelf intents and TDD"
  ```

---

### Task 11: BookDetailScreen Shelf Buttons

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailScreen.kt`

- [ ] **Step 1: Add shelf buttons after subjects section**

  In `BookDetailScreen.kt`, add imports:
  ```kotlin
  import androidx.compose.foundation.layout.Row
  import androidx.compose.foundation.layout.Arrangement
  import com.opclient.library.domain.Shelf
  import com.opclient.ui.components.PrimaryButton
  ```

  Inside the `DetailStatus.Success` column, add the shelf buttons row after the `FlowRow` subjects section and before the description section. The full column body (replacing lines 100–137) becomes:

  ```kotlin
  if (book.subjects.isNotEmpty()) {
      FlowRow(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          book.subjects.take(8).forEach { subject ->
              SubjectTag(text = subject, modifier = Modifier.padding(end = 4.dp, bottom = 4.dp))
          }
      }
  }
  Row(
      modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
      Shelf.entries.forEach { shelf ->
          if (shelf == uiState.currentShelf) {
              PrimaryButton(
                  text = shelf.label,
                  onClick = { viewModel.onIntent(BookDetailIntent.RemoveFromLibrary) },
              )
          } else {
              SecondaryButton(
                  text = shelf.label,
                  onClick = { viewModel.onIntent(BookDetailIntent.SetShelf(shelf)) },
              )
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
  ```

- [ ] **Step 2: Verify compilation**

  Run: `./gradlew :composeApp:compileKotlinJvm`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

  ```bash
  git add composeApp/src/commonMain/kotlin/com/opclient/book/presentation/BookDetailScreen.kt
  git commit -m "feat(library): add shelf buttons to BookDetailScreen"
  ```

---

### Task 12: Run All Tests + Live Verification

**Files:** none

- [ ] **Step 1: Run full test suite**

  Run: `./gradlew :composeApp:jvmTest`
  Expected: all tests PASSED (prior count was 144; expect ~162 after adding 18 new tests)

  If any test fails, fix it before continuing.

- [ ] **Step 2: Launch desktop app**

  Run: `./gradlew :composeApp:run &`

  Verify the following golden paths:
  1. LIBRARY tab is accessible — clicking it navigates to `LibraryScreen`
  2. Three FilterChip tabs visible: "Want to Read", "Reading", "Read"
  3. Empty state "No books here yet" shown on all shelves initially
  4. Navigate to any book detail — three shelf buttons appear: "Want to Read", "Reading", "Read"
  5. Tap "Want to Read" (SecondaryButton) → it becomes a PrimaryButton; LIBRARY tab now shows that book under "Want to Read"
  6. Tap "Reading" (SecondaryButton) on the same book → book moves to Reading shelf; "Want to Read" becomes SecondaryButton, "Reading" becomes PrimaryButton
  7. Tap the active PrimaryButton ("Reading") → book removed from library; all three buttons revert to SecondaryButton
  8. Tap book in LIBRARY tab → navigates to its BookDetailScreen

- [ ] **Step 3: Final commit (if any fixes needed after live test)**

  ```bash
  git add -p  # stage only intentional fixes
  git commit -m "fix(library): address issues found during live verification"
  ```
