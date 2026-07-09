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
import io.mockk.verify
import org.apache.catalina.AccessLog
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response

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

        test("log tolerates a null request and response from the AccessLog contract") {
            val context = mockk<LogbackAccessContext>(relaxed = true)
            // Call through the Java interface: its parameters are platform types, so Tomcat
            // could hand over nulls that a Kotlin caller cannot express.
            val accessLog: AccessLog = TomcatValve(context)

            shouldNotThrowAny { accessLog.log(null, null, 0L) }
            verify(exactly = 0) { context.emit(any()) }
        }

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

            TomcatValve(context).log(earlyRejectedRequest(), earlyRejectedResponse(), 0L)

            emitted.captured.serverName shouldBe NA
            emitted.captured.method shouldBe NA
            emitted.captured.protocol shouldBe NA
            emitted.captured.statusCode shouldBe 400
        }
    })
