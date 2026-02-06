package io.github.seijikohara.examples.tomcatwebflux;

import io.github.seijikohara.examples.test.webflux.AbstractRouterFunctionTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Tests for router function endpoints access logging on Tomcat.
 * Extends AbstractRouterFunctionTest for common router function tests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RouterFunctionTest extends AbstractRouterFunctionTest {

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
