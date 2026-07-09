package io.github.seijikohara.spring.boot.logback.access.tomcat

import org.apache.catalina.connector.Connector
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
import java.nio.ByteBuffer
import org.apache.coyote.OutputBuffer as CoyoteOutputBuffer
import org.apache.coyote.Request as CoyoteRequest
import org.apache.coyote.Response as CoyoteResponse

// Mimic the objects Tomcat passes to AccessLog.log for early-rejected requests
// (AbstractProcessor.logAccess): a connector Request over an empty coyote request whose
// getters return null. Real Tomcat objects are used because a relaxed mock returns ""
// instead of null and would mask platform-type null-check failures.

// A bare Connector skips lifecycle init; set parseBodyMethods so parameter parsing
// sees the initialized default, as it would on a running connector.
internal fun earlyRejectedRequest(): Request = Request(Connector("HTTP/1.1").apply { parseBodyMethods = "POST" }, CoyoteRequest())

// A processor always installs an output buffer before logging; a bare coyote response
// has none, so install a no-op one for getBytesWritten().
internal fun earlyRejectedResponse(): Response =
    Response(
        CoyoteResponse().apply {
            status = 400
            setOutputBuffer(
                object : CoyoteOutputBuffer {
                    override fun doWrite(chunk: ByteBuffer): Int = 0

                    override fun getBytesWritten(): Long = 0L
                },
            )
        },
    )
