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
            val data = createTestData()
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
            val data = createMinimalData()
            val event = LogbackAccessEvent(data)

            assertSoftly {
                event.serverName shouldBe NA
                event.remoteUser shouldBe NA
                event.requestURI shouldBe NA
                event.sessionID shouldBe NA
            }
        }

        test("returns SENTINEL for null numeric fields") {
            val data = createMinimalData()
            val event = LogbackAccessEvent(data)

            assertSoftly {
                event.elapsedTime shouldBe SENTINEL.toLong()
                event.elapsedSeconds shouldBe SENTINEL.toLong()
                event.sequenceNumber shouldBe SENTINEL.toLong()
            }
        }

        test("returns empty string for null content fields") {
            val data = createMinimalData()
            val event = LogbackAccessEvent(data)

            assertSoftly {
                event.requestContent shouldBe ""
                event.responseContent shouldBe ""
            }
        }

        test("getRequestHeader returns NA for missing header") {
            val event = LogbackAccessEvent(createMinimalData())

            event.getRequestHeader("X-Custom") shouldBe NA
        }

        test("getRequestHeader returns value for existing header") {
            val data = createTestData()
            val event = LogbackAccessEvent(data)

            event.getRequestHeader("Host") shouldBe "localhost"
        }

        test("getResponseHeader returns NA for missing header") {
            val event = LogbackAccessEvent(createMinimalData())

            event.getResponseHeader("X-Custom") shouldBe NA
        }

        test("getCookie returns NA for missing cookie") {
            val event = LogbackAccessEvent(createMinimalData())

            event.getCookie("missing") shouldBe NA
        }

        test("getCookie returns value for existing cookie") {
            val data = createTestData()
            val event = LogbackAccessEvent(data)

            event.getCookie("session") shouldBe "abc123"
        }

        test("getAttribute returns NA for missing attribute") {
            val event = LogbackAccessEvent(createMinimalData())

            event.getAttribute("missing") shouldBe NA
        }

        test("getRequestParameter returns NA array for missing parameter") {
            val event = LogbackAccessEvent(createMinimalData())

            event.getRequestParameter("missing") shouldBe arrayOf(NA)
        }

        test("getRequestHeaderNames returns enumeration of header keys") {
            val data = createTestData()
            val event = LogbackAccessEvent(data)

            val names = event.requestHeaderNames.toList()
            names shouldContain "Host"
        }

        test("getResponseHeaderNameList returns list of header keys") {
            val data = createTestData()
            val event = LogbackAccessEvent(data)

            event.responseHeaderNameList shouldContain "Content-Type"
        }

        test("setThreadName throws UnsupportedOperationException") {
            val event = LogbackAccessEvent(createMinimalData())

            shouldThrow<UnsupportedOperationException> {
                event.setThreadName("new-thread")
            }
        }

        test("prepareForDeferredProcessing is a no-op") {
            val event = LogbackAccessEvent(createTestData())

            event.prepareForDeferredProcessing()
        }

        test("getRequest and getResponse return null by default") {
            val event = LogbackAccessEvent(createMinimalData())

            assertSoftly {
                event.request shouldBe null
                event.response shouldBe null
            }
        }

        test("getServerAdapter returns null") {
            val event = LogbackAccessEvent(createMinimalData())

            event.serverAdapter shouldBe null
        }

        test("toString contains request URL and status code") {
            val data = createTestData()
            val event = LogbackAccessEvent(data)

            assertSoftly {
                event.toString() shouldContain "GET /test?foo=bar HTTP/1.1"
                event.toString() shouldContain "200"
            }
        }

        test("event is serializable") {
            val original = LogbackAccessEvent(createTestData())

            val baos = ByteArrayOutputStream()
            ObjectOutputStream(baos).use { it.writeObject(original) }

            val bais = ByteArrayInputStream(baos.toByteArray())
            val deserialized = ObjectInputStream(bais).use { it.readObject() as LogbackAccessEvent }

            assertSoftly {
                deserialized.timeStamp shouldBe original.timeStamp
                deserialized.method shouldBe original.method
                deserialized.requestURI shouldBe original.requestURI
                deserialized.statusCode shouldBe original.statusCode
            }
        }
    })

private fun createTestData(): AccessEventData =
    AccessEventData(
        timeStamp = 1000L,
        elapsedTime = 50L,
        sequenceNumber = 1L,
        threadName = "main",
        serverName = "localhost",
        localPort = 8080,
        remoteAddr = "127.0.0.1",
        remoteHost = "localhost",
        remoteUser = "testuser",
        protocol = "HTTP/1.1",
        method = "GET",
        requestURI = "/test",
        queryString = "?foo=bar",
        requestURL = "GET /test?foo=bar HTTP/1.1",
        requestHeaderMap = mapOf("Host" to "localhost"),
        cookieMap = mapOf("session" to "abc123"),
        requestParameterMap = mapOf("foo" to listOf("bar")),
        attributeMap = mapOf("attr1" to "value1"),
        sessionID = "session123",
        requestContent = "request body",
        statusCode = 200,
        responseHeaderMap = mapOf("Content-Type" to "text/plain"),
        contentLength = 13L,
        responseContent = "response body",
    )

private fun createMinimalData(): AccessEventData =
    AccessEventData(
        timeStamp = 1000L,
        elapsedTime = null,
        sequenceNumber = null,
        threadName = "main",
        serverName = null,
        localPort = 8080,
        remoteAddr = "127.0.0.1",
        remoteHost = "127.0.0.1",
        remoteUser = null,
        protocol = "HTTP/1.1",
        method = "GET",
        requestURI = null,
        queryString = "",
        requestURL = "GET / HTTP/1.1",
        requestHeaderMap = emptyMap(),
        cookieMap = emptyMap(),
        requestParameterMap = emptyMap(),
        attributeMap = emptyMap(),
        sessionID = null,
        requestContent = null,
        statusCode = 200,
        responseHeaderMap = emptyMap(),
        contentLength = 0L,
        responseContent = null,
    )
