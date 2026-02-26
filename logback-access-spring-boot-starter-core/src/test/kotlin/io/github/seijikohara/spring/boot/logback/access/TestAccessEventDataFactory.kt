package io.github.seijikohara.spring.boot.logback.access

object TestAccessEventDataFactory {
    fun createTestData(requestParameterMap: Map<String, List<String>> = mapOf("foo" to listOf("bar"))): AccessEventData =
        AccessEventData(
            timeStamp = 1000L,
            elapsedTime = 50L,
            sequenceNumber = 1L,
            threadName = "main",
            serverName = "localhost",
            localPort = 8080,
            remoteAddr = "127.0.0.1",
            remoteHost = "localhost",
            remoteUser = "testuser",
            protocol = "HTTP/1.1",
            method = "GET",
            requestURI = "/test",
            queryString = "?foo=bar",
            requestURL = "GET /test?foo=bar HTTP/1.1",
            requestHeaderMap = mapOf("Host" to "localhost"),
            cookieMap = mapOf("session" to "abc123"),
            requestParameterMap = requestParameterMap,
            attributeMap = mapOf("attr1" to "value1"),
            sessionID = "session123",
            requestContent = "request body",
            statusCode = 200,
            responseHeaderMap = mapOf("Content-Type" to "text/plain"),
            contentLength = 13L,
            responseContent = "response body",
        )

    fun createMinimalData(): AccessEventData =
        AccessEventData(
            timeStamp = 1000L,
            elapsedTime = null,
            sequenceNumber = null,
            threadName = "main",
            serverName = null,
            localPort = 8080,
            remoteAddr = "127.0.0.1",
            remoteHost = "127.0.0.1",
            remoteUser = null,
            protocol = "HTTP/1.1",
            method = "GET",
            requestURI = null,
            queryString = "",
            requestURL = "GET / HTTP/1.1",
            requestHeaderMap = emptyMap(),
            cookieMap = emptyMap(),
            requestParameterMap = emptyMap(),
            attributeMap = emptyMap(),
            sessionID = null,
            requestContent = null,
            statusCode = 200,
            responseHeaderMap = emptyMap(),
            contentLength = 0L,
            responseContent = null,
        )
}
