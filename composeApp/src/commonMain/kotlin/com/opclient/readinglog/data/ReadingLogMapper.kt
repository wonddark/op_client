package com.opclient.readinglog.data

import com.opclient.library.domain.LibraryEntry
import com.opclient.library.domain.Shelf
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

fun ReadingLogEntryDto.toLibraryEntry(shelf: Shelf): LibraryEntry =
    LibraryEntry(
        workKey = work.key,
        title = work.title ?: "Unknown",
        authorName = work.authorNames?.firstOrNull(),
        coverUrl = work.coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
        shelf = shelf,
        addedAt = loggedDate?.toEpochMillis() ?: Clock.System.now().toEpochMilliseconds(),
    )

private fun String.toEpochMillis(): Long =
    try {
        Instant.parse(this).toEpochMilliseconds()
    } catch (_: Exception) {
        Clock.System.now().toEpochMilliseconds()
    }
