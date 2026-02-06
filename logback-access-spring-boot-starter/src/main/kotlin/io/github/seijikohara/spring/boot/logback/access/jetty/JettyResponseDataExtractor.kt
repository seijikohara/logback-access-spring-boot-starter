package io.github.seijikohara.spring.boot.logback.access.jetty

import org.eclipse.jetty.server.Response
import java.util.Collections.unmodifiableMap

/**
 * Extracts response data (headers) from Jetty [Response].
 */
internal object JettyResponseDataExtractor {
    fun extractHeaders(response: Response): Map<String, String> =
        response.headers
            .associateTo(sortedMapOf(String.CASE_INSENSITIVE_ORDER)) {
                it.name to it.value
            }.let(::unmodifiableMap)
}
