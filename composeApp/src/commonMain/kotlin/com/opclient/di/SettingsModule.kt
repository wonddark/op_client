package com.opclient.di

import com.opclient.settings.SettingsDatabase
import com.opclient.settings.data.SettingsRepositoryImpl
import com.opclient.settings.domain.SettingsRepository
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val settingsModule: Module = module {
    single { SettingsDatabase(get(named("settings"))) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}
