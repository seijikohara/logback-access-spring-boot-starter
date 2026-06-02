package examples.test.webflux;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for testing the localPortStrategy=LOCAL configuration
 * on reactive (WebFlux) servers.
 * <p>
 * When LOCAL strategy is used, the access event captures the actual
 * local port that received the connection, rather than the server port
 * from the Host header or X-Forwarded-Port.
 * <p>
 * Subclasses must configure:
 * {@code @SpringBootTest(properties = "logback.access.local-port-strategy=LOCAL")}
 */
public abstract class AbstractReactiveLocalPortStrategyTest {

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
        AccessEventTestUtils.reset(listAppender);
    }

    /**
     * Returns the ListAppender for use in subclass tests.
     *
     * @return the ListAppender
     */
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
        // Send a Host header advertising a divergent port. The LOCAL strategy must still report the
        // actual connection port, not the port from the Host header.
        HttpClientTestUtils.getWithHostHeader("localhost", getPort(), "/api/hello", "example.invalid:1");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getLocalPort())
                .as("LOCAL strategy should return the actual connection port, not the Host header port")
                .isEqualTo(getPort());
    }
}
