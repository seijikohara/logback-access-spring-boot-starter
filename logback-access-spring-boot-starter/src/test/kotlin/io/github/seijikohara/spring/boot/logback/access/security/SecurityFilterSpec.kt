package io.github.seijikohara.spring.boot.logback.access.security

import io.github.seijikohara.spring.boot.logback.access.AccessEventData.Companion.REMOTE_USER_ATTR
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationTrustResolver
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextImpl

class SecurityFilterSpec :
    FunSpec({
        lateinit var request: HttpServletRequest
        lateinit var response: HttpServletResponse
        lateinit var filterChain: FilterChain

        beforeEach {
            request =
                mockk(relaxed = true) {
                    every { getAttribute(any()) } returns null
                }
            response = mockk(relaxed = true)
            filterChain = mockk(relaxed = true)
            SecurityContextHolder.clearContext()
        }

        afterEach {
            SecurityContextHolder.clearContext()
        }

        context("authenticated user") {
            test("sets REMOTE_USER_ATTR with username") {
                setAuthentication(
                    UsernamePasswordAuthenticationToken("testuser", "password", emptyList()),
                )
                val filter = SecurityFilter()

                filter.doFilter(request, response, filterChain)

                verify { request.setAttribute(REMOTE_USER_ATTR, "testuser") }
                verify { filterChain.doFilter(request, response) }
            }
        }

        context("anonymous authentication") {
            test("does not set REMOTE_USER_ATTR for AnonymousAuthenticationToken") {
                setAuthentication(
                    AnonymousAuthenticationToken(
                        "key",
                        "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"),
                    ),
                )
                val filter = SecurityFilter()

                filter.doFilter(request, response, filterChain)

                verify(exactly = 0) { request.setAttribute(REMOTE_USER_ATTR, any()) }
                verify { filterChain.doFilter(request, response) }
            }
        }

        context("no authentication") {
            test("does not set REMOTE_USER_ATTR when authentication is null") {
                SecurityContextHolder.setContext(SecurityContextImpl())
                val filter = SecurityFilter()

                filter.doFilter(request, response, filterChain)

                verify(exactly = 0) { request.setAttribute(REMOTE_USER_ATTR, any()) }
                verify { filterChain.doFilter(request, response) }
            }
        }

        context("custom AuthenticationTrustResolver") {
            test("delegates anonymous check to provided trust resolver") {
                val auth = UsernamePasswordAuthenticationToken("testuser", "password", emptyList())
                setAuthentication(auth)
                val customResolver =
                    mockk<AuthenticationTrustResolver> {
                        every { isAnonymous(auth) } returns true
                    }
                val filter = SecurityFilter(customResolver)

                filter.doFilter(request, response, filterChain)

                verify(exactly = 0) { request.setAttribute(REMOTE_USER_ATTR, any()) }
                verify { customResolver.isAnonymous(auth) }
                verify { filterChain.doFilter(request, response) }
            }
        }

        context("filter chain invocation") {
            test("always invokes filter chain regardless of authentication state") {
                SecurityContextHolder.clearContext()
                val filter = SecurityFilter()

                filter.doFilter(request, response, filterChain)

                verify { filterChain.doFilter(request, response) }
            }
        }
    }) {
    companion object {
        private fun setAuthentication(authentication: Authentication) {
            val context = SecurityContextImpl()
            context.authentication = authentication
            SecurityContextHolder.setContext(context)
        }
    }
}
