package com.opclient.searchinside.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchInsideResponseDto(
    @SerialName("numFound") val numFound: Int = 0,
    @SerialName("docs") val docs: List<SearchInsideDocDto> = emptyList(),
)

@Serializable
data class SearchInsideDocDto(
    val key: String? = null,
    val title: String? = null,
    @SerialName("author_name") val authorName: List<String>? = null,
    @SerialName("cover_i") val coverId: Int? = null,
    val text: List<String>? = null,
)
