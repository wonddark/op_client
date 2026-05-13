# UI Design System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a custom Compose Multiplatform design system (no Material Design) using the Jost typeface, sage green palette, 4dp radius, system-adaptive dark mode, and platform-specific navigation shells for Android (bottom nav) and Linux Desktop (icon sidebar).

**Architecture:** All theme tokens, typography, and components live in `commonMain` as pure Compose Multiplatform composables backed by `CompositionLocal`. Platform-specific navigation shells (`AppShell`) live in `androidMain` / `jvmMain` and wrap the shared `content` slot. No `MaterialTheme` wrapper exists anywhere in the tree.

**Tech Stack:** Compose Multiplatform (`compose.ui`, `compose.foundation`, `compose.animation`), Compose Resources for bundled Jost font, `kotlin.test` + `compose.uiTest` for testing.

---

## File Map

### commonMain
| File | Responsibility |
|------|---------------|
| `ui/theme/AppColors.kt` | `AppColors` data class + `LightColors` + `DarkColors` instances |
| `ui/theme/AppTypography.kt` | `AppTypography` data class + `buildTypography(FontFamily)` factory |
| `ui/theme/AppShapes.kt` | Shape radius constants |
| `ui/theme/AppTheme.kt` | `AppTheme` composable + `CompositionLocal` providers + `AppThemeTokens` accessor |
| `ui/navigation/Destination.kt` | `Destination` enum with label |
| `ui/navigation/DestinationIcon.kt` | Canvas-drawn icons per `Destination` |
| `ui/components/SectionLabel.kt` | Section header text |
| `ui/components/SubjectTag.kt` | Tag/chip pill |
| `ui/components/SearchInput.kt` | Full-width search input |
| `ui/components/FilterChip.kt` | Toggleable filter chip |
| `ui/components/BookRow.kt` | List item: cover + title + author + tag |
| `ui/components/BookCard.kt` | Grid card: dominant cover + title + author (Browse screen) |
| `ui/components/PrimaryButton.kt` | Filled accent button |
| `ui/components/SecondaryButton.kt` | Outlined button |
| `ui/components/feedback/LoadingState.kt` | Animated spinner |
| `ui/components/feedback/EmptyState.kt` | Centered empty label |
| `ui/components/feedback/ErrorState.kt` | Error text + retry link |

### androidMain
| File | Responsibility |
|------|---------------|
| `ui/navigation/BottomNavBar.kt` | 3-destination bottom nav |
| `ui/AppShell.kt` | Android content wrapper: `Column { content; BottomNavBar }` |

### jvmMain
| File | Responsibility |
|------|---------------|
| `ui/navigation/IconSidebar.kt` | 56dp icon rail with 3 destinations |
| `ui/AppShell.kt` | Desktop content wrapper: `Row { IconSidebar; content }` |

### Resources
| Path | Content |
|------|---------|
| `src/commonMain/composeResources/font/Jost_Light.ttf` | Jost weight 300 |
| `src/commonMain/composeResources/font/Jost_Regular.ttf` | Jost weight 400 |
| `src/commonMain/composeResources/font/Jost_Medium.ttf` | Jost weight 500 |

### Tests
| File | Tests |
|------|-------|
| `src/commonTest/kotlin/.../ui/theme/AppColorsTest.kt` | Token value assertions |
| `src/commonTest/kotlin/.../ui/theme/AppThemeTest.kt` | CompositionLocal resolution |
| `src/androidUnitTest/kotlin/.../ui/components/SearchInputTest.kt` | Placeholder, value change |
| `src/androidUnitTest/kotlin/.../ui/components/BookRowTest.kt` | Click, selected state |
| `src/androidUnitTest/kotlin/.../ui/components/BookCardTest.kt` | Click, title/author display |
| `src/androidUnitTest/kotlin/.../ui/components/ButtonTest.kt` | Enabled/disabled states |
| `src/androidUnitTest/kotlin/.../ui/navigation/BottomNavBarTest.kt` | Selection, callback |

---

## Task 1: Font resources + Gradle configuration

**Files:**
- Create: `src/commonMain/composeResources/font/Jost_Light.ttf`
- Create: `src/commonMain/composeResources/font/Jost_Regular.ttf`
- Create: `src/commonMain/composeResources/font/Jost_Medium.ttf`
- Modify: `build.gradle.kts` (or the module that contains shared UI)

- [ ] **Step 1: Download Jost font files**

Go to https://fonts.google.com/specimen/Jost, click "Download family". From the zip, extract:
- `static/Jost-Light.ttf` → rename to `Jost_Light.ttf`
- `static/Jost-Regular.ttf` → rename to `Jost_Regular.ttf`
- `static/Jost-Medium.ttf` → rename to `Jost_Medium.ttf`

(Underscores required — Kotlin identifier generated from filename cannot contain hyphens.)

- [ ] **Step 2: Place font files**

```
src/commonMain/composeResources/font/Jost_Light.ttf
src/commonMain/composeResources/font/Jost_Regular.ttf
src/commonMain/composeResources/font/Jost_Medium.ttf
```

Create the directory if it doesn't exist: `mkdir -p src/commonMain/composeResources/font`

- [ ] **Step 3: Configure compose resources in build.gradle.kts**

Add to the module's `build.gradle.kts`:

```kotlin
plugins {
    // existing plugins…
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.runtime)
        }
        commonTest.dependencies {
            implementation(compose.uiTest)
            implementation(kotlin("test"))
        }
        androidMain.dependencies {
            implementation(compose.uiTooling)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.opclient.resources"
    generateResClass = always
}
```

- [ ] **Step 4: Sync and verify resource generation**

```bash
./gradlew generateComposeResClass
```

Expected: `build/generated/compose/resourceGenerator/kotlin/…/Res.kt` contains `object font { val Jost_Light … }`. No errors.

- [ ] **Step 5: Commit**

```bash
git add src/commonMain/composeResources/font/ build.gradle.kts
git commit -m "chore: add Jost font resources and compose.resources config"
```

---

## Task 2: Color token system

**Files:**
- Create: `src/commonMain/kotlin/com/opclient/ui/theme/AppColors.kt`
- Create: `src/commonTest/kotlin/com/opclient/ui/theme/AppColorsTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
// src/commonTest/kotlin/com/opclient/ui/theme/AppColorsTest.kt
package com.opclient.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class AppColorsTest {

    @Test
    fun lightColors_accent_isCorrect() {
        assertEquals(Color(0xFF3A7D44), LightColors.accent)
    }

    @Test
    fun lightColors_background_isCorrect() {
        assertEquals(Color(0xFFF7F8F6), LightColors.background)
    }

    @Test
    fun darkColors_accent_isCorrect() {
        assertEquals(Color(0xFF6AB874), DarkColors.accent)
    }

    @Test
    fun darkColors_background_isCorrect() {
        assertEquals(Color(0xFF111C10), DarkColors.background)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :shared:commonTest --tests "com.opclient.ui.theme.AppColorsTest"
```

Expected: FAIL — `LightColors` not defined.

- [ ] **Step 3: Implement AppColors**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/theme/AppColors.kt
package com.opclient.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val accentLight: Color,
)

val LightColors = AppColors(
    background   = Color(0xFFF7F8F6),
    surface      = Color(0xFFFFFFFF),
    surface2     = Color(0xFFEEF1EB),
    border       = Color(0xFFDDE4DA),
    textPrimary  = Color(0xFF1F2B1E),
    textSecondary= Color(0xFF7A8C76),
    accent       = Color(0xFF3A7D44),
    accentLight  = Color(0xFFEEF1EB),
)

val DarkColors = AppColors(
    background   = Color(0xFF111C10),
    surface      = Color(0xFF1A2A18),
    surface2     = Color(0xFF243322),
    border       = Color(0xFF2E4A2C),
    textPrimary  = Color(0xFFE4EDE2),
    textSecondary= Color(0xFF7A9C76),
    accent       = Color(0xFF6AB874),
    accentLight  = Color(0xFF1E3A1C),
)

val LocalAppColors = staticCompositionLocalOf<AppColors> {
    error("No AppColors provided — wrap content in AppTheme")
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :shared:commonTest --tests "com.opclient.ui.theme.AppColorsTest"
```

Expected: PASS, 4 tests.

- [ ] **Step 5: Commit**

```bash
git add src/commonMain/kotlin/com/opclient/ui/theme/AppColors.kt \
        src/commonTest/kotlin/com/opclient/ui/theme/AppColorsTest.kt
git commit -m "feat: add AppColors token system with light and dark palettes"
```

---

## Task 3: Typography system

**Files:**
- Create: `src/commonMain/kotlin/com/opclient/ui/theme/AppTypography.kt`

- [ ] **Step 1: Implement AppTypography**

No pure-logic unit test for typography (values are design decisions, not invariants). Implement directly:

```kotlin
// src/commonMain/kotlin/com/opclient/ui/theme/AppTypography.kt
package com.opclient.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.opclient.resources.Res
import com.opclient.resources.font.Jost_Light
import com.opclient.resources.font.Jost_Medium
import com.opclient.resources.font.Jost_Regular
import org.jetbrains.compose.resources.Font

data class AppTypography(
    val screenTitle: TextStyle,
    val sectionLabel: TextStyle,
    val navLabel: TextStyle,
    val bookTitle: TextStyle,
    val bookAuthor: TextStyle,
    val tag: TextStyle,
    val body: TextStyle,
    val button: TextStyle,
)

@Composable
fun jostFontFamily(): FontFamily = FontFamily(
    Font(Res.font.Jost_Light,   weight = FontWeight.Light),
    Font(Res.font.Jost_Regular, weight = FontWeight.Normal),
    Font(Res.font.Jost_Medium,  weight = FontWeight.Medium),
)

fun buildTypography(fontFamily: FontFamily) = AppTypography(
    screenTitle  = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 14.sp, letterSpacing = 3.sp),
    sectionLabel = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 9.sp,  letterSpacing = 2.5.sp),
    navLabel     = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 8.sp,  letterSpacing = 1.5.sp),
    bookTitle    = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.3.sp),
    bookAuthor   = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 10.sp, letterSpacing = 2.sp),
    tag          = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 9.sp,  letterSpacing = 1.sp),
    body         = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Light,  fontSize = 12.sp, letterSpacing = 0.sp),
    button       = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = 9.sp,  letterSpacing = 2.sp),
)

val LocalAppTypography = staticCompositionLocalOf<AppTypography> {
    error("No AppTypography provided — wrap content in AppTheme")
}
```

Note: uppercase text transform is applied at call-site via `.uppercase()` on the string — `TextStyle` has no text transform property in Compose.

- [ ] **Step 2: Commit**

```bash
git add src/commonMain/kotlin/com/opclient/ui/theme/AppTypography.kt
git commit -m "feat: add AppTypography with Jost font family"
```

---

## Task 4: AppShapes + AppTheme composable

**Files:**
- Create: `src/commonMain/kotlin/com/opclient/ui/theme/AppShapes.kt`
- Create: `src/commonMain/kotlin/com/opclient/ui/theme/AppTheme.kt`
- Create: `src/commonTest/kotlin/com/opclient/ui/theme/AppThemeTest.kt`

- [ ] **Step 1: Implement AppShapes**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/theme/AppShapes.kt
package com.opclient.ui.theme

import androidx.compose.ui.unit.dp

object AppShapes {
    val radius = 4.dp
    val coverRadius = 2.dp  // book cover thumbnails: harder edge, intentional framing
}
```

- [ ] **Step 2: Write failing test for AppTheme**

```kotlin
// src/commonTest/kotlin/com/opclient/ui/theme/AppThemeTest.kt
package com.opclient.ui.theme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppThemeTest {

    @Test
    fun appTheme_providesLightColors_whenDarkFalse() = runComposeUiTest {
        var captured: AppColors? = null
        setContent {
            AppTheme(darkTheme = false) {
                captured = LocalAppColors.current
            }
        }
        assertEquals(LightColors.accent, captured?.accent)
        assertEquals(LightColors.background, captured?.background)
    }

    @Test
    fun appTheme_providesDarkColors_whenDarkTrue() = runComposeUiTest {
        var captured: AppColors? = null
        setContent {
            AppTheme(darkTheme = true) {
                captured = LocalAppColors.current
            }
        }
        assertEquals(DarkColors.accent, captured?.accent)
        assertEquals(DarkColors.background, captured?.background)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

```bash
./gradlew :shared:commonTest --tests "com.opclient.ui.theme.AppThemeTest"
```

Expected: FAIL — `AppTheme` not defined.

- [ ] **Step 4: Implement AppTheme**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/theme/AppTheme.kt
package com.opclient.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val typography = buildTypography(jostFontFamily())
    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalAppTypography provides typography,
        content = content,
    )
}

// Accessor object — use instead of LocalAppColors.current / LocalAppTypography.current
object AppThemeTokens {
    val colors: AppColors
        @Composable @ReadOnlyComposable get() = LocalAppColors.current
    val typography: AppTypography
        @Composable @ReadOnlyComposable get() = LocalAppTypography.current
    val shapes: AppShapes = AppShapes
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
./gradlew :shared:commonTest --tests "com.opclient.ui.theme.AppThemeTest"
```

Expected: PASS, 2 tests.

- [ ] **Step 6: Commit**

```bash
git add src/commonMain/kotlin/com/opclient/ui/theme/AppShapes.kt \
        src/commonMain/kotlin/com/opclient/ui/theme/AppTheme.kt \
        src/commonTest/kotlin/com/opclient/ui/theme/AppThemeTest.kt
git commit -m "feat: add AppShapes and AppTheme with system-adaptive dark mode"
```

---

## Task 5: Destination enum + DestinationIcon

**Files:**
- Create: `src/commonMain/kotlin/com/opclient/ui/navigation/Destination.kt`
- Create: `src/commonMain/kotlin/com/opclient/ui/navigation/DestinationIcon.kt`

- [ ] **Step 1: Implement Destination enum**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/navigation/Destination.kt
package com.opclient.ui.navigation

enum class Destination(val label: String) {
    SEARCH("Search"),
    BROWSE("Browse"),
    LIBRARY("Library"),
}
```

- [ ] **Step 2: Implement DestinationIcon using Canvas**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/navigation/DestinationIcon.kt
package com.opclient.ui.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun DestinationIcon(
    destination: Destination,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    // Modifier.size(size).then(modifier) — sets default size, caller can override
    Canvas(modifier = Modifier.size(size).then(modifier)) {
        val s = this.size.width
        val strokeWidth = 1.5.dp.toPx()
        when (destination) {
            Destination.SEARCH -> {
                drawCircle(color = tint, radius = s * 0.32f, center = Offset(s * 0.42f, s * 0.42f), style = Stroke(width = strokeWidth))
                drawLine(color = tint, start = Offset(s * 0.66f, s * 0.66f), end = Offset(s * 0.88f, s * 0.88f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            Destination.BROWSE -> {
                val r = 2.dp.toPx()
                listOf(
                    Offset(s * 0.30f, s * 0.30f), Offset(s * 0.70f, s * 0.30f),
                    Offset(s * 0.30f, s * 0.70f), Offset(s * 0.70f, s * 0.70f),
                ).forEach { drawCircle(color = tint, radius = r, center = it) }
            }
            Destination.LIBRARY -> {
                val x0 = s * 0.15f; val x1 = s * 0.85f
                listOf(0.30f, 0.50f, 0.70f).forEach { y ->
                    drawLine(color = tint, start = Offset(x0, s * y), end = Offset(x1, s * y), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                }
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/commonMain/kotlin/com/opclient/ui/navigation/
git commit -m "feat: add Destination enum and Canvas-drawn DestinationIcon"
```

---

## Task 6: SectionLabel + SubjectTag components

**Files:**
- Create: `src/commonMain/kotlin/com/opclient/ui/components/SectionLabel.kt`
- Create: `src/commonMain/kotlin/com/opclient/ui/components/SubjectTag.kt`

- [ ] **Step 1: Implement SectionLabel**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/SectionLabel.kt
package com.opclient.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    Text(
        text = text.uppercase(),
        style = typography.sectionLabel.copy(color = colors.textSecondary),
        modifier = modifier,
    )
}
```

Wait — `Text` is from `compose.material3` or `compose.foundation`. Use `androidx.compose.foundation.text.BasicText` or simply `androidx.compose.material3.Text`? Since we're excluding Material, use `androidx.compose.foundation.text.BasicText`:

Actually, `Text` composable is available from both `compose.material3` and also can be accessed via `compose.foundation` as `BasicText`. However, `BasicText` requires a different API. The cleanest no-Material approach: import `Text` from `androidx.compose.material3` is fine for the Text widget specifically — Material Design exclusion means no `MaterialTheme`, `Button`, `TextField`, `Scaffold` etc. Using `Text` from material3 as a text renderer is fine since it reads from `LocalTextStyle` which we control, not from `MaterialTheme`.

However, to be fully independent: use `androidx.compose.foundation.text.BasicText` with explicit `TextStyle`. For simplicity and correctness, use `Text` from `androidx.compose.material3` — it simply renders text and doesn't impose Material design.

Revised (no-Material-workaround needed for text rendering — `material3.Text` is fine):

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/SectionLabel.kt
package com.opclient.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    BasicText(
        text = text.uppercase(),
        style = typography.sectionLabel.copy(color = colors.textSecondary),
        modifier = modifier,
    )
}
```

- [ ] **Step 2: Implement SubjectTag**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/SubjectTag.kt
package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun SubjectTag(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    Box(
        modifier = modifier
            .background(color = colors.accentLight, shape = RoundedCornerShape(AppShapes.radius))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        BasicText(
            text = text.uppercase(),
            style = typography.tag.copy(color = colors.accent),
        )
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/commonMain/kotlin/com/opclient/ui/components/SectionLabel.kt \
        src/commonMain/kotlin/com/opclient/ui/components/SubjectTag.kt
git commit -m "feat: add SectionLabel and SubjectTag components"
```

---

## Task 7: SearchInput + FilterChip components

**Files:**
- Create: `src/commonMain/kotlin/com/opclient/ui/components/SearchInput.kt`
- Create: `src/commonMain/kotlin/com/opclient/ui/components/FilterChip.kt`
- Create: `src/androidUnitTest/kotlin/com/opclient/ui/components/SearchInputTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
// src/androidUnitTest/kotlin/com/opclient/ui/components/SearchInputTest.kt
package com.opclient.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.opclient.ui.theme.AppTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchInputTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun searchInput_showsPlaceholder_whenValueEmpty() {
        composeTestRule.setContent {
            AppTheme { SearchInput(value = "", onValueChange = {}) }
        }
        composeTestRule.onNodeWithText("FIND BOOKS, AUTHORS…").assertIsDisplayed()
    }

    @Test
    fun searchInput_callsOnValueChange_whenTextEntered() {
        var captured = ""
        composeTestRule.setContent {
            AppTheme { SearchInput(value = "", onValueChange = { captured = it }) }
        }
        composeTestRule.onNode(hasSetTextAction()).performTextInput("Middlemarch")
        assertEquals("Middlemarch", captured)
    }

    @Test
    fun searchInput_hidesPlaceholder_whenValueNonEmpty() {
        composeTestRule.setContent {
            AppTheme { SearchInput(value = "Middlemarch", onValueChange = {}) }
        }
        composeTestRule.onNodeWithText("FIND BOOKS, AUTHORS…").assertDoesNotExist()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :shared:connectedAndroidTest --tests "com.opclient.ui.components.SearchInputTest"
```

Expected: FAIL — `SearchInput` not defined.

- [ ] **Step 3: Implement SearchInput**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/SearchInput.kt
package com.opclient.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens
import androidx.compose.foundation.text.BasicText

@Composable
fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
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
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = colors.surface, shape = shape)
                    .border(width = 1.dp, color = colors.border, shape = shape)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(8.dp))
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

- [ ] **Step 4: Implement FilterChip**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/FilterChip.kt
package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    val shape = RoundedCornerShape(AppShapes.radius)
    val bgColor = if (selected) colors.accentLight else colors.surface
    val textColor = if (selected) colors.accent else colors.textSecondary
    val borderColor = if (selected) colors.accent else colors.border

    Box(
        modifier = modifier
            .background(color = bgColor, shape = shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        BasicText(
            text = label.uppercase(),
            style = typography.tag.copy(color = textColor),
        )
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :shared:connectedAndroidTest --tests "com.opclient.ui.components.SearchInputTest"
```

Expected: PASS, 3 tests.

- [ ] **Step 6: Commit**

```bash
git add src/commonMain/kotlin/com/opclient/ui/components/SearchInput.kt \
        src/commonMain/kotlin/com/opclient/ui/components/FilterChip.kt \
        src/androidUnitTest/kotlin/com/opclient/ui/components/SearchInputTest.kt
git commit -m "feat: add SearchInput and FilterChip components"
```

---

## Task 8: BookRow component

**Files:**
- Create: `src/commonMain/kotlin/com/opclient/ui/components/BookRow.kt`
- Create: `src/androidUnitTest/kotlin/com/opclient/ui/components/BookRowTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
// src/androidUnitTest/kotlin/com/opclient/ui/components/BookRowTest.kt
package com.opclient.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.opclient.ui.theme.AppTheme
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class BookRowTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun bookRow_displaysTitle() {
        composeTestRule.setContent {
            AppTheme {
                BookRow(title = "Middlemarch", author = "George Eliot", onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Middlemarch").assertIsDisplayed()
    }

    @Test
    fun bookRow_displaysAuthorUppercased() {
        composeTestRule.setContent {
            AppTheme {
                BookRow(title = "Middlemarch", author = "George Eliot", onClick = {})
            }
        }
        composeTestRule.onNodeWithText("GEORGE ELIOT").assertIsDisplayed()
    }

    @Test
    fun bookRow_callsOnClick_whenTapped() {
        var clicked = false
        composeTestRule.setContent {
            AppTheme {
                BookRow(title = "Middlemarch", author = "George Eliot", onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("Middlemarch").performClick()
        assertTrue(clicked)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :shared:connectedAndroidTest --tests "com.opclient.ui.components.BookRowTest"
```

Expected: FAIL — `BookRow` not defined.

- [ ] **Step 3: Implement BookRow**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/BookRow.kt
package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun BookRow(
    title: String,
    author: String,
    subject: String? = null,
    coverWidth: Dp = 34.dp,
    coverHeight: Dp = 48.dp,
    coverContent: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) colors.surface2 else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = coverWidth, height = coverHeight)
                .clip(RoundedCornerShape(AppShapes.coverRadius)),
            content = coverContent,
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).padding(top = 2.dp)) {
            BasicText(text = title, style = typography.bookTitle.copy(color = colors.textPrimary))
            Spacer(Modifier.height(3.dp))
            BasicText(text = author.uppercase(), style = typography.bookAuthor.copy(color = colors.textSecondary))
            if (subject != null) {
                Spacer(Modifier.height(5.dp))
                SubjectTag(text = subject)
            }
        }
    }
}
```

Fix missing import — add `import androidx.compose.foundation.layout.width`.

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :shared:connectedAndroidTest --tests "com.opclient.ui.components.BookRowTest"
```

Expected: PASS, 3 tests.

- [ ] **Step 5: Commit**

```bash
git add src/commonMain/kotlin/com/opclient/ui/components/BookRow.kt \
        src/androidUnitTest/kotlin/com/opclient/ui/components/BookRowTest.kt
git commit -m "feat: add BookRow list item component"
```

---

## Task 9: PrimaryButton + SecondaryButton

**Files:**
- Create: `src/commonMain/kotlin/com/opclient/ui/components/PrimaryButton.kt`
- Create: `src/commonMain/kotlin/com/opclient/ui/components/SecondaryButton.kt`
- Create: `src/androidUnitTest/kotlin/com/opclient/ui/components/ButtonTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
// src/androidUnitTest/kotlin/com/opclient/ui/components/ButtonTest.kt
package com.opclient.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.opclient.ui.theme.AppTheme
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import org.junit.Rule
import org.junit.Test

class ButtonTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun primaryButton_showsLabelUppercased() {
        composeTestRule.setContent {
            AppTheme { PrimaryButton(text = "save", onClick = {}) }
        }
        composeTestRule.onNodeWithText("SAVE").assertIsDisplayed()
    }

    @Test
    fun primaryButton_callsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            AppTheme { PrimaryButton(text = "Save", onClick = { clicked = true }) }
        }
        composeTestRule.onNodeWithText("SAVE").performClick()
        assertTrue(clicked)
    }

    @Test
    fun primaryButton_doesNotCallOnClick_whenDisabled() {
        var clicked = false
        composeTestRule.setContent {
            AppTheme { PrimaryButton(text = "Save", onClick = { clicked = true }, enabled = false) }
        }
        composeTestRule.onNodeWithText("SAVE").performClick()
        assertFalse(clicked)
    }

    @Test
    fun secondaryButton_showsLabel() {
        composeTestRule.setContent {
            AppTheme { SecondaryButton(text = "cancel", onClick = {}) }
        }
        composeTestRule.onNodeWithText("CANCEL").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :shared:connectedAndroidTest --tests "com.opclient.ui.components.ButtonTest"
```

Expected: FAIL — `PrimaryButton` not defined.

- [ ] **Step 3: Implement PrimaryButton**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/PrimaryButton.kt
package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    val bgColor = if (enabled) colors.accent else colors.border

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(AppShapes.radius))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text.uppercase(),
            style = typography.button.copy(color = Color.White),
        )
    }
}
```

- [ ] **Step 4: Implement SecondaryButton**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/SecondaryButton.kt
package com.opclient.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    Box(
        modifier = modifier
            .border(width = 1.dp, color = colors.border, shape = RoundedCornerShape(AppShapes.radius))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text.uppercase(),
            style = typography.button.copy(color = colors.textSecondary),
        )
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :shared:connectedAndroidTest --tests "com.opclient.ui.components.ButtonTest"
```

Expected: PASS, 4 tests.

- [ ] **Step 6: Commit**

```bash
git add src/commonMain/kotlin/com/opclient/ui/components/PrimaryButton.kt \
        src/commonMain/kotlin/com/opclient/ui/components/SecondaryButton.kt \
        src/androidUnitTest/kotlin/com/opclient/ui/components/ButtonTest.kt
git commit -m "feat: add PrimaryButton and SecondaryButton components"
```

---

## Task 10: Feedback state components

**Files:**
- Create: `src/commonMain/kotlin/com/opclient/ui/components/feedback/LoadingState.kt`
- Create: `src/commonMain/kotlin/com/opclient/ui/components/feedback/EmptyState.kt`
- Create: `src/commonMain/kotlin/com/opclient/ui/components/feedback/ErrorState.kt`

- [ ] **Step 1: Implement LoadingState**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/feedback/LoadingState.kt
package com.opclient.ui.components.feedback

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val colors = AppThemeTokens.colors
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing)),
    )
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(24.dp)) {
            drawArc(
                color = colors.accent,
                startAngle = rotation,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}
```

- [ ] **Step 2: Implement EmptyState**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/feedback/EmptyState.kt
package com.opclient.ui.components.feedback

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText(
            text = message.uppercase(),
            style = typography.sectionLabel.copy(color = colors.textSecondary),
        )
    }
}
```

- [ ] **Step 3: Implement ErrorState**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/feedback/ErrorState.kt
package com.opclient.ui.components.feedback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BasicText(text = message, style = typography.body.copy(color = colors.textSecondary))
        if (onRetry != null) {
            Spacer(Modifier.height(8.dp))
            BasicText(
                text = "RETRY",
                style = typography.button.copy(color = colors.accent),
                modifier = Modifier.clickable(onClick = onRetry),
            )
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/commonMain/kotlin/com/opclient/ui/components/feedback/
git commit -m "feat: add LoadingState, EmptyState, ErrorState feedback components"
```

---

## Task 10b: BookCard component

**Files:**
- Create: `src/commonMain/kotlin/com/opclient/ui/components/BookCard.kt`
- Create: `src/androidUnitTest/kotlin/com/opclient/ui/components/BookCardTest.kt`

Used by the Browse screen grid layout. Cover image is dominant; title + author below.

- [ ] **Step 1: Write failing test**

```kotlin
// src/androidUnitTest/kotlin/com/opclient/ui/components/BookCardTest.kt
package com.opclient.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.opclient.ui.theme.AppTheme
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

class BookCardTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun bookCard_displaysTitleAndAuthor() {
        composeTestRule.setContent {
            AppTheme {
                BookCard(title = "Middlemarch", author = "George Eliot", onClick = {})
            }
        }
        composeTestRule.onNodeWithText("Middlemarch").assertIsDisplayed()
        composeTestRule.onNodeWithText("GEORGE ELIOT").assertIsDisplayed()
    }

    @Test
    fun bookCard_callsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            AppTheme {
                BookCard(title = "Middlemarch", author = "George Eliot", onClick = { clicked = true })
            }
        }
        composeTestRule.onNodeWithText("Middlemarch").performClick()
        assertTrue(clicked)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :shared:connectedAndroidTest --tests "com.opclient.ui.components.BookCardTest"
```

Expected: FAIL — `BookCard` not defined.

- [ ] **Step 3: Implement BookCard**

```kotlin
// src/commonMain/kotlin/com/opclient/ui/components/BookCard.kt
package com.opclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.clip
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun BookCard(
    title: String,
    author: String,
    coverContent: @Composable BoxScope.() -> Unit = {},
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(AppShapes.coverRadius))
                .background(colors.surface2),
            content = coverContent,
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = title,
            style = typography.bookTitle.copy(color = colors.textPrimary),
            maxLines = 2,
        )
        Spacer(Modifier.height(2.dp))
        BasicText(
            text = author.uppercase(),
            style = typography.bookAuthor.copy(color = colors.textSecondary),
            maxLines = 1,
        )
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew :shared:connectedAndroidTest --tests "com.opclient.ui.components.BookCardTest"
```

Expected: PASS, 2 tests.

- [ ] **Step 5: Commit**

```bash
git add src/commonMain/kotlin/com/opclient/ui/components/BookCard.kt \
        src/androidUnitTest/kotlin/com/opclient/ui/components/BookCardTest.kt
git commit -m "feat: add BookCard grid component for Browse screen"
```

---

## Task 11: Android navigation shell

**Files:**
- Create: `src/androidMain/kotlin/com/opclient/ui/navigation/BottomNavBar.kt`
- Create: `src/androidMain/kotlin/com/opclient/ui/AppShell.kt`
- Create: `src/androidUnitTest/kotlin/com/opclient/ui/navigation/BottomNavBarTest.kt`

- [ ] **Step 1: Write failing test**

```kotlin
// src/androidUnitTest/kotlin/com/opclient/ui/navigation/BottomNavBarTest.kt
package com.opclient.ui.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.opclient.ui.theme.AppTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class BottomNavBarTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun bottomNavBar_showsAllDestinationLabels() {
        composeTestRule.setContent {
            AppTheme {
                BottomNavBar(selected = Destination.SEARCH, onSelect = {})
            }
        }
        composeTestRule.onNodeWithText("SEARCH").assertIsDisplayed()
        composeTestRule.onNodeWithText("BROWSE").assertIsDisplayed()
        composeTestRule.onNodeWithText("LIBRARY").assertIsDisplayed()
    }

    @Test
    fun bottomNavBar_callsOnSelect_withCorrectDestination() {
        var selected: Destination? = null
        composeTestRule.setContent {
            AppTheme {
                BottomNavBar(selected = Destination.SEARCH, onSelect = { selected = it })
            }
        }
        composeTestRule.onNodeWithText("BROWSE").performClick()
        assertEquals(Destination.BROWSE, selected)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
./gradlew :shared:connectedAndroidTest --tests "com.opclient.ui.navigation.BottomNavBarTest"
```

Expected: FAIL — `BottomNavBar` not defined.

- [ ] **Step 3: Implement BottomNavBar**

```kotlin
// src/androidMain/kotlin/com/opclient/ui/navigation/BottomNavBar.kt
package com.opclient.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun BottomNavBar(
    selected: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val typography = AppThemeTokens.typography
    val borderColor = colors.border

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(colors.surface)
            .drawBehind {
                drawLine(color = borderColor, start = Offset(0f, 0f), end = Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            },
    ) {
        Destination.entries.forEach { destination ->
            val isSelected = destination == selected
            val tint = if (isSelected) colors.accent else colors.textSecondary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(destination) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                DestinationIcon(destination = destination, tint = tint, size = 20.dp)
                Spacer(Modifier.height(3.dp))
                BasicText(
                    text = destination.label.uppercase(),
                    style = typography.navLabel.copy(
                        color = tint,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Light,
                    ),
                )
            }
        }
    }
}
```

- [ ] **Step 4: Implement Android AppShell**

```kotlin
// src/androidMain/kotlin/com/opclient/ui/AppShell.kt
package com.opclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.opclient.ui.navigation.BottomNavBar
import com.opclient.ui.navigation.Destination
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun AppShell(
    selectedDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = AppThemeTokens.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Box(modifier = Modifier.weight(1f)) { content() }
        BottomNavBar(selected = selectedDestination, onSelect = onDestinationChange)
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
./gradlew :shared:connectedAndroidTest --tests "com.opclient.ui.navigation.BottomNavBarTest"
```

Expected: PASS, 2 tests.

- [ ] **Step 6: Commit**

```bash
git add src/androidMain/kotlin/com/opclient/ui/navigation/BottomNavBar.kt \
        src/androidMain/kotlin/com/opclient/ui/AppShell.kt \
        src/androidUnitTest/kotlin/com/opclient/ui/navigation/BottomNavBarTest.kt
git commit -m "feat: add Android BottomNavBar and AppShell"
```

---

## Task 12: Desktop navigation shell

**Files:**
- Create: `src/jvmMain/kotlin/com/opclient/ui/navigation/IconSidebar.kt`
- Create: `src/jvmMain/kotlin/com/opclient/ui/AppShell.kt`

No automated UI test for Desktop shell (Compose Desktop test infra requires a running display; verify manually on Linux).

- [ ] **Step 1: Implement IconSidebar**

```kotlin
// src/jvmMain/kotlin/com/opclient/ui/navigation/IconSidebar.kt
package com.opclient.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.opclient.ui.theme.AppShapes
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun IconSidebar(
    selected: Destination,
    onSelect: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppThemeTokens.colors
    val borderColor = colors.border

    Column(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(colors.surface)
            .drawBehind {
                drawLine(color = borderColor, start = Offset(size.width, 0f), end = Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            }
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Destination.entries.forEach { destination ->
            val isSelected = destination == selected
            val bgColor = if (isSelected) colors.accentLight else colors.surface2
            val tint = if (isSelected) colors.accent else colors.textSecondary
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(color = bgColor, shape = RoundedCornerShape(AppShapes.radius))
                    .clickable { onSelect(destination) },
                contentAlignment = Alignment.Center,
            ) {
                DestinationIcon(destination = destination, tint = tint, size = 16.dp)
            }
        }
    }
}
```

- [ ] **Step 2: Implement Desktop AppShell**

```kotlin
// src/jvmMain/kotlin/com/opclient/ui/AppShell.kt
package com.opclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.opclient.ui.navigation.Destination
import com.opclient.ui.navigation.IconSidebar
import com.opclient.ui.theme.AppThemeTokens

@Composable
fun AppShell(
    selectedDestination: Destination,
    onDestinationChange: (Destination) -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = AppThemeTokens.colors
    Row(modifier = Modifier.fillMaxSize().background(colors.background)) {
        IconSidebar(selected = selectedDestination, onSelect = onDestinationChange)
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}
```

- [ ] **Step 3: Manual smoke test on Desktop**

Run the Desktop app:
```bash
./gradlew :shared:runRelease
# or
./gradlew :desktop:run
```

Verify:
- Sidebar visible on left, 56dp wide
- Three icon buttons present
- Clicking each icon fires `onDestinationChange` with correct destination
- Active icon has sage green background tint
- Sidebar has right-side border line

- [ ] **Step 4: Commit**

```bash
git add src/jvmMain/kotlin/com/opclient/ui/navigation/IconSidebar.kt \
        src/jvmMain/kotlin/com/opclient/ui/AppShell.kt
git commit -m "feat: add Desktop IconSidebar and AppShell"
```

---

## Task 13: Wire AppTheme into platform entry points

**Files:**
- Modify: `src/androidMain/kotlin/com/opclient/MainActivity.kt`
- Modify: `src/jvmMain/kotlin/com/opclient/Main.kt`

- [ ] **Step 1: Wrap Android entry point in AppTheme**

In `MainActivity.kt`, locate the `setContent { … }` block and wrap its content:

```kotlin
setContent {
    AppTheme {
        // existing content or AppShell(…)
    }
}
```

- [ ] **Step 2: Wrap Desktop entry point in AppTheme**

In `Main.kt`, locate the `Window(…) { … }` block and wrap its content:

```kotlin
Window(onCloseRequest = ::exitApplication, title = "op_client") {
    AppTheme {
        // existing content or AppShell(…)
    }
}
```

- [ ] **Step 3: Verify no MaterialTheme exists anywhere**

```bash
grep -r "MaterialTheme" src/
```

Expected: zero matches. If any found, replace the wrapping `MaterialTheme { … }` with `AppTheme { … }` and remove the `MaterialTheme` import.

- [ ] **Step 4: Run full test suite**

```bash
./gradlew allTests
```

Expected: all tests pass, no compilation errors on any target.

- [ ] **Step 5: Commit**

```bash
git add src/androidMain/kotlin/com/opclient/MainActivity.kt \
        src/jvmMain/kotlin/com/opclient/Main.kt
git commit -m "feat: wire AppTheme into Android and Desktop entry points"
```
