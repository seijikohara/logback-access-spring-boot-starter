package io.github.seijikohara.examples.tomcatmvc;

import io.github.seijikohara.examples.test.common.AbstractDisabledAccessLogTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;

/**
 * Tests that logback-access auto-configuration is disabled
 * when logback.access.enabled=false on Tomcat.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "logback.access.enabled=false"
)
class DisabledAccessLogTest extends AbstractDisabledAccessLogTest {

    @Autowired
    ApplicationContext applicationContext;

    @Override
    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
