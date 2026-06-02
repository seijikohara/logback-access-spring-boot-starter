package io.github.seijikohara.spring.boot.logback.access.jetty

import ch.qos.logback.access.common.spi.AccessContext
import ch.qos.logback.core.spi.SequenceNumberGenerator
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
import io.mockk.mockkStatic
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response

class JettyEventSourceSpec :
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

        // Stubs the static Jetty accessors that createAccessEventData reaches for a minimal request.
        fun stubStatics(
            request: Request,
            response: Response,
            contentBytesWritten: Long = 0L,
        ) {
            every { Request.getServerName(request) } returns "localhost"
            every { Request.getServerPort(request) } returns 8080
            every { Request.getRemoteAddr(request) } returns "127.0.0.1"
            every { Request.getCookies(request) } returns emptyList()
            every { Response.getContentBytesWritten(response) } returns contentBytesWritten
        }

        test("swallows a session lookup exception and records a null session id") {
            mockkStatic(Request::class, Response::class) {
                val request =
                    mockk<Request>(relaxed = true) {
                        every { beginNanoTime } returns System.nanoTime()
                        every { getSession(false) } throws IllegalStateException("session unavailable")
                    }
                val response = mockk<Response>(relaxed = true)
                stubStatics(request, response)

                val data = createAccessEventData(context(), request, response)

                data.sessionID.shouldBeNull()
            }
        }

        test("computes a non-negative elapsed time, null sequence number, and the written content length") {
            mockkStatic(Request::class, Response::class) {
                val request =
                    mockk<Request>(relaxed = true) {
                        every { beginNanoTime } returns System.nanoTime()
                        every { getSession(false) } returns null
                    }
                val response = mockk<Response>(relaxed = true)
                stubStatics(request, response, contentBytesWritten = 123L)

                val data = createAccessEventData(context(generator = null), request, response)

                data.sequenceNumber.shouldBeNull()
                data.contentLength shouldBe 123L
                data.elapsedTime.shouldNotBeNull().shouldBeGreaterThanOrEqual(0L)
            }
        }
    })
