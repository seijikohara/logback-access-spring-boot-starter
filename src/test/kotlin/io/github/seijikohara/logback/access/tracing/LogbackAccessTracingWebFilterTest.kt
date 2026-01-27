package io.github.seijikohara.logback.access.tracing

import io.kotest.matchers.shouldBe
import io.micrometer.tracing.Span
import io.micrometer.tracing.TraceContext
import io.micrometer.tracing.Tracer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

/**
 * Tests for [LogbackAccessTracingWebFilter].
 */
class LogbackAccessTracingWebFilterTest {

    private lateinit var tracer: Tracer
    private lateinit var filter: LogbackAccessTracingWebFilter
    private lateinit var exchange: MockServerWebExchange
    private lateinit var chain: WebFilterChain

    @BeforeEach
    fun setUp() {
        tracer = mockk()
        filter = LogbackAccessTracingWebFilter(tracer)

        val request = MockServerHttpRequest.get("/test").build()
        exchange = MockServerWebExchange.from(request)

        chain = mockk()
        every { chain.filter(any()) } returns Mono.empty()
    }

    @Test
    fun `Sets trace context attributes with parent ID`() {
        val traceContext = mockk<TraceContext>()
        every { traceContext.traceId() } returns "trace-123"
        every { traceContext.spanId() } returns "span-456"
        every { traceContext.parentId() } returns "parent-789"

        val currentSpan = mockk<Span>()
        every { currentSpan.`context`() } returns traceContext
        every { tracer.currentSpan() } returns currentSpan

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete()

        exchange.attributes[LogbackAccessTracingWebFilter.TRACE_ID_ATTRIBUTE] shouldBe "trace-123"
        exchange.attributes[LogbackAccessTracingWebFilter.SPAN_ID_ATTRIBUTE] shouldBe "span-456"
        exchange.attributes[LogbackAccessTracingWebFilter.PARENT_ID_ATTRIBUTE] shouldBe "parent-789"

        verify { chain.filter(exchange) }
    }

    @Test
    fun `Sets trace context attributes without parent ID`() {
        val traceContext = mockk<TraceContext>()
        every { traceContext.traceId() } returns "trace-abc"
        every { traceContext.spanId() } returns "span-def"
        every { traceContext.parentId() } returns null

        val currentSpan = mockk<Span>()
        every { currentSpan.`context`() } returns traceContext
        every { tracer.currentSpan() } returns currentSpan

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete()

        exchange.attributes[LogbackAccessTracingWebFilter.TRACE_ID_ATTRIBUTE] shouldBe "trace-abc"
        exchange.attributes[LogbackAccessTracingWebFilter.SPAN_ID_ATTRIBUTE] shouldBe "span-def"
        exchange.attributes.containsKey(LogbackAccessTracingWebFilter.PARENT_ID_ATTRIBUTE) shouldBe false
    }

    @Test
    fun `Does not set attributes when no current span`() {
        every { tracer.currentSpan() } returns null

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete()

        exchange.attributes.containsKey(LogbackAccessTracingWebFilter.TRACE_ID_ATTRIBUTE) shouldBe false
        exchange.attributes.containsKey(LogbackAccessTracingWebFilter.SPAN_ID_ATTRIBUTE) shouldBe false
        exchange.attributes.containsKey(LogbackAccessTracingWebFilter.PARENT_ID_ATTRIBUTE) shouldBe false

        verify { chain.filter(exchange) }
    }

    @Test
    fun `Has correct attribute names`() {
        LogbackAccessTracingWebFilter.TRACE_ID_ATTRIBUTE shouldBe "traceId"
        LogbackAccessTracingWebFilter.SPAN_ID_ATTRIBUTE shouldBe "spanId"
        LogbackAccessTracingWebFilter.PARENT_ID_ATTRIBUTE shouldBe "parentId"
    }
}
