package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.AccessConstants.LB_OUTPUT_BUFFER
import ch.qos.logback.access.common.servlet.Util.isImageResponse
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
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

    fun extractContent(
        request: Request,
        response: Response,
    ): String? =
        when {
            isImageResponse(response) -> IMAGE_CONTENTS_SUPPRESSED
            else -> (request.getAttribute(LB_OUTPUT_BUFFER) as? ByteArray)?.let { String(it, Charsets.UTF_8) }
        }
}
