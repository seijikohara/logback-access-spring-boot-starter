package io.github.seijikohara.spring.boot.logback.access.jetty

import io.github.seijikohara.spring.boot.logback.access.AccessEventData
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import java.util.concurrent.TimeUnit.NANOSECONDS

/**
 * Creates an [AccessEventData] snapshot from a Jetty [Request]/[Response].
 *
 * All values are extracted eagerly so the returned data is safe for
 * deferred processing without holding references to Jetty objects.
 *
 * Jetty-specific limitations:
 * - [AccessEventData.remoteHost] equals [AccessEventData.remoteAddr] (no reverse DNS lookup)
 * - [AccessEventData.requestParameterMap] is always empty to avoid consuming the request body
 * - [AccessEventData.requestContent] and [AccessEventData.responseContent] are always null
 *   (TeeFilter is not supported on the Jetty native RequestLog API)
 */
internal fun createAccessEventData(
    context: LogbackAccessContext,
    request: Request,
    response: Response,
): AccessEventData =
    System.currentTimeMillis().let { now ->
        AccessEventData(
            timeStamp = now,
            elapsedTime = request.beginNanoTime.takeIf { it > 0 }?.let { NANOSECONDS.toMillis(System.nanoTime() - it) },
            sequenceNumber = context.accessContext.sequenceNumberGenerator?.nextSequenceNumber(),
            threadName = Thread.currentThread().name,
            serverName = Request.getServerName(request),
            localPort = JettyRequestDataExtractor.resolveLocalPort(context, request),
            remoteAddr = Request.getRemoteAddr(request),
            remoteHost = Request.getRemoteAddr(request),
            remoteUser = JettyRequestDataExtractor.resolveRemoteUser(request),
            protocol = request.connectionMetaData.protocol,
            method = request.method,
            requestURI = request.httpURI.path,
            queryString =
                request.httpURI.query
                    ?.let { "?$it" }
                    .orEmpty(),
            requestURL = JettyRequestDataExtractor.buildRequestURL(request),
            requestHeaderMap = JettyRequestDataExtractor.extractHeaders(request),
            cookieMap = JettyRequestDataExtractor.extractCookies(request),
            requestParameterMap = emptyMap(),
            attributeMap = JettyRequestDataExtractor.extractAttributes(request),
            sessionID = request.getSession(false)?.id,
            requestContent = null,
            statusCode = response.status,
            responseHeaderMap = JettyResponseDataExtractor.extractHeaders(response),
            contentLength = Response.getContentBytesWritten(response),
            responseContent = null,
        )
    }
