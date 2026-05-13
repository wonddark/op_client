package com.opclient.di

import com.opclient.core.buildHttpClient
import com.opclient.core.createHttpEngine
import org.koin.core.module.Module
import org.koin.dsl.module

val commonModule: Module = module {
    single { buildHttpClient(createHttpEngine()) }
}
