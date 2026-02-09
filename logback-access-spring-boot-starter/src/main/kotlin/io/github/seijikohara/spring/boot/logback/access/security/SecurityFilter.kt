package io.github.seijikohara.spring.boot.logback.access.security

import io.github.seijikohara.spring.boot.logback.access.AccessEventData.Companion.REMOTE_USER_ATTR
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AuthenticationTrustResolver
import org.springframework.security.authentication.AuthenticationTrustResolverImpl
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Captures the authenticated username from the Spring Security context
 * and stores it as a request attribute so that the access log event
 * sources can include it regardless of the underlying server.
 *
 * Anonymous authentication tokens are excluded using [AuthenticationTrustResolver],
 * so only genuinely authenticated users appear in the `%u` log variable.
 */
internal class SecurityFilter(
    private val trustResolver: AuthenticationTrustResolver = AuthenticationTrustResolverImpl(),
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ): Unit =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.takeIf { it.isAuthenticated && !trustResolver.isAnonymous(it) }
            ?.let { request.setAttribute(REMOTE_USER_ATTR, it.name) }
            .let { filterChain.doFilter(request, response) }
}
