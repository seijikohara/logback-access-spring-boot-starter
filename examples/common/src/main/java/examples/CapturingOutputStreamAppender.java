package examples;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.core.OutputStreamAppender;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Test appender that captures its encoder's output (for example JSON) into memory, so tests can
 * validate the encoded bytes rather than only the in-memory {@link IAccessEvent}.
 *
 * <p>{@link ByteArrayOutputStream} guards its own writes, and the read/reset methods synchronize on
 * the same instance, so capture is safe across the appender's writer thread and the test thread.
 */
public final class CapturingOutputStreamAppender extends OutputStreamAppender<IAccessEvent> {

    private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

    @Override
    public void start() {
        setImmediateFlush(true);
        setOutputStream(stream);
        super.start();
    }

    /**
     * Returns the encoded output captured so far, decoded as UTF-8.
     *
     * @return the captured output
     */
    public String getCapturedOutput() {
        synchronized (stream) {
            return stream.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Clears the captured output.
     */
    public void resetCapture() {
        synchronized (stream) {
            stream.reset();
        }
    }
}
