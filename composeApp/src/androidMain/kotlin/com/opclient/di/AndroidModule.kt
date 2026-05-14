package com.opclient.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.opclient.library.LibraryDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val androidModule: Module = module {
    single<SqlDriver> {
        AndroidSqliteDriver(LibraryDatabase.Schema, androidContext(), "library.db")
    }
}
