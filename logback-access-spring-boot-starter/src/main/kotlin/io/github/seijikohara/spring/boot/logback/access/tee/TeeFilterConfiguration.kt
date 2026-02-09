package io.github.seijikohara.spring.boot.logback.access.tee

import ch.qos.logback.access.common.AccessConstants.TEE_FILTER_EXCLUDES_PARAM
import ch.qos.logback.access.common.AccessConstants.TEE_FILTER_INCLUDES_PARAM
import ch.qos.logback.access.common.servlet.TeeFilter
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * Registers the Logback-access [TeeFilter] for capturing request/response bodies.
 *
 * Activated when `logback.access.tee-filter.enabled` is `true` and Tomcat is on the classpath.
 * TeeFilter is not supported on Jetty because the Jetty event source uses the native
 * [org.eclipse.jetty.server.RequestLog] API, which does not expose Servlet filter attributes.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBooleanProperty(prefix = "logback.access.tee-filter", name = ["enabled"])
@ConditionalOnWebApplication(type = SERVLET)
@ConditionalOnClass(name = ["org.apache.catalina.startup.Tomcat"])
internal class TeeFilterConfiguration {
    @Bean
    fun logbackAccessTeeFilter(properties: LogbackAccessProperties): FilterRegistrationBean<TeeFilter> =
        FilterRegistrationBean(TeeFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE + ORDER_OFFSET
            addUrlPatterns("/*")
            val teeFilter = properties.teeFilter
            teeFilter.includeHosts?.let { addInitParameter(TEE_FILTER_INCLUDES_PARAM, it) }
            teeFilter.excludeHosts?.let { addInitParameter(TEE_FILTER_EXCLUDES_PARAM, it) }
        }

    private companion object {
        private const val ORDER_OFFSET = 10
    }
}
