package io.github.seijikohara.spring.boot.logback.access.jetty

import ch.qos.logback.access.common.spi.AccessContext
import io.github.seijikohara.spring.boot.logback.access.LocalPortStrategy
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessEvent
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response

class JettyRequestLogSpec :
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

        test("log does not propagate exceptions thrown while extracting access-event data") {
            val context = mockk<LogbackAccessContext>(relaxed = true)
            val request =
                mockk<Request>(relaxed = true) {
                    // Simulate a request whose extraction fails mid-flight.
                    every { beginNanoTime } throws RuntimeException("extraction failure")
                }
            val response = mockk<Response>(relaxed = true)

            shouldNotThrowAny { JettyRequestLog(context).log(request, response) }
        }

        test("log emits the captured access event") {
            mockkStatic(Request::class, Response::class) {
                val emitted = slot<LogbackAccessEvent>()
                val context =
                    mockk<LogbackAccessContext> {
                        every { properties } returns properties()
                        every { accessContext } returns
                            mockk<AccessContext>(relaxed = true) {
                                every { sequenceNumberGenerator } returns null
                            }
                        every { emit(capture(emitted)) } just runs
                    }
                val request =
                    mockk<Request>(relaxed = true) {
                        every { beginNanoTime } returns System.nanoTime()
                        every { getSession(false) } returns null
                        every { method } returns "GET"
                    }
                val response = mockk<Response>(relaxed = true) { every { status } returns 200 }
                every { Request.getServerName(request) } returns "localhost"
                every { Request.getServerPort(request) } returns 8080
                every { Request.getRemoteAddr(request) } returns "127.0.0.1"
                every { Request.getCookies(request) } returns emptyList()
                every { Response.getContentBytesWritten(response) } returns 0L

                JettyRequestLog(context).log(request, response)

                emitted.captured.method shouldBe "GET"
                emitted.captured.statusCode shouldBe 200
            }
        }
    })
