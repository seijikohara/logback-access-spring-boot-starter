package io.github.seijikohara.spring.boot.logback.access

import ch.qos.logback.access.common.spi.IAccessEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

@SpringBootTest(webEnvironment = RANDOM_PORT)
class TomcatServletTest {
    @Autowired
    lateinit var logbackAccessContext: LogbackAccessContext

    @LocalServerPort
    var port: Int = 0

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    private lateinit var listAppender: ListAppender<IAccessEvent>

    @BeforeEach
    fun setUp() {
        @Suppress("UNCHECKED_CAST")
        listAppender = logbackAccessContext.accessContext.getAppender("list") as ListAppender<IAccessEvent>
        listAppender.list.clear()
    }

    private fun get(path: String): Int {
        val request = HttpRequest.newBuilder(URI.create("http://localhost:$port$path")).build()
        return httpClient.send(request, BodyHandlers.ofString()).statusCode()
    }

    private fun awaitEvents(count: Int = 1): List<IAccessEvent> {
        val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            if (listAppender.list.size >= count) return listAppender.list.toList()
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return listAppender.list.toList()
    }

    @Test
    fun `GET request emits an access event with correct data`() {
        val statusCode = get("/test")
        statusCode shouldBe 200

        val events = awaitEvents()
        events.shouldNotBeEmpty()
        val event = events.first()
        event.method shouldBe "GET"
        event.requestURI shouldBe "/test"
        event.statusCode shouldBe 200
        event.protocol shouldContain "HTTP"
    }

    @Test
    fun `multiple requests emit multiple events`() {
        get("/test")
        get("/hello")

        val events = awaitEvents(count = 2)
        events shouldHaveSize 2
        events.map { it.requestURI } shouldBe listOf("/test", "/hello")
    }

    @Test
    fun `event captures request headers`() {
        get("/test")

        val events = awaitEvents()
        events.shouldNotBeEmpty()
        val event = events.first()
        event.requestHeaderMap.keys.map { it.lowercase() } shouldContain "host"
    }

    @Test
    fun `event captures response content length`() {
        get("/test")

        val events = awaitEvents()
        events.shouldNotBeEmpty()
        val event = events.first()
        event.contentLength shouldBe "OK".toByteArray().size.toLong()
    }

    @Test
    fun `404 response produces event with 404 status`() {
        get("/nonexistent")

        val events = awaitEvents()
        events.shouldNotBeEmpty()
        val event = events.first()
        event.statusCode shouldBe 404
    }

    @Test
    fun `event captures query string`() {
        get("/test?foo=bar")

        val events = awaitEvents()
        events.shouldNotBeEmpty()
        val event = events.first()
        event.queryString shouldBe "?foo=bar"
    }

    @Test
    fun `event requestURL contains method and URI`() {
        get("/test")

        val events = awaitEvents()
        events.shouldNotBeEmpty()
        val event = events.first()
        event.requestURL shouldContain "GET"
        event.requestURL shouldContain "/test"
    }

    companion object {
        private const val AWAIT_TIMEOUT_MS = 5000L
        private const val POLL_INTERVAL_MS = 50L
    }
}
