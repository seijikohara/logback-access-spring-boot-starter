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
            .also { headers ->
                response.headerNames.associateWithTo(headers) { response.getHeader(it) }
            }.let(::unmodifiableMap)

    /**
     * Extracts response body content captured by TeeFilter.
     *
     * Evaluates body capture policy (content type and size) before conversion.
     * Uses the response's character encoding for byte-to-string conversion,
     * falling back to UTF-8 when the encoding is not specified or unsupported.
     */
    fun extractContent(
        request: Request,
        response: Response,
        teeFilterProperties: TeeFilterProperties,
    ): String? {
        val buffer = request.getAttribute(LB_OUTPUT_BUFFER) as? ByteArray ?: return null
        return BodyCapturePolicy.evaluate(response.contentType, buffer.size, teeFilterProperties)
            ?: String(buffer, BodyCapturePolicy.resolveCharset(response.characterEncoding))
    }
}
