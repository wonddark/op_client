// composeApp/src/commonMain/kotlin/com/opclient/author/domain/AuthorRepository.kt
package com.opclient.author.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface AuthorRepository {
    suspend fun getAuthor(authorKey: String): Result<AuthorDetail, ApiError>
}
