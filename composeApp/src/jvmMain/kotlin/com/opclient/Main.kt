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
