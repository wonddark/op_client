package com.opclient.lists.data

import com.opclient.lists.domain.ListSeed
import com.opclient.lists.domain.ReadingList

fun ListDto.toDomain() = ReadingList(
    key = key,
    name = name,
    description = description?.value ?: "",
    seedCount = seedCount,
)

fun SeedEntryDto.toDomain(): ListSeed? {
    val work = work ?: return null
    return ListSeed(
        workKey = work.key,
        title = work.title,
        authorName = work.authorNames?.firstOrNull(),
        coverUrl = work.coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
    )
}
