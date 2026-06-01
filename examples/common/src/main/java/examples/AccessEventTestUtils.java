package examples;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessContext;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for testing access events.
 *
 * <p>{@link ListAppender#list} is a plain {@link java.util.ArrayList} mutated by logback's
 * {@code AppenderBase.doAppend}, which holds the appender's intrinsic monitor. Server worker threads
 * emit access events after the HTTP response returns, so they can mutate the list while a test thread
 * reads it. All access to {@code appender.list} here is therefore guarded by {@code synchronized
 * (appender)} to use the same monitor and avoid {@link java.util.ConcurrentModificationException} and
 * stale reads.
 */
public final class AccessEventTestUtils {

    private static final long DEFAULT_TIMEOUT_MS = 5000L;
    private static final long POLL_INTERVAL_MS = 50L;
    private static final long QUIESCENCE_MS = 200L;

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
     * Clears the appender under its own monitor, so the reset does not race with a concurrent emit.
     *
     * @param appender the ListAppender to reset
     */
    public static void reset(final ListAppender<IAccessEvent> appender) {
        synchronized (appender) {
            appender.list.clear();
        }
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
     * <p>Once {@code count} events are observed, the method waits a short quiescence period and
     * re-snapshots, so a caller asserting an exact size can detect a late duplicate or spurious event.
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
        var snapshot = snapshot(appender);
        while (snapshot.size() < count && System.currentTimeMillis() < deadline) {
            if (!sleep(POLL_INTERVAL_MS)) {
                break;
            }
            snapshot = snapshot(appender);
        }
        if (snapshot.size() >= count) {
            sleep(QUIESCENCE_MS);
            snapshot = snapshot(appender);
        }
        return snapshot;
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
        sleep(timeoutMs);
        final var snapshot = snapshot(appender);
        if (!snapshot.isEmpty()) {
            throw new AssertionError("Expected no events but found " + snapshot.size());
        }
    }

    private static List<IAccessEvent> snapshot(final ListAppender<IAccessEvent> appender) {
        synchronized (appender) {
            return List.copyOf(appender.list);
        }
    }

    private static boolean sleep(final long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
