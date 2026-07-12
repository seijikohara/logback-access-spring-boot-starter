package examples.test.common;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for RemoteIpValve forwarded-header tests (Tomcat only).
 * <p>
 * RemoteIpValve rewrites the remote address during request processing and restores the
 * original before access logging runs, publishing the forwarded values as request
 * attributes instead. The starter auto-enables request-attribute resolution when it
 * detects RemoteIpValve, so the logged event must report the forwarded address. This is
 * the end-to-end coverage of the request-attribute resolution path.
 * <p>
 * Subclasses must set {@code server.tomcat.remoteip.remote-ip-header} and an
 * {@code internal-proxies} pattern that trusts the test client's loopback address.
 */
public abstract class AbstractForwardedHeaderTest {

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

    @BeforeEach
    void setUpForwardedHeaderAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        AccessEventTestUtils.reset(listAppender);
    }

    @Test
    void forwardedForHeaderOverridesRemoteAddress() throws Exception {
        final var response = HttpClientTestUtils.getWithHeader(
                getBaseUrl() + "/api/hello",
                "X-Forwarded-For",
                "203.0.113.7");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getRemoteAddr()).isEqualTo("203.0.113.7");
    }
}
