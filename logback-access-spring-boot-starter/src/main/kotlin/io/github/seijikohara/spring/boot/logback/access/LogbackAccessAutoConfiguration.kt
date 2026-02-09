package io.github.seijikohara.spring.boot.logback.access

/**
 * Deprecated type alias for backward compatibility.
 *
 * Use [io.github.seijikohara.spring.boot.logback.access.autoconfigure.LogbackAccessAutoConfiguration] instead.
 * This alias will be removed in v2.0.0.
 */
@Deprecated(
    message = "Moved to io.github.seijikohara.spring.boot.logback.access.autoconfigure package",
    replaceWith =
        ReplaceWith(
            "LogbackAccessAutoConfiguration",
            "io.github.seijikohara.spring.boot.logback.access.autoconfigure.LogbackAccessAutoConfiguration",
        ),
    level = DeprecationLevel.WARNING,
)
typealias LogbackAccessAutoConfiguration =
    io.github.seijikohara.spring.boot.logback.access.autoconfigure.LogbackAccessAutoConfiguration
