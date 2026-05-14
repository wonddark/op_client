package com.opclient.subject.data

import com.opclient.subject.domain.SubjectPage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

private const val DEFAULT_TTL_MS = 30 * 60 * 1_000L

class SubjectCache(
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val timeSource: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()
    private val store = mutableMapOf<String, Pair<SubjectPage, Long>>()

    suspend fun get(key: String): SubjectPage? = mutex.withLock {
        store[key]?.takeIf { (_, ts) -> now() - ts < ttlMs }?.first
    }

    suspend fun put(key: String, value: SubjectPage): Unit = mutex.withLock {
        store[key] = value to now()
    }

    private fun now(): Long = timeSource()
}
