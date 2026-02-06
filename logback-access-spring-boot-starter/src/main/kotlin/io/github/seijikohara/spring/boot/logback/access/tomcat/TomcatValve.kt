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
 */
class TomcatValve(
    private val logbackAccessContext: LogbackAccessContext,
) : ValveBase(true),
    AccessLog {
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

    override fun log(
        request: Request,
        response: Response,
        time: Long,
    ): Unit =
        createAccessEventData(logbackAccessContext, request, response, requestAttributesEnabled)
            .let { data -> LogbackAccessEvent(data, request, response) }
            .let(logbackAccessContext::emit)

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
