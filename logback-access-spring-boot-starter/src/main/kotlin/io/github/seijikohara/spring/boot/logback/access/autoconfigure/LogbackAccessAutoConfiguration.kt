package io.github.seijikohara.spring.boot.logback.access.autoconfigure

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties
import io.github.seijikohara.spring.boot.logback.access.jetty.JettyConfiguration
import io.github.seijikohara.spring.boot.logback.access.security.SecurityConfiguration
import io.github.seijikohara.spring.boot.logback.access.tee.TeeFilterConfiguration
import io.github.seijikohara.spring.boot.logback.access.tomcat.TomcatConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader

/**
 * Auto-configuration for Logback-access integration with Spring Boot.
 *
 * Activates when the application is a web application and
 * `logback.access.enabled` is `true` (the default).
 */
@AutoConfiguration(
    beforeName = [
        "org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration",
        "org.springframework.boot.tomcat.autoconfigure.reactive.TomcatReactiveWebServerAutoConfiguration",
        "org.springframework.boot.jetty.autoconfigure.servlet.JettyServletWebServerAutoConfiguration",
        "org.springframework.boot.jetty.autoconfigure.reactive.JettyReactiveWebServerAutoConfiguration",
    ],
)
@ConditionalOnBooleanProperty(prefix = "logback.access", name = ["enabled"], matchIfMissing = true)
@ConditionalOnWebApplication
@EnableConfigurationProperties(LogbackAccessProperties::class)
@Import(
    TomcatConfiguration::class,
    JettyConfiguration::class,
    SecurityConfiguration::class,
    TeeFilterConfiguration::class,
)
class LogbackAccessAutoConfiguration {
    /** Creates the [LogbackAccessContext] bean that manages the Logback-access lifecycle. */
    @Bean
    @ConditionalOnMissingBean
    fun logbackAccessContext(
        properties: LogbackAccessProperties,
        resourceLoader: ResourceLoader,
        environment: Environment,
    ): LogbackAccessContext {
        val context = LogbackAccessContext(properties, resourceLoader, environment)
        logger.debug { "Created $context" }
        return context
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
