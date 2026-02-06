package io.github.seijikohara.spring.boot.logback.access.jetty

import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessEvent
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.RequestLog
import org.eclipse.jetty.server.Response

/**
 * Jetty [RequestLog] that captures HTTP access events and emits them
 * through the [LogbackAccessContext].
 *
 * Jetty calls [log] after each request completes, at which point
 * all response data is available.
 */
class JettyRequestLog(
    private val logbackAccessContext: LogbackAccessContext,
) : RequestLog {
    override fun log(
        request: Request,
        response: Response,
    ): Unit =
        createAccessEventData(logbackAccessContext, request, response)
            .let(::LogbackAccessEvent)
            .let(logbackAccessContext::emit)
}
