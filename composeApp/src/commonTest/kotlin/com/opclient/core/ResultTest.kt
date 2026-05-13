package com.opclient.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ResultTest {

    @Test
    fun success_holdsValue() {
        val result: Result<String, Nothing> = Result.Success("hello")
        assertIs<Result.Success<String>>(result)
        assertEquals("hello", result.value)
    }

    @Test
    fun failure_holdsError() {
        val result: Result<Nothing, String> = Result.Failure("error")
        assertIs<Result.Failure<String>>(result)
        assertEquals("error", result.error)
    }

    @Test
    fun getOrNull_onSuccess_returnsValue() {
        val result: Result<Int, String> = Result.Success(42)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun getOrNull_onFailure_returnsNull() {
        val result: Result<Int, String> = Result.Failure("bad")
        assertNull(result.getOrNull())
    }

    @Test
    fun errorOrNull_onFailure_returnsError() {
        val result: Result<Int, String> = Result.Failure("bad")
        assertEquals("bad", result.errorOrNull())
    }

    @Test
    fun errorOrNull_onSuccess_returnsNull() {
        val result: Result<Int, String> = Result.Success(42)
        assertNull(result.errorOrNull())
    }

    @Test
    fun map_onSuccess_transformsValue() {
        val result: Result<Int, String> = Result.Success(5)
        val mapped = result.map { it * 2 }
        assertIs<Result.Success<Int>>(mapped)
        assertEquals(10, mapped.value)
    }

    @Test
    fun map_onFailure_passesThrough() {
        val result: Result<Int, String> = Result.Failure("err")
        val mapped = result.map { it * 2 }
        assertIs<Result.Failure<String>>(mapped)
        assertEquals("err", mapped.error)
    }

    @Test
    fun flatMap_onSuccess_chains() {
        val result: Result<Int, String> = Result.Success(5)
        val chained = result.flatMap { n ->
            if (n > 0) Result.Success(n * 2) else Result.Failure("negative")
        }
        assertIs<Result.Success<Int>>(chained)
        assertEquals(10, chained.value)
    }

    @Test
    fun flatMap_onFailure_shortCircuits() {
        val result: Result<Int, String> = Result.Failure("err")
        var called = false
        val chained = result.flatMap { called = true; Result.Success(it) }
        assertIs<Result.Failure<String>>(chained)
        assertEquals("err", chained.error)
        assertEquals(false, called)
    }
}
