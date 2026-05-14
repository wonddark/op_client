// composeApp/src/commonMain/kotlin/com/opclient/book/data/BookMapper.kt
package com.opclient.book.data

import com.opclient.book.domain.AuthorRef
import com.opclient.book.domain.BookDetail
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

private const val COVER_BASE = "https://covers.openlibrary.org/b/id"

internal fun JsonElement?.toText(): String? = when {
    this == null || this is JsonNull -> null
    this is JsonPrimitive && isString -> content
    this is JsonObject -> (this["value"] as? JsonPrimitive)?.contentOrNull
    else -> null
}

internal fun List<Int>?.toBookCoverUrl(size: String = "M"): String? =
    this?.firstOrNull()?.let { "$COVER_BASE/$it-$size.jpg" }

internal fun WorkDto.toDomain(authorRefs: List<AuthorRef>): BookDetail =
    BookDetail(
        key = key,
        title = title ?: "Unknown Title",
        description = description.toText(),
        authors = authorRefs,
        subjects = subjects ?: emptyList(),
        firstPublishDate = firstPublishDate,
        coverUrl = covers.toBookCoverUrl(size = "L"),
    )
