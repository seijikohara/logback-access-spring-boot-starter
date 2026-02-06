package io.github.seijikohara.examples.jettymvc;

import io.github.seijikohara.examples.test.common.AbstractDisabledAccessLogTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;

/**
 * Tests that logback-access auto-configuration is disabled
 * when logback.access.enabled=false on Jetty.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "logback.access.enabled=false"
)
class JettyDisabledAccessLogTest extends AbstractDisabledAccessLogTest {

    @Autowired
    ApplicationContext applicationContext;

    @Override
    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
