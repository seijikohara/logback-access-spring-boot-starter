package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.spi.AccessContext
import ch.qos.logback.access.common.spi.IAccessEvent.NA
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
import io.mockk.runs
import io.mockk.slot
import org.apache.catalina.connector.Connector
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
import org.apache.coyote.Request as CoyoteRequest
import org.apache.coyote.Response as CoyoteResponse

class TomcatValveSpec :
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
                    // Simulate a malformed/early-rejected request whose getters fail.
                    every { method } throws RuntimeException("malformed request")
                }
            val response = mockk<Response>(relaxed = true)
            val valve = TomcatValve(context)

            shouldNotThrowAny { valve.log(request, response, 0L) }
        }

        test("log emits an access event for an early-rejected request instead of dropping it") {
            // Tomcat invokes AccessLog.log for failed TLS handshakes and unparseable request
            // lines with an unparsed Request whose getters return null (issue #205). The valve
            // must still emit an event; the exception guard alone would silently drop it.
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
            // Mimic the objects Tomcat passes to AccessLog.log for early-rejected requests
            // (AbstractProcessor.logAccess): a connector Request over an empty coyote request.
            // A bare Connector skips lifecycle init, so set parseBodyMethods as a running
            // connector would; a processor always installs an output buffer before logging.
            val request = Request(Connector("HTTP/1.1").apply { parseBodyMethods = "POST" }, CoyoteRequest())
            val response =
                Response(
                    CoyoteResponse().apply {
                        status = 400
                        setOutputBuffer(
                            object : org.apache.coyote.OutputBuffer {
                                override fun doWrite(chunk: java.nio.ByteBuffer): Int = 0

                                override fun getBytesWritten(): Long = 0L
                            },
                        )
                    },
                )

            TomcatValve(context).log(request, response, 0L)

            emitted.captured.serverName shouldBe NA
            emitted.captured.method shouldBe NA
            emitted.captured.protocol shouldBe NA
            emitted.captured.statusCode shouldBe 400
        }
    })
