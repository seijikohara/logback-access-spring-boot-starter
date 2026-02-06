package io.github.seijikohara.examples;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for Spring Security integration tests.
 * Subclasses must provide server-specific configuration via Spring annotations
 * and implement abstract methods to provide test credentials.
 */
public abstract class AbstractSecurityTest {

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

    /**
     * Returns the username for authentication tests.
     *
     * @return the username
     */
    protected abstract String getAuthenticatedUsername();

    /**
     * Returns the password for authentication tests.
     *
     * @return the password
     */
    protected abstract String getAuthenticatedPassword();

    @BeforeEach
    void setUpAppender() {
        listAppender = AccessEventTestUtils.getListAppender(getLogbackAccessContext(), "list");
        listAppender.list.clear();
    }

    protected ListAppender<IAccessEvent> getListAppender() {
        return listAppender;
    }

    @Test
    void authenticatedUserIsLoggedInAccessEvent() throws Exception {
        final var response = HttpClientTestUtils.getWithBasicAuth(
                getBaseUrl() + "/api/secure",
                getAuthenticatedUsername(),
                getAuthenticatedPassword());

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getRemoteUser()).isEqualTo(getAuthenticatedUsername());
    }

    @Test
    void unauthenticatedRequestOnPublicEndpointHasAnonymousUser() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/public");

        assertThat(response.statusCode()).isEqualTo(200);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        // Spring Security sets anonymous user when security filter chain is applied
        assertThat(event.getRemoteUser()).isEqualTo("anonymousUser");
    }

    @Test
    void unauthenticatedRequestOnSecureEndpointReturns401() throws Exception {
        final var response = HttpClientTestUtils.get(getBaseUrl() + "/api/secure");

        assertThat(response.statusCode()).isEqualTo(401);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(event.getStatusCode()).isEqualTo(401);
            softly.assertThat(event.getRemoteUser()).isEqualTo("-");
        });
    }

    @Test
    void invalidCredentialsReturns401() throws Exception {
        final var response = HttpClientTestUtils.getWithBasicAuth(
                getBaseUrl() + "/api/secure",
                getAuthenticatedUsername(),
                "wrongpassword");

        assertThat(response.statusCode()).isEqualTo(401);

        final var events = AccessEventTestUtils.awaitEvents(listAppender);

        assertThat(events).hasSize(1);
        final var event = events.get(0);
        assertThat(event.getStatusCode()).isEqualTo(401);
    }
}
