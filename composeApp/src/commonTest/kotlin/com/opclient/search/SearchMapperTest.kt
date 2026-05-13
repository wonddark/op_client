package com.opclient.search

import com.opclient.search.data.SearchDocDto
import com.opclient.search.data.SearchResponseDto
import com.opclient.search.data.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchMapperTest {

    @Test
    fun docDto_mapsTitle() {
        val dto = SearchDocDto(key = "/works/OL1W", title = "Dune")
        assertEquals("Dune", dto.toDomain().title)
    }

    @Test
    fun docDto_nullTitle_usesUnknownTitle() {
        val dto = SearchDocDto(key = "/works/OL1W", title = null)
        assertEquals("Unknown Title", dto.toDomain().title)
    }

    @Test
    fun docDto_mapsFirstAuthor() {
        val dto = SearchDocDto(key = "/works/OL1W", authorName = listOf("Frank Herbert", "Other"))
        assertEquals("Frank Herbert", dto.toDomain().author)
    }

    @Test
    fun docDto_nullAuthorName_usesUnknown() {
        val dto = SearchDocDto(key = "/works/OL1W", authorName = null)
        assertEquals("Unknown", dto.toDomain().author)
    }

    @Test
    fun docDto_emptyAuthorList_usesUnknown() {
        val dto = SearchDocDto(key = "/works/OL1W", authorName = emptyList())
        assertEquals("Unknown", dto.toDomain().author)
    }

    @Test
    fun docDto_coverId_buildsCoverUrl() {
        val dto = SearchDocDto(key = "/works/OL1W", coverId = 12345)
        assertEquals("https://covers.openlibrary.org/b/id/12345-M.jpg", dto.toDomain().coverUrl)
    }

    @Test
    fun docDto_nullCoverId_nullCoverUrl() {
        val dto = SearchDocDto(key = "/works/OL1W", coverId = null)
        assertNull(dto.toDomain().coverUrl)
    }

    @Test
    fun docDto_mapsFirstSubject() {
        val dto = SearchDocDto(key = "/works/OL1W", subject = listOf("Science fiction", "Adventure"))
        assertEquals("Science fiction", dto.toDomain().primarySubject)
    }

    @Test
    fun docDto_nullSubject_nullPrimarySubject() {
        val dto = SearchDocDto(key = "/works/OL1W", subject = null)
        assertNull(dto.toDomain().primarySubject)
    }

    @Test
    fun responseDto_mapsNumFoundAndStart() {
        val dto = SearchResponseDto(numFound = 500, start = 20, docs = emptyList())
        val result = dto.toDomain("dune")
        assertEquals(500, result.totalFound)
        assertEquals(20, result.offset)
        assertEquals("dune", result.query)
    }

    @Test
    fun responseDto_mapsAllDocs() {
        val dto = SearchResponseDto(
            numFound = 2,
            start = 0,
            docs = listOf(
                SearchDocDto(key = "/works/OL1W", title = "Dune"),
                SearchDocDto(key = "/works/OL2W", title = "Foundation"),
            ),
        )
        assertEquals(2, dto.toDomain("sci-fi").books.size)
    }
}
