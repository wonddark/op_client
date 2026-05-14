package com.opclient.library

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.turbine.test
import com.opclient.library.data.LibraryRepositoryImpl
import com.opclient.library.domain.LibraryEntry
import com.opclient.library.domain.LibraryRepository
import com.opclient.library.domain.Shelf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LibraryRepositoryTest {

    private fun createRepo(): LibraryRepository {
        val driver = JdbcSqliteDriver("jdbc:sqlite::memory:")
        LibraryDatabase.Schema.create(driver)
        val db = LibraryDatabase(driver)
        return LibraryRepositoryImpl(db)
    }

    private fun entry(
        workKey: String = "/works/OL1W",
        title: String = "Test Book",
        shelf: Shelf = Shelf.WANT_TO_READ,
        addedAt: Long = 1000L,
    ) = LibraryEntry(
        workKey = workKey,
        title = title,
        authorName = "Test Author",
        coverUrl = null,
        shelf = shelf,
        addedAt = addedAt,
    )

    @Test
    fun addToShelf_persistsEntry() = runTest {
        val repo = createRepo()
        repo.addToShelf(entry())
        repo.getShelf(Shelf.WANT_TO_READ).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("/works/OL1W", list.first().workKey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getShelf_emitsOnChange() = runTest {
        val repo = createRepo()
        repo.getShelf(Shelf.WANT_TO_READ).test {
            assertEquals(emptyList(), awaitItem())
            repo.addToShelf(entry())
            val list = awaitItem()
            assertEquals(1, list.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun moveToShelf_updatesShelf() = runTest {
        val repo = createRepo()
        repo.addToShelf(entry(shelf = Shelf.WANT_TO_READ))
        repo.moveToShelf("/works/OL1W", Shelf.READING)
        repo.getShelf(Shelf.READING).test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals(Shelf.READING, list.first().shelf)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun removeFromShelf_deletesEntry() = runTest {
        val repo = createRepo()
        repo.addToShelf(entry())
        repo.removeFromShelf("/works/OL1W")
        repo.getShelf(Shelf.WANT_TO_READ).test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getCurrentShelf_nullForUnknownKey() = runTest {
        val repo = createRepo()
        repo.getCurrentShelf("/works/UNKNOWN").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getShelf_isolatesEntriesByShelf() = runTest {
        val repo = createRepo()
        repo.addToShelf(entry(workKey = "/works/OL1W", shelf = Shelf.READING))
        repo.getShelf(Shelf.WANT_TO_READ).test {
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
