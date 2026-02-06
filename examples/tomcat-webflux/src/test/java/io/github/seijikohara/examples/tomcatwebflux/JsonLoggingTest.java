package io.github.seijikohara.examples.tomcatwebflux;

import io.github.seijikohara.examples.test.common.AbstractJsonLoggingTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Tests for JSON logging with LogstashAccessEncoder on Tomcat WebFlux.
 * Verifies that access events are correctly captured and can be encoded as JSON.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "logback.access.config-location=classpath:logback-access-json.xml"
)
class JsonLoggingTest extends AbstractJsonLoggingTest {

    @Autowired
    LogbackAccessContext logbackAccessContext;

    @LocalServerPort
    int port;

    @Override
    protected LogbackAccessContext getLogbackAccessContext() {
        return logbackAccessContext;
    }

    @Override
    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
