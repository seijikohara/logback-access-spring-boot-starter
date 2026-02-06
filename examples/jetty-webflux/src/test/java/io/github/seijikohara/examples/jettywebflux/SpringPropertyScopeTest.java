package io.github.seijikohara.examples.jettywebflux;

import io.github.seijikohara.examples.test.common.AbstractSpringPropertyScopeTest;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Tests that springProperty with scope="context" stores values
 * in the AccessContext, making them accessible after configuration on Jetty WebFlux.
 */
@SpringBootTest(
        webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = {
                "logback.access.config-location=classpath:logback-access-spring-property.xml",
                "spring.application.name=test-app",
                "custom.property=custom-test-value"
        }
)
class SpringPropertyScopeTest extends AbstractSpringPropertyScopeTest {

    @Autowired
    LogbackAccessContext logbackAccessContext;

    @Override
    protected LogbackAccessContext getLogbackAccessContext() {
        return logbackAccessContext;
    }
}
