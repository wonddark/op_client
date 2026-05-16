package com.opclient.di

import com.opclient.subject.data.SubjectApiClient
import com.opclient.subject.data.SubjectCache
import com.opclient.subject.data.SubjectRepositoryImpl
import com.opclient.subject.domain.SubjectRepository
import com.opclient.subject.presentation.SubjectBrowseViewModel
import com.opclient.subject.presentation.SubjectDetailViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val subjectModule: Module = module {
    single { SubjectCache() }
    factory { SubjectApiClient(get()) }
    single<SubjectRepository> { SubjectRepositoryImpl(apiClient = get(), cache = get()) }
    viewModel { SubjectBrowseViewModel(get()) }
    viewModel { SubjectDetailViewModel(get()) }
}
