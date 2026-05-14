package com.opclient.profile.domain

data class UserProfile(
    val username: String,
    val displayName: String,
    val bio: String?,
    val photoId: Int?,
) {
    val photoUrl: String? = photoId?.let { "https://covers.openlibrary.org/p/id/$it-M.jpg" }
}
