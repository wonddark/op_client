package com.opclient.subject

import com.opclient.subject.data.SubjectDto
import com.opclient.subject.data.SubjectWorkAuthorDto
import com.opclient.subject.data.SubjectWorkDto
import com.opclient.subject.data.toCoverUrl
import com.opclient.subject.data.toDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubjectMapperTest {

    @Test
    fun toCoverUrl_withCoverId_returnsUrl() {
        assertEquals(
            "https://covers.openlibrary.org/b/id/99-M.jpg",
            99.toCoverUrl(),
        )
    }

    @Test
    fun toCoverUrl_null_returnsNull() {
        assertNull(null.toCoverUrl())
    }

    @Test
    fun subjectWorkDto_toDomain_mapsAllFields() {
        val dto = SubjectWorkDto(
            key = "/works/OL1W",
            title = "Dune",
            authors = listOf(SubjectWorkAuthorDto("Frank Herbert")),
            coverId = 123,
        )
        val domain = dto.toDomain()
        assertEquals("/works/OL1W", domain.key)
        assertEquals("Dune", domain.title)
        assertEquals("Frank Herbert", domain.authorName)
        assertEquals("https://covers.openlibrary.org/b/id/123-M.jpg", domain.coverUrl)
    }

    @Test
    fun subjectWorkDto_nullTitle_defaultsToUnknown() {
        val dto = SubjectWorkDto(key = "/works/OL1W", title = null, authors = null, coverId = null)
        val domain = dto.toDomain()
        assertEquals("Unknown Title", domain.title)
        assertNull(domain.authorName)
        assertNull(domain.coverUrl)
    }

    @Test
    fun subjectWorkDto_emptyAuthors_nullAuthorName() {
        val dto = SubjectWorkDto(key = "/works/OL1W", title = "T", authors = emptyList(), coverId = null)
        assertNull(dto.toDomain().authorName)
    }

    @Test
    fun subjectDto_toDomain_mapsNameAndWorkCount() {
        val dto = SubjectDto(name = "Science Fiction", workCount = 100, works = null)
        val page = dto.toDomain()
        assertEquals("Science Fiction", page.subjectName)
        assertEquals(100, page.workCount)
        assertEquals(emptyList(), page.works)
    }

    @Test
    fun subjectDto_toDomain_mapsWorks() {
        val dto = SubjectDto(
            name = "SF",
            workCount = 1,
            works = listOf(SubjectWorkDto("/works/OL1W", "Dune", listOf(SubjectWorkAuthorDto("FH")), 5)),
        )
        val page = dto.toDomain()
        assertEquals(1, page.works.size)
        assertEquals("Dune", page.works[0].title)
        assertEquals("FH", page.works[0].authorName)
    }

    @Test
    fun subjectDto_nullWorks_returnsEmptyList() {
        val dto = SubjectDto(name = "SF", workCount = 0, works = null)
        assertEquals(emptyList(), dto.toDomain().works)
    }
}
