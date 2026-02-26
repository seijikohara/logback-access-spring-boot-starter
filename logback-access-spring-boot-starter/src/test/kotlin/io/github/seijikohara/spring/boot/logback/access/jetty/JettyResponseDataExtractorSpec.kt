package io.github.seijikohara.spring.boot.logback.access.jetty

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.eclipse.jetty.http.HttpField
import org.eclipse.jetty.http.HttpFields
import org.eclipse.jetty.server.Response

class JettyResponseDataExtractorSpec :
    FunSpec({
        context("extractHeaders") {
            test("returns case-insensitive sorted header map") {
                val field1 =
                    mockk<HttpField> {
                        every { name } returns "Content-Type"
                        every { value } returns "application/json"
                    }
                val field2 =
                    mockk<HttpField> {
                        every { name } returns "X-Request-Id"
                        every { value } returns "abc-123"
                    }
                val headers =
                    mockk<HttpFields.Mutable> {
                        every { iterator() } returns mutableListOf(field1, field2).iterator()
                    }
                val response =
                    mockk<Response> {
                        every { this@mockk.headers } returns headers
                    }

                val result = JettyResponseDataExtractor.extractHeaders(response)

                result["Content-Type"] shouldBe "application/json"
                result["X-Request-Id"] shouldBe "abc-123"
            }

            test("keeps first value for duplicate header names") {
                val field1 =
                    mockk<HttpField> {
                        every { name } returns "Set-Cookie"
                        every { value } returns "a=1"
                    }
                val field2 =
                    mockk<HttpField> {
                        every { name } returns "Set-Cookie"
                        every { value } returns "b=2"
                    }
                val headers =
                    mockk<HttpFields.Mutable> {
                        every { iterator() } returns mutableListOf(field1, field2).iterator()
                    }
                val response =
                    mockk<Response> {
                        every { this@mockk.headers } returns headers
                    }

                val result = JettyResponseDataExtractor.extractHeaders(response)

                result["Set-Cookie"] shouldBe "a=1"
            }

            test("returns empty map when no headers") {
                val headers =
                    mockk<HttpFields.Mutable> {
                        every { iterator() } returns mutableListOf<HttpField>().iterator()
                    }
                val response =
                    mockk<Response> {
                        every { this@mockk.headers } returns headers
                    }

                val result = JettyResponseDataExtractor.extractHeaders(response)

                result.shouldBeEmpty()
            }
        }
    })
