package io.github.seijikohara.examples.jettywebflux;

import io.github.seijikohara.examples.test.webflux.AbstractReactiveAccessLogTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Tests for reactive access logging on Jetty.
 * Extends AbstractReactiveAccessLogTest for common reactive tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ReactiveAccessLogTest extends AbstractReactiveAccessLogTest {

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
