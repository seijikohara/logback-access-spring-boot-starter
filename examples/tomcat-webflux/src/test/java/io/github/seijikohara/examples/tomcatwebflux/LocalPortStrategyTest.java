package io.github.seijikohara.examples.tomcatwebflux;

import io.github.seijikohara.examples.test.webflux.AbstractReactiveLocalPortStrategyTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Tests the localPortStrategy=LOCAL configuration on reactive Tomcat.
 * <p>
 * When LOCAL strategy is used, the access event captures the actual
 * local port that received the connection, rather than the server port
 * from the Host header or X-Forwarded-Port.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "logback.access.local-port-strategy=LOCAL"
)
class LocalPortStrategyTest extends AbstractReactiveLocalPortStrategyTest {

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

    @Override
    protected int getPort() {
        return port;
    }
}
