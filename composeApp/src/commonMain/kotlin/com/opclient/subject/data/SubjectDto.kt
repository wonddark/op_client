package com.opclient.subject.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SubjectDto(
    val name: String,
    @SerialName("work_count") val workCount: Int = 0,
    val works: List<SubjectWorkDto>? = null,
)

@Serializable
data class SubjectWorkDto(
    val key: String,
    val title: String? = null,
    val authors: List<SubjectWorkAuthorDto>? = null,
    @SerialName("cover_id") val coverId: Int? = null,
)

@Serializable
data class SubjectWorkAuthorDto(val name: String? = null)

@Serializable
data class SubjectSuggestionDto(
    val name: String,
    @SerialName("work_count") val workCount: Int = 0,
)

@Serializable
data class SubjectSearchResponseDto(
    val numFound: Int,
    val start: Int,
    val docs: List<SubjectSuggestionDto>,
)
