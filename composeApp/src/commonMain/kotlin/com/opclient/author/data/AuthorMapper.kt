// composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorMapper.kt
package com.opclient.author.data

import com.opclient.author.domain.AuthorDetail
import com.opclient.author.domain.AuthorWork
import com.opclient.book.data.toBookCoverUrl
import com.opclient.book.data.toText

private const val AUTHOR_COVER_BASE = "https://covers.openlibrary.org/a/id"

internal fun List<Int>?.toAuthorPhotoUrl(): String? =
    this?.firstOrNull()?.let { "$AUTHOR_COVER_BASE/$it-M.jpg" }

internal fun AuthorDto.toDomain(works: List<AuthorWorkEntryDto>): AuthorDetail =
    AuthorDetail(
        key = key,
        name = name ?: "Unknown Author",
        bio = bio.toText(),
        birthDate = birthDate,
        deathDate = deathDate,
        photoUrl = photos.toAuthorPhotoUrl(),
        works = works.map { entry ->
            AuthorWork(
                key = entry.key,
                title = entry.title ?: "Unknown Title",
                coverUrl = entry.covers.toBookCoverUrl(size = "M"),
            )
        },
    )
