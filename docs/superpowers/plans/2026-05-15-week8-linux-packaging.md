# Week 8 — Linux Packaging Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a 512×512 canvas-drawn app icon and three distributable Linux artifacts — `.deb`, `.rpm`, and fat JAR — all buildable from a single Gradle command.

**Architecture:** `GenerateIcon.kt` uses AWT `Graphics2D` to draw a book icon and save a PNG; a `generateAppIcon` JavaExec Gradle task runs it at build time; `compose.desktop` native distributions pick up the icon and metadata; `packageUberJarForCurrentOS` produces the fat JAR. No new feature packages, no new Gradle plugins.

**Tech Stack:** Compose Desktop Gradle plugin (`packageDeb`, `packageRpm`, `packageUberJarForCurrentOS`), Java AWT (`BufferedImage`, `Graphics2D`), `javax.imageio.ImageIO`.

---

## File Map

| File | Action | Purpose |
|------|--------|---------|
| `LICENSE` | Create | MIT license — required by deb/rpm package metadata |
| `composeApp/src/jvmMain/kotlin/com/opclient/icon/GenerateIcon.kt` | Create | AWT program that draws book icon and saves 512×512 PNG |
| `composeApp/build.gradle.kts` | Modify (3 increments) | `generateAppIcon` task; enhanced `compose.desktop`; fat JAR dep |

---

### Task 1: LICENSE File

**Files:**
- Create: `LICENSE`

- [ ] **Step 1: Create the LICENSE file**

  Create `LICENSE` at the project root (`/home/oz/Projects/Personal/op_client/LICENSE`):

  ```
  MIT License

  Copyright (c) 2026 op_client contributors

  Permission is hereby granted, free of charge, to any person obtaining a copy
  of this software and associated documentation files (the "Software"), to deal
  in the Software without restriction, including without limitation the rights
  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  copies of the Software, and to permit persons to whom the Software is
  furnished to do so, subject to the following conditions:

  The above copyright notice and this permission notice shall be included in all
  copies or substantial portions of the Software.

  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  SOFTWARE.
  ```

- [ ] **Step 2: Commit**

  ```bash
  git add LICENSE
  git commit -m "chore: add MIT LICENSE"
  ```

---

### Task 2: App Icon Generator

**Files:**
- Create: `composeApp/src/jvmMain/kotlin/com/opclient/icon/GenerateIcon.kt`

- [ ] **Step 1: Create GenerateIcon.kt**

  Create `composeApp/src/jvmMain/kotlin/com/opclient/icon/GenerateIcon.kt`:

  ```kotlin
  package com.opclient.icon

  import java.awt.Color
  import java.awt.RenderingHints
  import java.awt.geom.RoundRectangle2D
  import java.awt.image.BufferedImage
  import java.io.File
  import javax.imageio.ImageIO

  fun main(args: Array<String>) {
      val outDir = File(args[0])
      outDir.mkdirs()

      val size = 512
      val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
      val g = img.createGraphics()
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      // Background circle — DarkColors.background (#111C10)
      g.color = Color(0x11, 0x1C, 0x10)
      g.fillOval(26, 26, 460, 460)

      val identity = g.transform

      // Left page — rotated -4° around its center (172, 255)
      // DarkColors.textPrimary (#E4EDE2)
      g.color = Color(0xE4, 0xED, 0xE2)
      g.rotate(Math.toRadians(-4.0), 172.0, 255.0)
      g.fill(RoundRectangle2D.Float(80f, 130f, 185f, 250f, 12f, 12f))

      // Text lines on left page — DarkColors.textSecondary (#7A9C76)
      g.color = Color(0x7A, 0x9C, 0x76)
      listOf(Triple(105, 195, 140), Triple(105, 225, 120), Triple(105, 255, 130))
          .forEach { (x, y, w) -> g.fillRoundRect(x, y, w, 10, 4, 4) }
      g.transform = identity

      // Right page — rotated +4° around its center (340, 255)
      g.color = Color(0xE4, 0xED, 0xE2)
      g.rotate(Math.toRadians(4.0), 340.0, 255.0)
      g.fill(RoundRectangle2D.Float(247f, 130f, 185f, 250f, 12f, 12f))

      // Text lines on right page
      g.color = Color(0x7A, 0x9C, 0x76)
      listOf(Triple(267, 195, 140), Triple(267, 225, 120), Triple(267, 255, 100))
          .forEach { (x, y, w) -> g.fillRoundRect(x, y, w, 10, 4, 4) }
      g.transform = identity

      // Spine — DarkColors.accent (#6AB874)
      g.color = Color(0x6A, 0xB8, 0x74)
      g.fillRoundRect(245, 120, 22, 270, 6, 6)

      g.dispose()
      val out = File(outDir, "op_client.png")
      ImageIO.write(img, "PNG", out)
      println("Icon written to ${out.absolutePath}")
  }
  ```

- [ ] **Step 2: Verify it compiles**

  Run: `./gradlew compileKotlinJvm 2>&1 | tail -5`
  Expected: `BUILD SUCCESSFUL`

  If it fails, read the error — likely an import issue. All imports (`java.awt.*`, `javax.imageio.*`) are JDK built-ins, no extra dependencies needed.

- [ ] **Step 3: Commit**

  ```bash
  git add composeApp/src/jvmMain/kotlin/com/opclient/icon/GenerateIcon.kt
  git commit -m "feat(packaging): add AWT icon generator (book motif, 512×512)"
  ```

---

### Task 3: generateAppIcon Gradle Task

**Files:**
- Modify: `composeApp/build.gradle.kts`

Context: The `generateAppIcon` JavaExec task compiles `GenerateIcon.kt` (already done by `compileKotlinJvm`) then runs it, writing the PNG to `build/generated/icon/op_client.png`. The classpath needs the compiled classes + `jvmRuntimeClasspath` (which includes Kotlin stdlib).

- [ ] **Step 1: Add the generateAppIcon task to build.gradle.kts**

  In `composeApp/build.gradle.kts`, add this block **before** the `compose.desktop {` block (around line 132):

  ```kotlin
  val generateAppIcon by tasks.registering(JavaExec::class) {
      dependsOn("compileKotlinJvm")
      val outDir = layout.buildDirectory.dir("generated/icon")
      outputs.dir(outDir)
      inputs.dir(project.file("src/jvmMain/kotlin/com/opclient/icon"))
      doFirst { outDir.get().asFile.mkdirs() }
      classpath(
          kotlin.targets
              .named<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>("jvm")
              .get().compilations["main"].output.classesDirs,
          configurations["jvmRuntimeClasspath"],
      )
      mainClass.set("com.opclient.icon.GenerateIconKt")
      args(outDir.get().asFile.absolutePath)
  }
  ```

- [ ] **Step 2: Run generateAppIcon**

  Run: `./gradlew generateAppIcon 2>&1 | tail -10`
  Expected output contains: `Icon written to .../build/generated/icon/op_client.png`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Verify PNG dimensions**

  Run: `file composeApp/build/generated/icon/op_client.png`
  Expected output contains: `PNG image data, 512 x 512`

- [ ] **Step 4: Commit**

  ```bash
  git add composeApp/build.gradle.kts
  git commit -m "feat(packaging): add generateAppIcon Gradle task"
  ```

---

### Task 4: Native Distributions Config (deb + rpm)

**Files:**
- Modify: `composeApp/build.gradle.kts`

Context: The existing `compose.desktop` block has only minimal config. Replacing it with a full config that includes description, vendor, copyright, license, and Linux-specific icon + menu metadata. Then wiring `packageDeb` and `packageRpm` to depend on `generateAppIcon`.

- [ ] **Step 1: Replace the compose.desktop block**

  In `composeApp/build.gradle.kts`, replace the existing `compose.desktop { ... }` block:

  **Before:**
  ```kotlin
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
  ```

  **After:**
  ```kotlin
  compose.desktop {
      application {
          mainClass = "com.opclient.MainKt"
          nativeDistributions {
              targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
              packageName = "op-client"
              packageVersion = "1.0.0"
              description = "OpenLibrary desktop client"
              vendor = "op_client"
              copyright = "© 2026 op_client contributors"
              licenseFile.set(rootProject.file("LICENSE"))

              linux {
                  iconFile.set(
                      layout.buildDirectory.file("generated/icon/op_client.png")
                  )
                  menuGroup = "Office;Education;"
                  appCategory = "Education"
                  debMaintainer = "op_client"
                  rpmLicenseType = "MIT"
              }
          }
      }
  }
  ```

- [ ] **Step 2: Wire task dependencies for native packages**

  Add these lines immediately after the `compose.desktop { ... }` block:

  ```kotlin
  tasks.named("packageDeb") { dependsOn(generateAppIcon) }
  tasks.named("packageRpm") { dependsOn(generateAppIcon) }
  ```

- [ ] **Step 3: Verify compilation**

  Run: `./gradlew compileKotlinJvm 2>&1 | tail -5`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Build the deb package**

  Run: `./gradlew packageDeb 2>&1 | tail -10`
  Expected: `BUILD SUCCESSFUL`

  If it fails with `jpackage not found`: jpackage is bundled with JDK 14+. Verify: `jpackage --version`. If missing, install OpenJDK 17+ full JDK (not JRE-only): `sudo zypper install java-17-openjdk-devel` (openSUSE) or `sudo apt install openjdk-17-jdk` (Debian/Ubuntu).

- [ ] **Step 5: Verify deb artifact exists**

  Run: `ls composeApp/build/compose/binaries/main/deb/`
  Expected: a file matching `op-client_1.0.0_amd64.deb`

- [ ] **Step 6: Build the rpm package**

  Run: `./gradlew packageRpm 2>&1 | tail -10`
  Expected: `BUILD SUCCESSFUL`

  If it fails with `rpmbuild not found`: install rpm build tools: `sudo zypper install rpm-build` (openSUSE) or `sudo apt install rpm` (Debian/Ubuntu).

- [ ] **Step 7: Verify rpm artifact exists**

  Run: `ls composeApp/build/compose/binaries/main/rpm/`
  Expected: a file matching `op-client-1.0.0-1.x86_64.rpm`

- [ ] **Step 8: Commit**

  ```bash
  git add composeApp/build.gradle.kts
  git commit -m "feat(packaging): configure native distributions with icon, metadata, deb/rpm"
  ```

---

### Task 5: Fat JAR + Live Verification

**Files:**
- Modify: `composeApp/build.gradle.kts`

Context: `packageUberJarForCurrentOS` is a built-in Compose Desktop task that produces a fat JAR (all deps bundled, no JRE). Wire it to depend on `generateAppIcon` (for consistent build ordering), then verify all three artifacts work.

- [ ] **Step 1: Wire fat JAR task dependency**

  Add this line alongside the existing task dependency lines (after `compose.desktop` block):

  ```kotlin
  tasks.named("packageUberJarForCurrentOS") { dependsOn(generateAppIcon) }
  ```

- [ ] **Step 2: Build the fat JAR**

  Run: `./gradlew packageUberJarForCurrentOS 2>&1 | tail -10`
  Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Verify JAR artifact exists**

  Run: `ls composeApp/build/compose/jars/`
  Expected: a file matching `op-client-linux-x86_64-1.0.0.jar` (exact name may vary slightly)

- [ ] **Step 4: Run the fat JAR**

  Run: `java -jar composeApp/build/compose/jars/op-client-linux-*.jar &`
  Expected: app window opens — four tabs visible (Search, Browse, Library, Changes)
  Kill it after verifying: `kill %1` or close the window

- [ ] **Step 5: Install and verify deb**

  Run:
  ```bash
  sudo dpkg -i composeApp/build/compose/binaries/main/deb/op-client_1.0.0_amd64.deb
  ```
  Expected: `Setting up op-client (1.0.0) ...`

  Then: `op-client &`
  Expected: app window opens

  Verify app menu entry: check that `op-client` appears in your desktop environment's application menu under Office or Education category with the book icon.

  Uninstall after verifying: `sudo dpkg -r op-client`

- [ ] **Step 6: Commit**

  ```bash
  git add composeApp/build.gradle.kts
  git commit -m "feat(packaging): wire fat JAR task dependency, complete packaging setup"
  ```
