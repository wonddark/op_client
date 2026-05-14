package com.opclient.di

import com.opclient.library.LibraryDatabase
import com.opclient.library.data.LibraryRepositoryImpl
import com.opclient.library.domain.LibraryRepository
import com.opclient.library.presentation.LibraryViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val libraryModule: Module = module {
    single { LibraryDatabase(get()) }
    single<LibraryRepository> { LibraryRepositoryImpl(get()) }
    viewModel { LibraryViewModel(get()) }
}
