package io.github.seijikohara.spring.boot.logback.access.security

import io.github.seijikohara.spring.boot.logback.access.REMOTE_USER_ATTR
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Captures the authenticated username from the Spring Security context
 * and stores it as a request attribute so that the access log event
 * sources can include it regardless of the underlying server.
 */
internal class SecurityFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ): Unit =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.takeIf { it.isAuthenticated }
            ?.let { request.setAttribute(REMOTE_USER_ATTR, it.name) }
            .let { filterChain.doFilter(request, response) }
}
