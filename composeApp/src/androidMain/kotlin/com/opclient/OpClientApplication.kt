package com.opclient

import android.app.Application
import com.opclient.di.androidModule
import com.opclient.di.authorModule
import com.opclient.di.bookModule
import com.opclient.di.commonModule
import com.opclient.di.searchModule
import com.opclient.di.subjectModule
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
            modules(commonModule, androidModule, searchModule, authorModule, subjectModule, bookModule)
        }
    }
}
