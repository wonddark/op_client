package com.opclient.searchinside.data

import com.opclient.searchinside.domain.SearchInsideResult

fun SearchInsideDocDto.toDomain(): SearchInsideResult =
    SearchInsideResult(
        workKey = key!!,
        title = title ?: "Unknown",
        authorName = authorName?.firstOrNull(),
        coverUrl = coverId?.let { "https://covers.openlibrary.org/b/id/$it-M.jpg" },
        passage = text?.firstOrNull() ?: "",
    )
