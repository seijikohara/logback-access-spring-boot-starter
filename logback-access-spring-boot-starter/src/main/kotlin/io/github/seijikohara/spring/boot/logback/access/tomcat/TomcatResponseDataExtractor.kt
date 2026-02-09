package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.AccessConstants.LB_OUTPUT_BUFFER
import ch.qos.logback.access.common.servlet.Util.isImageResponse
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
import java.nio.charset.Charset
import java.util.Collections.unmodifiableMap

/**
 * Extracts response data (headers, content) from Tomcat [Response].
 */
internal object TomcatResponseDataExtractor {
    private const val IMAGE_CONTENTS_SUPPRESSED = "[IMAGE CONTENTS SUPPRESSED]"

    fun extractHeaders(response: Response): Map<String, String> =
        sortedMapOf<String, String>(String.CASE_INSENSITIVE_ORDER)
            .also { headers ->
                response.headerNames.associateWithTo(headers) { response.getHeader(it) }
            }.let(::unmodifiableMap)

    /**
     * Extracts response body content captured by TeeFilter.
     *
     * Uses the response's character encoding for byte-to-string conversion,
     * falling back to UTF-8 when the encoding is not specified or unsupported.
     */
    fun extractContent(
        request: Request,
        response: Response,
    ): String? =
        when {
            isImageResponse(response) -> {
                IMAGE_CONTENTS_SUPPRESSED
            }

            else -> {
                (request.getAttribute(LB_OUTPUT_BUFFER) as? ByteArray)
                    ?.let { String(it, resolveCharset(response.characterEncoding)) }
            }
        }

    private fun resolveCharset(encoding: String?): Charset =
        encoding?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
}
