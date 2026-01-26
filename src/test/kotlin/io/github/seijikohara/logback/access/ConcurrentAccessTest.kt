package io.github.seijikohara.logback.access

import io.github.seijikohara.logback.access.test.assertion.Assertions.assertLogbackAccessEventsEventually
import io.github.seijikohara.logback.access.test.extension.EventsCapture
import io.github.seijikohara.logback.access.test.extension.EventsCaptureExtension
import io.github.seijikohara.logback.access.test.type.JettyReactiveWebTest
import io.github.seijikohara.logback.access.test.type.JettyServletWebTest
import io.github.seijikohara.logback.access.test.type.NettyReactiveWebTest
import io.github.seijikohara.logback.access.test.type.TomcatReactiveWebTest
import io.github.seijikohara.logback.access.test.type.TomcatServletWebTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.RequestEntity
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Tests the concurrent access handling.
 */
@ExtendWith(EventsCaptureExtension::class)
@TestPropertySource(
    properties = [
        "logback.access.config=classpath:logback-access-test.capture.xml",
    ],
)
sealed class ConcurrentAccessTest {

    @Test
    fun `Captures all events from concurrent requests`(
        @Autowired rest: TestRestTemplate,
        capture: EventsCapture,
    ) {
        val requestCount = 50
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(requestCount)

        repeat(requestCount) { index ->
            executor.submit {
                try {
                    val request = RequestEntity.get("/mock-controller/text?index=$index").build()
                    val response = rest.exchange<String>(request)
                    response.statusCode.value().shouldBe(200)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        val events = assertLogbackAccessEventsEventually { capture.shouldHaveSize(requestCount) }
        events.size.shouldBe(requestCount)

        // Verify all events have valid status codes
        events.forEach { event ->
            event.statusCode.shouldBe(200)
        }
    }

    @Test
    fun `Maintains thread safety for concurrent requests`(
        @Autowired rest: TestRestTemplate,
        capture: EventsCapture,
    ) {
        val requestCount = 30
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(requestCount)

        repeat(requestCount) { index ->
            executor.submit {
                try {
                    val request = RequestEntity.get("/mock-controller/text?seq=$index").build()
                    rest.exchange<String>(request)
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()

        val events = assertLogbackAccessEventsEventually { capture.shouldHaveSize(requestCount) }

        // Verify all events have valid data
        events.forEach { event ->
            event.statusCode.shouldBe(200)
            event.method.shouldBe("GET")
            event.requestURI.shouldBe("/mock-controller/text")
        }

        // Verify all thread names are captured (thread safety check)
        val threadNames = events.map { it.threadName }
        threadNames.forEach { it.length.shouldBeGreaterThan(0) }
    }
}

/**
 * Tests the [ConcurrentAccessTest] using the Tomcat servlet web server.
 */
@TomcatServletWebTest
class TomcatServletWebConcurrentAccessTest : ConcurrentAccessTest()

/**
 * Tests the [ConcurrentAccessTest] using the Tomcat reactive web server.
 */
@TomcatReactiveWebTest
class TomcatReactiveWebConcurrentAccessTest : ConcurrentAccessTest()

/**
 * Tests the [ConcurrentAccessTest] using the Jetty servlet web server.
 */
@JettyServletWebTest
class JettyServletWebConcurrentAccessTest : ConcurrentAccessTest()

/**
 * Tests the [ConcurrentAccessTest] using the Jetty reactive web server.
 */
@JettyReactiveWebTest
class JettyReactiveWebConcurrentAccessTest : ConcurrentAccessTest()

/**
 * Tests the [ConcurrentAccessTest] using the Netty reactive web server.
 */
@NettyReactiveWebTest
class NettyReactiveWebConcurrentAccessTest : ConcurrentAccessTest()
