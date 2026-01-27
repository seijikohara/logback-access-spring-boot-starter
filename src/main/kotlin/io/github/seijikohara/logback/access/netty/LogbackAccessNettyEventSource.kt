package io.github.seijikohara.logback.access.netty

import ch.qos.logback.access.common.spi.ServerAdapter
import io.github.seijikohara.logback.access.LogbackAccessContext
import io.github.seijikohara.logback.access.LogbackAccessEventSource
import io.github.seijikohara.logback.access.value.LogbackAccessLocalPortStrategy
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import java.lang.System.currentTimeMillis
import java.lang.Thread.currentThread
import java.net.InetSocketAddress
import java.net.URLEncoder.encode
import java.util.Collections.unmodifiableList
import java.util.Collections.unmodifiableMap
import kotlin.text.Charsets.UTF_8

/**
 * The Logback-access event source for the Netty web server.
 *
 * @property logbackAccessContext The Logback-access context.
 * @property serverHttpRequest The server HTTP request.
 * @property serverHttpResponse The server HTTP response.
 * @property startTime The start time of the request.
 * @property responseContentLength The content length of the response body.
 * @property requestBody The captured request body.
 * @property responseBody The captured response body.
 * @property exchangeAttributes The attributes from the ServerWebExchange.
 */
@Suppress("LongParameterList")
class LogbackAccessNettyEventSource(
    private val logbackAccessContext: LogbackAccessContext,
    private val serverHttpRequest: ServerHttpRequest,
    private val serverHttpResponse: ServerHttpResponse,
    private val startTime: Long,
    private val responseContentLength: Long = 0L,
    private val requestBody: String? = null,
    private val responseBody: String? = null,
    private val exchangeAttributes: Map<String, Any> = emptyMap(),
) : LogbackAccessEventSource() {

    /**
     * Netty does not use Servlet API, so this returns null.
     */
    override val request: HttpServletRequest? = null

    /**
     * Netty does not use Servlet API, so this returns null.
     */
    override val response: HttpServletResponse? = null

    /**
     * Netty does not use ServerAdapter, so this returns null.
     */
    override val serverAdapter: ServerAdapter? = null

    override val timeStamp: Long = currentTimeMillis()

    override val elapsedTime: Long = timeStamp - startTime

    override val sequenceNumber: Long? = logbackAccessContext.raw.sequenceNumberGenerator?.nextSequenceNumber()

    override val threadName: String = currentThread().name

    override val serverName: String? by lazy(LazyThreadSafetyMode.NONE) {
        serverHttpRequest.uri.host
    }

    override val localPort: Int by lazy(LazyThreadSafetyMode.NONE) {
        when (logbackAccessContext.properties.localPortStrategy) {
            LogbackAccessLocalPortStrategy.LOCAL -> serverHttpRequest.localAddress?.port ?: -1

            LogbackAccessLocalPortStrategy.SERVER -> serverHttpRequest.uri.port.takeIf { it > 0 }
                ?: getDefaultPortForScheme(serverHttpRequest.uri.scheme)
        }
    }

    override val remoteAddr: String by lazy(LazyThreadSafetyMode.NONE) {
        serverHttpRequest.remoteAddress?.toResolvedIpAddress() ?: NA
    }

    override val remoteHost: String by lazy(LazyThreadSafetyMode.NONE) {
        serverHttpRequest.remoteAddress?.toResolvedIpAddress() ?: NA
    }

    override val remoteUser: String? by lazy(LazyThreadSafetyMode.NONE) {
        // Remote user is typically set via security filters
        // For Netty, we'll return null as there's no direct way to get it
        null
    }

    override val protocol: String by lazy(LazyThreadSafetyMode.NONE) {
        // HTTP/2 detection strategy:
        // 1. Check for HTTP/2 pseudo-headers (":method", ":path", ":scheme")
        //    Note: These are typically stripped by the server, so this may not work
        // 2. Check for Upgrade header with h2c (HTTP/2 cleartext upgrade)
        //    Note: The initial upgrade request is technically HTTP/1.1, but we report HTTP/2.0
        //    to indicate the connection is being upgraded to HTTP/2.
        // 3. Check for HTTP2-Settings header (h2c prior knowledge)
        //
        // Limitations:
        // - For TLS-based HTTP/2 (h2), protocol detection is not reliable as ALPN
        //   negotiation happens at the TLS layer, which is not exposed to application code.
        // - We default to HTTP/1.1 when we cannot determine the protocol.
        val headers = serverHttpRequest.headers
        val hasHttp2PseudoHeaders = headers.getFirst(":method") != null ||
            headers.getFirst(":path") != null ||
            headers.getFirst(":scheme") != null
        val upgradeHeader = headers.getFirst("Upgrade")
        val http2SettingsHeader = headers.getFirst("HTTP2-Settings")

        when {
            hasHttp2PseudoHeaders -> "HTTP/2.0"
            upgradeHeader?.contains("h2c", ignoreCase = true) == true -> "HTTP/2.0"
            http2SettingsHeader != null -> "HTTP/2.0"
            else -> "HTTP/1.1"
        }
    }

    override val method: String by lazy(LazyThreadSafetyMode.NONE) {
        // HttpMethod.name() returns the method name; handle potential null method
        serverHttpRequest.method?.name() ?: "UNKNOWN"
    }

    override val requestURI: String? by lazy(LazyThreadSafetyMode.NONE) {
        serverHttpRequest.uri.rawPath
    }

    override val queryString: String by lazy(LazyThreadSafetyMode.NONE) {
        serverHttpRequest.uri.rawQuery?.let { "?$it" }.orEmpty()
    }

    override val requestURL: String by lazy(LazyThreadSafetyMode.NONE) {
        "$method $requestURI$queryString $protocol"
    }

    override val requestHeaderMap: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
        val headers = sortedMapOf<String, String>(String.CASE_INSENSITIVE_ORDER)
        headers.putAll(serverHttpRequest.headers.toSingleValueMap())
        unmodifiableMap(headers)
    }

    override val cookieMap: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
        val cookies = linkedMapOf<String, String>()
        serverHttpRequest.cookies.forEach { (name, httpCookies) ->
            httpCookies.firstOrNull()?.let { cookies[name] = it.value }
        }
        unmodifiableMap(cookies)
    }

    override val requestParameterMap: Map<String, List<String>> by lazy(LazyThreadSafetyMode.NONE) {
        val params = linkedMapOf<String, List<String>>()
        serverHttpRequest.queryParams.forEach { (name, values) ->
            params[name] = unmodifiableList(values)
        }
        unmodifiableMap(params)
    }

    override val attributeMap: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
        // Convert exchange attributes to string map for compatibility with IAccessEvent
        val attrs = linkedMapOf<String, String>()
        exchangeAttributes.forEach { (key, value) ->
            attrs[key] = value.toString()
        }
        unmodifiableMap(attrs)
    }

    override val sessionID: String? by lazy(LazyThreadSafetyMode.NONE) {
        // Session handling in WebFlux is different from Servlet
        // Return null as there's no direct session ID access
        null
    }

    override val requestContent: String? by lazy(LazyThreadSafetyMode.NONE) {
        requestBody
    }

    override val statusCode: Int by lazy(LazyThreadSafetyMode.NONE) {
        serverHttpResponse.statusCode?.value() ?: DEFAULT_STATUS_CODE
    }

    override val responseHeaderMap: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
        val headers = sortedMapOf<String, String>(String.CASE_INSENSITIVE_ORDER)
        headers.putAll(serverHttpResponse.headers.toSingleValueMap())
        unmodifiableMap(headers)
    }

    override val contentLength: Long by lazy(LazyThreadSafetyMode.NONE) {
        // Use tracked bytes if available, otherwise fall back to Content-Length header
        responseContentLength.takeIf { it > 0 }
            ?: serverHttpResponse.headers.contentLength.takeIf { it >= 0 }
            ?: 0L
    }

    override val responseContent: String? by lazy(LazyThreadSafetyMode.NONE) {
        responseBody
    }

    /**
     * Resolves the IP address from an InetSocketAddress.
     * If the address is already resolved, returns the host address.
     * If not resolved, resolves the hostname to get the IP address.
     */
    private fun InetSocketAddress.toResolvedIpAddress(): String {
        // If address is already resolved, return the IP address
        address?.let { return it.hostAddress }
        // If not resolved, try to resolve the hostname
        return try {
            java.net.InetAddress.getByName(hostString).hostAddress
        } catch (_: Exception) {
            hostString
        }
    }

    /**
     * Returns the default port number for the given URI scheme.
     *
     * @param scheme The URI scheme (e.g., "http", "https").
     * @return The default port number for the scheme.
     */
    private fun getDefaultPortForScheme(scheme: String?): Int = when (scheme?.lowercase()) {
        "https" -> DEFAULT_HTTPS_PORT
        else -> DEFAULT_HTTP_PORT
    }

    companion object {
        private const val NA = "-"
        private const val DEFAULT_HTTP_PORT = 80
        private const val DEFAULT_HTTPS_PORT = 443
        private const val DEFAULT_STATUS_CODE = 200
    }
}
