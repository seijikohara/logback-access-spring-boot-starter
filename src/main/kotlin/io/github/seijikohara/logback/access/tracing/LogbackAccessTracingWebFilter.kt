package io.github.seijikohara.logback.access.tracing

import io.micrometer.tracing.Tracer
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * The tracing filter for the reactive web server.
 *
 * Extracts tracing context from Micrometer Tracing and stores it as exchange attributes
 * for use in logback-access events.
 *
 * @property tracer The Micrometer Tracer.
 */
class LogbackAccessTracingWebFilter(private val tracer: Tracer) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        tracer.currentSpan()?.context()?.let { context ->
            exchange.attributes[TRACE_ID_ATTRIBUTE] = context.traceId()
            exchange.attributes[SPAN_ID_ATTRIBUTE] = context.spanId()
            context.parentId()?.let { parentId ->
                exchange.attributes[PARENT_ID_ATTRIBUTE] = parentId
            }
        }
        return chain.filter(exchange)
    }

    companion object {

        /**
         * The attribute name for the trace ID.
         */
        const val TRACE_ID_ATTRIBUTE: String = "traceId"

        /**
         * The attribute name for the span ID.
         */
        const val SPAN_ID_ATTRIBUTE: String = "spanId"

        /**
         * The attribute name for the parent ID.
         */
        const val PARENT_ID_ATTRIBUTE: String = "parentId"
    }
}
