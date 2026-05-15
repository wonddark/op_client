package com.opclient.di

import com.opclient.profile.data.ProfileApiClient
import com.opclient.profile.data.UserProfileRepositoryImpl
import com.opclient.profile.domain.UserProfileRepository
import com.opclient.profile.presentation.ProfileViewModel
import com.opclient.readinglog.data.ReadingLogApiClient
import com.opclient.readinglog.data.ReadingLogSyncServiceImpl
import com.opclient.readinglog.domain.ReadingLogSyncService
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule: Module = module {
    factory { ProfileApiClient(get()) }
    single<UserProfileRepository> { UserProfileRepositoryImpl(apiClient = get()) }
    factory { ReadingLogApiClient(get()) }
    single<ReadingLogSyncService> { ReadingLogSyncServiceImpl(apiClient = get(), libraryRepository = get()) }
    viewModel { ProfileViewModel(settingsRepository = get(), profileRepository = get(), syncService = get(), libraryRepository = get()) }
}
