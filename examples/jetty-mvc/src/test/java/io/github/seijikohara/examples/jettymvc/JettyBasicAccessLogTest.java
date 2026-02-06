package io.github.seijikohara.examples.jettymvc;

import io.github.seijikohara.examples.AbstractBasicAccessLogTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Basic access log tests for Jetty + MVC.
 * Extends AbstractBasicAccessLogTest for common HTTP method tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class JettyBasicAccessLogTest extends AbstractBasicAccessLogTest {

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
