// composeApp/src/commonMain/kotlin/com/opclient/book/domain/BookModels.kt
package com.opclient.book.domain

data class BookDetail(
    val key: String,
    val title: String,
    val description: String?,
    val authors: List<AuthorRef>,
    val subjects: List<String>,
    val firstPublishDate: String?,
    val coverUrl: String?,
)

data class AuthorRef(val key: String, val name: String)
