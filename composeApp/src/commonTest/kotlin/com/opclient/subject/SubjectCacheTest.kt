package com.opclient.subject

import com.opclient.subject.data.SubjectCache
import com.opclient.subject.domain.SubjectPage
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubjectCacheTest {

    private fun page(name: String = "SF") = SubjectPage(name, 10, emptyList())

    @Test
    fun get_onMiss_returnsNull() = runTest {
        assertNull(SubjectCache().get("subject:sf"))
    }

    @Test
    fun get_afterPut_returnsEntry() = runTest {
        val cache = SubjectCache()
        val p = page()
        cache.put("subject:sf", p)
        assertEquals(p, cache.get("subject:sf"))
    }

    @Test
    fun get_expiredEntry_returnsNull() = runTest {
        var fakeTime = 0L
        val cache = SubjectCache(ttlMs = 100L, timeSource = { fakeTime })
        cache.put("subject:sf", page())
        fakeTime = 200L
        assertNull(cache.get("subject:sf"))
    }

    @Test
    fun put_overwritesExistingEntry() = runTest {
        val cache = SubjectCache()
        cache.put("subject:sf", page("old"))
        val updated = page("new")
        cache.put("subject:sf", updated)
        assertEquals(updated, cache.get("subject:sf"))
    }

    @Test
    fun get_differentKeys_independent() = runTest {
        val cache = SubjectCache()
        val p1 = page("Mystery")
        val p2 = page("Fantasy")
        cache.put("subject:mystery", p1)
        cache.put("subject:fantasy", p2)
        assertEquals(p1, cache.get("subject:mystery"))
        assertEquals(p2, cache.get("subject:fantasy"))
    }
}
