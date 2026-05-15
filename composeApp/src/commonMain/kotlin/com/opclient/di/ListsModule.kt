package com.opclient.di

import com.opclient.lists.data.ListsApiClient
import com.opclient.lists.data.ListsRepositoryImpl
import com.opclient.lists.domain.ListsRepository
import com.opclient.lists.presentation.ListsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val listsModule: Module = module {
    factory { ListsApiClient(get()) }
    single<ListsRepository> { ListsRepositoryImpl(get(), get()) }
    viewModel { ListsViewModel(get()) }
}
