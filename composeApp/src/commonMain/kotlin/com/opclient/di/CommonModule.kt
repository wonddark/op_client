package com.opclient.di

import com.opclient.core.PersistentCookieStorage
import com.opclient.core.buildHttpClient
import com.opclient.core.createHttpEngine
import com.opclient.navigation.NavigationViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule: Module = module {
    single { PersistentCookieStorage(get()) }
    single { buildHttpClient(createHttpEngine(), get<PersistentCookieStorage>()) }
    viewModel { NavigationViewModel() }
}
