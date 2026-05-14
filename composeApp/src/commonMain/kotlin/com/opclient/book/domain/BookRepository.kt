// composeApp/src/commonMain/kotlin/com/opclient/book/domain/BookRepository.kt
package com.opclient.book.domain

import com.opclient.core.ApiError
import com.opclient.core.Result

interface BookRepository {
    suspend fun getBook(workKey: String): Result<BookDetail, ApiError>
}
