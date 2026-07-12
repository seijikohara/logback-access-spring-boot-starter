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
 * Abstract base class for combined URL include/exclude filtering tests.
 * <p>
 * One context configured with {@code include-url-patterns=/api/.*} and
 * {@code exclude-url-patterns=/api/health} covers all four branch outcomes of the
 * include/exclude matching: include hit, include miss, exclude hit, and exclude miss.
 * The no-filter default (both properties unset) is covered by the basic access log tests.
 * <p>
 * Subclasses bind the filter properties via {@code @SpringBootTest} and implement the
 * abstract accessors.
 */
public abstract class AbstractUrlFilteringCombinedTest {

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
    void setUpUrlFilteringAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        AccessEventTestUtils.reset(listAppender);
    }

    @Test
    void includedButNotExcludedUrlEmitsEvent() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getRequestURI()).isEqualTo("/api/hello");
    }

    @Test
    void excludedUrlDoesNotEmitEvent() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/health");

        AccessEventTestUtils.awaitNoEvents(listAppender);
    }

    @Test
    void urlOutsideIncludePatternsDoesNotEmitEvent() throws Exception {
        // A 404 response emits an access event by default (see the basic tests), so an
        // absent event here proves the include-pattern miss suppressed it.
        HttpClientTestUtils.get(getBaseUrl() + "/nonexistent");

        AccessEventTestUtils.awaitNoEvents(listAppender);
    }
}
