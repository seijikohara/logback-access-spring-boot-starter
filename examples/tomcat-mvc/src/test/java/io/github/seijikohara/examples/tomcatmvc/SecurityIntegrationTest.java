package io.github.seijikohara.examples.tomcatmvc;

import io.github.seijikohara.examples.AbstractSecurityTest;
import io.github.seijikohara.examples.AccessEventTestUtils;
import io.github.seijikohara.examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Spring Security integration with access logging on Tomcat.
 * Extends AbstractSecurityTest for common security tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest extends AbstractSecurityTest {

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

    // Tomcat-specific additional tests

    @Test
    void adminUserIsLoggedInAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.getWithBasicAuth(
                getBaseUrl() + "/api/secure",
                "admin",
                "admin");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(getListAppender());

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getRemoteUser()).isEqualTo("admin");
    }
}
