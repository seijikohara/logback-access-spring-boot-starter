package examples.test.common;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for testing access logging of requests the server rejects before
 * normal processing.
 * <p>
 * Servers hand such requests to the access log with unparsed or synthesized attributes:
 * Tomcat passes an unparsed request whose getters return null, while Jetty synthesizes a
 * placeholder request. The starter must emit an access event instead of failing on the
 * missing values (issue #205).
 */
public abstract class AbstractMalformedRequestAccessLogTest {

    private ListAppender<IAccessEvent> listAppender;

    /**
     * Returns the LogbackAccessContext from the Spring context.
     *
     * @return the LogbackAccessContext
     */
    protected abstract LogbackAccessContext getLogbackAccessContext();

    /**
     * Returns the local server port.
     *
     * @return the port number
     */
    protected abstract int getPort();

    /**
     * Returns the HTTP method the server reports for an unparseable request line.
     *
     * @return "-" on Tomcat (unparsed request, NA fallback); "BAD" on Jetty (synthesized
     *         placeholder request)
     */
    protected abstract String getExpectedMalformedMethod();

    @BeforeEach
    void setUpMalformedRequestAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        AccessEventTestUtils.reset(listAppender);
    }

    @Test
    void malformedRequestLineEmitsAccessEventWith400Status() throws Exception {
        final var response = HttpClientTestUtils.sendRawRequest("localhost", getPort(), "GARBAGE\r\n\r\n");

        assertThat(response).startsWith("HTTP/1.1 400");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getStatusCode()).isEqualTo(400);
            softly.assertThat(event.getMethod()).isEqualTo(getExpectedMalformedMethod());
        });
    }
}
