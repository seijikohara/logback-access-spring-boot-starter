package io.github.seijikohara.spring.boot.logback.access

import ch.qos.logback.access.common.spi.IAccessEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.mock.env.MockEnvironment

class LogbackAccessContextSpec :
    FunSpec({
        context("initialization") {
            test("uses DEFAULT_CONFIGS when configLocation is null") {
                val properties = createProperties()
                val context = createContext(properties)

                try {
                    context.accessContext.name shouldContain "logback-access-test.xml"
                } finally {
                    context.close()
                }
            }

            test("uses FALLBACK_CONFIG when no config found") {
                val properties = createProperties()
                val resourceLoader = createResourceLoaderWithOnlyFallback()
                val environment = MockEnvironment()
                val context = LogbackAccessContext(properties, resourceLoader, environment)

                try {
                    context.accessContext.name shouldBe
                        LogbackAccessProperties.FALLBACK_CONFIG
                } finally {
                    context.close()
                }
            }

            test("uses specified configLocation when provided") {
                val properties =
                    createProperties(
                        configLocation = "classpath:logback-access-test.xml",
                    )
                val context = createContext(properties)

                try {
                    context.accessContext.name shouldBe "classpath:logback-access-test.xml"
                } finally {
                    context.close()
                }
            }
        }

        context("emit") {
            test("logs event when no filters configured") {
                val properties = createProperties()
                val context = createContext(properties)

                try {
                    val event = createTestEvent("/api/users")
                    context.emit(event)

                    val appender = getListAppender(context)
                    appender.list shouldHaveSize 1
                } finally {
                    context.close()
                }
            }

            test("includes event when URI matches include pattern") {
                val properties =
                    createProperties(
                        includeUrlPatterns = listOf("/api/.*"),
                    )
                val context = createContext(properties)

                try {
                    context.emit(createTestEvent("/api/users"))
                    context.emit(createTestEvent("/health"))

                    val appender = getListAppender(context)
                    appender.list shouldHaveSize 1
                    appender.list[0].requestURI shouldBe "/api/users"
                } finally {
                    context.close()
                }
            }

            test("excludes event when URI matches exclude pattern") {
                val properties =
                    createProperties(
                        excludeUrlPatterns = listOf("/health", "/actuator/.*"),
                    )
                val context = createContext(properties)

                try {
                    context.emit(createTestEvent("/api/users"))
                    context.emit(createTestEvent("/health"))
                    context.emit(createTestEvent("/actuator/health"))

                    val appender = getListAppender(context)
                    appender.list shouldHaveSize 1
                    appender.list[0].requestURI shouldBe "/api/users"
                } finally {
                    context.close()
                }
            }

            test("applies include before exclude patterns") {
                val properties =
                    createProperties(
                        includeUrlPatterns = listOf("/api/.*"),
                        excludeUrlPatterns = listOf("/api/internal/.*"),
                    )
                val context = createContext(properties)

                try {
                    context.emit(createTestEvent("/api/users"))
                    context.emit(createTestEvent("/api/internal/debug"))
                    context.emit(createTestEvent("/health"))

                    val appender = getListAppender(context)
                    appender.list shouldHaveSize 1
                    appender.list[0].requestURI shouldBe "/api/users"
                } finally {
                    context.close()
                }
            }

            test("catches exceptions from appenders without propagating") {
                val properties = createProperties()
                val context = createContext(properties)

                try {
                    val appender = getListAppender(context)
                    context.accessContext.detachAppender(appender.name)

                    val throwingAppender =
                        object : ch.qos.logback.core.AppenderBase<IAccessEvent>() {
                            override fun append(eventObject: IAccessEvent?): Unit = error("Simulated appender failure")
                        }
                    throwingAppender.context = context.accessContext
                    throwingAppender.name = "throwing"
                    throwingAppender.start()
                    context.accessContext.addAppender(throwingAppender)

                    context.emit(createTestEvent("/api/test"))
                } finally {
                    context.close()
                }
            }
        }

        context("close") {
            test("stops and resets access context") {
                val properties = createProperties()
                val context = createContext(properties)

                context.close()

                assertSoftly {
                    context.accessContext.isStarted shouldBe false
                }
            }

            test("detaches all appenders") {
                val properties = createProperties()
                val context = createContext(properties)

                context.close()

                context.accessContext.iteratorForAppenders().hasNext() shouldBe false
            }
        }

        context("toString") {
            test("includes context name") {
                val properties = createProperties()
                val context = createContext(properties)

                try {
                    context.toString() shouldContain "LogbackAccessContext"
                } finally {
                    context.close()
                }
            }
        }
    })

private fun createProperties(
    configLocation: String? = null,
    includeUrlPatterns: List<String>? = null,
    excludeUrlPatterns: List<String>? = null,
): LogbackAccessProperties =
    LogbackAccessProperties(
        enabled = true,
        configLocation = configLocation,
        localPortStrategy = LocalPortStrategy.SERVER,
        tomcat =
            LogbackAccessProperties.TomcatProperties(
                requestAttributesEnabled = null,
            ),
        teeFilter =
            LogbackAccessProperties.TeeFilterProperties(
                enabled = false,
                includeHosts = null,
                excludeHosts = null,
                maxPayloadSize = 65536L,
                allowedContentTypes = null,
            ),
        filter =
            LogbackAccessProperties.FilterProperties(
                includeUrlPatterns = includeUrlPatterns,
                excludeUrlPatterns = excludeUrlPatterns,
            ),
    )

private fun createContext(properties: LogbackAccessProperties): LogbackAccessContext {
    val resourceLoader =
        object : DefaultResourceLoader() {
            override fun getResource(location: String): org.springframework.core.io.Resource =
                if (location == "classpath:logback-access-test.xml") {
                    super.getResource(location)
                } else if (
                    location.startsWith("classpath:") &&
                    location != LogbackAccessProperties.FALLBACK_CONFIG
                ) {
                    createNonExistentResource(location)
                } else {
                    super.getResource(location)
                }
        }
    val environment = MockEnvironment()
    return LogbackAccessContext(properties, resourceLoader, environment)
}

private fun createResourceLoaderWithOnlyFallback(): DefaultResourceLoader =
    object : DefaultResourceLoader() {
        override fun getResource(location: String): org.springframework.core.io.Resource =
            if (location == LogbackAccessProperties.FALLBACK_CONFIG) {
                super.getResource(location)
            } else {
                createNonExistentResource(location)
            }
    }

private fun createNonExistentResource(location: String): org.springframework.core.io.Resource =
    object : org.springframework.core.io.AbstractResource() {
        override fun getDescription() = "Non-existent: $location"

        override fun getInputStream() = throw java.io.FileNotFoundException(description)

        override fun exists() = false
    }

private fun createTestEvent(requestURI: String): LogbackAccessEvent =
    LogbackAccessEvent(
        AccessEventData(
            timeStamp = System.currentTimeMillis(),
            elapsedTime = 10L,
            sequenceNumber = 1L,
            threadName = Thread.currentThread().name,
            serverName = "localhost",
            localPort = 8080,
            remoteAddr = "127.0.0.1",
            remoteHost = "127.0.0.1",
            remoteUser = null,
            protocol = "HTTP/1.1",
            method = "GET",
            requestURI = requestURI,
            queryString = "",
            requestURL = "GET $requestURI HTTP/1.1",
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
        ),
    )

@Suppress("UNCHECKED_CAST")
private fun getListAppender(context: LogbackAccessContext): ListAppender<IAccessEvent> =
    context.accessContext.getAppender("list") as ListAppender<IAccessEvent>
