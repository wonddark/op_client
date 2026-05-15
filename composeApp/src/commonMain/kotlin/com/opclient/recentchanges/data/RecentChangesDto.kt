package com.opclient.recentchanges.data

import kotlinx.serialization.Serializable

@Serializable
data class RecentChangeDto(
    val id: String? = null,
    val kind: String? = null,
    val timestamp: String? = null,
    val author: RecentChangeAuthorDto? = null,
    val changes: List<RecentChangeRefDto> = emptyList(),
)

@Serializable
data class RecentChangeAuthorDto(val key: String? = null)

@Serializable
data class RecentChangeRefDto(val key: String? = null)
