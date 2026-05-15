package com.opclient.searchinside.domain

data class SearchInsideResult(
    val workKey: String,
    val title: String,
    val authorName: String?,
    val coverUrl: String?,
    val passage: String,
)
