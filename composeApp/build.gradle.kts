import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.sqldelight)
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
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.napier)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.sqldelight.coroutines)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.koin.android)
            implementation(libs.androidx.activity.compose)
            implementation(libs.sqldelight.android.driver)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.sqlite.driver)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.turbine)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        androidUnitTest.dependencies {
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.desktop.uiTestJUnit4)
            implementation(libs.junit4)
            implementation(libs.robolectric)
        }

        jvmTest.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

sqldelight {
    databases {
        create("LibraryDatabase") {
            packageName.set("com.opclient.library")
            srcDirs("src/commonMain/sqldelight/library")
        }
        create("SettingsDatabase") {
            packageName.set("com.opclient.settings")
            srcDirs("src/commonMain/sqldelight/settings")
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        getByName("debug") {
            manifest.srcFile("src/androidDebug/AndroidManifest.xml")
        }
    }
}

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

val jdkForPackaging: Provider<JavaLauncher> = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(17))
    vendor.set(JvmVendorSpec.AZUL)
}

compose.desktop {
    application {
        mainClass = "com.opclient.MainKt"
        javaHome = jdkForPackaging.map { it.metadata.installationPath.asFile.absolutePath }.get()
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
afterEvaluate {
    tasks.named("packageDeb") { dependsOn(generateAppIcon) }
    tasks.named("packageRpm") { dependsOn(generateAppIcon) }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.opclient.resources"
    generateResClass = always
}

detekt {
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    exclude { element -> element.file.absolutePath.contains("/build/generated/") }
}

ktlint {
    version.set("1.3.1")
    android.set(true)
    filter {
        exclude { entry -> entry.file.path.contains("/build/generated/") }
    }
}
