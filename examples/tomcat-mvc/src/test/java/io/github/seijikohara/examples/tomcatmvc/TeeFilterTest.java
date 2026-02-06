package io.github.seijikohara.examples.tomcatmvc;

import io.github.seijikohara.examples.test.mvc.AbstractTeeFilterTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for TeeFilter request/response body capture functionality on Tomcat.
 * TeeFilter is enabled via logback.access.tee-filter.enabled=true.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("teefilter")
class TeeFilterTest extends AbstractTeeFilterTest {

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
