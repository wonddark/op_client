package com.opclient.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.opclient.library.LibraryDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val desktopModule: Module = module {
    single<SqlDriver> {
        JdbcSqliteDriver("jdbc:sqlite:library.db").also {
            LibraryDatabase.Schema.create(it)
        }
    }
}
