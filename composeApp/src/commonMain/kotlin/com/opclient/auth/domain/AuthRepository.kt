package com.opclient.auth.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<Unit, ApiError>
    suspend fun logout()
    suspend fun isLoggedIn(): Boolean
    suspend fun getLoggedInUsername(): String?
}
