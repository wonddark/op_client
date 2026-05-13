# Week 2 — Search API Implementation Design

## Scope

OpenLibrary full-text search: API client, DTOs, domain models, cache, repository, MVI ViewModel, Search screen. Satisfies Milestone 1 "functional search + cover image display."

---

## Decisions

| Concern | Decision |
|---|---|
| Pagination | Load-more button, `limit=20`, `offset` accumulates |
| Cache TTL | 5 minutes, unbounded, key = `"$query:$offset"` |
| Type filter | None — deferred to Week 4 advanced filters |
| Cover images | Coil 3.x `AsyncImage`, URL derived from `cover_i` |
| Data layer structure | `SearchCache` separate from `SearchRepositoryImpl` |

---

## File Layout

```
commonMain/com/opclient/
  search/
    data/
      SearchApiClient.kt
      SearchDto.kt
      SearchMapper.kt
      SearchCache.kt
      SearchRepositoryImpl.kt
    domain/
      SearchModels.kt
      SearchRepository.kt
    presentation/
      SearchViewModel.kt
      SearchScreen.kt
  di/
    SearchModule.kt        — adds to existing di/ package
```

---

## Data Layer

### API

Endpoint: `GET https://openlibrary.org/search.json`

Query params used: `q`, `offset`, `limit`.

### DTOs (`SearchDto.kt`)

```kotlin
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

### Domain Models (`SearchModels.kt`)

```kotlin
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

### Mapper (`SearchMapper.kt`)

- `SearchDocDto.toDomain(): Book`
  - `title` → `"Unknown Title"` if null
  - `authorName?.firstOrNull()` → `"Unknown"` if null/empty
  - `coverId` → `"https://covers.openlibrary.org/b/id/$coverId-M.jpg"` or null
  - `subject?.firstOrNull()` → `primarySubject`
- `SearchResponseDto.toDomain(query: String, offset: Int): SearchResults`

### Cache (`SearchCache.kt`)

- Koin `single`
- `Mutex`-guarded `mutableMapOf<String, Pair<SearchResults, Long>>`
- TTL = 5 minutes (constant `CACHE_TTL_MS = 5 * 60 * 1_000L`)
- Key = `"$query:$offset"`
- `suspend fun get(key: String): SearchResults?` — returns null on miss or expired entry
- `suspend fun put(key: String, results: SearchResults)`
- Clock via `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()`

### API Client (`SearchApiClient.kt`)

Extends `ApiClient(baseUrl = "https://openlibrary.org", httpClient)`.

```kotlin
suspend fun search(query: String, offset: Int, limit: Int): Result<SearchResponseDto, ApiError> =
    get("/search.json", mapOf("q" to query, "offset" to "$offset", "limit" to "$limit"))
```

### Repository

**Interface** (`domain/SearchRepository.kt`):
```kotlin
interface SearchRepository {
    suspend fun search(query: String, offset: Int = 0, limit: Int = 20): Result<SearchResults, ApiError>
}
```

**Implementation** (`data/SearchRepositoryImpl.kt`):
1. Check `cache.get("$query:$offset")`
2. Hit → `Result.Success(cached)`
3. Miss → `apiClient.search(query, offset, limit)` → map DTO → domain → `cache.put(...)` → return

---

## MVI Layer

### State

```kotlin
data class SearchUiState(
    val query: String = "",
    val books: List<Book> = emptyList(),
    val totalFound: Int = 0,
    val offset: Int = 0,
    val status: SearchStatus = SearchStatus.Idle,
    val canLoadMore: Boolean = false,
)

enum class SearchStatus { Idle, Loading, LoadingMore, Success, Empty, Error }
```

### Intent

```kotlin
sealed class SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent()
    data object Search : SearchIntent()
    data object LoadMore : SearchIntent()
    data object ClearSearch : SearchIntent()
}
```

### Effect

```kotlin
sealed class SearchEffect {
    data class SearchError(val error: ApiError) : SearchEffect()
}
```

### ViewModel (`SearchViewModel.kt`)

- `uiState: StateFlow<SearchUiState>` — primary state
- `effects: SharedFlow<SearchEffect>` — replay=0, one-shot events

Intent handling:

| Intent | Behaviour |
|---|---|
| `QueryChanged(q)` | Update `query` in state only — no search triggered |
| `Search` | Skip if `query` blank. Set `Loading`, call repo at `offset=0`. On success: populate `books`, set `Success` or `Empty`, set `canLoadMore = books.size < totalFound`. On failure: emit `SearchError` effect, set `Error` status. |
| `LoadMore` | Guard: only if `canLoadMore && status != LoadingMore`. Set `LoadingMore`, call repo at `offset + 20`. On success: append books, advance `offset`, recompute `canLoadMore`. On failure: emit `SearchError` effect, restore `Success` status. |
| `ClearSearch` | Reset to initial `SearchUiState()` |

---

## Search Screen (`SearchScreen.kt`)

`commonMain` composable. Param: `viewModel: SearchViewModel`, `onBookClick: (String) -> Unit`.

**`SearchInput` extension:** Add `onSearch: () -> Unit` param (triggered on IME done / keyboard submit). Default = no-op to keep existing callers unbroken.

Layout (top → bottom):

```
SearchInput(value = query, onValueChange = QueryChanged, onSearch = Search)

when status:
  Idle        → SectionLabel("RECENT") + empty area
  Loading     → LoadingState (fills content area)
  Success /
  LoadingMore →
    SectionLabel("RESULTS · {totalFound} BOOKS")
    LazyColumn:
      BookRow per book (coverContent = AsyncImage(coverUrl), onClick = onBookClick(book.key))
      if canLoadMore → PrimaryButton("LOAD MORE") → LoadMore
      if LoadingMore → small LoadingState below list
  Empty       → EmptyState
  Error       → ErrorState (message from last SearchError effect)
```

`SearchEffect.SearchError` collected in `LaunchedEffect(effects)` → updates local `errorMessage: String?` state shown in `ErrorState`.

`AsyncImage` config: `contentScale = ContentScale.Crop`, placeholder = `Box(colors.surface2)`.

### App.kt wiring

```kotlin
@Composable
fun App() {
    var selected by remember { mutableStateOf(Destination.SEARCH) }
    AppShell(selectedDestination = selected, onDestinationChange = { selected = it }) {
        when (selected) {
            Destination.SEARCH -> SearchScreen(onBookClick = {})
            else -> Box(Modifier.fillMaxSize())
        }
    }
}
```

ViewModel provided via `koinViewModel()` from `koin-compose-viewmodel` — works on both Android and Desktop targets in Compose Multiplatform.

---

## Koin Wiring (`SearchModule.kt`)

```kotlin
val searchModule = module {
    single { SearchCache() }
    factory { SearchApiClient(get()) }
    single<SearchRepository> { SearchRepositoryImpl(apiClient = get(), cache = get()) }
    viewModel { SearchViewModel(get()) }
}
```

`searchModule` added to `startKoin` in both `AndroidModule` and `DesktopModule`.

---

## Tests

All in `commonTest/` using `kotlin.test` + Ktor `MockEngine`. `SearchViewModelTest` uses Turbine.

| Class | Covers |
|---|---|
| `SearchCacheTest` | TTL expiry, cache hit, cache miss, concurrent access |
| `SearchMapperTest` | null author → "Unknown", null coverId → null URL, subject list → primarySubject, null title → "Unknown Title" |
| `SearchRepositoryTest` | Cache hit skips HTTP, cache miss calls MockEngine, API error propagates as `Result.Failure` |
| `SearchViewModelTest` | `QueryChanged` updates query only, `Search` full state flow, `LoadMore` appends and advances offset, `ClearSearch` resets, error emits `SearchEffect.SearchError` |

---

## V-Invariants

```
V1: SearchRepository interface ∈ domain/ — no Ktor/serialization imports
V2: SearchScreen ∉ data/ types — only domain models & design system components
V3: cache key = "$query:$offset" ∀ cache operations
V4: canLoadMore = books.size < totalFound (recomputed after every successful fetch)
V5: LoadMore ⊥ when status = LoadingMore | !canLoadMore
V6: coverUrl ⊥ when coverId = null
```
