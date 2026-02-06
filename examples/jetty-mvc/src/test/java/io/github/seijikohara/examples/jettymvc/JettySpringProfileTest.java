package io.github.seijikohara.examples.jettymvc;

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
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for Spring Profile integration with logback-access configuration on Jetty.
 * Uses logback-access-spring-profile.xml which defines profile-specific appenders.
 */
class JettySpringProfileTest {

    @Nested
    @SpringBootTest(
            webEnvironment = WebEnvironment.RANDOM_PORT,
            properties = "logback.access.config-location=classpath:logback-access-spring-profile.xml"
    )
    @ActiveProfiles("dev")
    class DevProfileTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        ListAppender<IAccessEvent> commonListAppender;
        ListAppender<IAccessEvent> devListAppender;

        @BeforeEach
        void setUp() {
            commonListAppender = AccessEventTestUtils.getListAppender(logbackAccessContext, "commonList");
            devListAppender = AccessEventTestUtils.getListAppender(logbackAccessContext, "devList");
            commonListAppender.list.clear();
            devListAppender.list.clear();
        }

        String baseUrl() {
            return "http://localhost:" + port;
        }

        @Test
        void devProfileActivatesDevAppender() throws Exception {
            final var response = HttpClientTestUtils.get(baseUrl() + "/api/hello");

            assertThat(response.statusCode()).isEqualTo(200);

            final var commonEvents = AccessEventTestUtils.awaitEvents(commonListAppender);
            final var devEvents = AccessEventTestUtils.awaitEvents(devListAppender);

            // Both common and dev appenders should receive events
            assertThat(commonEvents).hasSize(1);
            assertThat(devEvents).hasSize(1);
        }

        @Test
        void devProfileDoesNotActivateProdAppender() {
            // prodList appender should not exist when dev profile is active
            final var prodAppender = logbackAccessContext.getAccessContext().getAppender("prodList");
            assertThat(prodAppender).isNull();
        }
    }

    @Nested
    @SpringBootTest(
            webEnvironment = WebEnvironment.RANDOM_PORT,
            properties = "logback.access.config-location=classpath:logback-access-spring-profile.xml"
    )
    @ActiveProfiles("prod")
    class ProdProfileTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        ListAppender<IAccessEvent> commonListAppender;
        ListAppender<IAccessEvent> prodListAppender;

        @BeforeEach
        void setUp() {
            commonListAppender = AccessEventTestUtils.getListAppender(logbackAccessContext, "commonList");
            prodListAppender = AccessEventTestUtils.getListAppender(logbackAccessContext, "prodList");
            commonListAppender.list.clear();
            prodListAppender.list.clear();
        }

        String baseUrl() {
            return "http://localhost:" + port;
        }

        @Test
        void prodProfileActivatesProdAppender() throws Exception {
            final var response = HttpClientTestUtils.get(baseUrl() + "/api/hello");

            assertThat(response.statusCode()).isEqualTo(200);

            final var commonEvents = AccessEventTestUtils.awaitEvents(commonListAppender);
            final var prodEvents = AccessEventTestUtils.awaitEvents(prodListAppender);

            // Both common and prod appenders should receive events
            assertThat(commonEvents).hasSize(1);
            assertThat(prodEvents).hasSize(1);
        }

        @Test
        void prodProfileDoesNotActivateDevAppender() {
            // devList appender should not exist when prod profile is active
            final var devAppender = logbackAccessContext.getAccessContext().getAppender("devList");
            assertThat(devAppender).isNull();
        }
    }
}
