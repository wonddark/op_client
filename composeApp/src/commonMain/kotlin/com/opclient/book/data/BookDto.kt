// composeApp/src/commonMain/kotlin/com/opclient/book/data/BookDto.kt
package com.opclient.book.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WorkDto(
    val key: String,
    val title: String? = null,
    val description: JsonElement? = null,
    @SerialName("first_publish_date") val firstPublishDate: String? = null,
    val subjects: List<String>? = null,
    val covers: List<Int>? = null,
    val authors: List<WorkAuthorEntryDto>? = null,
)

@Serializable
data class WorkAuthorEntryDto(val author: WorkAuthorKeyDto)

@Serializable
data class WorkAuthorKeyDto(val key: String)
