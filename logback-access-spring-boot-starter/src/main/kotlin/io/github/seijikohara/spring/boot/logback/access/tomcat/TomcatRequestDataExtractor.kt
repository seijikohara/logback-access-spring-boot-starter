package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.AccessConstants.LB_INPUT_BUFFER
import ch.qos.logback.access.common.AccessConstants.LB_OUTPUT_BUFFER
import ch.qos.logback.access.common.servlet.Util.isFormUrlEncoded
import org.apache.catalina.connector.Request
import java.net.URLEncoder.encode
import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableMap

/**
 * Extracts request data (headers, cookies, parameters, attributes, content) from Tomcat [Request].
 */
internal object TomcatRequestDataExtractor {
    fun extractHeaders(request: Request): Map<String, String> =
        sortedMapOf<String, String>(String.CASE_INSENSITIVE_ORDER)
            .also { headers ->
                request.headerNames.asSequence().associateWithTo(headers) { request.getHeader(it) }
            }.let(::unmodifiableMap)

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

    fun extractContent(request: Request): String? =
        (request.getAttribute(LB_INPUT_BUFFER) as? ByteArray)
            ?.let { String(it, Charsets.UTF_8) }
            ?: encodeFormDataIfApplicable(request)

    private fun encodeFormDataIfApplicable(request: Request): String? =
        request
            .takeIf { isFormUrlEncoded(it) }
            ?.parameterMap
            ?.asSequence()
            ?.flatMap { (key, values) -> values.asSequence().map { key to it } }
            ?.joinToString("&") { (key, value) ->
                "${encode(key, Charsets.UTF_8.name())}=${encode(value, Charsets.UTF_8.name())}"
            }
}
