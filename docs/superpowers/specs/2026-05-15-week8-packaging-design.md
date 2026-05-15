# Week 8 — Linux Packaging Design

## Goal

Produce three distributable artifacts from a single `./gradlew` invocation:

1. **Fat JAR** — portable, runs with `java -jar` on any system with Java 17+
2. **`.deb` package** — installs on Debian/Ubuntu via `dpkg -i`, creates CLI entry + `.desktop` launcher
3. **`.rpm` package** — installs on RHEL/Fedora/openSUSE via `rpm -i`

All three artifacts include a canvas-drawn app icon (512×512 PNG generated at build time using AWT `Graphics2D`).

---

## Architecture

No new feature packages. All changes are build infrastructure + one icon generator class.

```
composeApp/
  src/jvmMain/kotlin/com/opclient/icon/
    GenerateIcon.kt          ← AWT main() draws book icon, saves PNG
  build.gradle.kts           ← generateAppIcon task + nativeDistributions config
LICENSE                      ← MIT license file (required by deb/rpm metadata)
```

No Android changes. No new Gradle plugins. Uses Compose Desktop's built-in jpackage integration (`packageDeb`, `packageRpm`, `packageUberJarForCurrentOS`).

---

## Icon Generator

### File

`composeApp/src/jvmMain/kotlin/com/opclient/icon/GenerateIcon.kt`

### Visual Design

Open book, top-down view. Same geometric language as nav icons (circles, lines, rectangles).

| Element | Shape | Color |
|---------|-------|-------|
| Background circle | r=230, centered | `#111C10` (DarkColors.background) |
| Left page | rounded rect, slight left tilt | `#E4EDE2` (DarkColors.textPrimary) |
| Right page | rounded rect, slight right tilt | `#E4EDE2` |
| Center spine | thin vertical rect | `#6AB874` (DarkColors.accent) |
| Text lines (×3 per page) | horizontal rects, varying widths | `#7A9C76` (DarkColors.textSecondary) |

Canvas size: 512×512 ARGB PNG.

### Implementation

```kotlin
package com.opclient.icon

import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
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

    // Background circle
    g.color = Color(0x11, 0x1C, 0x10)
    g.fillOval(26, 26, 460, 460)

    val identity = g.transform

    // Left page (rotated -4° around its center)
    g.color = Color(0xE4, 0xED, 0xE2)
    g.rotate(Math.toRadians(-4.0), 172.0, 255.0)
    g.fill(RoundRectangle2D.Float(80f, 130f, 185f, 250f, 12f, 12f))
    // Text lines on left page
    g.color = Color(0x7A, 0x9C, 0x76)
    listOf(Triple(105, 195, 140), Triple(105, 225, 120), Triple(105, 255, 130)).forEach { (x, y, w) ->
        g.fillRoundRect(x, y, w, 10, 4, 4)
    }
    g.transform = identity

    // Right page (rotated +4° around its center)
    g.color = Color(0xE4, 0xED, 0xE2)
    g.rotate(Math.toRadians(4.0), 340.0, 255.0)
    g.fill(RoundRectangle2D.Float(247f, 130f, 185f, 250f, 12f, 12f))
    // Text lines on right page
    g.color = Color(0x7A, 0x9C, 0x76)
    listOf(Triple(267, 195, 140), Triple(267, 225, 120), Triple(267, 255, 100)).forEach { (x, y, w) ->
        g.fillRoundRect(x, y, w, 10, 4, 4)
    }
    g.transform = identity

    // Spine
    g.color = Color(0x6A, 0xB8, 0x74)
    g.fillRoundRect(245, 120, 22, 270, 6, 6)

    g.dispose()
    ImageIO.write(img, "PNG", File(outDir, "op_client.png"))
    println("Icon written to ${outDir.absolutePath}/op_client.png")
}
```

---

## Build Configuration

### `generateAppIcon` Task

Added to `composeApp/build.gradle.kts`:

```kotlin
val generateAppIcon by tasks.registering(JavaExec::class) {
    dependsOn(tasks.named("compileKotlinJvm"))
    val outDir = layout.buildDirectory.dir("generated/icon").get().asFile
    outputs.dir(outDir)
    inputs.files(
        fileTree("src/jvmMain/kotlin/com/opclient/icon")
    )
    classpath = kotlin.targets.named<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>("jvm")
        .get().compilations["main"].output.classesDirs +
        configurations["jvmRuntimeClasspath"]
    mainClass.set("com.opclient.icon.GenerateIconKt")
    args(outDir.absolutePath)
}
```

### Enhanced `compose.desktop` Block

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

### Task Dependencies

```kotlin
tasks.named("packageDeb") { dependsOn(generateAppIcon) }
tasks.named("packageRpm") { dependsOn(generateAppIcon) }
tasks.named("packageUberJarForCurrentOS") { dependsOn(generateAppIcon) }
```

---

## LICENSE File

MIT license at project root (`LICENSE`). Required for deb/rpm package metadata. Content:

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

---

## Deliverable Commands

| Command | Output path |
|---------|------------|
| `./gradlew generateAppIcon` | `build/generated/icon/op_client.png` |
| `./gradlew packageDeb` | `build/compose/binaries/main/deb/op-client_1.0.0_amd64.deb` |
| `./gradlew packageRpm` | `build/compose/binaries/main/rpm/op-client-1.0.0-1.x86_64.rpm` |
| `./gradlew packageUberJarForCurrentOS` | `build/compose/jars/op-client-linux-x86_64-1.0.0.jar` |

---

## Verification

| Step | Command | Pass condition |
|------|---------|----------------|
| Icon generates | `./gradlew generateAppIcon` | PNG at correct path, 512×512 |
| Fat JAR runs | `./gradlew packageUberJarForCurrentOS && java -jar build/compose/jars/*.jar` | App window opens |
| Deb builds | `./gradlew packageDeb` | `.deb` present in `build/compose/binaries/main/deb/` |
| Deb installs | `sudo dpkg -i *.deb && op-client` | App launches, appears in app menu with icon |
| Rpm builds | `./gradlew packageRpm` | `.rpm` present in `build/compose/binaries/main/rpm/` |

---

## V-Invariants

```
V1: generateAppIcon → build/generated/icon/op_client.png exists & is 512×512 ∀ clean build
V2: packageDeb dependsOn generateAppIcon → icon bundled ∀ deb build
V3: packageRpm dependsOn generateAppIcon → icon bundled ∀ rpm build
V4: packageUberJarForCurrentOS mainClass = "com.opclient.MainKt" → app launches ∀ java -jar
V5: packageVersion = "1.0.0" consistent ∀ deb | rpm | jar artifacts
V6: LICENSE file exists at root → deb/rpm metadata valid ∀ packaging task
```
