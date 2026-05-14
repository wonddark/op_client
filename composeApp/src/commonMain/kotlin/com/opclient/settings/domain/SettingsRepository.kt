package com.opclient.settings.domain

interface SettingsRepository {
    suspend fun getUsername(): String?
    suspend fun setUsername(username: String)
    suspend fun getReadingGoal(year: Int): ReadingGoal?
    suspend fun setReadingGoal(year: Int, target: Int)
    suspend fun clearReadingGoal(year: Int)
}
