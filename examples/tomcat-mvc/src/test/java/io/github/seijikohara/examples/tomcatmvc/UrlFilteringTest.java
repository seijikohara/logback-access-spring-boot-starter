package io.github.seijikohara.examples.tomcatmvc;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.seijikohara.examples.AccessEventTestUtils;
import io.github.seijikohara.examples.HttpClientTestUtils;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for URL pattern filtering configuration.
 */
class UrlFilteringTest {

    /**
     * Tests exclude pattern filtering.
     * Requests matching exclude patterns should not emit access events.
     */
    @Nested
    @SpringBootTest(
            webEnvironment = WebEnvironment.RANDOM_PORT,
            properties = "logback.access.filter.exclude-url-patterns=/api/health"
    )
    class ExcludePatternTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        ListAppender<IAccessEvent> listAppender;

        @BeforeEach
        void setUp() {
            listAppender = AccessEventTestUtils.getListAppender(logbackAccessContext, "list");
            listAppender.list.clear();
        }

        String baseUrl() {
            return "http://localhost:" + port;
        }

        @Test
        void excludedUrlDoesNotEmitEvent() throws Exception {
            HttpClientTestUtils.get(baseUrl() + "/api/health");

            // Wait a bit and verify no events were emitted
            Thread.sleep(500);
            assertThat(listAppender.list).isEmpty();
        }

        @Test
        void nonExcludedUrlEmitsEvent() throws Exception {
            HttpClientTestUtils.get(baseUrl() + "/api/hello");

            final var events = AccessEventTestUtils.awaitEvents(listAppender);

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getRequestURI()).isEqualTo("/api/hello");
        }
    }

    /**
     * Tests include pattern filtering.
     * Only requests matching include patterns should emit access events.
     */
    @Nested
    @SpringBootTest(
            webEnvironment = WebEnvironment.RANDOM_PORT,
            properties = "logback.access.filter.include-url-patterns=/api/hello.*"
    )
    class IncludePatternTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        ListAppender<IAccessEvent> listAppender;

        @BeforeEach
        void setUp() {
            listAppender = AccessEventTestUtils.getListAppender(logbackAccessContext, "list");
            listAppender.list.clear();
        }

        String baseUrl() {
            return "http://localhost:" + port;
        }

        @Test
        void includedUrlEmitsEvent() throws Exception {
            HttpClientTestUtils.get(baseUrl() + "/api/hello");

            final var events = AccessEventTestUtils.awaitEvents(listAppender);

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getRequestURI()).isEqualTo("/api/hello");
        }

        @Test
        void nonIncludedUrlDoesNotEmitEvent() throws Exception {
            HttpClientTestUtils.get(baseUrl() + "/api/greet");

            // Wait a bit and verify no events were emitted
            Thread.sleep(500);
            assertThat(listAppender.list).isEmpty();
        }
    }

    /**
     * Tests combined include and exclude patterns.
     * Include patterns are applied first, then exclude patterns.
     */
    @Nested
    @SpringBootTest(
            webEnvironment = WebEnvironment.RANDOM_PORT,
            properties = {
                    "logback.access.filter.include-url-patterns=/api/.*",
                    "logback.access.filter.exclude-url-patterns=/api/health"
            }
    )
    class CombinedPatternTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        ListAppender<IAccessEvent> listAppender;

        @BeforeEach
        void setUp() {
            listAppender = AccessEventTestUtils.getListAppender(logbackAccessContext, "list");
            listAppender.list.clear();
        }

        String baseUrl() {
            return "http://localhost:" + port;
        }

        @Test
        void includedButNotExcludedUrlEmitsEvent() throws Exception {
            HttpClientTestUtils.get(baseUrl() + "/api/hello");

            final var events = AccessEventTestUtils.awaitEvents(listAppender);

            assertThat(events).hasSize(1);
            assertThat(events.get(0).getRequestURI()).isEqualTo("/api/hello");
        }

        @Test
        void includedAndExcludedUrlDoesNotEmitEvent() throws Exception {
            HttpClientTestUtils.get(baseUrl() + "/api/health");

            // Wait a bit and verify no events were emitted
            Thread.sleep(500);
            assertThat(listAppender.list).isEmpty();
        }
    }
}
