package com.opclient.di

import com.opclient.search.data.SearchApiClient
import com.opclient.search.data.SearchCache
import com.opclient.search.data.SearchRepositoryImpl
import com.opclient.search.domain.SearchRepository
// import com.opclient.search.presentation.SearchViewModel
import org.koin.core.module.Module
// import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchModule: Module = module {
    single { SearchCache() }
    factory { SearchApiClient(get()) }
    single<SearchRepository> { SearchRepositoryImpl(apiClient = get(), cache = get()) }
    // TODO: add in Task 9
    // viewModel { SearchViewModel(get()) }
}
