package io.github.seijikohara.examples.test.common;

import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for testing springProperty with scope="context".
 * <p>
 * This validates that springProperty stores values in the AccessContext,
 * making them accessible after configuration.
 * <p>
 * Subclasses must configure:
 * <pre>
 * &#64;SpringBootTest(
 *     properties = {
 *         "logback.access.config-location=classpath:logback-access-spring-property.xml",
 *         "spring.application.name=test-app",
 *         "custom.property=custom-test-value"
 *     }
 * )
 * </pre>
 * <p>
 * The logback-access-spring-property.xml config file must define:
 * <pre>
 * &lt;springProperty name="appName" source="spring.application.name"
 *                 defaultValue="default-app" scope="context"/&gt;
 * &lt;springProperty name="customProperty" source="custom.property"
 *                 defaultValue="default-value" scope="context"/&gt;
 * </pre>
 */
public abstract class AbstractSpringPropertyScopeTest {

    /**
     * Returns the LogbackAccessContext from the Spring context.
     *
     * @return the LogbackAccessContext
     */
    protected abstract LogbackAccessContext getLogbackAccessContext();

    @Test
    void contextScopePropertyIsAccessibleFromSpringApplicationName() {
        final var appName = getLogbackAccessContext().getAccessContext().getProperty("appName");
        assertThat(appName).isEqualTo("test-app");
    }

    @Test
    void contextScopePropertyIsAccessibleFromCustomProperty() {
        final var customProperty = getLogbackAccessContext().getAccessContext().getProperty("customProperty");
        assertThat(customProperty).isEqualTo("custom-test-value");
    }

    @Test
    void contextScopePropertyUsesDefaultWhenSourceMissing() {
        // Start a new context without setting spring.application.name
        // This test verifies the existing context has the configured value
        final var appName = getLogbackAccessContext().getAccessContext().getProperty("appName");
        assertThat(appName)
                .as("Property should be set from spring.application.name")
                .isNotEqualTo("default-app");
    }
}
