# op_client Implementation Plan

## Tech Stack

### Platforms
- **Android** — `androidMain` source set, targets API 26+
- **Linux Desktop** — `jvmMain` source set, Compose Desktop on JVM

### Core Libraries
| Purpose | Library | Source Set |
|---------|---------|------------|
| UI | Compose Multiplatform | commonMain |
| HTTP client | Ktor (OkHttp engine on Android, CIO on Desktop) | commonMain + platform |
| JSON | kotlinx.serialization | commonMain |
| Async / state | kotlinx.coroutines + Flow | commonMain |
| Date/time | kotlinx.datetime | commonMain |
| DI | Koin Multiplatform | commonMain + platform |
| Logging | Napier (KMP-compatible) | commonMain |
| Image loading | Coil 3.x (KMP support) | commonMain |

### Source Set Structure
```
commonMain      → business logic, models, repositories, ViewModels, API client interfaces
androidMain     → Ktor OkHttp engine, Android-specific DI bindings
jvmMain         → Ktor CIO engine, Desktop window setup, Linux-specific DI bindings
```

### Presentation Pattern
MVI (Model-View-Intent) with shared ViewModels in `commonMain` using `StateFlow`/`SharedFlow`.
Screen layouts are platform-specific (Compose Desktop `Window` vs Android `Activity`).

### Testing
| Tool | Purpose |
|------|---------|
| kotlin.test | Common unit tests (runs on all targets) |
| MockK | JVM/Android mocking |
| Turbine | Flow testing |
| Ktor MockEngine | API client tests without network |

### Build & Quality
- Gradle Version Catalog (`libs.versions.toml`) for all dependency versions
- Ktlint + Detekt for static analysis
- **Packaging:** Android APK/AAB, Linux fat JAR + RPM/deb

---

## Phase 1: Foundation (Weeks 1-2)

### Week 1: Project Bootstrap
- Initialize KMP project with `android` + `jvm` targets
- Configure Gradle Version Catalog (`libs.versions.toml`)
- Set up Compose Multiplatform (shared UI module)
- Configure Ktor HTTP client:
  - Shared client in `commonMain` (logging, timeout, retry interceptors)
  - OkHttp engine in `androidMain`
  - CIO engine in `jvmMain`
- Configure kotlinx.serialization for all `@Serializable` models
- Set up Koin modules: shared bindings in `commonMain`, platform-specific engines in platform source sets
- Integrate Napier logging with platform backends
- Implement sealed `Result<T, E>` error type for domain errors
- Create base `ApiClient` abstraction in `commonMain`
- Set up Ktlint + Detekt with shared config

### Week 1b: UI Design System
> **Prerequisite:** Week 1 bootstrap complete. KMP project compiles on both targets.
> **Plan:** `docs/superpowers/plans/2026-05-13-ui-design-system.md`
> **Spec:** `docs/superpowers/specs/2026-05-13-ui-design.md`

Builds the custom design system before any feature UI is written. All subsequent weeks consume components and tokens from this system — no Material Design anywhere.

- Font resources (Jost Light/Regular/Medium bundled via Compose Resources)
- Color token system: `AppColors`, `LightColors`, `DarkColors`, `LocalAppColors`
- Typography system: `AppTypography`, `buildTypography()`, `LocalAppTypography`
- Shape constants: `AppShapes` (4dp radius, 2dp cover)
- `AppTheme` composable with system-adaptive dark mode
- Navigation primitives: `Destination` enum, `DestinationIcon` (Canvas-drawn)
- Core components: `SearchInput`, `FilterChip`, `BookRow`, `BookCard`, `SectionLabel`, `SubjectTag`
- Action components: `PrimaryButton`, `SecondaryButton`
- Feedback components: `LoadingState`, `EmptyState`, `ErrorState`
- Platform shells: Android `BottomNavBar` + `AppShell`; Desktop `IconSidebar` + `AppShell`
- Wire `AppTheme` into `MainActivity` and `Main.kt` entry points

### Week 2: Search API Implementation
- Implement OpenLibrary Search API client (`commonMain`)
- Create `@Serializable` search result DTOs and domain mappers
- Build repository: interface in `commonMain`, Koin binding per platform
- Implement in-memory caching with TTL for search results
- Create `SearchViewModel` (MVI: `UiState`, `Intent`, `Effect`) using `StateFlow`
- Build Search screen using design system components (`SearchInput`, `BookRow`, `SectionLabel`, `LoadingState`, `EmptyState`, `ErrorState`)
- Write unit tests using `kotlin.test` + Ktor `MockEngine`
- Verify integration against live OpenLibrary API

---

## Phase 2: Core Features (Weeks 3-4)

### Week 3: Books and Authors Detail
- Implement Books API client for works and editions (`commonMain`)
- Create detailed book DTOs: all metadata fields, `@Serializable`
- Implement Authors API client for profiles and bibliographies
- Build repositories for book and author detail retrieval
- Integrate Covers API for image URLs; load with Coil 3.x in Compose
- Create `BookDetailViewModel` and `AuthorDetailViewModel` (MVI)
- Build shared detail Compose screens; platform screens wire navigation

### Week 4: Subjects and Categories
- Implement Subjects API client for browsing (`commonMain`)
- Create subject taxonomy models and hierarchical navigation state
- Build `SubjectViewModel` with pagination support (Flow-based)
- Add related-content suggestions to book/author detail ViewModels
- Implement advanced search filter state in `SearchViewModel`
- Apply field-selection (`fields` query param) to reduce response payload

---

## Phase 3: Personalization Features (Weeks 5-6)

### Week 5: Lists API Integration
- Implement Lists API client with CRUD operations (`commonMain`)
- Create list management models: `ReadingList`, `ListSeed`
- Build `ListViewModel` (MVI) for personal collection state
- Implement seed add/remove operations with optimistic UI updates
- Add list UI: creation dialog, seed browser, shared Compose components

### Week 6: My Books and Reading Tracking
- Implement My Books API for reading logs and shelves (`commonMain`)
- Create reading progress and shelf models
- Build `ReadingLogViewModel` with goal state
- Integrate user profile endpoint for display name / avatar
- Implement data sync: local cache → remote reconciliation via Flow
- Write integration tests against MockEngine for auth-required endpoints

---

## Phase 4: Advanced Features (Weeks 7-8)

### Week 7: Search Inside and Recent Changes
- Implement Search Inside API for full-text passage search
- Create `SearchInsideViewModel` with highlight and pagination state
- Integrate Recent Changes API for activity feed
- Build notification state for new content (in-app only)
- Add community contribution display (edits, additions)

### Week 8: Polish and Optimization
- Profile startup and scroll performance on Android and Linux Desktop
- Refine Desktop UI: keyboard shortcuts, window sizing/state persistence
- Implement Linux packaging: fat JAR via Gradle, RPM + deb via jpackage
- Accessibility: content descriptions, focus order, keyboard nav
- Comprehensive testing: unit, integration, screenshot tests
- Documentation: KDoc on all public APIs, usage examples
- Final release preparation

---

## Feature Prioritization

### Must Have (MVP)
1. Search across books, authors, subjects
2. Detailed book and author views
3. Cover image display
4. Basic reading list management
5. Platform-appropriate UI on Android and Linux Desktop

### Should Have
1. Subject/category browsing
2. Personal reading log integration
3. List creation and seed management
4. Offline caching for viewed content
5. Advanced search filters

### Could Have
1. Search Inside full-text search
2. Community activity feeds
3. Social features and sharing
4. Export/import capabilities

### Won't Have (Future Considerations)
1. Real-time collaborative features
2. AI-powered recommendations
3. Integrated ebook reader
4. Voice search
5. iOS / web targets (architecture supports adding later)

---

## Development Practices

### Code Quality
- Comprehensive tests: unit (kotlin.test), integration (MockEngine), Flow (Turbine)
- Static analysis: Ktlint + Detekt on every PR
- KDoc on all public `commonMain` APIs
- No platform APIs in `commonMain` — enforce via Detekt rule
- MVI: every screen has `UiState`, `Intent`, `Effect` — no ad-hoc state

### Version Control
- Feature branches for all new functionality
- PRs require passing CI before merge
- Conventional commit messages (`feat:`, `fix:`, `refactor:`, etc.)
- Tagged releases with changelogs (semantic versioning)

### Continuous Integration (GitHub Actions)
- `./gradlew build` — verifies compilation on all targets
- `./gradlew allTests` — runs common + Android + JVM tests
- `./gradlew ktlintCheck detekt` — quality gates
- Android APK artifact upload per PR
- Linux fat JAR artifact upload per PR; RPM + deb on tagged releases

---

## Risk Mitigation

### Technical Risks
| Risk | Mitigation |
|------|-----------|
| OpenLibrary rate limiting | Aggressive caching with TTL; exponential backoff in Ktor interceptor |
| Experimental API changes | Thin adapter layer in `commonMain`; DTOs separate from domain models |
| expect/actual abstraction boundary errors | Detekt rule: no platform imports in `commonMain`; CI enforces |
| Compose Desktop stability on Linux | Pin Compose Multiplatform version; test on target Linux distro in CI |
| Auth-required APIs (Lists, My Books) | Mock responses in tests; document auth flow before implementing |
| JVM platform code leaking to iOS-incompatible patterns | Keep `jvmAndroid` source set ready; use kotlinx.* over JVM-specific libs |

### Schedule Risks
| Risk | Mitigation |
|------|-----------|
| API complexity delays | Time-box investigation spikes to 1 day |
| Desktop-specific challenges | Parallel development; platform screens are thin wrappers |
| Integration complexities | Ktor MockEngine for deterministic testing without real network |

---

## Success Metrics

### Quantitative
- API response time < 2 seconds (p95)
- App startup time < 3 seconds (cold start, both platforms)
- Offline functionality for 50+ previously viewed items
- 90%+ test coverage on `commonMain` business logic
- < 5 crashes per 1000 sessions

### Qualitative
- Identical feature parity across Android and Linux Desktop
- Smooth scroll and navigation experience
- Reliable offline functionality
- Fast and intuitive personal collection management

---

## Milestones

### Milestone 1: Basic Discovery (End of Week 2 / Week 1b+2)
- KMP project compiles on Android and Linux Desktop
- Design system in place: custom theme, all core components, platform navigation shells
- Functional search across books/authors/subjects
- Cover image display
- Android APK and Linux Desktop JAR both runnable

### Milestone 2: Detailed Content (End of Week 4)
- Complete book and author detail views
- Subject browsing and navigation
- Performance meeting baseline metrics

### Milestone 3: Personalization (End of Week 6)
- Reading list CRUD
- Personal reading log integration
- Basic offline cache for viewed content

### Milestone 4: Complete Application (End of Week 8)
- All planned features implemented and tested
- Linux RPM + deb packages built via jpackage
- Full KDoc documentation
- CI green, ready for public release
