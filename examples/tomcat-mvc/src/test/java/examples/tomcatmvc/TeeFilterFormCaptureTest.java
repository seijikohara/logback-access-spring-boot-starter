package examples.tomcatmvc;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import examples.AccessEventTestUtils;
import examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests form-urlencoded body capture when the operator opts in via
 * {@code allowed-content-types}. Tomcat-only, like the other TeeFilter tests.
 * <p>
 * Capturing form bodies is opt-in because the default policy suppresses them to keep
 * credentials out of access logs (PR #209). The captured body is reconstructed from
 * {@code getParameterMap()} with query-string pairs removed, because the query string is
 * already logged separately (PR #212).
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "logback.access.tee-filter.allowed-content-types=application/x-www-form-urlencoded"
)
@ActiveProfiles("teefilter")
class TeeFilterFormCaptureTest {

    @Autowired
    LogbackAccessContext logbackAccessContext;

    @LocalServerPort
    int port;

    ListAppender<IAccessEvent> listAppender;

    @BeforeEach
    void setUp() {
        listAppender = AccessEventTestUtils.getListAppender(logbackAccessContext, "list");
        AccessEventTestUtils.reset(listAppender);
    }

    String baseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void formBodyIsCapturedWhenContentTypeIsAllowed() throws Exception {
        final var response = HttpClientTestUtils.postForm(
                baseUrl() + "/api/form",
                "username=alice&password=hunter2");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getRequestContent())
                .contains("username=alice")
                .contains("password=hunter2");
    }

    @Test
    void queryStringParametersAreNotAttributedToBody() throws Exception {
        final var response = HttpClientTestUtils.postForm(
                baseUrl() + "/api/form?source=query",
                "field=bodyvalue");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getRequestContent())
                .contains("field=bodyvalue")
                .doesNotContain("source");
    }
}
