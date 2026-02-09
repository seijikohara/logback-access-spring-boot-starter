package io.github.seijikohara.spring.boot.logback.access.tee

import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties.TeeFilterProperties
import java.nio.charset.Charset

/**
 * Evaluates whether captured body content should be included in log output
 * or suppressed based on content type and payload size.
 */
internal object BodyCapturePolicy {
    private const val IMAGE_CONTENTS_SUPPRESSED = "[IMAGE CONTENTS SUPPRESSED]"
    private const val BINARY_CONTENT_SUPPRESSED = "[BINARY CONTENT SUPPRESSED]"
    private const val CONTENT_TOO_LARGE = "[CONTENT TOO LARGE]"

    private val DEFAULT_ALLOWED_CONTENT_TYPES =
        listOf(
            "text/*",
            "application/json",
            "application/xml",
            "application/*+json",
            "application/*+xml",
            "application/x-www-form-urlencoded",
        )

    /**
     * Evaluates whether body content should be suppressed.
     *
     * @return null if capture is allowed, or a sentinel string if suppressed.
     */
    fun evaluate(
        contentType: String?,
        payloadSize: Int,
        properties: TeeFilterProperties,
    ): String? =
        when {
            payloadSize > properties.maxPayloadSize -> CONTENT_TOO_LARGE
            !isAllowedContentType(contentType, properties) -> selectBinarySentinel(contentType)
            else -> null
        }

    /**
     * Resolves a [Charset] from the given encoding name, falling back to UTF-8
     * when the encoding is null or unsupported.
     */
    fun resolveCharset(encoding: String?): Charset = encoding?.let { runCatching { Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8

    private fun isAllowedContentType(
        contentType: String?,
        properties: TeeFilterProperties,
    ): Boolean {
        if (contentType == null) return true
        val mimeType = contentType.substringBefore(';').trim().lowercase()
        val patterns = properties.allowedContentTypes ?: DEFAULT_ALLOWED_CONTENT_TYPES
        return patterns.any { matchesMimePattern(mimeType, it.lowercase()) }
    }

    private fun selectBinarySentinel(contentType: String?): String {
        val mimeType = contentType?.substringBefore(';')?.trim()?.lowercase()
        return if (mimeType != null && mimeType.startsWith("image/")) {
            IMAGE_CONTENTS_SUPPRESSED
        } else {
            BINARY_CONTENT_SUPPRESSED
        }
    }

    private fun matchesMimePattern(
        mimeType: String,
        pattern: String,
    ): Boolean =
        when {
            pattern == mimeType -> {
                true
            }

            pattern.endsWith("/*") -> {
                mimeType.startsWith(pattern.removeSuffix("*"))
            }

            pattern.contains("/*+") -> {
                val (type, suffix) = pattern.split("/*+", limit = 2)
                mimeType.startsWith("$type/") && mimeType.endsWith("+$suffix")
            }

            else -> {
                false
            }
        }
}
