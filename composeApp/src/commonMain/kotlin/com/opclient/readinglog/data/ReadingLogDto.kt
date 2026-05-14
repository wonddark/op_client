package com.opclient.readinglog.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReadingLogResponseDto(
    @SerialName("reading_log_entries") val entries: List<ReadingLogEntryDto> = emptyList(),
)

@Serializable
data class ReadingLogEntryDto(
    val work: ReadingLogWorkDto,
    @SerialName("logged_date") val loggedDate: String? = null,
)

@Serializable
data class ReadingLogWorkDto(
    val key: String,
    val title: String? = null,
    @SerialName("author_names") val authorNames: List<String>? = null,
    @SerialName("cover_id") val coverId: Int? = null,
)
