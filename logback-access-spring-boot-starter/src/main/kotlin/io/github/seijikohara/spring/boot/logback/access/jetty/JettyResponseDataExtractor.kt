package io.github.seijikohara.spring.boot.logback.access.jetty

import org.eclipse.jetty.server.Response
import java.util.Collections.unmodifiableMap

/**
 * Extracts response data (headers) from Jetty [Response].
 */
internal object JettyResponseDataExtractor {
    fun extractHeaders(response: Response): Map<String, String> =
        sortedMapOf<String, String>(String.CASE_INSENSITIVE_ORDER)
            .apply { response.headers.forEach { field -> putIfAbsent(field.name, field.value) } }
            .let(::unmodifiableMap)
}
