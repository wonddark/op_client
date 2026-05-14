package com.opclient.profile.data

import com.opclient.profile.domain.UserProfile
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

fun UserProfileDto.toDomain(): UserProfile {
    val bioText = when (val b = bio) {
        is JsonPrimitive -> b.contentOrNull
        is JsonObject -> b["value"]?.jsonPrimitive?.contentOrNull
        else -> null
    }
    return UserProfile(
        username = key?.removePrefix("/people/") ?: "",
        displayName = name ?: "",
        bio = bioText,
        photoId = photos?.firstOrNull(),
    )
}
