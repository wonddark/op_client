package com.opclient.core

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal fun buildHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Napier.v(message, tag = "HttpClient")
            }
        }
        level = LogLevel.INFO
    }
    HttpResponseValidator {
        validateResponse { response ->
            if (!response.status.isSuccess()) {
                val statusCode = response.status.value
                val body = response.bodyAsText()
                if (statusCode in 400..499) {
                    throw io.ktor.client.plugins.ClientRequestException(response, body)
                } else if (statusCode in 500..599) {
                    throw io.ktor.client.plugins.ServerResponseException(response, body)
                }
            }
        }
    }
}
