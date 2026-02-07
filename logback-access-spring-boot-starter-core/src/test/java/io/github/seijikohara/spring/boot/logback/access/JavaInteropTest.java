package io.github.seijikohara.spring.boot.logback.access;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that verify Kotlin/Java interoperability works correctly.
 */
class JavaInteropTest {

    @Test
    void shouldAccessDefaultConfigsDirectly() {
        // @JvmField allows direct field access without Companion
        List<String> configs = LogbackAccessProperties.DEFAULT_CONFIGS;
        assertNotNull(configs);
        assertFalse(configs.isEmpty());
        assertEquals(4, configs.size());
    }

    @Test
    void shouldAccessFallbackConfigDirectly() {
        // const val is already accessible as static field
        String fallback = LogbackAccessProperties.FALLBACK_CONFIG;
        assertNotNull(fallback);
        assertTrue(fallback.contains("logback-access-spring.xml"));
    }

    @Test
    void shouldCreateEventWithDataOnly() {
        // @JvmOverloads generates constructor overloads for default parameters
        AccessEventData data = createTestData();
        LogbackAccessEvent event = new LogbackAccessEvent(data);

        assertNotNull(event);
        assertNull(event.getRequest());
        assertNull(event.getResponse());
        assertEquals("GET", event.getMethod());
        assertEquals("/test", event.getRequestURI());
    }

    @Test
    void shouldCreateEventWithAllParameters() {
        AccessEventData data = createTestData();
        LogbackAccessEvent event = new LogbackAccessEvent(data, null, null);

        assertNotNull(event);
        assertEquals(200, event.getStatusCode());
    }

    private AccessEventData createTestData() {
        return new AccessEventData(
                System.currentTimeMillis(),
                100L,
                1L,
                "main",
                "localhost",
                8080,
                "127.0.0.1",
                "127.0.0.1",
                null,
                "HTTP/1.1",
                "GET",
                "/test",
                "",
                "GET /test HTTP/1.1",
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                null,
                200,
                Map.of(),
                0L,
                null
        );
    }
}
