package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.AccessConstants.LB_INPUT_BUFFER
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.apache.catalina.connector.Request

class TomcatRequestDataExtractorSpec :
    FunSpec({
        test("extractContent uses request character encoding for byte array conversion") {
            val request = mockk<Request>(relaxed = true)
            val shiftJisBytes = "テスト".toByteArray(charset("Shift_JIS"))
            every { request.getAttribute(LB_INPUT_BUFFER) } returns shiftJisBytes
            every { request.characterEncoding } returns "Shift_JIS"

            val content = TomcatRequestDataExtractor.extractContent(request)

            content shouldBe "テスト"
        }

        test("extractContent falls back to UTF-8 when encoding is null") {
            val request = mockk<Request>(relaxed = true)
            val utf8Bytes = "hello".toByteArray(Charsets.UTF_8)
            every { request.getAttribute(LB_INPUT_BUFFER) } returns utf8Bytes
            every { request.characterEncoding } returns null

            val content = TomcatRequestDataExtractor.extractContent(request)

            content shouldBe "hello"
        }

        test("extractContent falls back to UTF-8 when encoding is unsupported") {
            val request = mockk<Request>(relaxed = true)
            val utf8Bytes = "hello".toByteArray(Charsets.UTF_8)
            every { request.getAttribute(LB_INPUT_BUFFER) } returns utf8Bytes
            every { request.characterEncoding } returns "INVALID-CHARSET-NAME"

            val content = TomcatRequestDataExtractor.extractContent(request)

            content shouldBe "hello"
        }

        test("extractContent returns null when no body captured") {
            val request = mockk<Request>(relaxed = true)
            every { request.getAttribute(LB_INPUT_BUFFER) } returns null
            every { request.contentType } returns "application/json"

            val content = TomcatRequestDataExtractor.extractContent(request)

            content shouldBe null
        }
    })
