package io.github.seijikohara.logback.access.netty

import ch.qos.logback.access.common.spi.AccessContext
import io.github.seijikohara.logback.access.LogbackAccessContext
import io.github.seijikohara.logback.access.LogbackAccessProperties
import io.github.seijikohara.logback.access.value.LogbackAccessLocalPortStrategy
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpCookie
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.util.LinkedMultiValueMap
import java.net.InetSocketAddress
import java.net.URI

/**
 * Tests for [LogbackAccessNettyEventSource].
 */
class LogbackAccessNettyEventSourceTest {

    private lateinit var logbackAccessContext: LogbackAccessContext
    private lateinit var logbackAccessProperties: LogbackAccessProperties
    private lateinit var accessContext: AccessContext
    private lateinit var serverHttpRequest: ServerHttpRequest
    private lateinit var serverHttpResponse: ServerHttpResponse
    private lateinit var httpHeaders: HttpHeaders

    @BeforeEach
    fun setUp() {
        logbackAccessProperties = mockk()
        accessContext = mockk()
        every { accessContext.sequenceNumberGenerator } returns null

        logbackAccessContext = mockk()
        every { logbackAccessContext.properties } returns logbackAccessProperties
        every { logbackAccessContext.raw } returns accessContext

        httpHeaders = HttpHeaders()

        serverHttpRequest = mockk()
        every { serverHttpRequest.uri } returns URI.create("http://localhost:8080/test?param=value")
        every { serverHttpRequest.method } returns HttpMethod.GET
        every { serverHttpRequest.headers } returns httpHeaders
        every { serverHttpRequest.cookies } returns LinkedMultiValueMap()
        every { serverHttpRequest.queryParams } returns LinkedMultiValueMap()
        every { serverHttpRequest.remoteAddress } returns InetSocketAddress.createUnresolved("127.0.0.1", 12345)
        every { serverHttpRequest.localAddress } returns InetSocketAddress("localhost", 8080)

        serverHttpResponse = mockk()
        every { serverHttpResponse.statusCode } returns HttpStatusCode.valueOf(200)
        every { serverHttpResponse.headers } returns HttpHeaders()
    }

    // Protocol Detection Tests

    @Test
    fun `Returns HTTP 1_1 for standard request`() {
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.protocol shouldBe "HTTP/1.1"
    }

    @Test
    fun `Returns HTTP 2_0 for pseudo-header method`() {
        httpHeaders.add(":method", "GET")
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.protocol shouldBe "HTTP/2.0"
    }

    @Test
    fun `Returns HTTP 2_0 for pseudo-header path`() {
        httpHeaders.add(":path", "/test")
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.protocol shouldBe "HTTP/2.0"
    }

    @Test
    fun `Returns HTTP 2_0 for pseudo-header scheme`() {
        httpHeaders.add(":scheme", "https")
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.protocol shouldBe "HTTP/2.0"
    }

    @Test
    fun `Returns HTTP 2_0 for Upgrade h2c header`() {
        httpHeaders.add("Upgrade", "h2c")
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.protocol shouldBe "HTTP/2.0"
    }

    @Test
    fun `Returns HTTP 2_0 for HTTP2-Settings header`() {
        httpHeaders.add("HTTP2-Settings", "AAEAAEAAAAIAAAABAAMAAABkAAQBAAAAAAUAAEAA")
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.protocol shouldBe "HTTP/2.0"
    }

    // Local Port Tests

    @Test
    fun `Returns local port with LOCAL strategy`() {
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.LOCAL

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.localPort shouldBe 8080
    }

    @Test
    fun `Returns server port with SERVER strategy`() {
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.localPort shouldBe 8080
    }

    @Test
    fun `Returns default HTTPS port for https scheme with no port`() {
        every { serverHttpRequest.uri } returns URI.create("https://localhost/test")
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.localPort shouldBe 443
    }

    @Test
    fun `Returns default HTTP port for http scheme with no port`() {
        every { serverHttpRequest.uri } returns URI.create("http://localhost/test")
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.localPort shouldBe 80
    }

    @Test
    fun `Returns -1 when local address is null with LOCAL strategy`() {
        every { serverHttpRequest.localAddress } returns null
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.LOCAL

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.localPort shouldBe -1
    }

    // Method Tests

    @Test
    fun `Returns method name`() {
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.method shouldBe "GET"
    }

    // Attribute Map Tests

    @Test
    fun `Returns empty map when no attributes`() {
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.attributeMap shouldBe emptyMap()
    }

    @Test
    fun `Returns converted attributes from exchange`() {
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val attributes = mapOf(
            "traceId" to "abc123",
            "spanId" to "def456",
            "customInt" to 42,
        )

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
            exchangeAttributes = attributes,
        )

        eventSource.attributeMap shouldBe mapOf(
            "traceId" to "abc123",
            "spanId" to "def456",
            "customInt" to "42",
        )
    }

    // Remote Address Tests

    @Test
    fun `Returns remote address`() {
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.remoteAddr shouldNotBe "-"
    }

    @Test
    fun `Returns NA when remote address is null`() {
        every { serverHttpRequest.remoteAddress } returns null
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.remoteAddr shouldBe "-"
        eventSource.remoteHost shouldBe "-"
    }

    // Content Length Tests

    @Test
    fun `Returns tracked bytes when available`() {
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
            responseContentLength = 1024,
        )

        eventSource.contentLength shouldBe 1024
    }

    @Test
    fun `Returns header content length when tracked bytes is zero`() {
        val responseHeaders = HttpHeaders()
        responseHeaders.contentLength = 512
        every { serverHttpResponse.headers } returns responseHeaders
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
            responseContentLength = 0,
        )

        eventSource.contentLength shouldBe 512
    }

    // Cookies Tests

    @Test
    fun `Returns cookies from request`() {
        val cookies = LinkedMultiValueMap<String, HttpCookie>()
        cookies.add("session", HttpCookie("session", "abc123"))
        cookies.add("token", HttpCookie("token", "xyz789"))
        every { serverHttpRequest.cookies } returns cookies
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.cookieMap shouldBe mapOf(
            "session" to "abc123",
            "token" to "xyz789",
        )
    }

    // Query Params Tests

    @Test
    fun `Returns query parameters`() {
        val params = LinkedMultiValueMap<String, String>()
        params.add("key1", "value1")
        params.add("key2", "value2")
        every { serverHttpRequest.queryParams } returns params
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
        )

        eventSource.requestParameterMap shouldBe mapOf(
            "key1" to listOf("value1"),
            "key2" to listOf("value2"),
        )
    }

    // Body Content Tests

    @Test
    fun `Returns request body when provided`() {
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
            requestBody = "request content",
        )

        eventSource.requestContent shouldBe "request content"
    }

    @Test
    fun `Returns response body when provided`() {
        every { logbackAccessProperties.localPortStrategy } returns LogbackAccessLocalPortStrategy.SERVER

        val eventSource = LogbackAccessNettyEventSource(
            logbackAccessContext = logbackAccessContext,
            serverHttpRequest = serverHttpRequest,
            serverHttpResponse = serverHttpResponse,
            startTime = System.currentTimeMillis(),
            responseBody = "response content",
        )

        eventSource.responseContent shouldBe "response content"
    }
}
