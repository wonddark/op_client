package com.opclient.profile.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface UserProfileRepository {
    suspend fun getProfile(username: String): Result<UserProfile, ApiError>
}
