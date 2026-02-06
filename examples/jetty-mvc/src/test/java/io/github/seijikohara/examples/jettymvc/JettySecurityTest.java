package io.github.seijikohara.examples.jettymvc;

import io.github.seijikohara.examples.AbstractSecurityTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Tests for Spring Security integration with Jetty access logging.
 * Extends AbstractSecurityTest for common security tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class JettySecurityTest extends AbstractSecurityTest {

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

    @Override
    protected String getAuthenticatedUsername() {
        return "user";
    }

    @Override
    protected String getAuthenticatedPassword() {
        return "password";
    }
}
