package examples.jettywebflux;

import examples.test.common.AbstractSpringProfileDevTest;
import examples.test.common.AbstractSpringProfileProdTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for Spring Profile integration with logback-access configuration on reactive Jetty.
 * Uses logback-access-spring-profile.xml which defines profile-specific appenders.
 */
class SpringProfileTest {

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("dev")
    class DevProfileTest extends AbstractSpringProfileDevTest {

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

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
    @ActiveProfiles("prod")
    class ProdProfileTest extends AbstractSpringProfileProdTest {

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
