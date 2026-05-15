package com.opclient.recentchanges.domain

data class RecentChange(
    val id: String,
    val label: String,
    val targetKey: String?,
    val addedAt: Long,
)
