package io.github.seijikohara.spring.boot.logback.access.tomcat

import io.github.seijikohara.spring.boot.logback.access.AccessEventData
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
import java.util.concurrent.TimeUnit

/**
 * Creates an [AccessEventData] snapshot from a Tomcat [Request]/[Response].
 *
 * All values are extracted eagerly so the returned data is safe for
 * deferred processing without holding references to Tomcat objects.
 *
 * @param elapsedTimeNanos the processing time in nanoseconds provided by the Tomcat AccessLog contract.
 *                         Converted to milliseconds before storing.
 *                         Falls back to computing from [Request.getCoyoteRequest] start time when negative.
 */
internal fun createAccessEventData(
    context: LogbackAccessContext,
    request: Request,
    response: Response,
    requestAttributesEnabled: Boolean,
    elapsedTimeNanos: Long,
): AccessEventData =
    TomcatRequestAttributeResolver(context, requestAttributesEnabled).let { resolver ->
        AccessEventData(
            timeStamp = System.currentTimeMillis(),
            elapsedTime =
                elapsedTimeNanos
                    .takeIf { it >= 0 }
                    ?.let { TimeUnit.NANOSECONDS.toMillis(it) }
                    ?: (System.currentTimeMillis() - request.coyoteRequest.startTime).coerceAtLeast(0),
            sequenceNumber = context.accessContext.sequenceNumberGenerator?.nextSequenceNumber(),
            threadName = Thread.currentThread().name,
            serverName = resolver.resolveServerName(request),
            localPort = resolver.resolveLocalPort(request),
            remoteAddr = resolver.resolveRemoteAddr(request),
            remoteHost = resolver.resolveRemoteHost(request),
            remoteUser = resolver.resolveRemoteUser(request),
            protocol = resolver.resolveProtocol(request),
            method = request.method,
            requestURI = request.requestURI,
            queryString = request.queryString?.let { "?$it" }.orEmpty(),
            requestURL = resolver.buildRequestURL(request),
            requestHeaderMap = TomcatRequestDataExtractor.extractHeaders(request),
            cookieMap = TomcatRequestDataExtractor.extractCookies(request),
            requestParameterMap = TomcatRequestDataExtractor.extractParameters(request),
            attributeMap = TomcatRequestDataExtractor.extractAttributes(request),
            sessionID = request.getSession(false)?.id,
            requestContent = TomcatRequestDataExtractor.extractContent(request, context.properties.teeFilter),
            statusCode = response.status,
            responseHeaderMap = TomcatResponseDataExtractor.extractHeaders(response),
            contentLength = response.getBytesWritten(false),
            responseContent = TomcatResponseDataExtractor.extractContent(request, response, context.properties.teeFilter),
        )
    }
