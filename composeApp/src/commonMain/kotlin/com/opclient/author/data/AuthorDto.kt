// composeApp/src/commonMain/kotlin/com/opclient/author/data/AuthorDto.kt
package com.opclient.author.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class AuthorDto(
    val key: String,
    val name: String? = null,
    val bio: JsonElement? = null,
    @SerialName("birth_date") val birthDate: String? = null,
    @SerialName("death_date") val deathDate: String? = null,
    val photos: List<Int>? = null,
)

@Serializable
data class AuthorWorksDto(
    val entries: List<AuthorWorkEntryDto>? = null,
    val size: Int = 0,
)

@Serializable
data class AuthorWorkEntryDto(
    val key: String,
    val title: String? = null,
    val covers: List<Int>? = null,
)
