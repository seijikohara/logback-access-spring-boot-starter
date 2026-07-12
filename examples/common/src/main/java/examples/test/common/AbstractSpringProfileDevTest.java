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
 * Abstract base class for {@code <springProfile name="dev">} activation tests.
 * <p>
 * The shared {@code application-dev.yml} points {@code logback.access.config-location}
 * at {@code logback-access-spring-profile.xml}, which defines {@code commonList}
 * unconditionally, {@code devList} inside the dev profile block, and {@code prodList}
 * inside the prod profile block. Subclasses activate the dev profile via
 * {@code @ActiveProfiles("dev")}.
 */
public abstract class AbstractSpringProfileDevTest {

    private ListAppender<IAccessEvent> commonListAppender;
    private ListAppender<IAccessEvent> devListAppender;

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
    void setUpDevProfileAppenders() {
        commonListAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "commonList");
        devListAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "devList");
        AccessEventTestUtils.reset(commonListAppender);
        AccessEventTestUtils.reset(devListAppender);
    }

    @Test
    void devProfileActivatesDevAppender() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        assertThat(response.statusCode()).isEqualTo(200);

        final var commonEvents = AccessEventTestUtils.awaitEvents(commonListAppender);
        final var devEvents = AccessEventTestUtils.awaitEvents(devListAppender);

        assertThat(commonEvents).hasSize(1);
        assertThat(devEvents).hasSize(1);
    }

    @Test
    void devProfileDoesNotActivateProdAppender() {
        final var prodAppender = getLogbackAccessContext().getAccessContext().getAppender("prodList");

        assertThat(prodAppender).isNull();
    }
}
