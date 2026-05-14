# Week 4 — Subjects & Categories Design

## Scope

BROWSE tab subjects browsing, related-content suggestions in book detail, `fields` param optimization in search. Satisfies Milestone 2 "subject browsing and navigation."

---

## Decisions

| Concern | Decision |
|---|---|
| BROWSE tab initial state | Hardcoded curated subjects grid (12 subjects) — upgradeable to API later |
| Subject tags on BookDetailScreen | Non-clickable for now — cross-linking deferred |
| Pagination style | Load-more button on Desktop, infinite scroll on Android via `PlatformConfig.useLazyPagination` |
| Subject ViewModel split | Two ViewModels: `SubjectBrowseViewModel` (static list) + `SubjectDetailViewModel` (pagination) |
| Related content | "More in {subject}" section at bottom of BookDetailScreen — uses `SubjectRepository` |
| Author detail related content | Not included — author detail already shows works |
| Search filters UI | Deferred — only `fields` param optimization added to `SearchApiClient` |
| Cache scope | Offset=0 pages only; load-more responses not cached |
| Subject URL normalization | `name.replace(" ", "_").lowercase()` |

---

## File Layout

**Create:**
```
commonMain/com/opclient/
  subject/
    data/
      SubjectDto.kt
      SubjectMapper.kt
      SubjectApiClient.kt
      SubjectCache.kt
      SubjectRepositoryImpl.kt
    domain/
      SubjectModels.kt
      SubjectRepository.kt
    presentation/
      SubjectBrowseViewModel.kt
      SubjectDetailViewModel.kt
      SubjectBrowseScreen.kt
      SubjectDetailScreen.kt
  di/
    SubjectModule.kt
  platform/
    PlatformConfig.kt          ← expect object

androidMain/com/opclient/platform/
  PlatformConfig.kt            ← actual (useLazyPagination = true)

jvmMain/com/opclient/platform/
  PlatformConfig.kt            ← actual (useLazyPagination = false)
```

**Modify:**
- `navigation/Screen.kt` — add `SubjectList`, `SubjectDetail`
- `navigation/NavigationViewModel.kt` — `navigateToTab(destination: Destination)`
- `navigation/NavigationViewModelTest.kt` — update tab tests
- `App.kt` — route new screens, pass tab to `navigateToTab`
- `book/presentation/BookDetailViewModel.kt` — add `SubjectRepository` dep, load related works
- `book/presentation/BookDetailScreen.kt` — add "More in {subject}" section
- `book/presentation/BookDetailViewModelTest.kt` — update for new constructor + related works
- `di/BookModule.kt` — `viewModel { BookDetailViewModel(get(), get()) }`
- `search/data/SearchApiClient.kt` — add `fields` param
- `androidMain/OpClientApplication.kt` — add `subjectModule` before `bookModule`
- `jvmMain/Main.kt` — add `subjectModule` before `bookModule`

---

## Navigation

### `Screen.kt` additions

```kotlin
data object SubjectList : Screen()
data class SubjectDetail(val subjectName: String) : Screen()
```

### `NavigationViewModel.kt`

```kotlin
fun navigateToTab(destination: Destination) {
    _stack.update {
        when (destination) {
            Destination.SEARCH -> listOf(Screen.Search)
            Destination.BROWSE -> listOf(Screen.SubjectList)
            Destination.LIBRARY -> listOf(Screen.Search) // placeholder until Week 5
        }
    }
}
```

### `App.kt` wiring

```kotlin
onDestinationChange = { tab ->
    selectedTab = tab
    navVm.navigateToTab(tab)
}

is Screen.SubjectList -> SubjectBrowseScreen(
    onSubjectClick = { name -> navVm.navigateTo(Screen.SubjectDetail(name)) }
)
is Screen.SubjectDetail -> SubjectDetailScreen(
    subjectName = screen.subjectName,
    onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) },
    onBack = { navVm.navigateBack() },
)
```

---

## Platform Config

```kotlin
// commonMain
expect object PlatformConfig {
    val useLazyPagination: Boolean
}

// androidMain
actual object PlatformConfig {
    actual val useLazyPagination = true
}

// jvmMain
actual object PlatformConfig {
    actual val useLazyPagination = false
}
```

---

## Data Layer

### DTOs (`SubjectDto.kt`)

```kotlin
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

### Domain Models (`SubjectModels.kt`)

```kotlin
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

### Mapper helpers (`SubjectMapper.kt`)

```kotlin
private const val COVER_BASE = "https://covers.openlibrary.org/b/id"

internal fun Int?.toCoverUrl(): String? = this?.let { "$COVER_BASE/$it-M.jpg" }

internal fun SubjectDto.toDomain(): SubjectPage = SubjectPage(
    subjectName = name,
    workCount = workCount,
    works = works?.map { it.toDomain() } ?: emptyList(),
)

internal fun SubjectWorkDto.toDomain(): SubjectWork = SubjectWork(
    key = key,
    title = title ?: "",
    authorName = authors?.firstOrNull()?.name,
    coverUrl = coverId.toCoverUrl(),
)
```

### `SubjectApiClient.kt`

Extends `ApiClient(baseUrl = "https://openlibrary.org", httpClient)`.

```kotlin
suspend fun getSubject(subjectName: String, limit: Int, offset: Int): Result<SubjectDto, ApiError> {
    val normalized = subjectName.replace(" ", "_").lowercase()
    return get("/subjects/$normalized.json?limit=$limit&offset=$offset")
}
```

### `SubjectCache.kt`

Same Mutex+TTL pattern, `CACHE_TTL_MS = 30 * 60 * 1_000L`, injectable `timeSource`.
Key: `"subject:$normalizedName"`. Only offset=0 responses stored.

### Repository Interface (`SubjectRepository.kt`)

```kotlin
interface SubjectRepository {
    suspend fun getSubjectPage(
        subjectName: String,
        limit: Int = 12,
        offset: Int = 0,
    ): Result<SubjectPage, ApiError>
}
```

### `SubjectRepositoryImpl.kt`

- On `offset == 0`: check `cache.get("subject:$normalized")`; hit → return. Miss → fetch → cache → return.
- On `offset > 0`: skip cache (pagination), fetch directly, return (no caching).

### `SearchApiClient.kt` change

Add `fields` param to search URL:

```kotlin
return get("/search.json?q=$query&offset=$offset&limit=$limit&fields=key,title,author_name,cover_i,first_publish_year")
```

---

## MVI Layer

### `SubjectBrowseViewModel.kt`

```kotlin
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

### `SubjectDetailViewModel.kt`

```kotlin
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
```

Intent handling:

| Intent | Behaviour |
|---|---|
| `Load(name)` | Store `lastSubjectName`. Clear works. Set `Loading`. Fetch offset=0. On success: `Success` + works + workCount + canLoadMore. On failure: `Error` + emit `LoadError`. |
| `LoadMore` | Guard: `!isLoadingMore && canLoadMore`. Set `isLoadingMore = true`. Fetch `offset = works.size`. On success: append works, recompute `canLoadMore`. On failure: `isLoadingMore = false`, `canLoadMore = false`, emit `LoadError`. |
| `Retry` | Re-fire `Load(lastSubjectName)` if non-empty. |

`canLoadMore = works.size < workCount`

### `BookDetailUiState` update

```kotlin
data class BookDetailUiState(
    val status: DetailStatus = DetailStatus.Loading,
    val book: BookDetail? = null,
    val relatedWorks: List<SubjectWork> = emptyList(),
    val relatedSubjectName: String = "",
)
```

`BookDetailViewModel` constructor: `BookDetailViewModel(repository: BookRepository, subjectRepository: SubjectRepository)`.

After successful book load, if `book.subjects.isNotEmpty()`:

```kotlin
viewModelScope.launch {
    when (val r = subjectRepository.getSubjectPage(book.subjects.first(), limit = 6, offset = 0)) {
        is Result.Success -> _uiState.update {
            it.copy(
                relatedWorks = r.value.works.filter { w -> w.key != book.key },
                relatedSubjectName = book.subjects.first(),
            )
        }
        is Result.Failure -> { /* silent — detail screen stays Success */ }
    }
}
```

---

## Screens

### `SubjectBrowseScreen.kt`

Params: `onSubjectClick: (String) -> Unit`, `viewModel: SubjectBrowseViewModel = koinViewModel()`

```
LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = 16.dp):
  items(state.subjects):
    FilterChip(label = subject, selected = false, onClick = { onSubjectClick(subject) })
```

### `SubjectDetailScreen.kt`

Params: `subjectName: String`, `onBookClick: (String) -> Unit`, `onBack: () -> Unit`, `viewModel: SubjectDetailViewModel = koinViewModel()`

`LaunchedEffect(subjectName) { viewModel.onIntent(SubjectDetailIntent.Load(subjectName)) }`

```
Loading → LoadingState()
Error   → ErrorState(onRetry = { viewModel.onIntent(Retry) })
Success →
  if PlatformConfig.useLazyPagination:
    LazyColumn:
      item: SecondaryButton("← BACK", onClick = onBack)
      item: SectionLabel(state.subjectName)
      items(state.works, key = { it.key }):
        BookRow(title = work.title, author = work.authorName ?: "", coverContent = AsyncImage(work.coverUrl), onClick = { onBookClick(work.key) })
      item: if state.isLoadingMore → LoadingState()
    // infinite scroll trigger
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.works.size - 3 && state.canLoadMore && !state.isLoadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.onIntent(SubjectDetailIntent.LoadMore)
    }
  else:
    Column (vertically scrollable):
      SecondaryButton("← BACK", onClick = onBack)
      SectionLabel(state.subjectName)
      state.works.forEach:
        BookRow(...)
      if state.canLoadMore:
        SecondaryButton("Load more", onClick = { viewModel.onIntent(SubjectDetailIntent.LoadMore) })
      if state.isLoadingMore:
        LoadingState()
```

### `BookDetailScreen.kt` addition

After description section:

```
if state.relatedWorks.isNotEmpty():
  SectionLabel("More in ${state.relatedSubjectName}")
  state.relatedWorks.forEach:
    BookRow(title = work.title, author = work.authorName ?: "", coverContent = AsyncImage(work.coverUrl), onClick = { onBookClick(work.key) })
```

---

## Koin Modules

### `SubjectModule.kt`

```kotlin
val subjectModule: Module = module {
    single { SubjectCache() }
    factory { SubjectApiClient(get()) }
    single<SubjectRepository> { SubjectRepositoryImpl(apiClient = get(), cache = get()) }
    viewModel { SubjectBrowseViewModel() }
    viewModel { SubjectDetailViewModel(get()) }
}
```

### `BookModule.kt` change

```kotlin
single<BookRepository> { BookRepositoryImpl(apiClient = get(), cache = get(), authorRepository = get()) }
viewModel { BookDetailViewModel(get(), get()) }
```

Module load order: `subjectModule` before `bookModule` in both `OpClientApplication` and `Main.kt`.

---

## Tests

| Class | Covers |
|---|---|
| `SubjectMapperTest` | `toCoverUrl` with coverId/null, `SubjectDto.toDomain` work mapping, author name extraction, empty works |
| `SubjectRepositoryTest` | Cache hit skips HTTP (offset=0), cache miss fetches+caches, offset>0 bypasses cache, API error propagates |
| `SubjectBrowseViewModelTest` | Initial state contains all 12 featured subjects |
| `SubjectDetailViewModelTest` | `Load` → Loading → Success, `Load` → Error + effect, `LoadMore` appends works + updates canLoadMore, `LoadMore` no-op when isLoadingMore, `Retry` re-fetches |
| `BookDetailViewModelTest` | Update: mock SubjectRepository, relatedWorks populated on success, silent on subject API failure, empty subjects skips subject fetch |
| `NavigationViewModelTest` | Update: `navigateToTab(BROWSE)` → `[SubjectList]`, `navigateToTab(SEARCH)` → `[Search]`, `navigateToTab(LIBRARY)` → `[Search]` |

---

## V-Invariants

```
V1: SubjectBrowseViewModel ∈ subject/presentation/ — no API calls, emits static list only
V2: SubjectDetailViewModel — canLoadMore = works.size < workCount ∀ state updates
V3: SubjectRepositoryImpl — cache only ∀ offset == 0 responses
V4: PlatformConfig.useLazyPagination = true ∈ androidMain, false ∈ jvmMain
V5: related works failure → silent ignore, BookDetailUiState.status stays Success
V6: SearchApiClient.search → fields param included ∀ calls
V7: relatedWorks fetch guarded by book.subjects.isNotEmpty()
V8: relatedWorks filtered: work.key ≠ current book.key
V9: subject URL = name.replace(" ", "_").lowercase() + ".json" ∀ API calls
V10: subjectModule loaded before bookModule ∀ DI startup
```
