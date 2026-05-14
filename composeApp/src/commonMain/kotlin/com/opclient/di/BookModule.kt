package com.opclient.di

import com.opclient.book.data.BookApiClient
import com.opclient.book.data.BookCache
import com.opclient.book.data.BookRepositoryImpl
import com.opclient.book.domain.BookRepository
import com.opclient.book.presentation.BookDetailViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val bookModule: Module = module {
    single { BookCache() }
    factory { BookApiClient(get()) }
    single<BookRepository> { BookRepositoryImpl(apiClient = get(), cache = get(), authorRepository = get()) }
    viewModel { BookDetailViewModel(get(), get()) }
}
