package io.github.seijikohara.examples.test.common;

import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for testing that logback-access auto-configuration
 * is disabled when logback.access.enabled=false.
 * <p>
 * Subclasses must configure:
 * {@code @SpringBootTest(properties = "logback.access.enabled=false")}
 */
public abstract class AbstractDisabledAccessLogTest {

    /**
     * Returns the Spring ApplicationContext.
     *
     * @return the ApplicationContext
     */
    protected abstract ApplicationContext getApplicationContext();

    @Test
    void logbackAccessContextBeanNotPresent() {
        assertThat(getApplicationContext().containsBean("logbackAccessContext")).isFalse();
    }

    @Test
    void logbackAccessContextTypeNotFound() {
        assertThat(getApplicationContext().getBeanNamesForType(LogbackAccessContext.class)).isEmpty();
    }
}
