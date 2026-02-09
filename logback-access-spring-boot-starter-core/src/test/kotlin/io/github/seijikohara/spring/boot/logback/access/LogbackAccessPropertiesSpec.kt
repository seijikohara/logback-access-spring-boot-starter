package io.github.seijikohara.spring.boot.logback.access

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class LogbackAccessPropertiesSpec :
    FunSpec({
        test("DEFAULT_CONFIGS contains expected locations in order") {
            LogbackAccessProperties.DEFAULT_CONFIGS shouldContainExactly
                listOf(
                    "classpath:logback-access-test.xml",
                    "classpath:logback-access.xml",
                    "classpath:logback-access-test-spring.xml",
                    "classpath:logback-access-spring.xml",
                )
        }

        test("FALLBACK_CONFIG is built-in resource path") {
            LogbackAccessProperties.FALLBACK_CONFIG shouldBe
                "classpath:io/github/seijikohara/spring/boot/logback/access/logback-access-spring.xml"
        }

        test("data class can be instantiated with all parameters") {
            val properties =
                LogbackAccessProperties(
                    enabled = true,
                    configLocation = "classpath:custom.xml",
                    localPortStrategy = LocalPortStrategy.LOCAL,
                    tomcat =
                        LogbackAccessProperties.TomcatProperties(
                            requestAttributesEnabled = true,
                        ),
                    teeFilter =
                        LogbackAccessProperties.TeeFilterProperties(
                            enabled = true,
                            includeHosts = "localhost,example.com",
                            excludeHosts = "internal.example.com",
                            maxPayloadSize = 65536L,
                            allowedContentTypes = listOf("text/*", "application/json"),
                        ),
                    filter =
                        LogbackAccessProperties.FilterProperties(
                            includeUrlPatterns = listOf("/api/.*"),
                            excludeUrlPatterns = listOf("/health"),
                        ),
                )

            assertSoftly {
                properties.enabled shouldBe true
                properties.configLocation shouldBe "classpath:custom.xml"
                properties.localPortStrategy shouldBe LocalPortStrategy.LOCAL
                properties.tomcat.requestAttributesEnabled shouldBe true
                properties.teeFilter.enabled shouldBe true
                properties.teeFilter.includeHosts shouldBe "localhost,example.com"
                properties.teeFilter.excludeHosts shouldBe "internal.example.com"
                properties.teeFilter.maxPayloadSize shouldBe 65536L
                properties.teeFilter.allowedContentTypes shouldBe listOf("text/*", "application/json")
                properties.filter.includeUrlPatterns shouldBe listOf("/api/.*")
                properties.filter.excludeUrlPatterns shouldBe listOf("/health")
            }
        }

        test("nested data classes can be instantiated with null values") {
            val tomcat =
                LogbackAccessProperties.TomcatProperties(
                    requestAttributesEnabled = null,
                )
            val teeFilter =
                LogbackAccessProperties.TeeFilterProperties(
                    enabled = false,
                    includeHosts = null,
                    excludeHosts = null,
                    maxPayloadSize = 65536L,
                    allowedContentTypes = null,
                )
            val filter =
                LogbackAccessProperties.FilterProperties(
                    includeUrlPatterns = null,
                    excludeUrlPatterns = null,
                )

            assertSoftly {
                tomcat.requestAttributesEnabled shouldBe null
                teeFilter.enabled shouldBe false
                teeFilter.includeHosts shouldBe null
                teeFilter.excludeHosts shouldBe null
                teeFilter.maxPayloadSize shouldBe 65536L
                teeFilter.allowedContentTypes shouldBe null
                filter.includeUrlPatterns shouldBe null
                filter.excludeUrlPatterns shouldBe null
            }
        }
    })
