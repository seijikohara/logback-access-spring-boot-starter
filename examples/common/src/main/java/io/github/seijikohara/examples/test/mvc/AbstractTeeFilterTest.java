package io.github.seijikohara.examples.test.mvc;

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
 * Abstract base class for TeeFilter request/response body capture functionality tests.
 * <p>
 * TeeFilter must be enabled via {@code logback.access.tee-filter.enabled=true}
 * and the subclass must use {@code @ActiveProfiles("teefilter")}.
 * <p>
 * Subclasses must provide server-specific configuration via Spring annotations
 * and implement abstract methods to provide test context.
 */
public abstract class AbstractTeeFilterTest {

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
    void setUpTeeFilterAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        listAppender.list.clear();
    }

    protected ListAppender<IAccessEvent> getListAppender() {
        return listAppender;
    }

    @Test
    void teeFilterCapturesRequestBody() throws Exception {
        final var requestBody = "{\"message\":\"Hello, TeeFilter!\"}";
        final var response = HttpClientTestUtils.post(getBaseUrl() + "/api/echo", requestBody);

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getMethod()).isEqualTo("POST");
            softly.assertThat(event.getRequestURI()).isEqualTo("/api/echo");
            softly.assertThat(event.getRequestContent()).contains("Hello, TeeFilter!");
        });
    }

    @Test
    void teeFilterCapturesResponseBody() throws Exception {
        final var requestBody = "{\"echo\":\"test\"}";
        final var response = HttpClientTestUtils.post(getBaseUrl() + "/api/echo", requestBody);

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        // Response content should contain the echoed data
        assertThat(event.getResponseContent()).contains("echo");
    }

    @Test
    void teeFilterCapturesBothRequestAndResponseBody() throws Exception {
        final var requestBody = "{\"input\":\"value\"}";
        final var response = HttpClientTestUtils.post(getBaseUrl() + "/api/echo", requestBody);

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getRequestContent()).isNotNull();
            softly.assertThat(event.getRequestContent()).contains("input");
            softly.assertThat(event.getResponseContent()).isNotNull();
            softly.assertThat(event.getResponseContent()).contains("input");
        });
    }

    @Test
    void getRequestHasNoRequestBody() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getMethod()).isEqualTo("GET");
            // GET requests have no body, so request content should be empty or null
            softly.assertThat(event.getRequestContent()).satisfiesAnyOf(
                    content -> assertThat(content).isNull(),
                    content -> assertThat(content).isEmpty()
            );
        });
    }
}
