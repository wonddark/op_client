package com.opclient.lists.domain

data class ReadingList(
    val key: String,
    val name: String,
    val description: String,
    val seedCount: Int,
)

data class ListSeed(
    val workKey: String,
    val title: String,
    val authorName: String?,
    val coverUrl: String?,
)
