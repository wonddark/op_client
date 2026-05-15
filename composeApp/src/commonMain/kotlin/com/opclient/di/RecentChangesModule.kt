package com.opclient.di

import com.opclient.recentchanges.data.RecentChangesApiClient
import com.opclient.recentchanges.data.RecentChangesRepositoryImpl
import com.opclient.recentchanges.domain.RecentChangesRepository
import com.opclient.recentchanges.presentation.RecentChangesViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val recentChangesModule: Module = module {
    factory { RecentChangesApiClient(get()) }
    single<RecentChangesRepository> { RecentChangesRepositoryImpl(get()) }
    viewModel { RecentChangesViewModel(get()) }
}
