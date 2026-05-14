package com.opclient.profile.data

import com.opclient.core.ApiError
import com.opclient.core.Result
import com.opclient.profile.domain.UserProfile
import com.opclient.profile.domain.UserProfileRepository

class UserProfileRepositoryImpl(
    private val apiClient: ProfileApiClient,
) : UserProfileRepository {

    override suspend fun getProfile(username: String): Result<UserProfile, ApiError> =
        when (val result = apiClient.getProfile(username)) {
            is Result.Success -> Result.Success(result.value.toDomain())
            is Result.Failure -> result
        }
}
