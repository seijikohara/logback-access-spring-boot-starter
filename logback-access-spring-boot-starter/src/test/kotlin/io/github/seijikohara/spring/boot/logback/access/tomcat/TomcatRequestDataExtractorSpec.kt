package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.AccessConstants.LB_INPUT_BUFFER
import ch.qos.logback.access.common.AccessConstants.LB_OUTPUT_BUFFER
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties.TeeFilterProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.Cookie
import org.apache.catalina.connector.Request
import java.util.Collections

class TomcatRequestDataExtractorSpec :
    FunSpec({
        val defaultProperties =
            TeeFilterProperties(
                enabled = true,
                includeHosts = null,
                excludeHosts = null,
                maxPayloadSize = 65536L,
                allowedContentTypes = null,
            )

        context("extractHeaders") {
            test("returns case-insensitive sorted header map") {
                val request = mockk<Request>(relaxed = true)
                every { request.headerNames } returns Collections.enumeration(listOf("Content-Type", "Accept"))
                every { request.getHeader("Content-Type") } returns "text/html"
                every { request.getHeader("Accept") } returns "application/json"

                val headers = TomcatRequestDataExtractor.extractHeaders(request)

                headers shouldContainExactly mapOf("Accept" to "application/json", "Content-Type" to "text/html")
            }

            test("returns first value for duplicate headers") {
                val request = mockk<Request>(relaxed = true)
                every { request.headerNames } returns Collections.enumeration(listOf("X-Forwarded-For"))
                every { request.getHeader("X-Forwarded-For") } returns "10.0.0.1"

                val headers = TomcatRequestDataExtractor.extractHeaders(request)

                headers["X-Forwarded-For"] shouldBe "10.0.0.1"
            }

            test("returns empty map when no headers present") {
                val request = mockk<Request>(relaxed = true)
                every { request.headerNames } returns Collections.enumeration(emptyList())

                val headers = TomcatRequestDataExtractor.extractHeaders(request)

                headers.shouldBeEmpty()
            }
        }

        context("extractCookies") {
            test("returns cookie name-value pairs") {
                val request = mockk<Request>(relaxed = true)
                every { request.cookies } returns
                    arrayOf(
                        Cookie("session", "abc123"),
                        Cookie("theme", "dark"),
                    )

                val cookies = TomcatRequestDataExtractor.extractCookies(request)

                cookies shouldContainExactly mapOf("session" to "abc123", "theme" to "dark")
            }

            test("returns empty map when cookies are null") {
                val request = mockk<Request>(relaxed = true)
                every { request.cookies } returns null

                val cookies = TomcatRequestDataExtractor.extractCookies(request)

                cookies.shouldBeEmpty()
            }
        }

        context("extractParameters") {
            test("returns parameter map with multi-value support") {
                val request = mockk<Request>(relaxed = true)
                every { request.parameterMap } returns mapOf("key" to arrayOf("v1", "v2"), "single" to arrayOf("val"))

                val params = TomcatRequestDataExtractor.extractParameters(request)

                params["key"] shouldBe listOf("v1", "v2")
                params["single"] shouldBe listOf("val")
            }

            test("returns empty map when no parameters") {
                val request = mockk<Request>(relaxed = true)
                every { request.parameterMap } returns emptyMap()

                val params = TomcatRequestDataExtractor.extractParameters(request)

                params.shouldBeEmpty()
            }
        }

        context("extractAttributes") {
            test("returns attribute map excluding logback buffer attributes") {
                val request = mockk<Request>(relaxed = true)
                every { request.attributeNames } returns
                    Collections.enumeration(listOf("custom", LB_INPUT_BUFFER, LB_OUTPUT_BUFFER))
                every { request.getAttribute("custom") } returns "customValue"
                every { request.getAttribute(LB_INPUT_BUFFER) } returns ByteArray(10)
                every { request.getAttribute(LB_OUTPUT_BUFFER) } returns ByteArray(10)

                val attributes = TomcatRequestDataExtractor.extractAttributes(request)

                attributes shouldHaveSize 1
                attributes["custom"] shouldBe "customValue"
            }

            test("skips null attribute values") {
                val request = mockk<Request>(relaxed = true)
                every { request.attributeNames } returns Collections.enumeration(listOf("present", "absent"))
                every { request.getAttribute("present") } returns "value"
                every { request.getAttribute("absent") } returns null

                val attributes = TomcatRequestDataExtractor.extractAttributes(request)

                attributes shouldHaveSize 1
                attributes["present"] shouldBe "value"
            }

            test("returns empty map when no attributes") {
                val request = mockk<Request>(relaxed = true)
                every { request.attributeNames } returns Collections.enumeration(emptyList())

                val attributes = TomcatRequestDataExtractor.extractAttributes(request)

                attributes.shouldBeEmpty()
            }
        }

        context("extractContent — form data normal path") {
            test("returns URL-encoded form data when buffer is absent") {
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(LB_INPUT_BUFFER) } returns null
                every { request.contentType } returns "application/x-www-form-urlencoded"
                every { request.method } returns "POST"
                every { request.characterEncoding } returns null
                every { request.parameterMap } returns mapOf("key" to arrayOf("value"))

                val content = TomcatRequestDataExtractor.extractContent(request, defaultProperties)

                content shouldBe "key=value"
            }
        }

        test("extractContent uses request character encoding for byte array conversion") {
            val request = mockk<Request>(relaxed = true)
            val shiftJisBytes = "テスト".toByteArray(charset("Shift_JIS"))
            every { request.getAttribute(LB_INPUT_BUFFER) } returns shiftJisBytes
            every { request.characterEncoding } returns "Shift_JIS"
            every { request.contentType } returns "text/plain"

            val content = TomcatRequestDataExtractor.extractContent(request, defaultProperties)

            content shouldBe "テスト"
        }

        test("extractContent falls back to UTF-8 when encoding is null") {
            val request = mockk<Request>(relaxed = true)
            val utf8Bytes = "hello".toByteArray(Charsets.UTF_8)
            every { request.getAttribute(LB_INPUT_BUFFER) } returns utf8Bytes
            every { request.characterEncoding } returns null
            every { request.contentType } returns "text/plain"

            val content = TomcatRequestDataExtractor.extractContent(request, defaultProperties)

            content shouldBe "hello"
        }

        test("extractContent falls back to UTF-8 when encoding is unsupported") {
            val request = mockk<Request>(relaxed = true)
            val utf8Bytes = "hello".toByteArray(Charsets.UTF_8)
            every { request.getAttribute(LB_INPUT_BUFFER) } returns utf8Bytes
            every { request.characterEncoding } returns "INVALID-CHARSET-NAME"
            every { request.contentType } returns "application/json"

            val content = TomcatRequestDataExtractor.extractContent(request, defaultProperties)

            content shouldBe "hello"
        }

        test("extractContent returns null when no body captured") {
            val request = mockk<Request>(relaxed = true)
            every { request.getAttribute(LB_INPUT_BUFFER) } returns null
            every { request.contentType } returns "application/json"

            val content = TomcatRequestDataExtractor.extractContent(request, defaultProperties)

            content shouldBe null
        }

        test("extractContent suppresses binary content") {
            val request = mockk<Request>(relaxed = true)
            val binaryBytes = ByteArray(100) { 0xFF.toByte() }
            every { request.getAttribute(LB_INPUT_BUFFER) } returns binaryBytes
            every { request.contentType } returns "application/octet-stream"

            val content = TomcatRequestDataExtractor.extractContent(request, defaultProperties)

            content shouldBe "[BINARY CONTENT SUPPRESSED]"
        }

        test("extractContent suppresses oversized payload") {
            val request = mockk<Request>(relaxed = true)
            val largeBytes = ByteArray(70000) { 'a'.code.toByte() }
            every { request.getAttribute(LB_INPUT_BUFFER) } returns largeBytes
            every { request.contentType } returns "text/plain"

            val content = TomcatRequestDataExtractor.extractContent(request, defaultProperties)

            content shouldBe "[CONTENT TOO LARGE]"
        }

        test("extractContent returns null when TeeFilter is disabled") {
            val request = mockk<Request>(relaxed = true)
            every { request.getAttribute(LB_INPUT_BUFFER) } returns "hello".toByteArray()
            every { request.contentType } returns "text/plain"
            val disabledProperties = defaultProperties.copy(enabled = false)

            val content = TomcatRequestDataExtractor.extractContent(request, disabledProperties)

            content shouldBe null
        }

        test("extractContent returns null when TeeFilter is disabled even for form data") {
            val request = mockk<Request>(relaxed = true)
            every { request.getAttribute(LB_INPUT_BUFFER) } returns null
            every { request.contentType } returns "application/x-www-form-urlencoded"

            val disabledProperties = defaultProperties.copy(enabled = false)

            val content = TomcatRequestDataExtractor.extractContent(request, disabledProperties)

            content shouldBe null
        }

        test("extractContent suppresses oversized form data via BodyCapturePolicy") {
            val request = mockk<Request>(relaxed = true)
            every { request.getAttribute(LB_INPUT_BUFFER) } returns null
            every { request.contentType } returns "application/x-www-form-urlencoded"
            every { request.method } returns "POST"
            every { request.characterEncoding } returns null
            val largeValue = "x".repeat(70000)
            every { request.parameterMap } returns mapOf("key" to arrayOf(largeValue))

            val smallLimit = defaultProperties.copy(maxPayloadSize = 100L)

            val content = TomcatRequestDataExtractor.extractContent(request, smallLimit)

            content shouldBe "[CONTENT TOO LARGE]"
        }

        test("extractContent applies allowed-content-types to form data fallback") {
            val request = mockk<Request>(relaxed = true)
            every { request.getAttribute(LB_INPUT_BUFFER) } returns null
            every { request.contentType } returns "application/x-www-form-urlencoded"
            every { request.method } returns "POST"
            every { request.characterEncoding } returns null
            every { request.parameterMap } returns mapOf("key" to arrayOf("value"))

            val jsonOnly = defaultProperties.copy(allowedContentTypes = listOf("application/json"))

            val content = TomcatRequestDataExtractor.extractContent(request, jsonOnly)

            content shouldBe "[BINARY CONTENT SUPPRESSED]"
        }
    })
