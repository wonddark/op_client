package com.opclient.book

import com.opclient.book.data.BookCache
import com.opclient.book.domain.BookDetail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookCacheTest {

    private fun detail(key: String = "/works/OL1W") = BookDetail(
        key = key, title = "T", description = null, authors = emptyList(),
        subjects = emptyList(), firstPublishDate = null, coverUrl = null,
    )

    @Test
    fun get_onMiss_returnsNull() = runTest {
        assertNull(BookCache().get("work:OL1W"))
    }

    @Test
    fun get_afterPut_returnsEntry() = runTest {
        val cache = BookCache()
        val d = detail()
        cache.put("work:OL1W", d)
        assertEquals(d, cache.get("work:OL1W"))
    }

    @Test
    fun get_expiredEntry_returnsNull() = runTest {
        var fakeTime = 0L
        val cache = BookCache(ttlMs = 100L, timeSource = { fakeTime })
        cache.put("work:OL1W", detail())
        fakeTime = 200L
        assertNull(cache.get("work:OL1W"))
    }

    @Test
    fun put_overwritesExistingEntry() = runTest {
        val cache = BookCache()
        cache.put("work:OL1W", detail("old"))
        val updated = detail("new")
        cache.put("work:OL1W", updated)
        assertEquals(updated, cache.get("work:OL1W"))
    }
}
