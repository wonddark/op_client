package com.opclient.auth.data

import com.opclient.auth.domain.AuthRepository
import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.settings.domain.SettingsRepository

class AuthRepositoryImpl(
    private val apiClient: AuthApiClient,
    private val settings: SettingsRepository,
) : AuthRepository {

    override suspend fun login(username: String, password: String): Result<Unit, ApiError> {
        settings.clearSessionCookie()
        val apiResult = apiClient.login(username, password)
        if (apiResult is Result.Failure) return Result.Failure(apiResult.error)
        return if (settings.getSessionCookie() != null) {
            settings.setUsername(username)
            Result.Success(Unit)
        } else {
            Result.Failure(ApiError.HttpError(401, "Invalid credentials"))
        }
    }

    override suspend fun logout() {
        settings.clearSessionCookie()
    }

    override suspend fun isLoggedIn(): Boolean = settings.getSessionCookie() != null

    override suspend fun getLoggedInUsername(): String? = settings.getUsername()
}
