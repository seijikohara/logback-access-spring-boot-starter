package io.github.seijikohara.spring.boot.logback.access.tomcat

import io.github.seijikohara.spring.boot.logback.access.AccessEventData
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response

/**
 * Creates an [AccessEventData] snapshot from a Tomcat [Request]/[Response].
 *
 * All values are extracted eagerly so the returned data is safe for
 * deferred processing without holding references to Tomcat objects.
 */
internal fun createAccessEventData(
    context: LogbackAccessContext,
    request: Request,
    response: Response,
    requestAttributesEnabled: Boolean,
): AccessEventData =
    TomcatRequestAttributeResolver(context, requestAttributesEnabled).let { resolver ->
        System.currentTimeMillis().let { now ->
            AccessEventData(
                timeStamp = now,
                elapsedTime = now - request.coyoteRequest.startTime,
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
                requestContent = TomcatRequestDataExtractor.extractContent(request),
                statusCode = response.status,
                responseHeaderMap = TomcatResponseDataExtractor.extractHeaders(response),
                contentLength = response.getBytesWritten(false),
                responseContent = TomcatResponseDataExtractor.extractContent(request, response),
            )
        }
    }
