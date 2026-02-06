package io.github.seijikohara.examples.tomcatmvc;

import io.github.seijikohara.examples.AbstractBasicAccessLogTest;
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
 * Basic access log tests for Tomcat + MVC.
 * Extends AbstractBasicAccessLogTest for common HTTP method tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BasicAccessLogTest extends AbstractBasicAccessLogTest {

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

    // Tomcat-specific additional tests

    @Test
    void getRequestWithPathVariableEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/items/42");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(getListAppender());

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getRequestURI()).isEqualTo("/api/items/42");
    }

    @Test
    void eventCapturesRequestHeaders() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        final var events = AccessEventTestUtils.awaitEvents(getListAppender());

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getRequestHeaderMap()).containsKey("Host");
    }
}
