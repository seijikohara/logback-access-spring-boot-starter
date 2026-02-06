package io.github.seijikohara.examples;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for basic access log tests.
 * Subclasses must provide server-specific configuration via Spring annotations
 * and implement abstract methods to provide test context.
 */
public abstract class AbstractBasicAccessLogTest {

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
    void setUpAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        listAppender.list.clear();
    }

    protected ListAppender<IAccessEvent> getListAppender() {
        return listAppender;
    }

    @Test
    void getRequestEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getMethod()).isEqualTo("GET");
            softly.assertThat(event.getRequestURI()).isEqualTo("/api/hello");
            softly.assertThat(event.getStatusCode()).isEqualTo(200);
            softly.assertThat(event.getProtocol()).contains("HTTP");
        });
    }

    @Test
    void getRequestWithQueryStringEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.getWithQuery(getBaseUrl() + "/api/greet", "name=Test");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getQueryString()).isEqualTo("?name=Test");
    }

    @Test
    void postRequestEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.post(getBaseUrl() + "/api/echo", "{\"message\":\"Hello\"}");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getMethod()).isEqualTo("POST");
            softly.assertThat(event.getRequestURI()).isEqualTo("/api/echo");
        });
    }

    @Test
    void putRequestEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.put(getBaseUrl() + "/api/items/1", "{\"name\":\"Updated\"}");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getMethod()).isEqualTo("PUT");
            softly.assertThat(event.getRequestURI()).isEqualTo("/api/items/1");
        });
    }

    @Test
    void deleteRequestEmitsAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.delete(getBaseUrl() + "/api/items/1");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getMethod()).isEqualTo("DELETE");
            softly.assertThat(event.getRequestURI()).isEqualTo("/api/items/1");
        });
    }

    @Test
    void multipleRequestsEmitMultipleEvents() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/hello");
        HttpClientTestUtils.get(getBaseUrl() + "/api/greet");

        final var events = AccessEventTestUtils.awaitEvents(listAppender, 2);

        assertThat(events).hasSize(2);
    }

    @Test
    void notFoundResponseProducesEventWith404Status() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/nonexistent");

        assertThat(response.statusCode()).isEqualTo(404);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getStatusCode()).isEqualTo(404);
    }
}
