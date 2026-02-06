package io.github.seijikohara.spring.boot.logback.access

import ch.qos.logback.access.common.spi.IAccessEvent
import ch.qos.logback.access.common.spi.IAccessEvent.NA
import ch.qos.logback.access.common.spi.IAccessEvent.SENTINEL
import ch.qos.logback.access.common.spi.ServerAdapter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.io.Serializable
import java.util.Collections.enumeration
import java.util.Enumeration
import java.util.concurrent.TimeUnit.MILLISECONDS

/**
 * [IAccessEvent] implementation backed by an immutable [AccessEventData] snapshot.
 *
 * Since all data is captured eagerly in [AccessEventData],
 * [prepareForDeferredProcessing] is a no-op and serialization works naturally.
 */
class LogbackAccessEvent
    @JvmOverloads
    constructor(
        private val data: AccessEventData,
        @Transient private val httpRequest: HttpServletRequest? = null,
        @Transient private val httpResponse: HttpServletResponse? = null,
    ) : IAccessEvent,
        Serializable {
        override fun getRequest(): HttpServletRequest? = httpRequest

        override fun getResponse(): HttpServletResponse? = httpResponse

        override fun getServerAdapter(): ServerAdapter? = null

        override fun getTimeStamp(): Long = data.timeStamp

        override fun getElapsedTime(): Long = data.elapsedTime ?: SENTINEL.toLong()

        override fun getElapsedSeconds(): Long = data.elapsedTime?.let { MILLISECONDS.toSeconds(it) } ?: SENTINEL.toLong()

        override fun getSequenceNumber(): Long = data.sequenceNumber ?: SENTINEL.toLong()

        override fun getThreadName(): String = data.threadName

        override fun setThreadName(value: String): Unit =
            throw UnsupportedOperationException(
                "LogbackAccessEvent is immutable",
            )

        override fun getServerName(): String = data.serverName ?: NA

        override fun getLocalPort(): Int = data.localPort

        override fun getRemoteAddr(): String = data.remoteAddr

        override fun getRemoteHost(): String = data.remoteHost

        override fun getRemoteUser(): String = data.remoteUser ?: NA

        override fun getProtocol(): String = data.protocol

        override fun getMethod(): String = data.method

        override fun getRequestURI(): String = data.requestURI ?: NA

        override fun getQueryString(): String = data.queryString

        override fun getRequestURL(): String = data.requestURL

        override fun getRequestHeaderMap(): Map<String, String> = data.requestHeaderMap

        override fun getRequestHeaderNames(): Enumeration<String> = enumeration(data.requestHeaderMap.keys)

        override fun getRequestHeader(key: String): String = data.requestHeaderMap[key] ?: NA

        override fun getCookie(key: String): String = data.cookieMap[key] ?: NA

        override fun getRequestParameterMap(): Map<String, Array<String>> = data.requestParameterArrayMap

        override fun getRequestParameter(key: String): Array<String> = data.requestParameterArrayMap[key] ?: EMPTY_PARAM_ARRAY

        override fun getAttribute(key: String): String = data.attributeMap[key] ?: NA

        override fun getSessionID(): String = data.sessionID ?: NA

        override fun getRequestContent(): String = data.requestContent.orEmpty()

        override fun getStatusCode(): Int = data.statusCode

        override fun getResponseHeaderMap(): Map<String, String> = data.responseHeaderMap

        override fun getResponseHeaderNameList(): List<String> = data.responseHeaderMap.keys.toList()

        override fun getResponseHeader(key: String): String = data.responseHeaderMap[key] ?: NA

        override fun getContentLength(): Long = data.contentLength

        override fun getResponseContent(): String = data.responseContent.orEmpty()

        override fun prepareForDeferredProcessing(): Unit =
            Unit // No-op: AccessEventData is already an eagerly-evaluated immutable snapshot.

        override fun toString(): String = "${this::class.simpleName}(${data.requestURL} ${data.statusCode})"

        companion object {
            private const val serialVersionUID: Long = 1L

            /** Reusable empty parameter array to avoid repeated allocation. */
            private val EMPTY_PARAM_ARRAY: Array<String> = arrayOf(NA)
        }
    }
