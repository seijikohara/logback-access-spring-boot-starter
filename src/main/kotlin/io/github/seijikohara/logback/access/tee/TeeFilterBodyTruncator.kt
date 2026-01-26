package io.github.seijikohara.logback.access.tee

import org.springframework.util.unit.DataSize
import kotlin.text.Charsets.UTF_8

/**
 * Utility for truncating captured request/response bodies.
 */
object TeeFilterBodyTruncator {

    /**
     * The indicator appended when content is truncated.
     */
    private const val TRUNCATION_INDICATOR = "... [TRUNCATED]"

    /**
     * Maximum safe size to prevent integer overflow when converting Long to Int.
     * Using Int.MAX_VALUE as the upper bound (approximately 2GB).
     */
    private const val MAX_SAFE_BYTES = Int.MAX_VALUE.toLong()

    /**
     * Truncates a byte array to the specified maximum size and converts to String.
     *
     * @param bytes The byte array to truncate.
     * @param maxSize The maximum allowed size.
     * @return The truncated string with indicator if truncation occurred, or null if bytes is null.
     */
    fun truncateToString(bytes: ByteArray?, maxSize: DataSize): String? {
        if (bytes == null) return null

        // Handle edge cases: negative or zero size
        val maxBytesLong = maxSize.toBytes()
        if (maxBytesLong <= 0) {
            return TRUNCATION_INDICATOR
        }

        // Safely convert Long to Int, capping at Int.MAX_VALUE to prevent overflow
        val maxBytes = maxBytesLong.coerceAtMost(MAX_SAFE_BYTES).toInt()

        return if (bytes.size <= maxBytes) {
            String(bytes, UTF_8)
        } else {
            String(bytes, 0, maxBytes, UTF_8) + TRUNCATION_INDICATOR
        }
    }
}
