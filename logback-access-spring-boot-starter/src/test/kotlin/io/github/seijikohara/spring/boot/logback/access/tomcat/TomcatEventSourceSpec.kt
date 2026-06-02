package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.spi.AccessContext
import ch.qos.logback.core.spi.SequenceNumberGenerator
import io.github.seijikohara.spring.boot.logback.access.AccessEventData
import io.github.seijikohara.spring.boot.logback.access.LocalPortStrategy
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.longs.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response

class TomcatEventSourceSpec :
    FunSpec({
        fun properties(): LogbackAccessProperties =
            LogbackAccessProperties(
                enabled = true,
                configLocation = null,
                localPortStrategy = LocalPortStrategy.SERVER,
                tomcat = LogbackAccessProperties.TomcatProperties(requestAttributesEnabled = null),
                teeFilter = LogbackAccessProperties.TeeFilterProperties(false, null, null, 65536L, null),
                filter = LogbackAccessProperties.FilterProperties(null, null),
            )

        fun context(generator: SequenceNumberGenerator? = null): LogbackAccessContext =
            mockk {
                every { properties } returns properties()
                every { accessContext } returns
                    mockk<AccessContext>(relaxed = true) {
                        every { sequenceNumberGenerator } returns generator
                    }
            }

        fun request(): Request =
            mockk(relaxed = true) {
                every { method } returns "GET"
                every { requestURI } returns "/api/users"
                every { queryString } returns null
            }

        fun response(): Response = mockk(relaxed = true) { every { status } returns 200 }

        fun event(
            elapsedTimeNanos: Long,
            context: LogbackAccessContext = context(),
            request: Request = request(),
        ): AccessEventData =
            createAccessEventData(context, request, response(), requestAttributesEnabled = false, elapsedTimeNanos = elapsedTimeNanos)

        test("converts a non-negative elapsedTimeNanos to milliseconds") {
            val data = event(elapsedTimeNanos = 5_000_000L)

            data.elapsedTime shouldBe 5L
            data.statusCode shouldBe 200
        }

        test("falls back to a non-negative elapsed time when elapsedTimeNanos is negative") {
            val request = request()
            every { request.coyoteRequest.startTime } returns System.currentTimeMillis()

            val data = event(elapsedTimeNanos = -1L, request = request)

            data.elapsedTime.shouldNotBeNull().shouldBeGreaterThanOrEqual(0L)
        }

        test("produces a null sequence number when no generator is configured") {
            val data = event(elapsedTimeNanos = 0L, context = context(generator = null))

            data.sequenceNumber.shouldBeNull()
        }

        test("uses the configured sequence number generator") {
            val generator = mockk<SequenceNumberGenerator> { every { nextSequenceNumber() } returns 42L }

            val data = event(elapsedTimeNanos = 0L, context = context(generator = generator))

            data.sequenceNumber shouldBe 42L
        }
    })
