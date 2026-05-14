package com.opclient.search

import com.opclient.search.data.SearchCache
import com.opclient.search.domain.SearchResults
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchCacheTest {

    private fun results(query: String = "dune") =
        SearchResults(query = query, totalFound = 1, books = emptyList(), offset = 0)

    @Test
    fun get_onMiss_returnsNull() = runTest {
        val cache = SearchCache()
        assertNull(cache.get("dune:0"))
    }

    @Test
    fun get_afterPut_returnsEntry() = runTest {
        val cache = SearchCache()
        val data = results()
        cache.put("dune:0", data)
        assertEquals(data, cache.get("dune:0"))
    }

    @Test
    fun get_expiredEntry_returnsNull() = runTest {
        var fakeTime = 0L
        val cache = SearchCache(ttlMs = 100L, timeSource = { fakeTime })
        cache.put("dune:0", results())
        fakeTime = 200L
        assertNull(cache.get("dune:0"))
    }

    @Test
    fun get_differentKeys_independent() = runTest {
        val cache = SearchCache()
        val r1 = results("dune")
        val r2 = results("foundation")
        cache.put("dune:0", r1)
        cache.put("foundation:0", r2)
        assertEquals(r1, cache.get("dune:0"))
        assertEquals(r2, cache.get("foundation:0"))
    }

    @Test
    fun put_overwritesExistingEntry() = runTest {
        val cache = SearchCache()
        cache.put("dune:0", results("old"))
        val updated = results("updated")
        cache.put("dune:0", updated)
        assertEquals(updated, cache.get("dune:0"))
    }
}
