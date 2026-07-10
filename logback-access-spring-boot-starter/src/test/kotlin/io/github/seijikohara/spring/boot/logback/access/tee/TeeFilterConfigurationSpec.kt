package io.github.seijikohara.spring.boot.logback.access.tee

import ch.qos.logback.access.common.AccessConstants.TEE_FILTER_EXCLUDES_PARAM
import ch.qos.logback.access.common.AccessConstants.TEE_FILTER_INCLUDES_PARAM
import io.github.seijikohara.spring.boot.logback.access.LocalPortStrategy
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.springframework.core.Ordered

class TeeFilterConfigurationSpec :
    FunSpec({
        fun properties(
            includeHosts: String? = null,
            excludeHosts: String? = null,
        ): LogbackAccessProperties =
            LogbackAccessProperties(
                enabled = true,
                configLocation = null,
                localPortStrategy = LocalPortStrategy.SERVER,
                tomcat = LogbackAccessProperties.TomcatProperties(requestAttributesEnabled = null),
                teeFilter =
                    LogbackAccessProperties.TeeFilterProperties(
                        enabled = true,
                        includeHosts = includeHosts,
                        excludeHosts = excludeHosts,
                        maxPayloadSize = 65536L,
                        allowedContentTypes = null,
                    ),
                filter = LogbackAccessProperties.FilterProperties(null, null),
            )

        test("registers include and exclude hosts as TeeFilter init parameters") {
            val registration =
                TeeFilterConfiguration()
                    .logbackAccessTeeFilter(properties(includeHosts = "host-a,host-b", excludeHosts = "host-c"))

            registration.initParameters shouldContainExactly
                mapOf(
                    TEE_FILTER_INCLUDES_PARAM to "host-a,host-b",
                    TEE_FILTER_EXCLUDES_PARAM to "host-c",
                )
        }

        test("registers no init parameters when hosts are not configured") {
            val registration = TeeFilterConfiguration().logbackAccessTeeFilter(properties())

            registration.initParameters.shouldBeEmpty()
        }

        test("ignores blank host lists") {
            val registration = TeeFilterConfiguration().logbackAccessTeeFilter(properties(includeHosts = " ", excludeHosts = ""))

            registration.initParameters.shouldBeEmpty()
        }

        test("applies near-highest precedence and a catch-all URL pattern") {
            val registration = TeeFilterConfiguration().logbackAccessTeeFilter(properties())

            registration.order shouldBe Ordered.HIGHEST_PRECEDENCE + 10
            registration.urlPatterns shouldBe setOf("/*")
        }
    })
