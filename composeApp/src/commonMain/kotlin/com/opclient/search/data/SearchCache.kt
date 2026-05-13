package com.opclient.search.data

import com.opclient.search.domain.SearchResults
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

private const val DEFAULT_TTL_MS = 5 * 60 * 1_000L

class SearchCache(private val ttlMs: Long = DEFAULT_TTL_MS) {
    private val mutex = Mutex()
    private val store = mutableMapOf<String, Pair<SearchResults, Long>>()

    suspend fun get(key: String): SearchResults? = mutex.withLock {
        store[key]?.takeIf { (_, ts) -> now() - ts < ttlMs }?.first
    }

    suspend fun put(key: String, results: SearchResults): Unit = mutex.withLock {
        store[key] = results to now()
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
