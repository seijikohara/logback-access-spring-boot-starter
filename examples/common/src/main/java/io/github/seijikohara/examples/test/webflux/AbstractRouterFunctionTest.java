package io.github.seijikohara.examples.test.webflux;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.seijikohara.examples.AccessEventTestUtils;
import io.github.seijikohara.examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for router function endpoints access logging tests.
 * <p>
 * Subclasses must provide server-specific configuration via Spring annotations
 * and implement abstract methods to provide test context.
 */
public abstract class AbstractRouterFunctionTest {

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
    void setUpRouterFunctionAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        listAppender.list.clear();
    }

    protected ListAppender<IAccessEvent> getListAppender() {
        return listAppender;
    }

    @Test
    void routerFunctionEndpointEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/functional/hello");

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(200);
            softly.assertThat(response.body()).isEqualTo("Hello from Router Function!");
        });

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getMethod()).isEqualTo("GET");
            softly.assertThat(event.getRequestURI()).isEqualTo("/functional/hello");
            softly.assertThat(event.getStatusCode()).isEqualTo(200);
        });
    }

    @Test
    void routerFunctionWithQueryParamEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.getWithQuery(getBaseUrl() + "/functional/greet", "name=Functional");

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.statusCode()).isEqualTo(200);
            softly.assertThat(response.body()).isEqualTo("Hello, Functional!");
        });

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getRequestURI()).isEqualTo("/functional/greet");
            softly.assertThat(event.getQueryString()).isEqualTo("?name=Functional");
        });
    }
}
