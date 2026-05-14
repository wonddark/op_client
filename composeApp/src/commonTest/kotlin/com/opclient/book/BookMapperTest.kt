// composeApp/src/commonTest/kotlin/com/opclient/book/BookMapperTest.kt
package com.opclient.book

import com.opclient.book.data.WorkAuthorEntryDto
import com.opclient.book.data.WorkAuthorKeyDto
import com.opclient.book.data.WorkDto
import com.opclient.book.data.toText
import com.opclient.book.data.toDomain
import com.opclient.book.data.toBookCoverUrl
import com.opclient.book.domain.AuthorRef
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookMapperTest {

    @Test
    fun toText_null_returnsNull() {
        assertNull(null.toText())
    }

    @Test
    fun toText_jsonNull_returnsNull() {
        assertNull(JsonNull.toText())
    }

    @Test
    fun toText_string_returnsContent() {
        assertEquals("Some description", JsonPrimitive("Some description").toText())
    }

    @Test
    fun toText_object_returnsValue() {
        val obj = JsonObject(mapOf(
            "type" to JsonPrimitive("/type/text"),
            "value" to JsonPrimitive("Rich text"),
        ))
        assertEquals("Rich text", obj.toText())
    }

    @Test
    fun toText_objectMissingValue_returnsNull() {
        val obj = JsonObject(mapOf("type" to JsonPrimitive("/type/text")))
        assertNull(obj.toText())
    }

    @Test
    fun toBookCoverUrl_null_returnsNull() {
        assertNull(null.toBookCoverUrl())
    }

    @Test
    fun toBookCoverUrl_emptyList_returnsNull() {
        assertNull(emptyList<Int>().toBookCoverUrl())
    }

    @Test
    fun toBookCoverUrl_returnsLargeCover() {
        assertEquals(
            "https://covers.openlibrary.org/b/id/12345-L.jpg",
            listOf(12345).toBookCoverUrl(size = "L"),
        )
    }

    @Test
    fun toBookCoverUrl_defaultSize_isM() {
        assertEquals(
            "https://covers.openlibrary.org/b/id/99-M.jpg",
            listOf(99).toBookCoverUrl(),
        )
    }

    @Test
    fun toDomain_mapsAllFields() {
        val dto = WorkDto(
            key = "/works/OL82563W",
            title = "Dune",
            description = JsonPrimitive("A great book"),
            firstPublishDate = "1965",
            subjects = listOf("Science Fiction", "Adventure"),
            covers = listOf(12345),
            authors = listOf(
                WorkAuthorEntryDto(author = WorkAuthorKeyDto(key = "/authors/OL222A")),
            ),
        )
        val authorRefs = listOf(AuthorRef(key = "/authors/OL222A", name = "Frank Herbert"))

        val domain = dto.toDomain(authorRefs)

        assertEquals("/works/OL82563W", domain.key)
        assertEquals("Dune", domain.title)
        assertEquals("A great book", domain.description)
        assertEquals("1965", domain.firstPublishDate)
        assertEquals(listOf("Science Fiction", "Adventure"), domain.subjects)
        assertEquals("https://covers.openlibrary.org/b/id/12345-L.jpg", domain.coverUrl)
        assertEquals(authorRefs, domain.authors)
    }

    @Test
    fun toDomain_nullTitle_fallsBackToUnknown() {
        val dto = WorkDto(key = "/works/OL1W", title = null)
        val domain = dto.toDomain(emptyList())
        assertEquals("Unknown Title", domain.title)
    }

    @Test
    fun toDomain_noCovers_coverUrlIsNull() {
        val dto = WorkDto(key = "/works/OL1W", covers = null)
        val domain = dto.toDomain(emptyList())
        assertNull(domain.coverUrl)
    }

    @Test
    fun extractOlid_fromKeyPath() {
        assertEquals("OL82563W", "/works/OL82563W".substringAfterLast("/"))
        assertEquals("OL26320A", "/authors/OL26320A".substringAfterLast("/"))
    }
}
