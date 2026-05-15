package com.opclient.di

import com.opclient.searchinside.data.SearchInsideApiClient
import com.opclient.searchinside.data.SearchInsideRepositoryImpl
import com.opclient.searchinside.domain.SearchInsideRepository
import com.opclient.searchinside.presentation.SearchInsideViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val searchInsideModule: Module = module {
    factory { SearchInsideApiClient(get()) }
    single<SearchInsideRepository> { SearchInsideRepositoryImpl(get()) }
    viewModel { SearchInsideViewModel(get()) }
}
