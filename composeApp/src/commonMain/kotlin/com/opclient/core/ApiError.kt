package com.opclient.core

sealed class ApiError {
    data class NetworkError(val cause: Throwable) : ApiError()
    data class HttpError(val code: Int, val body: String) : ApiError()
    data class ParseError(val cause: Throwable) : ApiError()
    data object Unknown : ApiError()
}
