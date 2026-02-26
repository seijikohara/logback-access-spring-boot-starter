package io.github.seijikohara.spring.boot.logback.access.jetty

import io.github.seijikohara.spring.boot.logback.access.AccessEventData.Companion.REMOTE_USER_ATTR
import io.github.seijikohara.spring.boot.logback.access.LocalPortStrategy
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import org.eclipse.jetty.server.Request
import java.util.Collections.unmodifiableMap

/**
 * Extracts and resolves request data from Jetty [Request].
 *
 * Provides extraction of headers, cookies, and attributes, as well as resolution
 * of local port, remote user, and request URL.
 */
internal object JettyRequestDataExtractor {
    fun resolveLocalPort(
        context: LogbackAccessContext,
        request: Request,
    ): Int =
        when (context.properties.localPortStrategy) {
            LocalPortStrategy.LOCAL -> Request.getLocalPort(request)
            LocalPortStrategy.SERVER -> Request.getServerPort(request)
        }

    fun resolveRemoteUser(request: Request): String? = request.getAttribute(REMOTE_USER_ATTR) as? String

    fun buildRequestURL(request: Request): String =
        "${request.method} ${request.httpURI.path}${request.httpURI.query?.let { "?$it" }.orEmpty()} ${request.connectionMetaData.protocol}"

    fun extractHeaders(request: Request): Map<String, String> =
        sortedMapOf<String, String>(String.CASE_INSENSITIVE_ORDER)
            .apply { request.headers.forEach { field -> putIfAbsent(field.name, field.value) } }
            .let(::unmodifiableMap)

    fun extractCookies(request: Request): Map<String, String> =
        try {
            Request.getCookies(request)
        } catch (_: Exception) {
            emptyList()
        }.associateTo(linkedMapOf()) { it.name to it.value }
            .let(::unmodifiableMap)

    fun extractAttributes(request: Request): Map<String, String> =
        request.attributeNameSet
            .mapNotNull { name ->
                request.getAttribute(name)?.let { name to it.toString() }
            }.toMap(linkedMapOf())
            .let(::unmodifiableMap)
}
