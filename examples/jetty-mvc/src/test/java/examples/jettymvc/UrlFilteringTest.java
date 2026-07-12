package examples.jettymvc;

import examples.test.common.AbstractUrlFilteringCombinedTest;
import examples.test.common.AbstractUrlFilteringEmptyPatternsTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Tests for URL pattern filtering configuration on Jetty.
 */
class UrlFilteringTest {

    /**
     * Combined include and exclude patterns in one context.
     */
    @Nested
    @SpringBootTest(
            webEnvironment = WebEnvironment.RANDOM_PORT,
            properties = {
                    "logback.access.filter.include-url-patterns=/api/.*",
                    "logback.access.filter.exclude-url-patterns=/api/health"
            }
    )
    class CombinedPatternTest extends AbstractUrlFilteringCombinedTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        @Override
        protected LogbackAccessContext getLogbackAccessContext() {
            return logbackAccessContext;
        }

        @Override
        protected String getBaseUrl() {
            return "http://localhost:" + port;
        }
    }

    /**
     * Empty pattern values bind to empty lists; every request must still be logged
     * (issue #220 regression guard).
     */
    @Nested
    @SpringBootTest(
            webEnvironment = WebEnvironment.RANDOM_PORT,
            properties = {
                    "logback.access.filter.include-url-patterns=",
                    "logback.access.filter.exclude-url-patterns="
            }
    )
    class EmptyPatternsTest extends AbstractUrlFilteringEmptyPatternsTest {

        @Autowired
        LogbackAccessContext logbackAccessContext;

        @LocalServerPort
        int port;

        @Override
        protected LogbackAccessContext getLogbackAccessContext() {
            return logbackAccessContext;
        }

        @Override
        protected String getBaseUrl() {
            return "http://localhost:" + port;
        }
    }
}
