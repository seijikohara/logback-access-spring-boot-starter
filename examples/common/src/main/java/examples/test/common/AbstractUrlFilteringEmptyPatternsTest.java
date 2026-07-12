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
 * Abstract base class for the empty URL pattern regression guard (issue #220, PR #208).
 * <p>
 * Spring's relaxed binding turns an empty property value (for example
 * {@code logback.access.filter.include-url-patterns=}, or an unset placeholder such as
 * {@code ${PATTERNS:}}) into an empty, non-null list. Starter versions up to 2.0.1
 * treated the empty include list as "match nothing" and silently dropped every access
 * event. This guards that all requests are still logged when both pattern lists bind
 * to empty lists.
 */
public abstract class AbstractUrlFilteringEmptyPatternsTest {

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
    void setUpEmptyPatternsAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        AccessEventTestUtils.reset(listAppender);
    }

    @Test
    void emptyPatternListsLogAllRequests() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getRequestURI()).isEqualTo("/api/hello");
    }
}
