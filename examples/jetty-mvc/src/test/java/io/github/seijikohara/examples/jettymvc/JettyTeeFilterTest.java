package io.github.seijikohara.examples.jettymvc;

import io.github.seijikohara.examples.test.mvc.AbstractTeeFilterTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for TeeFilter request/response body capture functionality on Jetty.
 * TeeFilter is enabled via logback.access.tee-filter.enabled=true.
 * <p>
 * <strong>Note:</strong> This test is disabled because TeeFilter does not work on Jetty 12.
 * Jetty 12's RequestLog API operates at the core server level, separate from the Servlet API.
 * TeeFilter sets LB_INPUT_BUFFER/LB_OUTPUT_BUFFER attributes on the Servlet request,
 * but these attributes are not visible to the RequestLog which uses Jetty's native Request object.
 * This is a known limitation of the Jetty 12 architecture.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("teefilter")
@Disabled("TeeFilter does not work on Jetty 12 - RequestLog API cannot access Servlet request attributes")
class JettyTeeFilterTest extends AbstractTeeFilterTest {

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
