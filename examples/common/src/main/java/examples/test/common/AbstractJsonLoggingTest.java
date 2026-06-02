package examples.test.common;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.CapturingOutputStreamAppender;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import net.logstash.logback.encoder.LogstashAccessEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for JSON logging with LogstashAccessEncoder tests.
 * <p>
 * Verifies that access events are correctly captured and can be encoded as JSON.
 * <p>
 * Subclasses must configure:
 * {@code @SpringBootTest(properties = "logback.access.config-location=classpath:logback-access-json.xml")}
 */
public abstract class AbstractJsonLoggingTest {

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
    void setUpJsonLoggingAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        AccessEventTestUtils.reset(listAppender);
        jsonCaptureAppender().resetCapture();
    }

    protected ListAppender<IAccessEvent> getListAppender() {
        return listAppender;
    }

    private CapturingOutputStreamAppender jsonCaptureAppender() {
        return (CapturingOutputStreamAppender) getLogbackAccessContext().getAccessContext().getAppender("jsonCapture");
    }

    @Test
    void logstashAccessEncoderIsConfigured() {
        // Verify that LogstashAccessEncoder is properly configured
        var jsonConsoleAppender = getLogbackAccessContext().getAccessContext().getAppender("jsonConsole");
        assertThat(jsonConsoleAppender).isNotNull();

        // The encoder should be a LogstashAccessEncoder
        var consoleAppender = (ch.qos.logback.core.ConsoleAppender<?>) jsonConsoleAppender;
        assertThat(consoleAppender.getEncoder()).isInstanceOf(LogstashAccessEncoder.class);
    }

    @Test
    void jsonEncoderEmitsAccessEvents() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getMethod()).isEqualTo("GET");
        assertThat(event.getRequestURI()).isEqualTo("/api/hello");
        assertThat(event.getStatusCode()).isEqualTo(200);
    }

    @Test
    void jsonEncoderCapturesAllRequiredFields() throws Exception {
        HttpClientTestUtils.post(getBaseUrl() + "/api/echo", "{\"test\":\"data\"}");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);

        // Verify all fields that LogstashAccessEncoder would serialize
        assertThat(event.getMethod()).isEqualTo("POST");
        assertThat(event.getRequestURI()).isEqualTo("/api/echo");
        assertThat(event.getProtocol()).contains("HTTP");
        assertThat(event.getStatusCode()).isEqualTo(200);
        assertThat(event.getRemoteHost()).isNotEmpty();
        assertThat(event.getElapsedTime()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void jsonEncoderHandlesQueryStrings() throws Exception {
        HttpClientTestUtils.getWithQuery(getBaseUrl() + "/api/greet", "name=JSON");

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getQueryString()).isEqualTo("?name=JSON");
    }

    @Test
    void jsonEncoderProducesStructuredJsonOutput() throws Exception {
        HttpClientTestUtils.get(getBaseUrl() + "/api/hello");
        AccessEventTestUtils.awaitEvents(listAppender);

        final var output = jsonCaptureAppender().getCapturedOutput().strip();
        final var lines = output.split("\\R");
        final var json = lines[lines.length - 1].strip();

        // The LogstashAccessEncoder must emit a single JSON object carrying the request fields.
        assertThat(json).startsWith("{").endsWith("}");
        assertThat(json).contains("\"method\"").contains("\"GET\"");
        assertThat(json).contains("\"status_code\"").contains("200");
        assertThat(json).contains("\"requested_uri\"").contains("/api/hello");
        assertThat(json).contains("\"@timestamp\"");
    }
}
