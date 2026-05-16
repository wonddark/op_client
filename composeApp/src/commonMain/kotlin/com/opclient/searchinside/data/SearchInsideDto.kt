package com.opclient.searchinside.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchInsideResponseDto(
    val hits: SearchInsideHitsWrapperDto,
)

@Serializable
data class SearchInsideHitsWrapperDto(
    val hits: List<SearchInsideHitDto> = emptyList(),
)

@Serializable
data class SearchInsideHitDto(
    val highlight: SearchInsideHighlightDto? = null,
    val edition: SearchInsideEditionDto? = null,
)

@Serializable
data class SearchInsideHighlightDto(
    val text: List<String> = emptyList(),
)

@Serializable
data class SearchInsideEditionDto(
    @SerialName("work_key") val workKey: String? = null,
    val title: String? = null,
    val authors: List<SearchInsideAuthorDto> = emptyList(),
    @SerialName("cover_url") val coverUrl: String? = null,
)

@Serializable
data class SearchInsideAuthorDto(
    val name: String? = null,
)
