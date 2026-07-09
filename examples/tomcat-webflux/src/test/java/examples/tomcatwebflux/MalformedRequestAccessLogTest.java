package examples.tomcatwebflux;

import examples.test.common.AbstractMalformedRequestAccessLogTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Malformed request access log tests for Tomcat + WebFlux.
 * Tomcat rejects the request before parsing, so the event carries NA fallback values.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MalformedRequestAccessLogTest extends AbstractMalformedRequestAccessLogTest {

    @Autowired
    LogbackAccessContext logbackAccessContext;

    @LocalServerPort
    int port;

    @Override
    protected LogbackAccessContext getLogbackAccessContext() {
        return logbackAccessContext;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getExpectedMalformedMethod() {
        return "-";
    }
}
