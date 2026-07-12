package examples.tomcatmvc;

import examples.test.common.AbstractForwardedHeaderTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Forwarded-header (RemoteIpValve) tests for Tomcat + MVC.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "server.tomcat.remoteip.remote-ip-header=x-forwarded-for",
                // Trust both loopback forms so the JDK client's connection is accepted
                // as coming from an internal proxy regardless of IPv4/IPv6 resolution.
                "server.tomcat.remoteip.internal-proxies=127\\.0\\.0\\.1|0:0:0:0:0:0:0:1"
        }
)
class ForwardedHeaderTest extends AbstractForwardedHeaderTest {

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
