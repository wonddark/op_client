package com.opclient

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.opclient.di.authorModule
import com.opclient.di.bookModule
import com.opclient.di.commonModule
import com.opclient.di.desktopModule
import com.opclient.di.searchModule
import com.opclient.di.subjectModule
import com.opclient.ui.theme.AppTheme
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.context.startKoin

fun main() {
    Napier.base(DebugAntilog())
    startKoin {
        modules(commonModule, desktopModule, searchModule, authorModule, subjectModule, bookModule)
    }
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "op_client",
        ) {
            AppTheme {
                App()
            }
        }
    }
}
