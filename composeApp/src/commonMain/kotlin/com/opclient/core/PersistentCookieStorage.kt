package com.opclient.core

import com.opclient.settings.domain.SettingsRepository
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url

class PersistentCookieStorage(private val settings: SettingsRepository) : CookiesStorage {

    override suspend fun get(requestUrl: Url): List<Cookie> {
        if (!requestUrl.host.endsWith("openlibrary.org")) return emptyList()
        val session = settings.getSessionCookie() ?: return emptyList()
        return listOf(
            Cookie(name = "session", value = session, domain = ".openlibrary.org", path = "/"),
        )
    }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        if (!requestUrl.host.endsWith("openlibrary.org")) return
        if (cookie.name == "session") {
            settings.setSessionCookie(cookie.value)
        }
    }

    override fun close() = Unit
}
