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
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpMethod
import org.springframework.http.RequestEntity
import org.springframework.test.context.TestPropertySource

/**
 * Tests the error handling scenarios.
 */
@ExtendWith(EventsCaptureExtension::class)
@TestPropertySource(
    properties = [
        "logback.access.config=classpath:logback-access-test.capture.xml",
    ],
)
sealed class ErrorHandlingTest {

    @Test
    fun `Logs 500 error from exception`(
        @Autowired rest: TestRestTemplate,
        capture: EventsCapture,
    ) {
        val request = RequestEntity.get("/mock-controller/exception").build()
        val response = rest.exchange<String>(request)
        response.statusCode.value().shouldBe(500)
        val event = assertLogbackAccessEventsEventually { capture.shouldBeSingleton().single() }
        event.statusCode.shouldBe(500)
        event.method.shouldBe("GET")
        event.requestURI.shouldBe("/mock-controller/exception")
    }

    @Test
    fun `Logs 404 error for non-existent path`(
        @Autowired rest: TestRestTemplate,
        capture: EventsCapture,
    ) {
        val request = RequestEntity.get("/non-existent-path").build()
        val response = rest.exchange<String>(request)
        response.statusCode.value().shouldBe(404)
        val event = assertLogbackAccessEventsEventually { capture.shouldBeSingleton().single() }
        event.statusCode.shouldBe(404)
        event.method.shouldBe("GET")
        event.requestURI.shouldBe("/non-existent-path")
    }

    @Test
    fun `Logs 405 error for method not allowed`(
        @Autowired rest: TestRestTemplate,
        capture: EventsCapture,
    ) {
        val request = RequestEntity.method(HttpMethod.DELETE, "/mock-controller/text").build()
        val response = rest.exchange<String>(request)
        response.statusCode.value().shouldBe(405)
        val event = assertLogbackAccessEventsEventually { capture.shouldBeSingleton().single() }
        event.statusCode.shouldBe(405)
        event.method.shouldBe("DELETE")
        event.requestURI.shouldBe("/mock-controller/text")
    }

    @Test
    fun `Logs 400 error for bad request`(
        @Autowired rest: TestRestTemplate,
        capture: EventsCapture,
    ) {
        val request = RequestEntity.post("/mock-controller/json")
            .header("Content-Type", "application/json")
            .body("{invalid-json}")
        val response = rest.exchange<String>(request)
        response.statusCode.value().shouldBe(400)
        val event = assertLogbackAccessEventsEventually { capture.shouldBeSingleton().single() }
        event.statusCode.shouldBe(400)
        event.method.shouldBe("POST")
        event.requestURI.shouldBe("/mock-controller/json")
    }
}

/**
 * Tests the [ErrorHandlingTest] using the Tomcat servlet web server.
 */
@TomcatServletWebTest
class TomcatServletWebErrorHandlingTest : ErrorHandlingTest()

/**
 * Tests the [ErrorHandlingTest] using the Tomcat reactive web server.
 */
@TomcatReactiveWebTest
class TomcatReactiveWebErrorHandlingTest : ErrorHandlingTest()

/**
 * Tests the [ErrorHandlingTest] using the Jetty servlet web server.
 */
@JettyServletWebTest
class JettyServletWebErrorHandlingTest : ErrorHandlingTest()

/**
 * Tests the [ErrorHandlingTest] using the Jetty reactive web server.
 */
@JettyReactiveWebTest
class JettyReactiveWebErrorHandlingTest : ErrorHandlingTest()

/**
 * Tests the [ErrorHandlingTest] using the Netty reactive web server.
 */
@NettyReactiveWebTest
class NettyReactiveWebErrorHandlingTest : ErrorHandlingTest()
