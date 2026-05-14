package com.opclient.library.domain

enum class Shelf(val label: String) {
    WANT_TO_READ("Want to Read"),
    READING("Reading"),
    READ("Read"),
}

data class LibraryEntry(
    val workKey: String,
    val title: String,
    val authorName: String?,
    val coverUrl: String?,
    val shelf: Shelf,
    val addedAt: Long,
)
