# Week 5 — Library (Reading List) Design

## Scope

LIBRARY tab reading list: save books to WANT_TO_READ / READING / READ shelves. Local persistence via SQLDelight 2.0.2. Shelf buttons on BookDetailScreen. Satisfies Milestone 3 "personal library."

---

## Decisions

| Concern | Decision |
|---|---|
| Persistence | SQLDelight 2.0.2 with coroutines-extensions (Flow queries) |
| Shelf model | 3 shelves: WANT_TO_READ, READING, READ |
| Entry point | BookDetailScreen only — shelf buttons after subjects section |
| Shelf UI | 3 buttons: PrimaryButton (active shelf), SecondaryButton (inactive); tap active = remove |
| LIBRARY tab layout | FilterChip row (3 tabs) + LazyColumn of BookRow entries |
| Empty state | EmptyState("No books here yet") per shelf |
| Reactive updates | `LibraryRepository` returns `Flow` — BookDetailScreen currentShelf updates live |
| Platform drivers | androidModule: AndroidSqliteDriver; desktopModule: JdbcSqliteDriver |
| Test driver | JdbcSqliteDriver("jdbc:sqlite::memory:") — no file cleanup |
| Module load order | libraryModule before bookModule (BookDetailViewModel depends on LibraryRepository) |

---

## File Layout

**Create:**
```
commonMain/sqldelight/com/opclient/library/
  Library.sq

commonMain/com/opclient/
  library/
    domain/
      LibraryModels.kt
      LibraryRepository.kt
    data/
      LibraryRepositoryImpl.kt
    presentation/
      LibraryViewModel.kt
      LibraryScreen.kt
  di/
    LibraryModule.kt

commonTest/com/opclient/library/
  LibraryRepositoryTest.kt
  LibraryViewModelTest.kt
```

**Modify:**
- `composeApp/build.gradle.kts` — SQLDelight plugin + dependencies
- `androidMain/com/opclient/di/AndroidModule.kt` — add `SqlDriver` binding
- `jvmMain/com/opclient/di/DesktopModule.kt` — add `SqlDriver` binding
- `commonMain/com/opclient/navigation/Screen.kt` — add `Library`
- `commonMain/com/opclient/navigation/NavigationViewModel.kt` — `LIBRARY → listOf(Screen.Library)`
- `commonMain/com/opclient/navigation/NavigationViewModelTest.kt` — update LIBRARY tab test
- `commonMain/com/opclient/App.kt` — route `Screen.Library`
- `commonMain/com/opclient/book/presentation/BookDetailViewModel.kt` — add `LibraryRepository` dep + `currentShelf` flow + new intents
- `commonMain/com/opclient/book/presentation/BookDetailScreen.kt` — add shelf buttons
- `commonTest/com/opclient/book/BookDetailViewModelTest.kt` — update for new constructor + shelf tests
- `commonMain/com/opclient/di/BookModule.kt` — `viewModel { BookDetailViewModel(get(), get(), get()) }`
- `androidMain/com/opclient/OpClientApplication.kt` — add `libraryModule` before `bookModule`
- `jvmMain/com/opclient/Main.kt` — add `libraryModule` before `bookModule`

---

## Gradle Setup

```kotlin
// composeApp/build.gradle.kts
plugins {
    id("app.cash.sqldelight") version "2.0.2"
}

sqldelight {
    databases {
        create("LibraryDatabase") {
            packageName.set("com.opclient.library")
        }
    }
}

// in dependencies block:
commonMainImplementation("app.cash.sqldelight:coroutines-extensions:2.0.2")
androidMainImplementation("app.cash.sqldelight:android-driver:2.0.2")
jvmMainImplementation("app.cash.sqldelight:sqlite-driver:2.0.2")
```

---

## SQLDelight Schema

```sql
-- commonMain/sqldelight/com/opclient/library/Library.sq

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

`shelf` column stores enum name string: `"WANT_TO_READ"`, `"READING"`, `"READ"`.

---

## Domain Models

```kotlin
// library/domain/LibraryModels.kt
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

---

## Repository Interface

```kotlin
// library/domain/LibraryRepository.kt
interface LibraryRepository {
    fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>>
    fun getCurrentShelf(workKey: String): Flow<Shelf?>
    suspend fun addToShelf(entry: LibraryEntry)
    suspend fun removeFromShelf(workKey: String)
    suspend fun moveToShelf(workKey: String, shelf: Shelf)
}
```

---

## Repository Implementation

```kotlin
// library/data/LibraryRepositoryImpl.kt
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

---

## MVI Layer

### `LibraryViewModel`

```kotlin
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

    init { collectShelf(Shelf.WANT_TO_READ) }

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

### `BookDetailUiState` update

```kotlin
data class BookDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val book: BookDetail? = null,
    val relatedWorks: List<SubjectWork> = emptyList(),
    val relatedSubjectName: String = "",
    val currentShelf: Shelf? = null,
)
```

### `BookDetailIntent` additions

```kotlin
data class SetShelf(val shelf: Shelf) : BookDetailIntent()
data object RemoveFromLibrary : BookDetailIntent()
```

### `BookDetailViewModel` changes

Constructor: `BookDetailViewModel(repository, subjectRepository, libraryRepository)`.

After successful book load, collect `libraryRepository.getCurrentShelf(book.key)`:

```kotlin
private fun observeShelf(workKey: String) {
    viewModelScope.launch {
        libraryRepository.getCurrentShelf(workKey).collect { shelf ->
            _uiState.update { it.copy(currentShelf = shelf) }
        }
    }
}
```

Intent handling:

```kotlin
is BookDetailIntent.SetShelf -> {
    val book = _uiState.value.book ?: return
    viewModelScope.launch {
        if (_uiState.value.currentShelf == null) {
            libraryRepository.addToShelf(
                LibraryEntry(
                    workKey = book.key,
                    title = book.title,
                    authorName = book.authors.firstOrNull()?.name,
                    coverUrl = book.coverUrl,
                    shelf = intent.shelf,
                    addedAt = Clock.System.now().toEpochMilliseconds(),
                )
            )
        } else {
            libraryRepository.moveToShelf(book.key, intent.shelf)
        }
    }
}
BookDetailIntent.RemoveFromLibrary -> {
    val book = _uiState.value.book ?: return
    viewModelScope.launch { libraryRepository.removeFromShelf(book.key) }
}
```

---

## Screens

### `LibraryScreen.kt`

```
Column:
  Row (shelf tabs):
    FilterChip("Want to Read", selected = selectedShelf == WANT_TO_READ, onToggle = { SelectShelf(WANT_TO_READ) })
    FilterChip("Reading",      selected = selectedShelf == READING,      onToggle = { SelectShelf(READING) })
    FilterChip("Read",         selected = selectedShelf == READ,         onToggle = { SelectShelf(READ) })
  if entries.isEmpty():
    EmptyState("No books here yet")
  else:
    LazyColumn:
      items(entries, key = { it.workKey }):
        BookRow(title, author = authorName ?: "", onClick = { onBookClick(workKey) })
```

### `BookDetailScreen.kt` addition

After subjects/tags section, before "More in {subject}":

```
Row (shelf buttons, horizontalArrangement = spacedBy(8.dp), modifier = padding(16.dp)):
  for shelf in [WANT_TO_READ, READING, READ]:
    if shelf == currentShelf:
      PrimaryButton(text = shelf.label, onClick = { RemoveFromLibrary })
    else:
      SecondaryButton(text = shelf.label, onClick = { SetShelf(shelf) })
```

Shelf button text = `shelf.label` (defined on `Shelf` enum).

---

## Navigation

### `Screen.kt` addition

```kotlin
data object Library : Screen()
```

### `NavigationViewModel.kt` change

```kotlin
Destination.LIBRARY -> listOf(Screen.Library)
```

### `App.kt` addition

```kotlin
Screen.Library -> LibraryScreen(
    onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) }
)
```

---

## Koin Modules

### `LibraryModule.kt`

```kotlin
val libraryModule: Module = module {
    single { LibraryDatabase(get()) }
    single<LibraryRepository> { LibraryRepositoryImpl(get()) }
    viewModel { LibraryViewModel(get()) }
}
```

### `AndroidModule.kt` addition

```kotlin
single<SqlDriver> {
    AndroidSqliteDriver(LibraryDatabase.Schema, get(), "library.db")
}
```

### `DesktopModule.kt` addition

```kotlin
single<SqlDriver> {
    JdbcSqliteDriver("jdbc:sqlite:library.db").also {
        LibraryDatabase.Schema.create(it)
    }
}
```

### Module load order

Both `OpClientApplication` and `Main.kt`:
```
commonModule, androidModule/desktopModule, searchModule, authorModule, subjectModule, libraryModule, bookModule
```

### `BookModule.kt` change

```kotlin
viewModel { BookDetailViewModel(get(), get(), get()) }
```

---

## Tests

| Class | Covers |
|---|---|
| `LibraryRepositoryTest` | In-memory driver; `addToShelf` persists; `getShelf` flow emits on change; `moveToShelf` updates; `removeFromShelf` deletes; `getCurrentShelf` null for unknown key; shelf isolation (READING entry not in WANT_TO_READ) |
| `LibraryViewModelTest` | Initial state = WANT_TO_READ, empty entries; `SelectShelf` switches subscription; entries reflect fake repo flow; switching shelves cancels previous job |
| `BookDetailViewModelTest` | Update: add fake `LibraryRepository`; `currentShelf` null initially, updates from flow; `SetShelf` calls `addToShelf` when `currentShelf == null`; `SetShelf` calls `moveToShelf` when already on shelf; `RemoveFromLibrary` calls `removeFromShelf`; all existing tests still pass |
| `NavigationViewModelTest` | Update: `navigateToTab(LIBRARY)` → `[Library]` |

Fake `LibraryRepository` for VM tests — `MutableStateFlow<Map<String, Shelf>>` backing store, derives `getShelf` and `getCurrentShelf` flows from map.

---

## V-Invariants

```
V1: LibraryEntry.shelf ∈ {"WANT_TO_READ", "READING", "READ"} ∀ DB rows
V2: getCurrentShelf(key) flow → null ∀ key not in DB
V3: getShelf(s) flow → excludes entries ∀ shelf ≠ s
V4: SetShelf → addToShelf ∀ currentShelf == null; moveToShelf ∀ currentShelf ≠ null
V5: RemoveFromLibrary → removeFromShelf ∀ calls; no-ops ∀ book == null
V6: libraryModule loaded before bookModule ∀ DI startup
V7: AndroidSqliteDriver ∈ androidModule; JdbcSqliteDriver ∈ desktopModule
V8: PrimaryButton ∀ shelf == currentShelf; SecondaryButton ∀ shelf ≠ currentShelf
V9: LibraryViewModel.collectShelf cancels previous job ∀ SelectShelf intent
V10: BookDetailViewModel.observeShelf fires ∀ successful book load only
```
