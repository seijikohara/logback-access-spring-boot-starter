package io.github.seijikohara.spring.boot.logback.access.tomcat

import io.github.seijikohara.spring.boot.logback.access.AccessEventData.Companion.REMOTE_USER_ATTR
import io.github.seijikohara.spring.boot.logback.access.LocalPortStrategy
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.apache.catalina.AccessLog.PROTOCOL_ATTRIBUTE
import org.apache.catalina.AccessLog.REMOTE_ADDR_ATTRIBUTE
import org.apache.catalina.AccessLog.REMOTE_HOST_ATTRIBUTE
import org.apache.catalina.AccessLog.SERVER_NAME_ATTRIBUTE
import org.apache.catalina.AccessLog.SERVER_PORT_ATTRIBUTE
import org.apache.catalina.connector.Request

class TomcatRequestAttributeResolverSpec :
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

        context("requestAttributesEnabled = true") {
            test("resolveRemoteAddr returns attribute value when present") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = true)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(REMOTE_ADDR_ATTRIBUTE) } returns "10.0.0.1"
                every { request.remoteAddr } returns "192.168.1.1"

                resolver.resolveRemoteAddr(request) shouldBe "10.0.0.1"
            }

            test("resolveRemoteAddr falls back to request when attribute is null") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = true)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(REMOTE_ADDR_ATTRIBUTE) } returns null
                every { request.remoteAddr } returns "192.168.1.1"

                resolver.resolveRemoteAddr(request) shouldBe "192.168.1.1"
            }

            test("resolveRemoteHost returns attribute value when present") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = true)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(REMOTE_HOST_ATTRIBUTE) } returns "proxy.example.com"
                every { request.remoteHost } returns "direct.example.com"

                resolver.resolveRemoteHost(request) shouldBe "proxy.example.com"
            }

            test("resolveServerName returns attribute value when present") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = true)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(SERVER_NAME_ATTRIBUTE) } returns "www.example.com"
                every { request.serverName } returns "localhost"

                resolver.resolveServerName(request) shouldBe "www.example.com"
            }

            test("resolveProtocol returns attribute value when present") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = true)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(PROTOCOL_ATTRIBUTE) } returns "HTTP/2.0"
                every { request.protocol } returns "HTTP/1.1"

                resolver.resolveProtocol(request) shouldBe "HTTP/2.0"
            }

            test("resolveLocalPort with SERVER strategy returns attribute value") {
                val context = mockContext(LocalPortStrategy.SERVER)
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = true)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(SERVER_PORT_ATTRIBUTE) } returns 443
                every { request.serverPort } returns 8080

                resolver.resolveLocalPort(request) shouldBe 443
            }

            test("resolveLocalPort with LOCAL strategy returns request localPort") {
                val context = mockContext(LocalPortStrategy.LOCAL)
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = true)
                val request = mockk<Request>(relaxed = true)
                every { request.localPort } returns 8080

                resolver.resolveLocalPort(request) shouldBe 8080
            }
        }

        context("requestAttributesEnabled = false") {
            test("resolveRemoteAddr ignores attributes and returns request value") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = false)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(REMOTE_ADDR_ATTRIBUTE) } returns "10.0.0.1"
                every { request.remoteAddr } returns "192.168.1.1"

                resolver.resolveRemoteAddr(request) shouldBe "192.168.1.1"
            }

            test("resolveLocalPort with SERVER strategy returns serverPort directly") {
                val context = mockContext(LocalPortStrategy.SERVER)
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = false)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(SERVER_PORT_ATTRIBUTE) } returns 443
                every { request.serverPort } returns 8080

                resolver.resolveLocalPort(request) shouldBe 8080
            }
        }

        context("resolveRemoteUser") {
            test("returns REMOTE_USER_ATTR attribute when present") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = false)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(REMOTE_USER_ATTR) } returns "securityUser"
                every { request.remoteUser } returns "servletUser"

                resolver.resolveRemoteUser(request) shouldBe "securityUser"
            }

            test("falls back to request.remoteUser when attribute is absent") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = false)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(REMOTE_USER_ATTR) } returns null
                every { request.remoteUser } returns "servletUser"

                resolver.resolveRemoteUser(request) shouldBe "servletUser"
            }

            test("returns null when both attribute and request user are null") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = false)
                val request = mockk<Request>(relaxed = true)
                every { request.getAttribute(REMOTE_USER_ATTR) } returns null
                every { request.remoteUser } returns null

                resolver.resolveRemoteUser(request) shouldBe null
            }
        }

        context("buildRequestURL") {
            test("includes method, URI, query string, and protocol") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = false)
                val request = mockk<Request>(relaxed = true)
                every { request.method } returns "GET"
                every { request.requestURI } returns "/api/users"
                every { request.queryString } returns "page=1"
                every { request.protocol } returns "HTTP/1.1"

                resolver.buildRequestURL(request) shouldBe "GET /api/users?page=1 HTTP/1.1"
            }

            test("omits query string when null") {
                val context = mockContext()
                val resolver = TomcatRequestAttributeResolver(context, requestAttributesEnabled = false)
                val request = mockk<Request>(relaxed = true)
                every { request.method } returns "POST"
                every { request.requestURI } returns "/api/data"
                every { request.queryString } returns null
                every { request.protocol } returns "HTTP/1.1"

                resolver.buildRequestURL(request) shouldBe "POST /api/data HTTP/1.1"
            }
        }
    })
