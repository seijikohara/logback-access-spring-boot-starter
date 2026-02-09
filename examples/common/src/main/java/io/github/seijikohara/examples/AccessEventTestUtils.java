package io.github.seijikohara.examples;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for testing access events.
 */
public final class AccessEventTestUtils {

    private static final long DEFAULT_TIMEOUT_MS = 5000L;
    private static final long POLL_INTERVAL_MS = 50L;

    private AccessEventTestUtils() {
    }

    /**
     * Gets the ListAppender from the LogbackAccessContext.
     *
     * @param context      the LogbackAccessContext
     * @param appenderName the name of the appender
     * @return the ListAppender
     */
    @SuppressWarnings("unchecked")
    public static ListAppender<IAccessEvent> getListAppender(
            final LogbackAccessContext context,
            final String appenderName) {
        return (ListAppender<IAccessEvent>) context.getAccessContext().getAppender(appenderName);
    }

    /**
     * Waits for a single access event to be logged.
     *
     * @param appender the ListAppender to wait on
     * @return the list of events
     */
    public static List<IAccessEvent> awaitEvents(final ListAppender<IAccessEvent> appender) {
        return awaitEvents(appender, 1);
    }

    /**
     * Waits for the specified number of access events to be logged.
     *
     * @param appender the ListAppender to wait on
     * @param count    the number of events to wait for
     * @return the list of events
     */
    public static List<IAccessEvent> awaitEvents(final ListAppender<IAccessEvent> appender, final int count) {
        return awaitEvents(appender, count, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Waits for the specified number of access events to be logged with a custom timeout.
     *
     * @param appender  the ListAppender to wait on
     * @param count     the number of events to wait for
     * @param timeoutMs the timeout in milliseconds
     * @return the list of events
     */
    public static List<IAccessEvent> awaitEvents(
            final ListAppender<IAccessEvent> appender,
            final int count,
            final long timeoutMs) {
        final var deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (appender.list.size() >= count) {
                return List.copyOf(appender.list);
            }
            try {
                TimeUnit.MILLISECONDS.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return List.copyOf(appender.list);
    }

    /**
     * Waits for a period and asserts that no events were logged.
     *
     * @param appender the ListAppender to check
     */
    public static void awaitNoEvents(final ListAppender<IAccessEvent> appender) {
        awaitNoEvents(appender, 500L);
    }

    /**
     * Waits for the specified period and asserts that no events were logged.
     *
     * @param appender  the ListAppender to check
     * @param timeoutMs the wait period in milliseconds
     */
    public static void awaitNoEvents(
            final ListAppender<IAccessEvent> appender,
            final long timeoutMs) {
        try {
            TimeUnit.MILLISECONDS.sleep(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (!appender.list.isEmpty()) {
            throw new AssertionError(
                    "Expected no events but found " + appender.list.size());
        }
    }
}
