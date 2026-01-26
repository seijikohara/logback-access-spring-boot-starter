package io.github.seijikohara.logback.access.test.configuration

import brave.Tracing
import io.micrometer.tracing.Tracer
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext
import io.micrometer.tracing.brave.bridge.BraveTracer
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * The test configuration for Micrometer Tracing.
 */
@Configuration(proxyBeanMethods = false)
class TracingTestConfiguration {

    /**
     * Provides a Brave-based Tracing instance for testing.
     *
     * @return The Brave Tracing instance.
     */
    @Bean
    fun braveTracing(): Tracing = Tracing.newBuilder()
        .localServiceName("test-service")
        .build()

    /**
     * Provides a Micrometer Tracer for testing.
     *
     * @param braveTracing The Brave Tracing instance.
     * @return The Micrometer Tracer.
     */
    @Bean
    fun micrometerTracer(braveTracing: Tracing): Tracer = BraveTracer(
        braveTracing.tracer(),
        BraveCurrentTraceContext(braveTracing.currentTraceContext()),
    )

    /**
     * Provides a WebFilter that starts a trace span for reactive web requests.
     * This is needed for testing because Spring Boot's observation autoconfiguration
     * is not fully active in tests.
     *
     * @param braveTracing The Brave Tracing instance.
     * @return The test tracing WebFilter.
     */
    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    fun testTracingWebFilter(braveTracing: Tracing): WebFilter = object : WebFilter, Ordered {
        // Run before LogbackAccessNettyWebFilter (which has HIGHEST_PRECEDENCE)
        // Note: HIGHEST_PRECEDENCE + 1 is higher number = lower priority, but we want
        // to run first. Since we can't go lower than HIGHEST_PRECEDENCE, we use
        // HIGHEST_PRECEDENCE itself which means same priority - Spring will order them
        // based on bean registration order. So instead, we use HIGHEST_PRECEDENCE and
        // rely on the bean being registered before the Netty filter.
        override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

        override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
            val span = braveTracing.tracer().nextSpan().name("test-request").start()
            // Put the span in scope using try-with-resources pattern
            val scope = braveTracing.tracer().withSpanInScope(span)
            return try {
                chain.filter(exchange)
                    .doFinally {
                        scope.close()
                        span.finish()
                    }
            } catch (e: Exception) {
                scope.close()
                span.finish()
                throw e
            }
        }
    }
}
