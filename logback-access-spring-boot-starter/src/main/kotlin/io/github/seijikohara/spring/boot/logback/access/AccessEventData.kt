package io.github.seijikohara.spring.boot.logback.access

import java.io.Serializable

/** Request attribute key for the remote user set by the security filter. */
internal const val REMOTE_USER_ATTR: String =
    "io.github.seijikohara.spring.boot.logback.access.remoteUser"

/**
 * Immutable snapshot of all access event data.
 *
 * Captures all values eagerly at construction time, making it safe for
 * deferred processing and serialization without holding references to
 * server-specific request/response objects.
 */
data class AccessEventData(
    /** Timestamp when the request was received (epoch milliseconds). */
    val timeStamp: Long,
    /** Time elapsed processing the request in milliseconds, or null if unavailable. */
    val elapsedTime: Long?,
    /** Sequence number for ordering events, or null if not configured. */
    val sequenceNumber: Long?,
    /** Name of the thread that processed the request. */
    val threadName: String,
    /** Server name from the Host header or server configuration. */
    val serverName: String?,
    /** Local port on which the request was received. */
    val localPort: Int,
    /** IP address of the remote client. */
    val remoteAddr: String,
    /** Hostname of the remote client (may equal remoteAddr if DNS lookup is disabled). */
    val remoteHost: String,
    /** Authenticated username, or null if not authenticated. */
    val remoteUser: String?,
    /** Protocol and version (e.g., "HTTP/1.1"). */
    val protocol: String,
    /** HTTP method (e.g., "GET", "POST"). */
    val method: String,
    /** Request URI path without query string. */
    val requestURI: String?,
    /** Query string with leading "?" or empty string if none. */
    val queryString: String,
    /** Full request line (method + URI + query + protocol). */
    val requestURL: String,
    /** Request headers as a case-insensitive map. */
    val requestHeaderMap: Map<String, String>,
    /** Cookies from the request. */
    val cookieMap: Map<String, String>,
    /** Request parameters (query string and/or form data). */
    val requestParameterMap: Map<String, List<String>>,
    /** Request attributes set by filters or valves. */
    val attributeMap: Map<String, String>,
    /** Session ID if a session exists, or null. */
    val sessionID: String?,
    /** Request body content if captured by TeeFilter, or null. */
    val requestContent: String?,
    /** HTTP response status code. */
    val statusCode: Int,
    /** Response headers as a case-insensitive map. */
    val responseHeaderMap: Map<String, String>,
    /** Number of bytes written in the response body. */
    val contentLength: Long,
    /** Response body content if captured by TeeFilter, or null. */
    val responseContent: String?,
) : Serializable {
    /**
     * Lazily computed array-backed parameter map for [ch.qos.logback.access.common.spi.IAccessEvent] compatibility.
     * Excluded from serialization to avoid redundant data.
     */
    @delegate:Transient
    val requestParameterArrayMap: Map<String, Array<String>> by lazy {
        requestParameterMap.mapValues { (_, values) -> values.toTypedArray() }
    }

    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
