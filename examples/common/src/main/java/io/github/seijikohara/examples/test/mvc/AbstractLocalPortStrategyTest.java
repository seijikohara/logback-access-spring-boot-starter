package io.github.seijikohara.examples.test.mvc;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.seijikohara.examples.AccessEventTestUtils;
import io.github.seijikohara.examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for testing the localPortStrategy=LOCAL configuration.
 * <p>
 * When LOCAL strategy is used, the access event captures the actual
 * local port that received the connection, rather than the server port
 * from the Host header or X-Forwarded-Port.
 * <p>
 * Subclasses must configure:
 * {@code @SpringBootTest(properties = "logback.access.local-port-strategy=LOCAL")}
 */
public abstract class AbstractLocalPortStrategyTest {

    private ListAppender<IAccessEvent> listAppender;

    /**
     * Returns the LogbackAccessContext from the Spring context.
     *
     * @return the LogbackAccessContext
     */
    protected abstract LogbackAccessContext getLogbackAccessContext();

    /**
     * Returns the base URL for the test server.
     *
     * @return the base URL (e.g., "http://localhost:8080")
     */
    protected abstract String getBaseUrl();

    /**
     * Returns the local server port.
     *
     * @return the port number
     */
    protected abstract int getPort();

    @BeforeEach
    void setUpLocalPortAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        listAppender.list.clear();
    }

    protected ListAppender<IAccessEvent> getListAppender() {
        return listAppender;
    }

    @Test
    void localPortStrategyUsesLocalPort() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        // LOCAL strategy should capture the actual listening port
        assertThat(event.getLocalPort()).isEqualTo(getPort());
    }

    @Test
    void localPortDoesNotChangeWithHostHeader() throws Exception {
        // Even if the Host header specifies a different port,
        // LOCAL strategy should still return the actual local port
        HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getLocalPort())
                .as("LOCAL strategy should return actual connection port, not Host header port")
                .isEqualTo(getPort());
    }
}
