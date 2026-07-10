package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.AccessConstants.LB_INPUT_BUFFER
import ch.qos.logback.access.common.AccessConstants.LB_OUTPUT_BUFFER
import ch.qos.logback.access.common.servlet.Util.isFormUrlEncoded
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties.TeeFilterProperties
import io.github.seijikohara.spring.boot.logback.access.tee.BodyCapturePolicy
import org.apache.catalina.connector.Request
import java.net.URLDecoder.decode
import java.net.URLEncoder.encode
import java.nio.charset.Charset
import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableMap

/**
 * Extracts request data (headers, cookies, parameters, attributes, content) from Tomcat [Request].
 */
internal object TomcatRequestDataExtractor {
    fun extractHeaders(request: Request): Map<String, String> =
        sortedMapOf<String, String>(String.CASE_INSENSITIVE_ORDER)
            .apply { for (name in request.headerNames) putIfAbsent(name, request.getHeader(name)) }
            .let(::unmodifiableMap)

    fun extractCookies(request: Request): Map<String, String> =
        request.cookies
            .orEmpty()
            .associateTo(linkedMapOf()) { it.name to it.value }
            .let(::unmodifiableMap)

    fun extractParameters(request: Request): Map<String, List<String>> =
        request.parameterMap
            .mapValuesTo(linkedMapOf()) { unmodifiableList(it.value.asList()) }
            .let(::unmodifiableMap)

    fun extractAttributes(request: Request): Map<String, String> =
        request.attributeNames
            .asSequence()
            .filter { it != LB_INPUT_BUFFER && it != LB_OUTPUT_BUFFER }
            .mapNotNull { name ->
                request.getAttribute(name)?.let { name to it.toString() }
            }.toMap(linkedMapOf())
            .let(::unmodifiableMap)

    private fun decodeBufferContent(
        request: Request,
        teeFilterProperties: TeeFilterProperties,
    ): String? =
        (request.getAttribute(LB_INPUT_BUFFER) as? ByteArray)?.let { buffer ->
            BodyCapturePolicy.evaluate(request.contentType, buffer.size.toLong(), teeFilterProperties)
                ?: String(buffer, BodyCapturePolicy.resolveCharset(request.characterEncoding))
        }

    private fun decodeFormDataContent(
        request: Request,
        teeFilterProperties: TeeFilterProperties,
    ): String? =
        encodeFormDataIfApplicable(request)?.let { formData ->
            val charset = BodyCapturePolicy.resolveCharset(request.characterEncoding)
            BodyCapturePolicy.evaluate(request.contentType, formData.toByteArray(charset).size.toLong(), teeFilterProperties)
                ?: formData
        }

    /**
     * Extracts request body content captured by TeeFilter.
     *
     * Returns `null` immediately when TeeFilter is disabled to prevent
     * unintended exposure of form data (e.g. login credentials).
     *
     * Evaluates body capture policy (content type and size) before conversion
     * for both TeeFilter-captured buffers and form data fallback paths.
     * Uses the request's character encoding for byte-to-string conversion,
     * falling back to UTF-8 when the encoding is not specified or unsupported.
     */
    fun extractContent(
        request: Request,
        teeFilterProperties: TeeFilterProperties,
    ): String? =
        teeFilterProperties
            .takeIf { it.enabled }
            ?.let { decodeBufferContent(request, it) ?: decodeFormDataContent(request, it) }

    private fun encodeFormDataIfApplicable(request: Request): String? =
        BodyCapturePolicy.resolveCharset(request.characterEncoding).let { charset ->
            request
                .takeIf { isFormUrlEncoded(it) }
                ?.let { bodyParameterPairs(it, charset) }
                ?.joinToString("&") { (key, value) ->
                    "${encode(key, charset.name())}=${encode(value, charset.name())}"
                }
        }

    /**
     * Rebuilds body parameter pairs from [Request.getParameterMap], which merges query-string
     * and body parameters with no origin marker. Each pair that also appears in the query
     * string is removed once so the result reflects only the body; a pair submitted
     * identically in both places is attributed to the query string, which is already logged
     * separately.
     */
    private fun bodyParameterPairs(
        request: Request,
        charset: Charset,
    ): List<Pair<String, String>> {
        val queryPairs = parseQueryPairs(request.queryString, charset).toMutableList()
        return request.parameterMap
            .asSequence()
            .flatMap { (key, values) -> values.asSequence().map { key to it } }
            .filterNot { queryPairs.remove(it) }
            .toList()
    }

    // A malformed query string falls back to no subtraction instead of failing the event.
    private fun parseQueryPairs(
        queryString: String?,
        charset: Charset,
    ): List<Pair<String, String>> =
        try {
            queryString
                ?.split('&')
                ?.filter { it.isNotEmpty() }
                ?.map { parameter ->
                    decode(parameter.substringBefore('='), charset) to decode(parameter.substringAfter('=', ""), charset)
                }.orEmpty()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
}
