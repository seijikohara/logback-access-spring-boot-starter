package io.github.seijikohara.spring.boot.logback.access.tomcat

import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response

class TomcatValveSpec :
    FunSpec({
        test("log does not propagate exceptions thrown while extracting access-event data") {
            val context = mockk<LogbackAccessContext>(relaxed = true)
            val request =
                mockk<Request>(relaxed = true) {
                    // Simulate a malformed/early-rejected request whose getters fail.
                    every { method } throws RuntimeException("malformed request")
                }
            val response = mockk<Response>(relaxed = true)
            val valve = TomcatValve(context)

            shouldNotThrowAny { valve.log(request, response, 0L) }
        }
    })
