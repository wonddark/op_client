# Week 1: KMP Project Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bootstrap a compilable Kotlin Multiplatform project targeting Android (API 26+) and Linux Desktop (JVM), with shared HTTP client infrastructure, a custom sealed `Result<T, E>` type, an abstract `ApiClient`, Koin DI, Napier logging, and Ktlint + Detekt.

**Architecture:** Single `composeApp` module containing `commonMain`, `androidMain`, and `jvmMain` source sets. HTTP engine varies per platform via `expect`/`actual` — OkHttp for Android, CIO for Desktop. Everything else (HTTP client config, error types, API base class, DI modules) lives in `commonMain`.

**Tech Stack:** Kotlin 2.1.0, Compose Multiplatform 1.7.1, AGP 8.7.3, Ktor 3.0.3, Koin 4.0.0, kotlinx.serialization 1.7.3, kotlinx.coroutines 1.9.0, Napier 2.7.1, Coil 3.0.4, Ktlint Gradle plugin 12.1.1, Detekt 1.23.7.

---

## File Map

### Root
| File | Responsibility |
|------|---------------|
| `.gitignore` | Ignore build outputs, IDE files, local properties |
| `.editorconfig` | Ktlint base rules |
| `settings.gradle.kts` | Project name, plugin repos, `include(":composeApp")` |
| `build.gradle.kts` | Root plugins block (`apply false`) |
| `gradle/libs.versions.toml` | All dependency versions and aliases |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 8.11.1 distribution URL |
| `config/detekt/detekt.yml` | Detekt rule overrides |

### composeApp
| File | Responsibility |
|------|---------------|
| `composeApp/build.gradle.kts` | KMP targets, source set deps, Android config, Compose Desktop, Ktlint, Detekt |

### commonMain (`composeApp/src/commonMain/kotlin/com/opclient/`)
| File | Responsibility |
|------|---------------|
| `App.kt` | Placeholder `@Composable fun App()` entry point |
| `core/Result.kt` | `sealed class Result<out T, out E>` + `map`, `flatMap`, `getOrNull`, `errorOrNull` |
| `core/ApiError.kt` | `sealed class ApiError` (NetworkError, HttpError, ParseError, Unknown) |
| `core/HttpClientEngine.kt` | `internal expect fun createHttpEngine(): HttpClientEngine` |
| `core/HttpClientFactory.kt` | `internal fun buildHttpClient(engine): HttpClient` with ContentNegotiation + Logging |
| `core/ApiClient.kt` | `abstract class ApiClient(baseUrl, httpClient)` with typed `get()` and `post()` |
| `di/CommonModule.kt` | Koin `commonModule` — binds `HttpClient` |

### androidMain (`composeApp/src/androidMain/`)
| File | Responsibility |
|------|---------------|
| `AndroidManifest.xml` | App manifest, INTERNET permission, Application class ref |
| `kotlin/com/opclient/OpClientApplication.kt` | Koin + Napier init via `Application.onCreate()` |
| `kotlin/com/opclient/MainActivity.kt` | Single Activity, calls `setContent { App() }` |
| `kotlin/com/opclient/core/HttpClientEngine.android.kt` | `actual fun createHttpEngine() = OkHttp.create()` |
| `kotlin/com/opclient/di/AndroidModule.kt` | Koin `androidModule` (placeholder for Phase 2+) |

### jvmMain (`composeApp/src/jvmMain/kotlin/com/opclient/`)
| File | Responsibility |
|------|---------------|
| `Main.kt` | Desktop entry point — Koin + Napier init, Compose `application { Window { App() } }` |
| `core/HttpClientEngine.jvm.kt` | `actual fun createHttpEngine() = CIO.create()` |
| `di/DesktopModule.kt` | Koin `desktopModule` (placeholder for Phase 2+) |

### Tests (`composeApp/src/commonTest/kotlin/com/opclient/`)
| File | Tests |
|------|-------|
| `core/ResultTest.kt` | Success/Failure construction, `map`, `flatMap`, `getOrNull`, `errorOrNull` |
| `core/ApiErrorTest.kt` | Each `ApiError` subtype construction and field access |
| `core/ApiClientTest.kt` | `get()` success, `get()` 404→HttpError via Ktor MockEngine |

---

## Task 1: Repository init + Gradle wrapper

**Files:**
- Create: `.gitignore`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `gradlew` (generated)

- [ ] **Step 1: Initialise git repository**

```bash
cd /home/oz/Projects/Personal/op_client
git init
```

Expected: `Initialized empty Git repository in .../op_client/.git/`

- [ ] **Step 2: Create `.gitignore`**

```
# Gradle
.gradle/
build/
local.properties

# Android
*.apk
*.aab
*.keystore

# IDE
.idea/
*.iml
.DS_Store
*.swp

# Kotlin
*.class
*.klib

# OS
Thumbs.db
```

- [ ] **Step 3: Bootstrap Gradle wrapper**

Requires Gradle installed locally. Check first:

```bash
which gradle || echo "gradle not found"
```

If not found, install via SDKMAN:
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install gradle 8.11.1
```

Generate wrapper:
```bash
gradle wrapper --gradle-version 8.11.1 --distribution-type bin
```

- [ ] **Step 4: Verify wrapper**

```bash
./gradlew --version
```

Expected output contains: `Gradle 8.11.1`

- [ ] **Step 5: Commit**

```bash
git add .gitignore gradlew gradlew.bat gradle/wrapper/
git commit -m "chore: init repository with gradle wrapper"
```

---

## Task 2: Version catalog + settings + root build

**Files:**
- Create: `gradle/libs.versions.toml`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`

- [ ] **Step 1: Create `gradle/libs.versions.toml`**

```toml
[versions]
kotlin = "2.1.0"
agp = "8.7.3"
compose-multiplatform = "1.7.1"
ktor = "3.0.3"
koin = "4.0.0"
kotlinx-serialization = "1.7.3"
kotlinx-coroutines = "1.9.0"
kotlinx-datetime = "0.6.1"
napier = "2.7.1"
coil = "3.0.4"
activity-compose = "1.9.3"
detekt = "1.23.7"
ktlint-gradle = "12.1.1"

[libraries]
# Ktor
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { module = "io.ktor:ktor-client-logging", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }

# kotlinx
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }

# Koin
koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-compose", version.ref = "koin" }

# Napier
napier = { module = "io.github.aakira:napier", version.ref = "napier" }

# Coil
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network-ktor3 = { module = "io.coil-kt.coil3:coil-network-ktor3", version.ref = "coil" }

# AndroidX
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activity-compose" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
android-application = { id = "com.android.application", version.ref = "agp" }
jetbrains-compose = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint-gradle" }
```

- [ ] **Step 2: Create `settings.gradle.kts`**

```kotlin
rootProject.name = "op_client"

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

include(":composeApp")
```

- [ ] **Step 3: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}
```

- [ ] **Step 4: Verify Gradle syncs**

```bash
./gradlew tasks
```

Expected: task list printed without errors (`:composeApp` tasks absent until next task adds the module build file).

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts build.gradle.kts
git commit -m "chore: add gradle version catalog and root build scripts"
```

---

## Task 3: composeApp module — KMP targets + entry points

**Files:**
- Create: `composeApp/build.gradle.kts`
- Create: `composeApp/src/androidMain/AndroidManifest.xml`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/App.kt`
- Create: `composeApp/src/androidMain/kotlin/com/opclient/MainActivity.kt`
- Create: `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`

- [ ] **Step 1: Create source set directories**

```bash
mkdir -p composeApp/src/{commonMain,androidMain,jvmMain,commonTest,androidUnitTest,jvmTest}/kotlin/com/opclient
mkdir -p composeApp/src/androidMain/kotlin/com/opclient/core
mkdir -p composeApp/src/androidMain/kotlin/com/opclient/di
mkdir -p composeApp/src/jvmMain/kotlin/com/opclient/core
mkdir -p composeApp/src/jvmMain/kotlin/com/opclient/di
mkdir -p composeApp/src/commonMain/kotlin/com/opclient/core
mkdir -p composeApp/src/commonMain/kotlin/com/opclient/di
mkdir -p composeApp/src/commonTest/kotlin/com/opclient/core
mkdir -p composeApp/src/commonMain/composeResources
```

- [ ] **Step 2: Create `composeApp/build.gradle.kts`**

```kotlin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.napier)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

android {
    namespace = "com.opclient"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.opclient"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

compose.desktop {
    application {
        mainClass = "com.opclient.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "op_client"
            packageVersion = "1.0.0"
        }
    }
}

detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}

ktlint {
    version.set("1.3.1")
    android.set(true)
}
```

- [ ] **Step 3: Create `composeApp/src/androidMain/AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <application
        android:name=".OpClientApplication"
        android:label="op_client"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 4: Create `composeApp/src/commonMain/kotlin/com/opclient/App.kt`**

```kotlin
package com.opclient

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun App() {
    Box(modifier = Modifier.fillMaxSize())
}
```

- [ ] **Step 5: Create `composeApp/src/androidMain/kotlin/com/opclient/MainActivity.kt`**

```kotlin
package com.opclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
```

- [ ] **Step 6: Create `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`**

```kotlin
package com.opclient

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "op_client",
        ) {
            App()
        }
    }
}
```

- [ ] **Step 7: Create `local.properties` for Android SDK**

```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

If `$ANDROID_HOME` is not set, find the SDK path:
```bash
# Common locations on Linux:
# ~/Android/Sdk
# /opt/android-sdk
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
```

- [ ] **Step 8: Verify both targets compile**

```bash
./gradlew :composeApp:compileCommonMainKotlinMetadata
./gradlew :composeApp:compileDebugKotlinAndroid
./gradlew :composeApp:compileKotlinJvm
```

Expected: all three tasks succeed with `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add composeApp/ local.properties
git commit -m "feat: scaffold composeapp kmp module with android and jvm targets"
```

---

## Task 4: Sealed `Result<T, E>` type

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/opclient/core/ResultTest.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/core/Result.kt`

- [ ] **Step 1: Write failing test**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/core/ResultTest.kt
package com.opclient.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ResultTest {

    @Test
    fun success_holdsValue() {
        val result: Result<String, Nothing> = Result.Success("hello")
        assertIs<Result.Success<String>>(result)
        assertEquals("hello", result.value)
    }

    @Test
    fun failure_holdsError() {
        val result: Result<Nothing, String> = Result.Failure("error")
        assertIs<Result.Failure<String>>(result)
        assertEquals("error", result.error)
    }

    @Test
    fun getOrNull_onSuccess_returnsValue() {
        val result: Result<Int, String> = Result.Success(42)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun getOrNull_onFailure_returnsNull() {
        val result: Result<Int, String> = Result.Failure("bad")
        assertNull(result.getOrNull())
    }

    @Test
    fun errorOrNull_onFailure_returnsError() {
        val result: Result<Int, String> = Result.Failure("bad")
        assertEquals("bad", result.errorOrNull())
    }

    @Test
    fun errorOrNull_onSuccess_returnsNull() {
        val result: Result<Int, String> = Result.Success(42)
        assertNull(result.errorOrNull())
    }

    @Test
    fun map_onSuccess_transformsValue() {
        val result: Result<Int, String> = Result.Success(5)
        val mapped = result.map { it * 2 }
        assertIs<Result.Success<Int>>(mapped)
        assertEquals(10, mapped.value)
    }

    @Test
    fun map_onFailure_passesThrough() {
        val result: Result<Int, String> = Result.Failure("err")
        val mapped = result.map { it * 2 }
        assertIs<Result.Failure<String>>(mapped)
        assertEquals("err", mapped.error)
    }

    @Test
    fun flatMap_onSuccess_chains() {
        val result: Result<Int, String> = Result.Success(5)
        val chained = result.flatMap { n ->
            if (n > 0) Result.Success(n * 2) else Result.Failure("negative")
        }
        assertIs<Result.Success<Int>>(chained)
        assertEquals(10, chained.value)
    }

    @Test
    fun flatMap_onFailure_shortCircuits() {
        val result: Result<Int, String> = Result.Failure("err")
        var called = false
        val chained = result.flatMap { called = true; Result.Success(it) }
        assertIs<Result.Failure<String>>(chained)
        assertEquals("err", chained.error)
        assertEquals(false, called)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.core.ResultTest"
```

Expected: `FAILED` — `Result` not found.

- [ ] **Step 3: Implement `Result.kt`**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/core/Result.kt
package com.opclient.core

sealed class Result<out T, out E> {
    data class Success<out T>(val value: T) : Result<T, Nothing>()
    data class Failure<out E>(val error: E) : Result<Nothing, E>()
}

fun <T, E> Result<T, E>.getOrNull(): T? = when (this) {
    is Result.Success -> value
    is Result.Failure -> null
}

fun <T, E> Result<T, E>.errorOrNull(): E? = when (this) {
    is Result.Success -> null
    is Result.Failure -> error
}

inline fun <T, E, R> Result<T, E>.map(transform: (T) -> R): Result<R, E> = when (this) {
    is Result.Success -> Result.Success(transform(value))
    is Result.Failure -> this
}

inline fun <T, E, R> Result<T, E>.flatMap(transform: (T) -> Result<R, E>): Result<R, E> = when (this) {
    is Result.Success -> transform(value)
    is Result.Failure -> this
}
```

- [ ] **Step 4: Run tests — all pass**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.core.ResultTest"
```

Expected: `10 tests completed, 0 failed`

- [ ] **Step 5: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/core/Result.kt \
        composeApp/src/commonTest/kotlin/com/opclient/core/ResultTest.kt
git commit -m "feat(core): add sealed result type with map and flatmap"
```

---

## Task 5: ApiError + HttpClientFactory + expect/actual engine

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/opclient/core/ApiErrorTest.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/core/ApiError.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/core/HttpClientEngine.kt`
- Create: `composeApp/src/androidMain/kotlin/com/opclient/core/HttpClientEngine.android.kt`
- Create: `composeApp/src/jvmMain/kotlin/com/opclient/core/HttpClientEngine.jvm.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/core/HttpClientFactory.kt`

- [ ] **Step 1: Write failing test for `ApiError`**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/core/ApiErrorTest.kt
package com.opclient.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApiErrorTest {

    @Test
    fun networkError_storesThrowable() {
        val cause = RuntimeException("timeout")
        val err = ApiError.NetworkError(cause)
        assertIs<ApiError.NetworkError>(err)
        assertEquals(cause, err.cause)
    }

    @Test
    fun httpError_storesCodeAndBody() {
        val err = ApiError.HttpError(404, "Not Found")
        assertIs<ApiError.HttpError>(err)
        assertEquals(404, err.code)
        assertEquals("Not Found", err.body)
    }

    @Test
    fun parseError_storesThrowable() {
        val cause = IllegalArgumentException("bad json")
        val err = ApiError.ParseError(cause)
        assertIs<ApiError.ParseError>(err)
        assertEquals(cause, err.cause)
    }

    @Test
    fun unknown_isSingleton() {
        val a = ApiError.Unknown
        val b = ApiError.Unknown
        assertEquals(a, b)
    }
}
```

- [ ] **Step 2: Run test to confirm failure**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.core.ApiErrorTest"
```

Expected: `FAILED` — `ApiError` not found.

- [ ] **Step 3: Implement `ApiError.kt`**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/core/ApiError.kt
package com.opclient.core

sealed class ApiError {
    data class NetworkError(val cause: Throwable) : ApiError()
    data class HttpError(val code: Int, val body: String) : ApiError()
    data class ParseError(val cause: Throwable) : ApiError()
    data object Unknown : ApiError()
}
```

- [ ] **Step 4: Run `ApiErrorTest` — all pass**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.core.ApiErrorTest"
```

Expected: `4 tests completed, 0 failed`

- [ ] **Step 5: Implement `expect fun createHttpEngine()`**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/core/HttpClientEngine.kt
package com.opclient.core

import io.ktor.client.engine.HttpClientEngine

internal expect fun createHttpEngine(): HttpClientEngine
```

```kotlin
// composeApp/src/androidMain/kotlin/com/opclient/core/HttpClientEngine.android.kt
package com.opclient.core

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

internal actual fun createHttpEngine(): HttpClientEngine = OkHttp.create()
```

```kotlin
// composeApp/src/jvmMain/kotlin/com/opclient/core/HttpClientEngine.jvm.kt
package com.opclient.core

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

internal actual fun createHttpEngine(): HttpClientEngine = CIO.create()
```

- [ ] **Step 6: Implement `HttpClientFactory.kt`**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/core/HttpClientFactory.kt
package com.opclient.core

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun buildHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Napier.v(message, tag = "HttpClient")
            }
        }
        level = LogLevel.INFO
    }
}
```

- [ ] **Step 7: Verify both targets compile with expect/actual wired**

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/core/ApiError.kt \
        composeApp/src/commonMain/kotlin/com/opclient/core/HttpClientEngine.kt \
        composeApp/src/commonMain/kotlin/com/opclient/core/HttpClientFactory.kt \
        composeApp/src/androidMain/kotlin/com/opclient/core/HttpClientEngine.android.kt \
        composeApp/src/jvmMain/kotlin/com/opclient/core/HttpClientEngine.jvm.kt \
        composeApp/src/commonTest/kotlin/com/opclient/core/ApiErrorTest.kt
git commit -m "feat(core): add apierror, http engine expect/actual, and http client factory"
```

---

## Task 6: `ApiClient` abstract base class

**Files:**
- Create: `composeApp/src/commonTest/kotlin/com/opclient/core/ApiClientTest.kt`
- Create: `composeApp/src/commonMain/kotlin/com/opclient/core/ApiClient.kt`

- [ ] **Step 1: Write failing test**

```kotlin
// composeApp/src/commonTest/kotlin/com/opclient/core/ApiClientTest.kt
package com.opclient.core

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApiClientTest {

    @Serializable
    private data class TestBody(val message: String)

    private fun makeClient(
        handler: suspend (io.ktor.client.request.HttpRequestData) -> io.ktor.client.engine.mock.MockRequestHandleScope.() -> io.ktor.client.engine.HttpClientCallData,
    ): ApiClient {
        val engine = MockEngine { request -> handler(request) }
        return object : ApiClient("https://api.test", buildHttpClient(engine)) {}
    }

    @Test
    fun get_200_returnsSuccess() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"message":"hello"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = object : ApiClient("https://api.test", buildHttpClient(engine)) {}
        val result = client.get<TestBody>("/items")
        assertIs<Result.Success<TestBody>>(result)
        assertEquals("hello", result.value.message)
    }

    @Test
    fun get_404_returnsHttpError() = runTest {
        val engine = MockEngine {
            respond(
                content = "Not Found",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }
        val client = object : ApiClient("https://api.test", buildHttpClient(engine)) {}
        val result = client.get<TestBody>("/missing")
        assertIs<Result.Failure<ApiError>>(result)
        assertIs<ApiError.HttpError>(result.error)
        assertEquals(404, (result.error as ApiError.HttpError).code)
    }

    @Test
    fun get_appendsQueryParameters() = runTest {
        var capturedUrl: String? = null
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(
                content = """{"message":"ok"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = object : ApiClient("https://api.test", buildHttpClient(engine)) {}
        client.get<TestBody>("/search", mapOf("q" to "kotlin"))
        assertEquals(true, capturedUrl?.contains("q=kotlin"))
    }
}
```

- [ ] **Step 2: Run test to confirm failure**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.core.ApiClientTest"
```

Expected: `FAILED` — `ApiClient` not found.

- [ ] **Step 3: Implement `ApiClient.kt`**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/core/ApiClient.kt
package com.opclient.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

abstract class ApiClient(
    private val baseUrl: String,
    private val httpClient: HttpClient,
) {
    protected suspend inline fun <reified T> get(
        path: String,
        queryParams: Map<String, String> = emptyMap(),
    ): Result<T, ApiError> = try {
        val response = httpClient.get(baseUrl + path) {
            queryParams.forEach { (key, value) -> url.parameters.append(key, value) }
        }
        Result.Success(response.body())
    } catch (e: ClientRequestException) {
        Result.Failure(ApiError.HttpError(e.response.status.value, e.message ?: ""))
    } catch (e: ServerResponseException) {
        Result.Failure(ApiError.HttpError(e.response.status.value, e.message ?: ""))
    } catch (e: Exception) {
        Result.Failure(ApiError.NetworkError(e))
    }

    protected suspend inline fun <reified T, reified B : Any> post(
        path: String,
        body: B,
    ): Result<T, ApiError> = try {
        val response = httpClient.post(baseUrl + path) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        Result.Success(response.body())
    } catch (e: ClientRequestException) {
        Result.Failure(ApiError.HttpError(e.response.status.value, e.message ?: ""))
    } catch (e: ServerResponseException) {
        Result.Failure(ApiError.HttpError(e.response.status.value, e.message ?: ""))
    } catch (e: Exception) {
        Result.Failure(ApiError.NetworkError(e))
    }
}
```

- [ ] **Step 4: Run tests — all pass**

```bash
./gradlew :composeApp:jvmTest --tests "com.opclient.core.ApiClientTest"
```

Expected: `3 tests completed, 0 failed`

- [ ] **Step 5: Run all tests**

```bash
./gradlew :composeApp:jvmTest
```

Expected: All `ResultTest`, `ApiErrorTest`, `ApiClientTest` pass.

- [ ] **Step 6: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/core/ApiClient.kt \
        composeApp/src/commonTest/kotlin/com/opclient/core/ApiClientTest.kt
git commit -m "feat(core): add apiclient abstract base class with get and post"
```

---

## Task 7: Koin DI modules + Napier logging

**Files:**
- Create: `composeApp/src/commonMain/kotlin/com/opclient/di/CommonModule.kt`
- Create: `composeApp/src/androidMain/kotlin/com/opclient/di/AndroidModule.kt`
- Create: `composeApp/src/jvmMain/kotlin/com/opclient/di/DesktopModule.kt`
- Create: `composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt`
- Modify: `composeApp/src/androidMain/kotlin/com/opclient/MainActivity.kt`
- Modify: `composeApp/src/jvmMain/kotlin/com/opclient/Main.kt`

- [ ] **Step 1: Create `CommonModule.kt`**

```kotlin
// composeApp/src/commonMain/kotlin/com/opclient/di/CommonModule.kt
package com.opclient.di

import com.opclient.core.buildHttpClient
import com.opclient.core.createHttpEngine
import org.koin.core.module.Module
import org.koin.dsl.module

val commonModule: Module = module {
    single { buildHttpClient(createHttpEngine()) }
}
```

- [ ] **Step 2: Create `AndroidModule.kt`**

```kotlin
// composeApp/src/androidMain/kotlin/com/opclient/di/AndroidModule.kt
package com.opclient.di

import org.koin.core.module.Module
import org.koin.dsl.module

val androidModule: Module = module {
    // Android-specific bindings added in Phase 2+
}
```

- [ ] **Step 3: Create `DesktopModule.kt`**

```kotlin
// composeApp/src/jvmMain/kotlin/com/opclient/di/DesktopModule.kt
package com.opclient.di

import org.koin.core.module.Module
import org.koin.dsl.module

val desktopModule: Module = module {
    // Desktop-specific bindings added in Phase 2+
}
```

- [ ] **Step 4: Create `OpClientApplication.kt`**

```kotlin
// composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt
package com.opclient

import android.app.Application
import com.opclient.di.androidModule
import com.opclient.di.commonModule
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
            modules(commonModule, androidModule)
        }
    }
}
```

- [ ] **Step 5: Update `MainActivity.kt`**

No Koin or Napier init needed in `MainActivity` — the `Application` class handles it. Update `MainActivity.kt` to remove any init calls if present (the placeholder from Task 3 is already correct).

Verify `MainActivity.kt` reads exactly:

```kotlin
package com.opclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}
```

- [ ] **Step 6: Update `Main.kt` with Koin + Napier**

```kotlin
// composeApp/src/jvmMain/kotlin/com/opclient/Main.kt
package com.opclient

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.opclient.di.commonModule
import com.opclient.di.desktopModule
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin

fun main() {
    Napier.base(DebugAntilog())
    startKoin {
        modules(commonModule, desktopModule)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "op_client",
        ) {
            App()
        }
    }
}
```

- [ ] **Step 7: Verify both targets compile**

```bash
./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:compileKotlinJvm
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Smoke-test Desktop run**

```bash
./gradlew :composeApp:run
```

Expected: empty window titled "op_client" opens and closes cleanly on `Ctrl+C`.

- [ ] **Step 9: Commit**

```bash
git add composeApp/src/commonMain/kotlin/com/opclient/di/CommonModule.kt \
        composeApp/src/androidMain/kotlin/com/opclient/di/AndroidModule.kt \
        composeApp/src/jvmMain/kotlin/com/opclient/di/DesktopModule.kt \
        composeApp/src/androidMain/kotlin/com/opclient/OpClientApplication.kt \
        composeApp/src/androidMain/kotlin/com/opclient/MainActivity.kt \
        composeApp/src/jvmMain/kotlin/com/opclient/Main.kt
git commit -m "feat(di): configure koin modules and napier logging"
```

---

## Task 8: Ktlint + Detekt

**Files:**
- Create: `.editorconfig`
- Create: `config/detekt/detekt.yml`

The plugins were already applied to `composeApp/build.gradle.kts` in Task 3.

- [ ] **Step 1: Create `.editorconfig`**

```editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
indent_size = 4
indent_style = space
insert_final_newline = true
trim_trailing_whitespace = true

[*.{kt,kts}]
max_line_length = 120
ktlint_standard_no-empty-first-line-in-class-body = disabled
ktlint_standard_multiline-expression-wrapping = disabled
```

- [ ] **Step 2: Create `config/detekt/detekt.yml`**

```bash
mkdir -p config/detekt
```

```yaml
# config/detekt/detekt.yml
build:
  maxIssues: 0

complexity:
  LongParameterList:
    active: true
    functionThreshold: 7
    constructorThreshold: 8
  LongMethod:
    active: true
    threshold: 60
  CyclomaticComplexMethod:
    active: true
    threshold: 15

style:
  MagicNumber:
    active: true
    ignoreNumbers:
      - '-1'
      - '0'
      - '1'
      - '2'
    ignoreAnnotated:
      - 'Preview'
      - 'Composable'
  MaxLineLength:
    active: true
    maxLineLength: 120
  WildcardImport:
    active: true
    excludeImports:
      - 'kotlinx.coroutines.*'

naming:
  FunctionNaming:
    active: true
    functionPattern: '[a-z][a-zA-Z0-9]*'
    ignoreAnnotated:
      - 'Composable'
```

- [ ] **Step 3: Run Ktlint check**

```bash
./gradlew :composeApp:ktlintCheck
```

If there are violations, auto-format:
```bash
./gradlew :composeApp:ktlintFormat
```

Then re-run check:
```bash
./gradlew :composeApp:ktlintCheck
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Run Detekt**

```bash
./gradlew :composeApp:detekt
```

Expected: `BUILD SUCCESSFUL` (or review and fix any flagged issues)

- [ ] **Step 5: Run full check**

```bash
./gradlew :composeApp:allTests :composeApp:ktlintCheck :composeApp:detekt
```

Expected: all pass.

- [ ] **Step 6: Commit**

```bash
git add .editorconfig config/detekt/detekt.yml
git commit -m "chore: configure ktlint and detekt"
```

---

## Verification Checklist

After all 8 tasks complete, run:

```bash
./gradlew :composeApp:compileCommonMainKotlinMetadata \
          :composeApp:compileDebugKotlinAndroid \
          :composeApp:compileKotlinJvm \
          :composeApp:jvmTest \
          :composeApp:ktlintCheck \
          :composeApp:detekt
```

All should report `BUILD SUCCESSFUL`.

Manual smoke test:
```bash
./gradlew :composeApp:run
```
An empty window titled "op_client" must open. Close it. No errors in console.

Week 1 is complete when:
- [ ] KMP project compiles on both Android and JVM targets
- [ ] `ResultTest`, `ApiErrorTest`, `ApiClientTest` all pass on JVM
- [ ] Desktop app window opens and closes cleanly
- [ ] Ktlint and Detekt pass with zero issues
