// composeApp/src/commonTest/kotlin/com/opclient/author/AuthorMapperTest.kt
package com.opclient.author

import com.opclient.author.data.AuthorDto
import com.opclient.author.data.AuthorWorkEntryDto
import com.opclient.author.data.toAuthorPhotoUrl
import com.opclient.author.data.toDomain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthorMapperTest {

    @Test
    fun toAuthorPhotoUrl_null_returnsNull() {
        assertNull(null.toAuthorPhotoUrl())
    }

    @Test
    fun toAuthorPhotoUrl_emptyList_returnsNull() {
        assertNull(emptyList<Int>().toAuthorPhotoUrl())
    }

    @Test
    fun toAuthorPhotoUrl_returnsMediumUrl() {
        assertEquals(
            "https://covers.openlibrary.org/a/id/7777-M.jpg",
            listOf(7777).toAuthorPhotoUrl(),
        )
    }

    @Test
    fun toDomain_mapsAllFields() {
        val dto = AuthorDto(
            key = "/authors/OL26320A",
            name = "Frank Herbert",
            bio = JsonPrimitive("American author"),
            birthDate = "1920",
            deathDate = "1986",
            photos = listOf(7777),
        )
        val works = listOf(
            AuthorWorkEntryDto(key = "/works/OL82563W", title = "Dune", covers = listOf(12345)),
        )

        val domain = dto.toDomain(works)

        assertEquals("/authors/OL26320A", domain.key)
        assertEquals("Frank Herbert", domain.name)
        assertEquals("American author", domain.bio)
        assertEquals("1920", domain.birthDate)
        assertEquals("1986", domain.deathDate)
        assertEquals("https://covers.openlibrary.org/a/id/7777-M.jpg", domain.photoUrl)
        assertEquals(1, domain.works.size)
        assertEquals("/works/OL82563W", domain.works[0].key)
        assertEquals("Dune", domain.works[0].title)
        assertEquals("https://covers.openlibrary.org/b/id/12345-M.jpg", domain.works[0].coverUrl)
    }

    @Test
    fun toDomain_nullName_fallsBackToUnknown() {
        val dto = AuthorDto(key = "/authors/OL1A", name = null)
        assertEquals("Unknown Author", dto.toDomain(emptyList()).name)
    }

    @Test
    fun toDomain_bioAsObject_extractsValue() {
        val dto = AuthorDto(
            key = "/authors/OL1A",
            bio = JsonObject(mapOf(
                "type" to JsonPrimitive("/type/text"),
                "value" to JsonPrimitive("Object bio"),
            )),
        )
        assertEquals("Object bio", dto.toDomain(emptyList()).bio)
    }

    @Test
    fun toDomain_noPhotos_photoUrlIsNull() {
        val dto = AuthorDto(key = "/authors/OL1A", photos = null)
        assertNull(dto.toDomain(emptyList()).photoUrl)
    }

    @Test
    fun toDomain_workWithNoCovers_coverUrlIsNull() {
        val dto = AuthorDto(key = "/authors/OL1A")
        val works = listOf(AuthorWorkEntryDto(key = "/works/OL1W", title = "Book", covers = null))
        assertNull(dto.toDomain(works).works[0].coverUrl)
    }

    @Test
    fun toDomain_workWithNullTitle_fallsBackToUnknown() {
        val dto = AuthorDto(key = "/authors/OL1A")
        val works = listOf(AuthorWorkEntryDto(key = "/works/OL1W", title = null))
        assertEquals("Unknown Title", dto.toDomain(works).works[0].title)
    }
}
