package com.opclient.author

import com.opclient.author.data.AuthorCache
import com.opclient.author.domain.AuthorDetail
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthorCacheTest {

    private fun detail(key: String = "/authors/OL1A") = AuthorDetail(
        key = key, name = "N", bio = null, birthDate = null,
        deathDate = null, photoUrl = null, works = emptyList(),
    )

    @Test
    fun get_onMiss_returnsNull() = runTest {
        assertNull(AuthorCache().get("author:OL1A"))
    }

    @Test
    fun get_afterPut_returnsEntry() = runTest {
        val cache = AuthorCache()
        val d = detail()
        cache.put("author:OL1A", d)
        assertEquals(d, cache.get("author:OL1A"))
    }

    @Test
    fun get_expiredEntry_returnsNull() = runTest {
        var fakeTime = 0L
        val cache = AuthorCache(ttlMs = 100L, timeSource = { fakeTime })
        cache.put("author:OL1A", detail())
        fakeTime = 200L
        assertNull(cache.get("author:OL1A"))
    }

    @Test
    fun put_overwritesExistingEntry() = runTest {
        val cache = AuthorCache()
        cache.put("author:OL1A", detail("old"))
        val updated = detail("new")
        cache.put("author:OL1A", updated)
        assertEquals(updated, cache.get("author:OL1A"))
    }
}
