package io.github.seijikohara.spring.boot.logback.access.jetty

import io.github.seijikohara.spring.boot.logback.access.AccessEventData.Companion.REMOTE_USER_ATTR
import io.github.seijikohara.spring.boot.logback.access.LocalPortStrategy
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.eclipse.jetty.http.HttpField
import org.eclipse.jetty.http.HttpFields
import org.eclipse.jetty.http.HttpURI
import org.eclipse.jetty.server.ConnectionMetaData
import org.eclipse.jetty.server.Request

class JettyRequestDataExtractorSpec :
    FunSpec({
        fun createProperties(localPortStrategy: LocalPortStrategy = LocalPortStrategy.SERVER): LogbackAccessProperties =
            LogbackAccessProperties(
                enabled = true,
                configLocation = null,
                localPortStrategy = localPortStrategy,
                tomcat = LogbackAccessProperties.TomcatProperties(requestAttributesEnabled = null),
                teeFilter =
                    LogbackAccessProperties.TeeFilterProperties(
                        enabled = false,
                        includeHosts = null,
                        excludeHosts = null,
                        maxPayloadSize = 65536L,
                        allowedContentTypes = null,
                    ),
                filter = LogbackAccessProperties.FilterProperties(includeUrlPatterns = null, excludeUrlPatterns = null),
            )

        fun mockContext(localPortStrategy: LocalPortStrategy = LocalPortStrategy.SERVER): LogbackAccessContext =
            mockk {
                every { properties } returns createProperties(localPortStrategy)
            }

        context("extractHeaders") {
            test("returns case-insensitive sorted header map") {
                val field1 =
                    mockk<HttpField> {
                        every { name } returns "Content-Type"
                        every { value } returns "text/html"
                    }
                val field2 =
                    mockk<HttpField> {
                        every { name } returns "Accept"
                        every { value } returns "application/json"
                    }
                val headers =
                    mockk<HttpFields> {
                        every { iterator() } returns mutableListOf(field1, field2).iterator()
                    }
                val request =
                    mockk<Request> {
                        every { this@mockk.headers } returns headers
                    }

                val result = JettyRequestDataExtractor.extractHeaders(request)

                result shouldContainExactly mapOf("Accept" to "application/json", "Content-Type" to "text/html")
            }

            test("keeps first value for duplicate header names") {
                val field1 =
                    mockk<HttpField> {
                        every { name } returns "X-Custom"
                        every { value } returns "first"
                    }
                val field2 =
                    mockk<HttpField> {
                        every { name } returns "X-Custom"
                        every { value } returns "second"
                    }
                val headers =
                    mockk<HttpFields> {
                        every { iterator() } returns mutableListOf(field1, field2).iterator()
                    }
                val request =
                    mockk<Request> {
                        every { this@mockk.headers } returns headers
                    }

                val result = JettyRequestDataExtractor.extractHeaders(request)

                result["X-Custom"] shouldBe "first"
            }

            test("returns empty map when no headers") {
                val headers =
                    mockk<HttpFields> {
                        every { iterator() } returns mutableListOf<HttpField>().iterator()
                    }
                val request =
                    mockk<Request> {
                        every { this@mockk.headers } returns headers
                    }

                val result = JettyRequestDataExtractor.extractHeaders(request)

                result.shouldBeEmpty()
            }
        }

        context("extractAttributes") {
            test("returns attribute map with null values filtered out") {
                val request =
                    mockk<Request> {
                        every { attributeNameSet } returns setOf("present", "absent")
                        every { getAttribute("present") } returns "value"
                        every { getAttribute("absent") } returns null
                    }

                val result = JettyRequestDataExtractor.extractAttributes(request)

                result shouldContainExactly mapOf("present" to "value")
            }

            test("returns empty map when no attributes") {
                val request =
                    mockk<Request> {
                        every { attributeNameSet } returns emptySet()
                    }

                val result = JettyRequestDataExtractor.extractAttributes(request)

                result.shouldBeEmpty()
            }
        }

        context("resolveRemoteUser") {
            test("returns REMOTE_USER_ATTR when present") {
                val request =
                    mockk<Request> {
                        every { getAttribute(REMOTE_USER_ATTR) } returns "authenticatedUser"
                    }

                JettyRequestDataExtractor.resolveRemoteUser(request) shouldBe "authenticatedUser"
            }

            test("returns null when attribute is absent") {
                val request =
                    mockk<Request> {
                        every { getAttribute(REMOTE_USER_ATTR) } returns null
                    }

                JettyRequestDataExtractor.resolveRemoteUser(request) shouldBe null
            }
        }

        context("resolveLocalPort") {
            test("returns local port for LOCAL strategy") {
                val context = mockContext(LocalPortStrategy.LOCAL)

                mockkStatic(Request::class) {
                    val request = mockk<Request>()
                    every { Request.getLocalPort(request) } returns 8080

                    JettyRequestDataExtractor.resolveLocalPort(context, request) shouldBe 8080
                }
            }

            test("returns server port for SERVER strategy") {
                val context = mockContext(LocalPortStrategy.SERVER)

                mockkStatic(Request::class) {
                    val request = mockk<Request>()
                    every { Request.getServerPort(request) } returns 443

                    JettyRequestDataExtractor.resolveLocalPort(context, request) shouldBe 443
                }
            }
        }

        context("buildRequestURL") {
            test("builds URL with query string") {
                val httpURI =
                    mockk<HttpURI> {
                        every { path } returns "/api/users"
                        every { query } returns "page=1"
                    }
                val connectionMetaData =
                    mockk<ConnectionMetaData> {
                        every { protocol } returns "HTTP/1.1"
                    }
                val request =
                    mockk<Request> {
                        every { method } returns "GET"
                        every { this@mockk.httpURI } returns httpURI
                        every { this@mockk.connectionMetaData } returns connectionMetaData
                    }

                JettyRequestDataExtractor.buildRequestURL(request) shouldBe "GET /api/users?page=1 HTTP/1.1"
            }

            test("builds URL without query string when null") {
                val httpURI =
                    mockk<HttpURI> {
                        every { path } returns "/api/data"
                        every { query } returns null
                    }
                val connectionMetaData =
                    mockk<ConnectionMetaData> {
                        every { protocol } returns "HTTP/2.0"
                    }
                val request =
                    mockk<Request> {
                        every { method } returns "POST"
                        every { this@mockk.httpURI } returns httpURI
                        every { this@mockk.connectionMetaData } returns connectionMetaData
                    }

                JettyRequestDataExtractor.buildRequestURL(request) shouldBe "POST /api/data HTTP/2.0"
            }
        }

        context("extractCookies") {
            test("returns cookie map for valid cookies") {
                mockkStatic(Request::class) {
                    val cookie1 =
                        mockk<org.eclipse.jetty.http.HttpCookie> {
                            every { name } returns "session"
                            every { value } returns "abc123"
                        }
                    val cookie2 =
                        mockk<org.eclipse.jetty.http.HttpCookie> {
                            every { name } returns "lang"
                            every { value } returns "en"
                        }
                    val request = mockk<Request>()
                    every { Request.getCookies(request) } returns listOf(cookie1, cookie2)

                    val result = JettyRequestDataExtractor.extractCookies(request)

                    result shouldContainExactly mapOf("session" to "abc123", "lang" to "en")
                }
            }

            test("returns empty map on parsing error") {
                mockkStatic(Request::class) {
                    val request = mockk<Request>()
                    every { Request.getCookies(request) } throws IllegalArgumentException("Bad cookie header")

                    val result = JettyRequestDataExtractor.extractCookies(request)

                    result.shouldBeEmpty()
                }
            }
        }

        afterSpec {
            unmockkStatic(Request::class)
        }
    })
