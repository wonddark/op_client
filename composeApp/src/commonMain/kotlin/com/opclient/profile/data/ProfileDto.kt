package com.opclient.profile.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class UserProfileDto(
    val key: String? = null,
    val name: String? = null,
    val bio: JsonElement? = null,
    val photos: List<Int>? = null,
    val location: String? = null,
)
