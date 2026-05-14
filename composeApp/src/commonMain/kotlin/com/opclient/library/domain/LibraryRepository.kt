package com.opclient.library.domain

import kotlinx.coroutines.flow.Flow

interface LibraryRepository {
    fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>>
    fun getCurrentShelf(workKey: String): Flow<Shelf?>
    suspend fun addToShelf(entry: LibraryEntry)
    suspend fun removeFromShelf(workKey: String)
    suspend fun moveToShelf(workKey: String, shelf: Shelf)
}
