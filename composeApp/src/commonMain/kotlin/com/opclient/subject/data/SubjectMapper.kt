package com.opclient.subject.data

import com.opclient.subject.domain.SubjectPage
import com.opclient.subject.domain.SubjectWork

private const val COVER_BASE = "https://covers.openlibrary.org/b/id"

internal fun Int?.toCoverUrl(): String? = this?.let { "$COVER_BASE/$it-M.jpg" }

internal fun SubjectWorkDto.toDomain(): SubjectWork = SubjectWork(
    key = key,
    title = title ?: "Unknown Title",
    authorName = authors?.firstOrNull()?.name,
    coverUrl = coverId.toCoverUrl(),
)

internal fun SubjectDto.toDomain(): SubjectPage = SubjectPage(
    subjectName = name,
    workCount = workCount,
    works = works?.map { it.toDomain() } ?: emptyList(),
)
