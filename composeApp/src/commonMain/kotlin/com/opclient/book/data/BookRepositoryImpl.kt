// composeApp/src/commonMain/kotlin/com/opclient/book/data/BookRepositoryImpl.kt
package com.opclient.book.data

import com.opclient.author.domain.AuthorRepository
import com.opclient.book.domain.AuthorRef
import com.opclient.book.domain.BookDetail
import com.opclient.book.domain.BookRepository
import com.opclient.core.ApiError
import com.opclient.core.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class BookRepositoryImpl(
    private val apiClient: BookApiClient,
    private val cache: BookCache,
    private val authorRepository: AuthorRepository,
) : BookRepository {

    override suspend fun getBook(workKey: String): Result<BookDetail, ApiError> {
        val olid = workKey.substringAfterLast("/")
        val cacheKey = "work:$olid"

        cache.get(cacheKey)?.let { return Result.Success(it) }

        return when (val result = apiClient.getWork(olid)) {
            is Result.Failure -> result
            is Result.Success -> {
                val dto = result.value
                val authorKeys = dto.authors?.map { it.author.key } ?: emptyList()

                val authors = coroutineScope {
                    authorKeys.map { key -> async { authorRepository.getAuthor(key) } }
                        .mapNotNull { deferred ->
                            val r = deferred.await()
                            if (r is Result.Success) {
                                AuthorRef(key = r.value.key, name = r.value.name)
                            } else null
                        }
                }

                val domain = dto.toDomain(authors)
                cache.put(cacheKey, domain)
                Result.Success(domain)
            }
        }
    }
}
