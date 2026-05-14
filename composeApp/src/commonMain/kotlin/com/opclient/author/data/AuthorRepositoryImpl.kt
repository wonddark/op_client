// composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorRepositoryImpl.kt
package com.opclient.author.data

import com.opclient.author.domain.AuthorDetail
import com.opclient.author.domain.AuthorRepository
import com.opclient.core.ApiError
import com.opclient.core.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AuthorRepositoryImpl(
    private val apiClient: AuthorApiClient,
    private val cache: AuthorCache,
) : AuthorRepository {

    override suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError> {
        val olid = authorKey.substringAfterLast("/")
        val cacheKey = "author:$olid"

        cache.get(cacheKey)?.let { return Result.Success(it) }

        return coroutineScope {
            val profileDeferred = async { apiClient.getAuthor(olid) }
            val worksDeferred = async { apiClient.getAuthorWorks(olid) }

            val profileResult = profileDeferred.await()
            if (profileResult is Result.Failure) return@coroutineScope profileResult

            val worksEntries = (worksDeferred.await() as? Result.Success)
                ?.value?.entries ?: emptyList()

            val domain = (profileResult as Result.Success).value.toDomain(worksEntries)
            cache.put(cacheKey, domain)
            Result.Success(domain)
        }
    }
}
