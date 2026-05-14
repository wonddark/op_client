package com.opclient.book.data

import com.opclient.book.domain.BookDetail
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

private const val DEFAULT_TTL_MS = 30 * 60 * 1_000L

class BookCache(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val timeSource: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()
    private val store = mutableMapOf<String, Pair<BookDetail, Long>>()

    suspend fun get(key: String): BookDetail? = mutex.withLock {
        store[key]?.takeIf { (_, ts) -> now() - ts < ttlMs }?.first
    }

    suspend fun put(key: String, value: BookDetail): Unit = mutex.withLock {
        store[key] = value to now()
    }

    private fun now(): Long = timeSource()
}
