package com.opclient.search.domain

data class Book(
    val key: String,
    val title: String,
    val author: String,
    val firstPublishYear: Int?,
    val coverUrl: String?,
    val primarySubject: String?,
)

data class SearchResults(
    val query: String,
    val totalFound: Int,
    val books: List<Book>,
    val offset: Int,
)
