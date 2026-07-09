package examples.jettymvc;

import examples.test.common.AbstractMalformedRequestAccessLogTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Malformed request access log tests for Jetty + MVC.
 * Jetty synthesizes a "BAD /badMessage HTTP/1.0" placeholder request for unparseable requests.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class JettyMalformedRequestAccessLogTest extends AbstractMalformedRequestAccessLogTest {

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
        return "BAD";
    }
}
