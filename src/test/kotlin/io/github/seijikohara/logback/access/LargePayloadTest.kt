package io.github.seijikohara.logback.access

import io.github.seijikohara.logback.access.test.assertion.Assertions.assertLogbackAccessEventsEventually
import io.github.seijikohara.logback.access.test.extension.EventsCapture
import io.github.seijikohara.logback.access.test.extension.EventsCaptureExtension
import io.github.seijikohara.logback.access.test.type.JettyReactiveWebTest
import io.github.seijikohara.logback.access.test.type.JettyServletWebTest
import io.github.seijikohara.logback.access.test.type.NettyReactiveWebTest
import io.github.seijikohara.logback.access.test.type.TomcatReactiveWebTest
import io.github.seijikohara.logback.access.test.type.TomcatServletWebTest
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.RequestEntity
import org.springframework.test.context.TestPropertySource

/**
 * Tests the large payload handling.
 */
@ExtendWith(EventsCaptureExtension::class)
@TestPropertySource(
    properties = [
        "logback.access.config=classpath:logback-access-test.capture.xml",
    ],
)
sealed class LargePayloadTest {

    @Test
    fun `Logs event for 1MB response`(
        @Autowired rest: TestRestTemplate,
        capture: EventsCapture,
    ) {
        val request = RequestEntity.get("/mock-controller/large-response").build()
        val response = rest.exchange<String>(request)
        response.statusCode.value().shouldBe(200)
        response.body?.length.shouldBe(1_000_000)
        val event = assertLogbackAccessEventsEventually { capture.shouldBeSingleton().single() }
        event.statusCode.shouldBe(200)
        event.method.shouldBe("GET")
        event.requestURI.shouldBe("/mock-controller/large-response")
        event.contentLength.shouldBeGreaterThan(0)
    }

    @Test
    fun `Logs event for 100KB request`(
        @Autowired rest: TestRestTemplate,
        capture: EventsCapture,
    ) {
        val largeBody = "x".repeat(100_000)
        val request = RequestEntity.post("/mock-controller/large-request")
            .header("Content-Type", "text/plain")
            .body(largeBody)
        val response = rest.exchange<String>(request)
        response.statusCode.value().shouldBe(200)
        response.body.shouldContain("received: 100000 bytes")
        val event = assertLogbackAccessEventsEventually { capture.shouldBeSingleton().single() }
        event.statusCode.shouldBe(200)
        event.method.shouldBe("POST")
        event.requestURI.shouldBe("/mock-controller/large-request")
    }
}

/**
 * Tests the [LargePayloadTest] using the Tomcat servlet web server.
 */
@TomcatServletWebTest
class TomcatServletWebLargePayloadTest : LargePayloadTest()

/**
 * Tests the [LargePayloadTest] using the Tomcat reactive web server.
 */
@TomcatReactiveWebTest
class TomcatReactiveWebLargePayloadTest : LargePayloadTest()

/**
 * Tests the [LargePayloadTest] using the Jetty servlet web server.
 */
@JettyServletWebTest
class JettyServletWebLargePayloadTest : LargePayloadTest()

/**
 * Tests the [LargePayloadTest] using the Jetty reactive web server.
 */
@JettyReactiveWebTest
class JettyReactiveWebLargePayloadTest : LargePayloadTest()

/**
 * Tests the [LargePayloadTest] using the Netty reactive web server.
 */
@NettyReactiveWebTest
class NettyReactiveWebLargePayloadTest : LargePayloadTest()
