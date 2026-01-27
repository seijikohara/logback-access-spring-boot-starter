package io.github.seijikohara.logback.access.tracing

import io.micrometer.tracing.Tracer
import org.slf4j.Logger
import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.server.WebFilter

/**
 * The configuration to register the tracing filter for the reactive web server.
 *
 * This configuration is activated when Micrometer Tracing is on the classpath
 * and a [Tracer] bean is available.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Tracer::class, WebFilter::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
class LogbackAccessTracingWebFilterConfiguration {

    /**
     * Provides the tracing filter for the reactive web server.
     *
     * @param tracer The Micrometer Tracer.
     * @return The tracing filter for the reactive web server.
     */
    @Bean
    @ConditionalOnBean(Tracer::class)
    @ConditionalOnMissingBean(LogbackAccessTracingWebFilter::class)
    fun logbackAccessTracingWebFilter(tracer: Tracer): LogbackAccessTracingWebFilter {
        val filter = LogbackAccessTracingWebFilter(tracer)
        log.debug(
            "Providing the {}: {}",
            LogbackAccessTracingWebFilter::class.simpleName,
            filter,
        )
        return filter
    }

    companion object {

        /**
         * The logger.
         */
        private val log: Logger = getLogger(LogbackAccessTracingWebFilterConfiguration::class.java)
    }
}
