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
 * Abstract base class for {@code <springProfile name="prod">} activation tests.
 * <p>
 * Mirror of {@link AbstractSpringProfileDevTest} for the prod profile: {@code prodList}
 * must be active and {@code devList} absent. Subclasses activate the prod profile via
 * {@code @ActiveProfiles("prod")}.
 */
public abstract class AbstractSpringProfileProdTest {

    private ListAppender<IAccessEvent> commonListAppender;
    private ListAppender<IAccessEvent> prodListAppender;

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
    void setUpProdProfileAppenders() {
        commonListAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "commonList");
        prodListAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "prodList");
        AccessEventTestUtils.reset(commonListAppender);
        AccessEventTestUtils.reset(prodListAppender);
    }

    @Test
    void prodProfileActivatesProdAppender() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/hello");

        assertThat(response.statusCode()).isEqualTo(200);

        final var commonEvents = AccessEventTestUtils.awaitEvents(commonListAppender);
        final var prodEvents = AccessEventTestUtils.awaitEvents(prodListAppender);

        assertThat(commonEvents).hasSize(1);
        assertThat(prodEvents).hasSize(1);
    }

    @Test
    void prodProfileDoesNotActivateDevAppender() {
        final var devAppender = getLogbackAccessContext().getAccessContext().getAppender("devList");

        assertThat(devAppender).isNull();
    }
}
