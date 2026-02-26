package io.github.seijikohara.spring.boot.logback.access

import ch.qos.logback.access.common.spi.IAccessEvent.NA
import ch.qos.logback.access.common.spi.IAccessEvent.SENTINEL
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.concurrent.TimeUnit.MILLISECONDS

class LogbackAccessEventSpec :
    FunSpec({
        test("delegates to AccessEventData for all properties") {
            val data = TestAccessEventDataFactory.createTestData()
            val event = LogbackAccessEvent(data)

            assertSoftly {
                event.timeStamp shouldBe 1000L
                event.elapsedTime shouldBe 50L
                event.elapsedSeconds shouldBe MILLISECONDS.toSeconds(50L)
                event.sequenceNumber shouldBe 1L
                event.threadName shouldBe "main"
                event.serverName shouldBe "localhost"
                event.localPort shouldBe 8080
                event.remoteAddr shouldBe "127.0.0.1"
                event.remoteHost shouldBe "localhost"
                event.remoteUser shouldBe "testuser"
                event.protocol shouldBe "HTTP/1.1"
                event.method shouldBe "GET"
                event.requestURI shouldBe "/test"
                event.queryString shouldBe "?foo=bar"
                event.requestURL shouldBe "GET /test?foo=bar HTTP/1.1"
                event.statusCode shouldBe 200
                event.contentLength shouldBe 13L
                event.requestContent shouldBe "request body"
                event.responseContent shouldBe "response body"
            }
        }

        test("returns NA for null string fields") {
            val data = TestAccessEventDataFactory.createMinimalData()
            val event = LogbackAccessEvent(data)

            assertSoftly {
                event.serverName shouldBe NA
                event.remoteUser shouldBe NA
                event.requestURI shouldBe NA
                event.sessionID shouldBe NA
            }
        }

        test("returns SENTINEL for null numeric fields") {
            val data = TestAccessEventDataFactory.createMinimalData()
            val event = LogbackAccessEvent(data)

            assertSoftly {
                event.elapsedTime shouldBe SENTINEL.toLong()
                event.elapsedSeconds shouldBe SENTINEL.toLong()
                event.sequenceNumber shouldBe SENTINEL.toLong()
            }
        }

        test("returns empty string for null content fields") {
            val data = TestAccessEventDataFactory.createMinimalData()
            val event = LogbackAccessEvent(data)

            assertSoftly {
                event.requestContent shouldBe ""
                event.responseContent shouldBe ""
            }
        }

        test("getRequestHeader returns NA for missing header") {
            val event = LogbackAccessEvent(TestAccessEventDataFactory.createMinimalData())

            event.getRequestHeader("X-Custom") shouldBe NA
        }

        test("getRequestHeader returns value for existing header") {
            val data = TestAccessEventDataFactory.createTestData()
            val event = LogbackAccessEvent(data)

            event.getRequestHeader("Host") shouldBe "localhost"
        }

        test("getResponseHeader returns NA for missing header") {
            val event = LogbackAccessEvent(TestAccessEventDataFactory.createMinimalData())

            event.getResponseHeader("X-Custom") shouldBe NA
        }

        test("getResponseHeader returns value for existing header") {
            val data = TestAccessEventDataFactory.createTestData()
            val event = LogbackAccessEvent(data)

            event.getResponseHeader("Content-Type") shouldBe "text/plain"
        }

        test("getCookie returns NA for missing cookie") {
            val event = LogbackAccessEvent(TestAccessEventDataFactory.createMinimalData())

            event.getCookie("missing") shouldBe NA
        }

        test("getCookie returns value for existing cookie") {
            val data = TestAccessEventDataFactory.createTestData()
            val event = LogbackAccessEvent(data)

            event.getCookie("session") shouldBe "abc123"
        }

        test("getAttribute returns NA for missing attribute") {
            val event = LogbackAccessEvent(TestAccessEventDataFactory.createMinimalData())

            event.getAttribute("missing") shouldBe NA
        }

        test("getRequestParameter returns NA array for missing parameter") {
            val event = LogbackAccessEvent(TestAccessEventDataFactory.createMinimalData())

            event.getRequestParameter("missing") shouldBe arrayOf(NA)
        }

        test("getRequestParameter returns independent arrays on each call") {
            val event = LogbackAccessEvent(TestAccessEventDataFactory.createMinimalData())

            val first = event.getRequestParameter("missing")
            val second = event.getRequestParameter("missing")

            first shouldBe arrayOf(NA)
            (first !== second) shouldBe true // different instances, not shared
        }

        test("getRequestHeaderNames returns enumeration of header keys") {
            val data = TestAccessEventDataFactory.createTestData()
            val event = LogbackAccessEvent(data)

            val names = event.requestHeaderNames.toList()
            names shouldContain "Host"
        }

        test("getResponseHeaderNameList returns list of header keys") {
            val data = TestAccessEventDataFactory.createTestData()
            val event = LogbackAccessEvent(data)

            event.responseHeaderNameList shouldContain "Content-Type"
        }

        test("setThreadName throws UnsupportedOperationException") {
            val event = LogbackAccessEvent(TestAccessEventDataFactory.createMinimalData())

            shouldThrow<UnsupportedOperationException> {
                event.setThreadName("new-thread")
            }
        }

        test("prepareForDeferredProcessing is a no-op") {
            val event = LogbackAccessEvent(TestAccessEventDataFactory.createTestData())

            event.prepareForDeferredProcessing()
        }

        test("getRequest and getResponse return null by default") {
            val event = LogbackAccessEvent(TestAccessEventDataFactory.createMinimalData())

            assertSoftly {
                event.request shouldBe null
                event.response shouldBe null
            }
        }

        test("getServerAdapter returns null") {
            val event = LogbackAccessEvent(TestAccessEventDataFactory.createMinimalData())

            event.serverAdapter shouldBe null
        }

        test("toString contains request URL and status code") {
            val data = TestAccessEventDataFactory.createTestData()
            val event = LogbackAccessEvent(data)

            assertSoftly {
                event.toString() shouldContain "GET /test?foo=bar HTTP/1.1"
                event.toString() shouldContain "200"
            }
        }

        test("event is serializable") {
            val original = LogbackAccessEvent(TestAccessEventDataFactory.createTestData())

            val baos = ByteArrayOutputStream()
            ObjectOutputStream(baos).use { it.writeObject(original) }

            val bais = ByteArrayInputStream(baos.toByteArray())
            val deserialized = ObjectInputStream(bais).use { it.readObject() as LogbackAccessEvent }

            assertSoftly {
                deserialized.timeStamp shouldBe original.timeStamp
                deserialized.method shouldBe original.method
                deserialized.requestURI shouldBe original.requestURI
                deserialized.statusCode shouldBe original.statusCode
                deserialized.requestHeaderMap shouldBe original.requestHeaderMap
                deserialized.getCookie("session") shouldBe original.getCookie("session")
                deserialized.responseHeaderMap shouldBe original.responseHeaderMap
            }
        }

        test("requestParameterMap is cached after first access") {
            val data =
                TestAccessEventDataFactory.createTestData(
                    requestParameterMap = mapOf("key" to listOf("v1", "v2")),
                )
            val event = LogbackAccessEvent(data)

            val first = event.requestParameterMap
            val second = event.requestParameterMap

            (first === second) shouldBe true // same cached instance (reference identity)
        }
    })
