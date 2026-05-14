package com.opclient.library.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.opclient.library.LibraryDatabase
import com.opclient.library.domain.LibraryEntry
import com.opclient.library.domain.LibraryRepository
import com.opclient.library.domain.Shelf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class LibraryRepositoryImpl(private val db: LibraryDatabase) : LibraryRepository {

    override fun getShelf(shelf: Shelf): Flow<List<LibraryEntry>> =
        db.libraryQueries.getShelf(shelf.name)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { it.toDomain() } }

    override fun getCurrentShelf(workKey: String): Flow<Shelf?> =
        db.libraryQueries.getShelfForBook(workKey)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { shelfName -> shelfName?.let { Shelf.valueOf(it) } }

    override suspend fun addToShelf(entry: LibraryEntry): Unit = withContext(Dispatchers.IO) {
        db.libraryQueries.upsertEntry(
            entry.workKey, entry.title, entry.authorName, entry.coverUrl,
            entry.shelf.name, entry.addedAt,
        )
        Unit
    }

    override suspend fun removeFromShelf(workKey: String): Unit = withContext(Dispatchers.IO) {
        db.libraryQueries.removeEntry(workKey)
        Unit
    }

    override suspend fun moveToShelf(workKey: String, shelf: Shelf): Unit = withContext(Dispatchers.IO) {
        db.libraryQueries.updateShelf(shelf.name, workKey)
        Unit
    }
}

private fun com.opclient.library.LibraryEntry.toDomain() = LibraryEntry(
    workKey = work_key,
    title = title,
    authorName = author_name,
    coverUrl = cover_url,
    shelf = Shelf.valueOf(shelf),
    addedAt = added_at,
)
