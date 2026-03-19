package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.AccessConstants.LB_OUTPUT_BUFFER
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties.TeeFilterProperties
import io.github.seijikohara.spring.boot.logback.access.tee.BodyCapturePolicy
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
import java.util.Collections.unmodifiableMap

/**
 * Extracts response data (headers, content) from Tomcat [Response].
 */
internal object TomcatResponseDataExtractor {
    fun extractHeaders(response: Response): Map<String, String> =
        sortedMapOf<String, String>(String.CASE_INSENSITIVE_ORDER)
            .apply { response.headerNames.forEach { name -> putIfAbsent(name, response.getHeader(name)) } }
            .let(::unmodifiableMap)

    /**
     * Extracts response body content captured by TeeFilter.
     *
     * Returns `null` immediately when TeeFilter is disabled.
     *
     * Evaluates body capture policy (content type and size) before conversion.
     * Resolves the charset from the explicit `charset` parameter of the Content-Type header.
     * Falls back to UTF-8 when no charset is specified, because RFC 8259 §8.1 mandates UTF-8
     * for JSON and Tomcat returns ISO-8859-1 (the HTTP/1.1 default) from
     * `response.characterEncoding` when no charset is set in the Content-Type header.
     */
    fun extractContent(
        request: Request,
        response: Response,
        teeFilterProperties: TeeFilterProperties,
    ): String? =
        if (!teeFilterProperties.enabled) {
            null
        } else {
            (request.getAttribute(LB_OUTPUT_BUFFER) as? ByteArray)?.let { buffer ->
                BodyCapturePolicy.evaluate(response.contentType, buffer.size.toLong(), teeFilterProperties)
                    ?: String(buffer, BodyCapturePolicy.resolveCharset(resolveContentTypeCharset(response.contentType)))
            }
        }

    private fun resolveContentTypeCharset(contentType: String?): String? =
        contentType
            ?.splitToSequence(';')
            ?.drop(1)
            ?.map { it.trim() }
            ?.firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter('=')
}
