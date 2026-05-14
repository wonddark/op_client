package com.opclient.di

import com.opclient.author.data.AuthorApiClient
import com.opclient.author.data.AuthorCache
import com.opclient.author.data.AuthorRepositoryImpl
import com.opclient.author.domain.AuthorRepository
import com.opclient.author.presentation.AuthorDetailViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authorModule: Module = module {
    single { AuthorCache() }
    factory { AuthorApiClient(get()) }
    single<AuthorRepository> { AuthorRepositoryImpl(apiClient = get(), cache = get()) }
    viewModel { AuthorDetailViewModel(get()) }
}
