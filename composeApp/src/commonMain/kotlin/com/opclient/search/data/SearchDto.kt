package com.opclient.search.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponseDto(
    val numFound: Int,
    val start: Int,
    val docs: List<SearchDocDto>,
)

@Serializable
data class SearchDocDto(
    val key: String,
    val title: String? = null,
    @SerialName("author_name") val authorName: List<String>? = null,
    @SerialName("first_publish_year") val firstPublishYear: Int? = null,
    @SerialName("cover_i") val coverId: Int? = null,
    val subject: List<String>? = null,
)
