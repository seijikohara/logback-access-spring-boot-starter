package io.github.seijikohara.spring.boot.logback.access.tomcat

import ch.qos.logback.access.common.AccessConstants.LB_OUTPUT_BUFFER
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response

class TomcatResponseDataExtractorSpec :
    FunSpec({
        test("extractContent uses response character encoding for byte array conversion") {
            val request = mockk<Request>(relaxed = true)
            val response = mockk<Response>(relaxed = true)
            val isoBytes = "café".toByteArray(Charsets.ISO_8859_1)
            every { request.getAttribute(LB_OUTPUT_BUFFER) } returns isoBytes
            every { response.characterEncoding } returns "ISO-8859-1"
            every { response.contentType } returns "text/plain"

            val content = TomcatResponseDataExtractor.extractContent(request, response)

            content shouldBe "café"
        }

        test("extractContent falls back to UTF-8 when response encoding is null") {
            val request = mockk<Request>(relaxed = true)
            val response = mockk<Response>(relaxed = true)
            val utf8Bytes = "hello".toByteArray(Charsets.UTF_8)
            every { request.getAttribute(LB_OUTPUT_BUFFER) } returns utf8Bytes
            every { response.characterEncoding } returns null
            every { response.contentType } returns "text/plain"

            val content = TomcatResponseDataExtractor.extractContent(request, response)

            content shouldBe "hello"
        }

        test("extractContent returns sentinel for image response") {
            val request = mockk<Request>(relaxed = true)
            val response = mockk<Response>(relaxed = true)
            every { response.contentType } returns "image/png"

            val content = TomcatResponseDataExtractor.extractContent(request, response)

            content shouldBe "[IMAGE CONTENTS SUPPRESSED]"
        }

        test("extractContent returns null when no body captured") {
            val request = mockk<Request>(relaxed = true)
            val response = mockk<Response>(relaxed = true)
            every { request.getAttribute(LB_OUTPUT_BUFFER) } returns null
            every { response.contentType } returns "text/plain"

            val content = TomcatResponseDataExtractor.extractContent(request, response)

            content shouldBe null
        }
    })
