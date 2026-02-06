package io.github.seijikohara.spring.boot.logback.access.jetty

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import org.eclipse.jetty.server.Server
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.jetty.ConfigurableJettyWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Registers Jetty-specific access logging infrastructure.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Server::class)
@ConditionalOnWebApplication
internal class JettyConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = ["logbackAccessJettyCustomizer"])
    fun logbackAccessJettyCustomizer(
        logbackAccessContext: LogbackAccessContext,
    ): WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory> =
        WebServerFactoryCustomizer { factory ->
            factory.addServerCustomizers({ server ->
                server.requestLog = JettyRequestLog(logbackAccessContext)
            })
            logger.debug { "Added JettyRequestLog to $factory" }
        }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
