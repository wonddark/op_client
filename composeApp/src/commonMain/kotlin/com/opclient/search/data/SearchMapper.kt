package com.opclient.search.data

import com.opclient.search.domain.Book
import com.opclient.search.domain.SearchResults

private const val COVER_BASE_URL = "https://covers.openlibrary.org/b/id"

internal fun SearchDocDto.toDomain(): Book =
    Book(
        key = key,
        title = title ?: "Unknown Title",
        author = authorName?.firstOrNull()?.takeIf { it.isNotBlank() } ?: "Unknown",
        firstPublishYear = firstPublishYear,
        coverUrl = coverId?.let { "$COVER_BASE_URL/$it-M.jpg" },
        primarySubject = subject?.firstOrNull(),
    )

internal fun SearchResponseDto.toDomain(query: String): SearchResults =
    SearchResults(
        query = query,
        totalFound = numFound,
        books = docs.map { it.toDomain() },
        offset = start,
    )
