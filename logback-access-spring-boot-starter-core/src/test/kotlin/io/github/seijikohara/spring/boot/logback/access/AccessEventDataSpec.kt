package io.github.seijikohara.spring.boot.logback.access

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class AccessEventDataSpec :
    FunSpec({
        test("data class preserves all fields") {
            val data = TestAccessEventDataFactory.createTestData()

            assertSoftly {
                data.timeStamp shouldBe 1000L
                data.elapsedTime shouldBe 50L
                data.sequenceNumber shouldBe 1L
                data.threadName shouldBe "main"
                data.serverName shouldBe "localhost"
                data.localPort shouldBe 8080
                data.remoteAddr shouldBe "127.0.0.1"
                data.remoteHost shouldBe "localhost"
                data.remoteUser shouldBe "testuser"
                data.protocol shouldBe "HTTP/1.1"
                data.method shouldBe "GET"
                data.requestURI shouldBe "/test"
                data.queryString shouldBe "?foo=bar"
                data.requestURL shouldBe "GET /test?foo=bar HTTP/1.1"
                data.requestHeaderMap shouldBe mapOf("Host" to "localhost")
                data.cookieMap shouldBe mapOf("session" to "abc123")
                data.requestParameterMap shouldBe mapOf("foo" to listOf("bar"))
                data.attributeMap shouldBe mapOf("attr1" to "value1")
                data.sessionID shouldBe "session123"
                data.requestContent shouldBe "request body"
                data.statusCode shouldBe 200
                data.responseHeaderMap shouldBe mapOf("Content-Type" to "text/plain")
                data.contentLength shouldBe 13L
                data.responseContent shouldBe "response body"
            }
        }

        test("requestParameterArrayMap converts list to array") {
            val data =
                TestAccessEventDataFactory.createTestData(
                    requestParameterMap = mapOf("key" to listOf("value1", "value2")),
                )

            val arrayMap = data.requestParameterArrayMap
            arrayMap["key"] shouldBe arrayOf("value1", "value2")
        }

        test("nullable fields can be null") {
            val data = TestAccessEventDataFactory.createMinimalData()

            assertSoftly {
                data.elapsedTime shouldBe null
                data.sequenceNumber shouldBe null
                data.serverName shouldBe null
                data.remoteUser shouldBe null
                data.requestURI shouldBe null
                data.sessionID shouldBe null
                data.requestContent shouldBe null
                data.responseContent shouldBe null
            }
        }

        test("data class is serializable") {
            val original = TestAccessEventDataFactory.createTestData()

            val baos = ByteArrayOutputStream()
            ObjectOutputStream(baos).use { it.writeObject(original) }

            val bais = ByteArrayInputStream(baos.toByteArray())
            val deserialized = ObjectInputStream(bais).use { it.readObject() as AccessEventData }

            deserialized shouldBe original
        }

        test("requestParameterArrayMap is accessible after deserialization") {
            val original = TestAccessEventDataFactory.createTestData(requestParameterMap = mapOf("key" to listOf("v1", "v2")))

            val baos = ByteArrayOutputStream()
            ObjectOutputStream(baos).use { it.writeObject(original) }

            val bais = ByteArrayInputStream(baos.toByteArray())
            val deserialized = ObjectInputStream(bais).use { it.readObject() as AccessEventData }

            deserialized.requestParameterArrayMap["key"] shouldBe arrayOf("v1", "v2")
        }

        test("copy creates independent instance") {
            val original = TestAccessEventDataFactory.createTestData()
            val copied = original.copy(statusCode = 404)

            assertSoftly {
                copied.statusCode shouldBe 404
                original.statusCode shouldBe 200
            }
        }
    })
