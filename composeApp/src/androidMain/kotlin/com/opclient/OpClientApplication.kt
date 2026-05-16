package com.opclient

import android.app.Application
import com.opclient.di.androidModule
import com.opclient.di.authModule
import com.opclient.di.authorModule
import com.opclient.di.bookModule
import com.opclient.di.commonModule
import com.opclient.di.libraryModule
import com.opclient.di.listsModule
import com.opclient.di.profileModule
import com.opclient.di.searchInsideModule
import com.opclient.di.searchModule
import com.opclient.di.settingsModule
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
            modules(commonModule, androidModule, settingsModule, searchModule, searchInsideModule,
                    authorModule, subjectModule, libraryModule, bookModule, profileModule,
                    authModule, listsModule)
        }
    }
}
