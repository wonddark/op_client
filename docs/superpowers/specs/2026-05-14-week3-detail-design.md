# Week 3 — Books & Authors Detail Design

## Scope

Navigation stack, Book Detail (work-level), Author Detail (profile + bibliography). Satisfies Milestone 2 "tap a search result → see full book detail → tap an author → see their profile and works."

---

## Decisions

| Concern | Decision |
|---|---|
| Navigation | Simple state-based stack (`List<Screen>`) in `NavigationViewModel`, no library |
| Book detail scope | Work-level only — no editions list |
| Author detail scope | Profile + works list from `/authors/{OLID}/works.json` |
| Polymorphic text fields | `JsonElement?` in DTOs, normalized to `String?` in mappers |
| Author name resolution | `BookDetailViewModel` fetches each author key in parallel |
| Cover size | `-L` for book detail hero, `-M` for author works list |
| Cache TTL | 30 minutes for book/author detail (data changes rarely) |

---

## File Layout

**Create:**
```
commonMain/com/opclient/
  navigation/
    Screen.kt
    NavigationViewModel.kt
  book/
    data/
      BookApiClient.kt
      BookDto.kt
      BookMapper.kt
      BookCache.kt
      BookRepositoryImpl.kt
    domain/
      BookModels.kt
      BookRepository.kt
    presentation/
      BookDetailViewModel.kt
      BookDetailScreen.kt
  author/
    data/
      AuthorApiClient.kt
      AuthorDto.kt
      AuthorMapper.kt
      AuthorCache.kt
      AuthorRepositoryImpl.kt
    domain/
      AuthorModels.kt
      AuthorRepository.kt
    presentation/
      AuthorDetailViewModel.kt
      AuthorDetailScreen.kt
  di/
    BookModule.kt
    AuthorModule.kt
```

**Modify:**
- `commonMain/App.kt` — consume `NavigationViewModel`, route all screens
- `androidMain/OpClientApplication.kt` — add `bookModule`, `authorModule`
- `jvmMain/Main.kt` — add `bookModule`, `authorModule`

---

## Navigation

### `Screen.kt`

```kotlin
sealed class Screen {
    data object Search : Screen()
    data class BookDetail(val workKey: String) : Screen()
    data class AuthorDetail(val authorKey: String) : Screen()
}
```

### `NavigationViewModel.kt`

```kotlin
class NavigationViewModel : ViewModel() {
    private val _stack = MutableStateFlow<List<Screen>>(listOf(Screen.Search))
    val stack: StateFlow<List<Screen>> = _stack.asStateFlow()

    val current: Screen get() = _stack.value.last()

    fun navigateTo(screen: Screen) {
        _stack.update { it + screen }
    }

    fun navigateBack() {
        _stack.update { if (it.size > 1) it.dropLast(1) else it }
    }

    fun navigateToTab(destination: Destination) {
        _stack.value = listOf(Screen.Search) // extend when other tabs get sub-screens
    }
}
```

### `App.kt` wiring

```kotlin
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
                onBookClick = { key -> navVm.navigateTo(Screen.BookDetail(key)) }
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

---

## Data Layer

### DTOs (`BookDto.kt`)

```kotlin
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
data class WorkAuthorKeyDto(val key: String)  // "/authors/OL26320A"
```

### DTOs (`AuthorDto.kt`)

```kotlin
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

### Domain Models (`BookModels.kt`)

```kotlin
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

### Domain Models (`AuthorModels.kt`)

```kotlin
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

### Mapper helpers

Both `description` (WorkDto) and `bio` (AuthorDto) are `JsonElement?` that can be either:
- A `JsonPrimitive` string: `"Some text"`
- A `JsonObject`: `{"type": "/type/text", "value": "Some text"}`

Normalize via:
```kotlin
internal fun JsonElement?.toText(): String? = when {
    this == null || this is JsonNull -> null
    this is JsonPrimitive && isString -> contentOrNull
    this is JsonObject -> this["value"]?.jsonPrimitive?.contentOrNull
    else -> null
}
```

Cover URL helpers:
```kotlin
private const val COVER_BASE = "https://covers.openlibrary.org/b/id"
private const val AUTHOR_COVER_BASE = "https://covers.openlibrary.org/a/id"

internal fun List<Int>?.toBookCoverUrl(size: String = "M"): String? =
    this?.firstOrNull()?.let { "$COVER_BASE/$it-$size.jpg" }

internal fun List<Int>?.toAuthorPhotoUrl(size: String = "M"): String? =
    this?.firstOrNull()?.let { "$AUTHOR_COVER_BASE/$it-$size.jpg" }
```

OLID extraction from key path: `"/authors/OL26320A".substringAfterLast("/")` → `"OL26320A"`.

### API Clients

**`BookApiClient.kt`** — extends `ApiClient(baseUrl = "https://openlibrary.org", httpClient)`

```kotlin
suspend fun getWork(workOlid: String): Result<WorkDto, ApiError> =
    get("/works/$workOlid.json")
```

**`AuthorApiClient.kt`** — extends `ApiClient(baseUrl = "https://openlibrary.org", httpClient)`

```kotlin
suspend fun getAuthor(authorOlid: String): Result<AuthorDto, ApiError> =
    get("/authors/$authorOlid.json")

suspend fun getAuthorWorks(authorOlid: String): Result<AuthorWorksDto, ApiError> =
    get("/authors/$authorOlid/works.json")
```

Note: `BookRepositoryImpl` resolves author names by calling `AuthorRepository.getAuthor()` for each author key — not `BookApiClient` directly. This avoids a second cache for author data and ensures author name resolution is always cache-coherent with `AuthorCache`. `BookApiClient` therefore has no `getAuthorSummary` method; remove it from the design above.

### Caches

`BookCache` and `AuthorCache` — same `Mutex`-guarded map pattern as `SearchCache`, TTL = 30 minutes (`CACHE_TTL_MS = 30 * 60 * 1_000L`), injectable `timeSource: () -> Long`.

Cache keys: `"work:{olid}"`, `"author:{olid}"`, `"author-works:{olid}"`.

### Repositories

**Interface (`BookRepository.kt`):**
```kotlin
interface BookRepository {
    suspend fun getBook(workKey: String): Result<BookDetail, ApiError>
}
```

**Implementation (`BookRepositoryImpl.kt`):**

Constructor: `BookRepositoryImpl(apiClient: BookApiClient, cache: BookCache, authorRepository: AuthorRepository)`

1. Extract OLID from `workKey` (e.g., `/works/OL82563W` → `OL82563W`)
2. Check `cache.get("work:$olid")`; hit → return
3. Miss → `apiClient.getWork(olid)`
4. For each author key in `WorkDto.authors`, call `authorRepository.getAuthor(key)` in parallel via `coroutineScope { async { } }` to resolve names (uses `AuthorCache`)
5. Map to `BookDetail`, cache as `"work:$olid"`, return

**Interface (`AuthorRepository.kt`):**
```kotlin
interface AuthorRepository {
    suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError>
}
```

**Implementation (`AuthorRepositoryImpl.kt`):**
1. Extract OLID from `authorKey`
2. Check `cache.get("author:$olid")`; hit → return
3. Miss → fetch profile + works in parallel (`async { }`)
4. Map to `AuthorDetail`, cache, return

---

## MVI Layer

### `DetailStatus`

```kotlin
enum class DetailStatus { Loading, Success, Error }
```

Shared enum in `commonMain` — both ViewModels use it.

### BookDetailViewModel

```kotlin
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

    fun onIntent(intent: BookDetailIntent) { ... }
}
```

Intent handling:
| Intent | Behaviour |
|---|---|
| `Load(key)` | Store `lastKey`. Set `Loading`. Call `repository.getBook(key)`. On success: `Success` + book. On failure: `Error` + emit `LoadError`. |
| `Retry` | Re-fire `Load(lastKey)` |

### AuthorDetailViewModel

Same structure, uses `AuthorRepository`, emits `AuthorDetail`.

---

## Screens

### `BookDetailScreen.kt`

Params: `workKey: String`, `onAuthorClick: (String) -> Unit`, `onBack: () -> Unit`, `viewModel: BookDetailViewModel = koinViewModel()`

`LaunchedEffect(Unit) { viewModel.onIntent(BookDetailIntent.Load(workKey)) }`

Layout:
```
when status:
  Loading → LoadingState() (full screen)
  Error   → ErrorState(onRetry = { viewModel.onIntent(Retry) })
  Success →
    Column (vertically scrollable):
      AsyncImage(coverUrl, size-L, placeholder = surface2, fillMaxWidth, height = 220.dp, contentScale = Crop)
      SectionLabel(book.title, modifier padding)
      Authors row: book.authors.forEach →
        BasicText(author.name, clickable → onAuthorClick(author.key))
      if firstPublishDate != null → BasicText(firstPublishDate)
      Subjects: book.subjects.take(8) → SubjectTag per subject (FlowRow)
      if description != null → BasicText(description)
```

Back button: `SecondaryButton("← BACK", onClick = onBack)` at top of Column (or use a simple clickable `BasicText`).

### `AuthorDetailScreen.kt`

Params: `authorKey: String`, `onBookClick: (String) -> Unit`, `onBack: () -> Unit`, `viewModel: AuthorDetailViewModel = koinViewModel()`

`LaunchedEffect(Unit) { viewModel.onIntent(AuthorDetailIntent.Load(authorKey)) }`

Layout:
```
when status:
  Loading → LoadingState()
  Error   → ErrorState(onRetry = { viewModel.onIntent(Retry) })
  Success →
    LazyColumn:
      item: AsyncImage(photoUrl, circular clip 80dp, placeholder = surface2)
      item: BasicText(author.name) — large title style
      item: dates row (birthDate – deathDate, if present)
      item: if bio != null → BasicText(bio)
      item: SectionLabel("WORKS")
      items(author.works, key = { it.key }):
        BookRow(title = work.title, author = "", coverContent = AsyncImage(work.coverUrl, size-M), onClick = { onBookClick(work.key) })
```

Note: `BookRow` author param is empty string for works list (the screen context makes it redundant).

---

## Koin Modules

### `BookModule.kt`

```kotlin
val bookModule = module {
    single { BookCache() }
    factory { BookApiClient(get()) }
    single<BookRepository> { BookRepositoryImpl(apiClient = get(), cache = get()) }
    viewModel { BookDetailViewModel(get()) }
}
```

### `AuthorModule.kt`

```kotlin
val authorModule = module {
    single { AuthorCache() }
    factory { AuthorApiClient(get()) }
    single<AuthorRepository> { AuthorRepositoryImpl(apiClient = get(), cache = get()) }
    viewModel { AuthorDetailViewModel(get()) }
}
```

Add `NavigationViewModel` to `CommonModule`:
```kotlin
viewModel { NavigationViewModel() }
```

---

## Tests

All in `commonTest/` using `kotlin.test` + Ktor `MockEngine`. No UI tests.

| Class | Covers |
|---|---|
| `BookMapperTest` | `toText()` with string/object/null, cover URL helpers, OLID extraction |
| `AuthorMapperTest` | Same `toText()` paths for bio, photo URL, works list mapping |
| `BookRepositoryTest` | Cache hit skips HTTP, cache miss calls MockEngine, author parallel fetch, API error propagates |
| `AuthorRepositoryTest` | Cache hit, profile+works parallel fetch, API error propagates |
| `BookDetailViewModelTest` | `Load` → Loading → Success, `Load` → Loading → Error + effect, `Retry` re-fetches |
| `AuthorDetailViewModelTest` | Same patterns |
| `NavigationViewModelTest` | `navigateTo` pushes, `navigateBack` pops, back on root is no-op, `navigateToTab` resets stack |

---

## V-Invariants

```
V1: BookRepository ∈ book/domain/ — no Ktor/serialization imports
V2: AuthorRepository ∈ author/domain/ — no Ktor/serialization imports
V3: Screen sealed class drives all navigation — no local nav state in screens
V4: BookDetail.coverUrl ⊥ when covers null or empty
V5: AuthorDetail.photoUrl ⊥ when photos null or empty
V6: description/bio JsonElement? → String? in mapper; never exposed to domain/UI
V7: BookRepositoryImpl fetches author keys in parallel via coroutineScope + async
V8: cache TTL = 30 * 60 * 1_000L ∀ book/author caches
V9: OLID extracted via substringAfterLast("/") from key path ∀ API calls
```
