package com.opclient.recentchanges.data

import com.opclient.recentchanges.domain.RecentChange
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun RecentChangeDto.toDomain(): RecentChange =
    RecentChange(
        id = id!!,
        label = when (kind) {
            "add-book" -> "Book added"
            "update-work" -> "Work updated"
            "update-author" -> "Author updated"
            "edit-book" -> "Book edited"
            else -> "Community edit"
        },
        targetKey = changes.firstOrNull()?.key,
        addedAt = timestamp?.toEpochMillis() ?: Clock.System.now().toEpochMilliseconds(),
    )

private fun String.toEpochMillis(): Long =
    try {
        val normalized = if (endsWith("Z") || contains("+") || indexOf("-", 10) >= 0) {
            this
        } else {
            "${this}Z"
        }
        Instant.parse(normalized).toEpochMilliseconds()
    } catch (_: Exception) {
        Clock.System.now().toEpochMilliseconds()
    }
