package io.github.seijikohara.spring.boot.logback.access.tomcat

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessEvent
import org.apache.catalina.AccessLog
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
import org.apache.catalina.valves.RemoteIpValve
import org.apache.catalina.valves.ValveBase

/**
 * Tomcat [ValveBase] that captures HTTP access events and emits them
 * through the [LogbackAccessContext].
 *
 * Implements [AccessLog] so that Tomcat invokes [log] after each request
 * completes, at which point all response data is available.
 *
 * This class is auto-configured by the starter. Direct instantiation is not needed.
 */
internal class TomcatValve(
    private val logbackAccessContext: LogbackAccessContext,
) : ValveBase(true),
    AccessLog {
    @Volatile
    private var requestAttributesEnabledValue: Boolean = false

    override fun getRequestAttributesEnabled(): Boolean = requestAttributesEnabledValue

    override fun setRequestAttributesEnabled(value: Boolean) = run { requestAttributesEnabledValue = value }

    override fun initInternal(): Unit =
        super.initInternal().also {
            requestAttributesEnabled =
                logbackAccessContext.properties.tomcat.requestAttributesEnabled
                    ?: container.pipeline.valves.any { it is RemoteIpValve }
            logger.debug { "Initialized TomcatValve (requestAttributesEnabled=$requestAttributesEnabled)" }
        }

    override fun invoke(
        request: Request,
        response: Response,
    ): Unit = next.invoke(request, response)

    /**
     * Tomcat's [AccessLog] contract requires implementations to tolerate null or
     * malformed request/response objects from early-rejected requests. Extraction
     * runs before [LogbackAccessContext.emit] (which has its own guard), so wrap it
     * here to ensure an extraction failure never escapes into the Tomcat engine.
     */
    @Suppress("TooGenericExceptionCaught")
    override fun log(
        request: Request,
        response: Response,
        time: Long,
    ) {
        try {
            createAccessEventData(logbackAccessContext, request, response, requestAttributesEnabled, time)
                .let(::LogbackAccessEvent)
                .let(logbackAccessContext::emit)
        } catch (e: Exception) {
            logger.error(e) { "Failed to capture Tomcat access event" }
        }
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
