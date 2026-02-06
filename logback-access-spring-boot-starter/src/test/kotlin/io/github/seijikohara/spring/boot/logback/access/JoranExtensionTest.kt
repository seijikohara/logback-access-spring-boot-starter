package io.github.seijikohara.spring.boot.logback.access

import ch.qos.logback.access.common.spi.IAccessEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

class JoranExtensionTest {
    @Nested
    @SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = [
            "logback.access.config-location=classpath:logback-access-spring-profile-test.xml",
        ],
    )
    @ActiveProfiles("testprofile")
    inner class SpringProfileActiveTest {
        @Autowired
        lateinit var logbackAccessContext: LogbackAccessContext

        @LocalServerPort
        var port: Int = 0

        @Test
        fun `appender is registered when profile is active`() {
            val appender = logbackAccessContext.accessContext.getAppender("list")
            appender.shouldNotBeNull()
        }

        @Test
        fun `events are captured when profile is active`() {
            val appender = logbackAccessContext.accessContext.getAppender("list")
            appender.shouldNotBeNull()
            @Suppress("UNCHECKED_CAST")
            val listAppender = appender as ListAppender<IAccessEvent>
            listAppender.list.clear()

            val client = HttpClient.newHttpClient()
            val request = HttpRequest.newBuilder(URI.create("http://localhost:$port/test")).build()
            client.send(request, BodyHandlers.ofString())

            val deadline = System.currentTimeMillis() + AWAIT_TIMEOUT_MS
            while (System.currentTimeMillis() < deadline && listAppender.list.isEmpty()) {
                Thread.sleep(POLL_INTERVAL_MS)
            }
            listAppender.list.shouldNotBeEmpty()
        }
    }

    @Nested
    @SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = [
            "logback.access.config-location=classpath:logback-access-spring-profile-test.xml",
        ],
    )
    @ActiveProfiles("other")
    inner class SpringProfileInactiveTest {
        @Autowired
        lateinit var logbackAccessContext: LogbackAccessContext

        @Test
        fun `appender is not registered when profile is inactive`() {
            val appender = logbackAccessContext.accessContext.getAppender("list")
            appender.shouldBeNull()
        }
    }

    @Nested
    @SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = [
            "logback.access.config-location=classpath:logback-access-spring-property-test.xml",
            "spring.application.name=test-app",
        ],
    )
    inner class SpringPropertyTest {
        @Autowired
        lateinit var logbackAccessContext: LogbackAccessContext

        @Test
        fun `spring property is resolved from environment`() {
            val value = logbackAccessContext.accessContext.getProperty("appName")
            value shouldBe "test-app"
        }
    }

    @Nested
    @SpringBootTest(
        webEnvironment = RANDOM_PORT,
        properties = [
            "logback.access.config-location=classpath:logback-access-spring-property-test.xml",
        ],
    )
    inner class SpringPropertyDefaultValueTest {
        @Autowired
        lateinit var logbackAccessContext: LogbackAccessContext

        @Test
        fun `spring property falls back to default value`() {
            val value = logbackAccessContext.accessContext.getProperty("appName")
            value shouldBe "default-app"
        }
    }

    companion object {
        private const val AWAIT_TIMEOUT_MS = 5000L
        private const val POLL_INTERVAL_MS = 50L
    }
}
