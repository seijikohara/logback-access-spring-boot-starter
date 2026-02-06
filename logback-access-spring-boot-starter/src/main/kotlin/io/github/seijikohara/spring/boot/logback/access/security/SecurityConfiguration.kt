package io.github.seijikohara.spring.boot.logback.access.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.security.web.SecurityFilterChain

/**
 * Registers the [SecurityFilter] when Spring Security is on the classpath.
 *
 * The filter is ordered after the Spring Security filter chain so that
 * the authentication context is available when the filter executes.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SecurityFilterChain::class)
@ConditionalOnWebApplication(type = SERVLET)
internal class SecurityConfiguration {
    @Bean
    fun logbackAccessSecurityFilter(): FilterRegistrationBean<SecurityFilter> =
        FilterRegistrationBean(SecurityFilter()).apply {
            order = Ordered.LOWEST_PRECEDENCE - ORDER_OFFSET
            addUrlPatterns("/*")
        }

    private companion object {
        private const val ORDER_OFFSET = 10
    }
}
