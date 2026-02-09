package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.AccessConstants.LB_INPUT_BUFFER
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties.TeeFilterProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.apache.catalina.connector.Request

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
    })
