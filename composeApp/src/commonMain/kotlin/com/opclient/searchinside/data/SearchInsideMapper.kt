package com.opclient.searchinside.data

import com.opclient.searchinside.domain.SearchInsideResult

fun SearchInsideHitDto.toDomain(): SearchInsideResult? {
    val workKey = edition?.workKey ?: return null
    val passage = highlight?.text?.firstOrNull()
        ?.replace("{{{", "")
        ?.replace("}}}", "") ?: return null
    return SearchInsideResult(
        workKey = workKey,
        title = edition.title ?: "Unknown",
        authorName = edition.authors.firstOrNull()?.name,
        coverUrl = edition.coverUrl?.let { if (it.startsWith("//")) "https:$it" else it },
        passage = passage,
    )
}
