package com.opclient.lists.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListsResponseDto(
    @SerialName("lists") val lists: List<ListDto> = emptyList(),
)

@Serializable
data class ListDto(
    @SerialName("key") val key: String = "",
    @SerialName("name") val name: String = "",
    @SerialName("description") val description: DescriptionDto? = null,
    @SerialName("seed_count") val seedCount: Int = 0,
)

@Serializable
data class DescriptionDto(
    @SerialName("value") val value: String = "",
)

@Serializable
data class CreateListRequestDto(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String,
    @SerialName("seeds") val seeds: List<String> = emptyList(),
    @SerialName("tags") val tags: List<String> = emptyList(),
)

@Serializable
data class CreateListResponseDto(
    @SerialName("key") val key: String = "",
    @SerialName("name") val name: String = "",
)

@Serializable
data class DeleteResponseDto(
    @SerialName("status") val status: String = "",
)

@Serializable
data class SeedUpdateRequestDto(
    @SerialName("add") val add: List<SeedRefDto> = emptyList(),
    @SerialName("remove") val remove: List<SeedRefDto> = emptyList(),
)

@Serializable
data class SeedRefDto(
    @SerialName("key") val key: String,
    @SerialName("type") val type: String = "work",
)

@Serializable
data class SeedsResponseDto(
    @SerialName("size") val size: Int = 0,
    @SerialName("entries") val entries: List<SeedEntryDto> = emptyList(),
)

@Serializable
data class SeedEntryDto(
    @SerialName("work") val work: SeedWorkDto? = null,
)

@Serializable
data class SeedWorkDto(
    @SerialName("key") val key: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("author_names") val authorNames: List<String>? = null,
    @SerialName("cover_id") val coverId: Int? = null,
)
