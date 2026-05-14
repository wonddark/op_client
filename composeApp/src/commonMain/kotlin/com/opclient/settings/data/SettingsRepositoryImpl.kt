package com.opclient.settings.data

import com.opclient.settings.SettingsDatabase
import com.opclient.settings.domain.ReadingGoal
import com.opclient.settings.domain.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(private val db: SettingsDatabase) : SettingsRepository {

    private companion object {
        const val KEY_USERNAME = "username"
    }

    override suspend fun getUsername(): String? = withContext(Dispatchers.IO) {
        db.settingsQueries.getPreference(KEY_USERNAME).executeAsOneOrNull()
    }

    override suspend fun setUsername(username: String): Unit = withContext(Dispatchers.IO) {
        db.settingsQueries.upsertPreference(KEY_USERNAME, username)
    }

    override suspend fun getReadingGoal(year: Int): ReadingGoal? = withContext(Dispatchers.IO) {
        db.settingsQueries.getGoal(year.toLong()).executeAsOneOrNull()
            ?.let { ReadingGoal(it.year.toInt(), it.target.toInt()) }
    }

    override suspend fun setReadingGoal(year: Int, target: Int): Unit = withContext(Dispatchers.IO) {
        db.settingsQueries.upsertGoal(year.toLong(), target.toLong())
    }

    override suspend fun clearReadingGoal(year: Int): Unit = withContext(Dispatchers.IO) {
        db.settingsQueries.deleteGoal(year.toLong())
    }
}
