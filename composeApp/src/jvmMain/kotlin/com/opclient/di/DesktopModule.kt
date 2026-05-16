package com.opclient.di

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.opclient.library.LibraryDatabase
import com.opclient.settings.SettingsDatabase
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

private fun SqlDriver.isNewDatabase(): Boolean =
    executeQuery(
        identifier = null,
        sql = "SELECT COUNT(*) FROM sqlite_master WHERE type='table'",
        mapper = { cursor ->
            cursor.next()
            QueryResult.Value((cursor.getLong(0) ?: 0L) == 0L)
        },
        parameters = 0,
    ).value

val desktopModule: Module = module {
    single<SqlDriver> {
        JdbcSqliteDriver("jdbc:sqlite:library.db").also { driver ->
            if (driver.isNewDatabase()) LibraryDatabase.Schema.create(driver)
        }
    }
    single<SqlDriver>(named("settings")) {
        JdbcSqliteDriver("jdbc:sqlite:settings.db").also { driver ->
            if (driver.isNewDatabase()) SettingsDatabase.Schema.create(driver)
        }
    }
}
