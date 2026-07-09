package io.github.seijikohara.spring.boot.logback.access.jetty

import io.github.oshai.kotlinlogging.KotlinLogging
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
 *
 * This class operates at the Jetty core server level, not the Servlet level.
 * TeeFilter attributes (LB_INPUT_BUFFER/LB_OUTPUT_BUFFER) set on the Servlet
 * request are not visible from this API.
 *
 * This class is auto-configured by the starter. Direct instantiation is not needed.
 */
internal class JettyRequestLog(
    private val logbackAccessContext: LogbackAccessContext,
) : RequestLog {
    /**
     * Jetty also logs rejected requests through a synthesized placeholder request, so the
     * inputs are not guaranteed to be fully populated. Extraction runs before
     * [LogbackAccessContext.emit] (which has its own guard), so wrap it here to ensure an
     * extraction failure never escapes into Jetty's request-completion path, mirroring the
     * Tomcat valve.
     */
    @Suppress("TooGenericExceptionCaught")
    override fun log(
        request: Request,
        response: Response,
    ) {
        try {
            createAccessEventData(logbackAccessContext, request, response)
                .let(::LogbackAccessEvent)
                .let(logbackAccessContext::emit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to capture Jetty access event" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
