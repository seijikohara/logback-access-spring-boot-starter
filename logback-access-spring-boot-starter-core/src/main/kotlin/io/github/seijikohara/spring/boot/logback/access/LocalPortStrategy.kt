package io.github.seijikohara.spring.boot.logback.access

/**
 * Strategy for resolving the local port reported in access log events.
 */
enum class LocalPortStrategy {
    /**
     * Returns the port number of the interface on which the request was received.
     */
    LOCAL,

    /**
     * Returns the port number to which the request was sent.
     * Useful when forward headers are enabled to identify the original destination port.
     */
    SERVER,
}
