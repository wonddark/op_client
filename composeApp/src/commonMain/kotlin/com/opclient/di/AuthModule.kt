package com.opclient.di

import com.opclient.auth.data.AuthApiClient
import com.opclient.auth.data.AuthRepositoryImpl
import com.opclient.auth.domain.AuthRepository
import com.opclient.auth.presentation.AuthViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val authModule: Module = module {
    factory { AuthApiClient(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    viewModel { AuthViewModel(get()) }
}
