package io.github.seijikohara.spring.boot.logback.access.tomcat

import io.github.seijikohara.spring.boot.logback.access.LocalPortStrategy
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import io.github.seijikohara.spring.boot.logback.access.REMOTE_USER_ATTR
import org.apache.catalina.AccessLog.PROTOCOL_ATTRIBUTE
import org.apache.catalina.AccessLog.REMOTE_ADDR_ATTRIBUTE
import org.apache.catalina.AccessLog.REMOTE_HOST_ATTRIBUTE
import org.apache.catalina.AccessLog.SERVER_NAME_ATTRIBUTE
import org.apache.catalina.AccessLog.SERVER_PORT_ATTRIBUTE
import org.apache.catalina.connector.Request

/**
 * Resolves request attributes from Tomcat [Request], supporting AccessLog valve attributes.
 *
 * When [requestAttributesEnabled] is true, values are first looked up from request attributes
 * (set by RemoteIpValve or similar) before falling back to the direct request values.
 */
internal class TomcatRequestAttributeResolver(
    private val context: LogbackAccessContext,
    private val requestAttributesEnabled: Boolean,
) {
    fun resolveServerName(request: Request): String = request.accessLogAttr<String>(SERVER_NAME_ATTRIBUTE) ?: request.serverName

    fun resolveLocalPort(request: Request): Int =
        when (context.properties.localPortStrategy) {
            LocalPortStrategy.LOCAL -> request.localPort
            LocalPortStrategy.SERVER -> request.accessLogAttr<Int>(SERVER_PORT_ATTRIBUTE) ?: request.serverPort
        }

    fun resolveRemoteAddr(request: Request): String = request.accessLogAttr<String>(REMOTE_ADDR_ATTRIBUTE) ?: request.remoteAddr

    fun resolveRemoteHost(request: Request): String = request.accessLogAttr<String>(REMOTE_HOST_ATTRIBUTE) ?: request.remoteHost

    fun resolveRemoteUser(request: Request): String? = request.getAttribute(REMOTE_USER_ATTR) as? String ?: request.remoteUser

    fun resolveProtocol(request: Request): String = request.accessLogAttr<String>(PROTOCOL_ATTRIBUTE) ?: request.protocol

    fun buildRequestURL(request: Request): String =
        "${request.method} ${request.requestURI}${request.queryString?.let { "?$it" }.orEmpty()} ${resolveProtocol(request)}"

    private inline fun <reified T> Request.accessLogAttr(name: String): T? =
        if (requestAttributesEnabled) getAttribute(name) as? T else null
}
