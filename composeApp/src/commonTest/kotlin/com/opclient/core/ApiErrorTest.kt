package com.opclient.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ApiErrorTest {

    @Test
    fun networkError_storesThrowable() {
        val cause = RuntimeException("timeout")
        val err = ApiError.NetworkError(cause)
        assertIs<ApiError.NetworkError>(err)
        assertEquals(cause, err.cause)
    }

    @Test
    fun httpError_storesCodeAndBody() {
        val err = ApiError.HttpError(404, "Not Found")
        assertIs<ApiError.HttpError>(err)
        assertEquals(404, err.code)
        assertEquals("Not Found", err.body)
    }

    @Test
    fun parseError_storesThrowable() {
        val cause = IllegalArgumentException("bad json")
        val err = ApiError.ParseError(cause)
        assertIs<ApiError.ParseError>(err)
        assertEquals(cause, err.cause)
    }

    @Test
    fun unknown_isSingleton() {
        val a = ApiError.Unknown
        val b = ApiError.Unknown
        assertEquals(a, b)
    }
}
