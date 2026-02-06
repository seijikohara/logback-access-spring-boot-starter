package io.github.seijikohara.spring.boot.logback.access.tomcat

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import org.apache.catalina.startup.Tomcat
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Registers Tomcat-specific access logging infrastructure.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Tomcat::class)
@ConditionalOnWebApplication
internal class TomcatConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = ["logbackAccessTomcatCustomizer"])
    fun logbackAccessTomcatCustomizer(
        logbackAccessContext: LogbackAccessContext,
    ): WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory> =
        WebServerFactoryCustomizer { factory ->
            factory.addEngineValves(TomcatValve(logbackAccessContext))
            logger.debug { "Added TomcatValve to $factory" }
        }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
