# Week 7 — Search Inside & Recent Changes Design

## Goal

Add two features to op_client:

1. **Search Inside** — full-text passage search across OpenLibrary books, surfaced as a toggle mode on the existing `SearchScreen`. Result tap navigates to `BookDetailScreen`.
2. **Recent Changes** — community activity feed (edits, additions) as a new top-level navigation tab. Minimal items (label + timestamp); tap navigates to `BookDetailScreen` or `AuthorDetailScreen`.

Both follow the same layered architecture used in all prior weeks: DTO → mapper → domain model → repository interface → repository impl → ViewModel → Screen.

---

## Architecture

Two independent packages with no shared state.

```
searchinside/
  data/   SearchInsideDto, SearchInsideMapper, SearchInsideApiClient, SearchInsideRepositoryImpl
  domain/ SearchInsideModels, SearchInsideRepository
  presentation/ SearchInsideViewModel

recentchanges/
  data/   RecentChangesDto, RecentChangesMapper, RecentChangesApiClient, RecentChangesRepositoryImpl
  domain/ RecentChangesModels, RecentChangesRepository
  presentation/ RecentChangesViewModel, RecentChangesScreen
```

`SearchInsideViewModel` lives in `searchinside/presentation/` but is consumed by the existing `SearchScreen` (not a new screen). `RecentChangesScreen` is a new top-level screen.

---

## Data Models

### Search Inside

**API:** `GET https://openlibrary.org/search/inside.json?q=<query>`

**DTO:**
```kotlin
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

**Domain:**
```kotlin
data class SearchInsideResult(
    val workKey: String,
    val title: String,
    val authorName: String?,
    val coverUrl: String?,
    val passage: String,
)
```

**Mapper:** `SearchInsideDocDto.toDomain()` — extracts `key` as `workKey`, `authorName.firstOrNull()`, `text.firstOrNull() ?: ""` as `passage`, `coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" }` as `coverUrl`. Docs with null `key` are filtered out.

---

### Recent Changes

**API:** `GET https://openlibrary.org/recentchanges.json`

**DTO:**
```kotlin
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

**Domain:**
```kotlin
data class RecentChange(
    val id: String,
    val label: String,
    val targetKey: String?,
    val addedAt: Long,
)
```

**Mapper:** `RecentChangeDto.toDomain()` — derives `label` from `kind`:

| kind | label |
|------|-------|
| `add-book` | "Book added" |
| `update-work` | "Work updated" |
| `update-author` | "Author updated" |
| `edit-book` | "Book edited" |
| anything else | "Community edit" |

`targetKey` = `changes.firstOrNull()?.key`. `addedAt` = `Instant.parse(timestamp)` epoch millis, fallback `Clock.System.now()`. Items with null `id` are filtered out.

---

## Repositories

### SearchInsideRepository

```kotlin
interface SearchInsideRepository {
    suspend fun search(query: String): Result<List<SearchInsideResult>, ApiError>
}
```

`SearchInsideRepositoryImpl(apiClient)` — delegates to `SearchInsideApiClient`, maps response docs, filters null keys.

### RecentChangesRepository

```kotlin
interface RecentChangesRepository {
    suspend fun getRecentChanges(): Result<List<RecentChange>, ApiError>
}
```

`RecentChangesRepositoryImpl(apiClient)` — delegates to `RecentChangesApiClient`, maps and filters null ids. No local caching — community data changes constantly.

---

## ViewModels

### SearchInsideViewModel

```kotlin
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
```

- `init` — no-op (search is user-triggered)
- `Search` — sets `isLoading = true`, calls repository, updates results or error
- `Clear` — resets to initial state

### RecentChangesViewModel

```kotlin
data class RecentChangesUiState(
    val changes: List<RecentChange> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed class RecentChangesIntent {
    data object Load : RecentChangesIntent()
    data object Refresh : RecentChangesIntent()
}
```

- `init` — dispatches `Load`
- `Load` — fetches if not already loaded
- `Refresh` — clears results, sets `isLoading = true`, re-fetches

---

## SearchScreen Changes

`SearchScreen` gains a mode toggle above the results list:

```kotlin
var searchMode by remember { mutableStateOf(SearchMode.BOOKS) }
// SearchMode.BOOKS    → SearchViewModel
// SearchMode.FULL_TEXT → SearchInsideViewModel
```

Two `FilterChip` components (existing component) render the toggle. `SearchInput.onSearch` dispatches to whichever VM is active based on mode. Switching mode hides the inactive VM's results — results are preserved so switching back restores them without re-fetching. Both VMs are acquired via `koinViewModel()` at the top of the composable.

---

## RecentChangesScreen

Minimal list screen:
- `LoadingState` while `isLoading`
- `ErrorState` with retry button dispatching `Refresh` on error
- `EmptyState` on empty list
- `LazyColumn` of change items: `label` in `typography.bookTitle`, formatted `addedAt` in `typography.bookAuthor`, full-width tappable row
- Tap: derive destination from `targetKey` prefix → `/works/` or `/books/` → `onBookClick(key)`, `/authors/` → `onAuthorClick(key)`, else no-op
- Pull-to-refresh via `Refresh` intent (no Compose swipe-refresh library — simple "REFRESH" `SecondaryButton` in header)

---

## Navigation

**`Screen.kt`** — add:
```kotlin
data object RecentChanges : Screen()
```

**`Destination.kt`** — add `CHANGES` entry with canvas-drawn icon (clock or activity icon).

**`App.kt`** — add branch:
```kotlin
Screen.RecentChanges -> RecentChangesScreen(
    onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
    onAuthorClick = { key -> navVm.navigateTo(Screen.AuthorDetail(key)) },
)
```

**`AppShell.kt`** — add `CHANGES` to nav destinations list. Tab order: Search, Library, Changes.

---

## Koin Wiring

**`SearchInsideModule.kt`:**
```kotlin
val searchInsideModule: Module = module {
    factory { SearchInsideApiClient(get()) }
    single<SearchInsideRepository> { SearchInsideRepositoryImpl(get()) }
    viewModel { SearchInsideViewModel(get()) }
}
```

**`RecentChangesModule.kt`:**
```kotlin
val recentChangesModule: Module = module {
    factory { RecentChangesApiClient(get()) }
    single<RecentChangesRepository> { RecentChangesRepositoryImpl(get()) }
    viewModel { RecentChangesViewModel(get()) }
}
```

Both added to `OpClientApplication.kt` and `Main.kt` module lists.

---

## Tests

### SearchInsideRepositoryTest (commonTest, MockEngine)
| Test | Verifies |
|------|---------|
| `search_parsesResults` | docs mapped to domain, passage extracted from first text element |
| `search_emptyResponse_returnsEmptyList` | `numFound=0` → empty list, no error |
| `search_404_returnsFailure` | HTTP 404 → `Result.Failure` |
| `search_filtersDocsWithNullKey` | docs missing `key` excluded from results |

### SearchInsideViewModelTest (commonTest, UnconfinedTestDispatcher)
| Test | Verifies |
|------|---------|
| `initialState_isEmpty` | `hasSearched=false`, results empty |
| `search_loadsResults` | after `Search` intent, results populated, `hasSearched=true` |
| `search_replacesExistingResults` | second `Search` clears first results |
| `clear_resetsState` | `Clear` intent → back to initial state |

### RecentChangesRepositoryTest (commonTest, MockEngine)
| Test | Verifies |
|------|---------|
| `getRecentChanges_parsesItems` | kind → label mapping, timestamp → epoch millis |
| `getRecentChanges_emptyList` | `[]` response → empty list, no error |
| `getRecentChanges_500_returnsFailure` | HTTP 500 → `Result.Failure` |

### RecentChangesViewModelTest (commonTest, UnconfinedTestDispatcher)
| Test | Verifies |
|------|---------|
| `init_loadsChanges` | init dispatches Load, changes populated |
| `refresh_reFetches` | Refresh clears then repopulates |
| `error_setsErrorState` | API failure → error non-null, changes empty |

---

## V-Invariants

```
V1: SearchInsideResult.workKey ∉ null ∀ results (null-key docs filtered in mapper)
V2: SearchMode toggle → inactive VM results hidden (not cleared) ∀ mode switch; query text persists across modes
V3: RecentChange.id ∉ null ∀ domain list (null-id DTOs filtered in mapper)
V4: targetKey prefix "/works/" | "/books/" → Screen.BookDetail ∀ navigation
V5: targetKey prefix "/authors/" → Screen.AuthorDetail ∀ navigation
V6: targetKey ∉ "/works/" | "/books/" | "/authors/" → no-op ∀ navigation
V7: RecentChangesViewModel.init → Load dispatched exactly once
V8: Refresh → changes = [] before fetch, isLoading = true ∀ calls
V9: SearchInsideModule loaded ∀ Koin startup (both platforms)
V10: RecentChangesModule loaded ∀ Koin startup (both platforms)
```
