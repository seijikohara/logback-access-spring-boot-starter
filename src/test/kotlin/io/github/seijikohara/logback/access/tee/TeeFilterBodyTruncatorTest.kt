package io.github.seijikohara.logback.access.tee

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test
import org.springframework.util.unit.DataSize

/**
 * Tests the [TeeFilterBodyTruncator].
 */
class TeeFilterBodyTruncatorTest {

    @Test
    fun `Returns null for null bytes`() {
        val result = TeeFilterBodyTruncator.truncateToString(null, DataSize.ofBytes(100))
        result shouldBe null
    }

    @Test
    fun `Returns empty string for empty bytes`() {
        val result = TeeFilterBodyTruncator.truncateToString(byteArrayOf(), DataSize.ofBytes(100))
        result shouldBe ""
    }

    @Test
    fun `Does not truncate body within limit`() {
        val bytes = "A".repeat(50).toByteArray()
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofBytes(100))
        result shouldBe "A".repeat(50)
    }

    @Test
    fun `Does not truncate body at exact limit`() {
        val bytes = "A".repeat(100).toByteArray()
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofBytes(100))
        result shouldBe "A".repeat(100)
    }

    @Test
    fun `Truncates body exceeding max size`() {
        val bytes = "A".repeat(150).toByteArray()
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofBytes(50))
        result shouldStartWith "A".repeat(50)
        result shouldEndWith "... [TRUNCATED]"
    }

    @Test
    fun `Truncates large body correctly`() {
        val bytes = "X".repeat(1000).toByteArray()
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofKilobytes(1))
        // 1KB = 1024 bytes, content is 1000 bytes, so no truncation
        result shouldBe "X".repeat(1000)
    }

    @Test
    fun `Truncates at kilobyte boundary`() {
        val bytes = "Y".repeat(2000).toByteArray()
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofKilobytes(1))
        // 1KB = 1024 bytes, content is 2000 bytes, so truncation occurs
        result shouldStartWith "Y".repeat(1024)
        result shouldEndWith "... [TRUNCATED]"
    }

    @Test
    fun `Handles UTF-8 content`() {
        val bytes = "こんにちは".toByteArray(Charsets.UTF_8)
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofBytes(100))
        result shouldBe "こんにちは"
    }

    @Test
    fun `Truncates UTF-8 content at byte boundary`() {
        // Each Japanese character is 3 bytes in UTF-8
        // "こんにちは" = 5 chars = 15 bytes
        val bytes = "こんにちは".toByteArray(Charsets.UTF_8)
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofBytes(10))
        // Truncation at byte 10 may split a multi-byte character
        result shouldEndWith "... [TRUNCATED]"
    }

    @Test
    fun `Returns truncation indicator for zero max size`() {
        val bytes = "content".toByteArray()
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofBytes(0))
        result shouldBe "... [TRUNCATED]"
    }

    @Test
    fun `Returns truncation indicator for negative max size`() {
        val bytes = "content".toByteArray()
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofBytes(-100))
        result shouldBe "... [TRUNCATED]"
    }

    @Test
    fun `Handles large DataSize without overflow`() {
        // Test with a DataSize larger than Int.MAX_VALUE
        val bytes = "small content".toByteArray()
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofGigabytes(3))
        // 3GB > Int.MAX_VALUE (~2GB), should still work without overflow
        result shouldBe "small content"
    }

    @Test
    fun `Truncates with large DataSize when content exceeds limit`() {
        // Content is 100 bytes, limit is 50 bytes
        val bytes = "A".repeat(100).toByteArray()
        val result = TeeFilterBodyTruncator.truncateToString(bytes, DataSize.ofBytes(50))
        result shouldStartWith "A".repeat(50)
        result shouldEndWith "... [TRUNCATED]"
    }
}
