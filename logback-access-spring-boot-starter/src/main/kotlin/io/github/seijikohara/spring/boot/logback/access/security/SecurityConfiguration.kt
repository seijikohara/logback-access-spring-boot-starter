package io.github.seijikohara.spring.boot.logback.access.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.security.authentication.AuthenticationTrustResolver
import org.springframework.security.authentication.AuthenticationTrustResolverImpl
import org.springframework.security.web.SecurityFilterChain

/**
 * Registers the [SecurityFilter] when Spring Security is on the classpath.
 *
 * The filter is ordered after the Spring Security filter chain so that
 * the authentication context is available when the filter executes.
 *
 * A default [AuthenticationTrustResolver] is provided to distinguish
 * anonymous tokens from genuinely authenticated users. Users can
 * override this bean to customize the trust resolution logic.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SecurityFilterChain::class)
@ConditionalOnWebApplication(type = SERVLET)
internal class SecurityConfiguration {
    @Bean
    @ConditionalOnMissingBean(AuthenticationTrustResolver::class)
    fun logbackAccessAuthenticationTrustResolver(): AuthenticationTrustResolver = AuthenticationTrustResolverImpl()

    @Bean
    fun logbackAccessSecurityFilter(trustResolver: AuthenticationTrustResolver): FilterRegistrationBean<SecurityFilter> =
        FilterRegistrationBean(SecurityFilter(trustResolver)).apply {
            order = Ordered.LOWEST_PRECEDENCE - ORDER_OFFSET
            addUrlPatterns("/*")
        }

    private companion object {
        private const val ORDER_OFFSET = 10
    }
}
