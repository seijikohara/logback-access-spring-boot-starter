package io.github.seijikohara.spring.boot.logback.access

import ch.qos.logback.access.common.spi.AccessContext
import ch.qos.logback.core.spi.FilterReply
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties.Companion.DEFAULT_CONFIGS
import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties.Companion.FALLBACK_CONFIG
import io.github.seijikohara.spring.boot.logback.access.joran.AccessJoranConfigurator
import org.springframework.core.env.Environment
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.util.ResourceUtils.getURL

/**
 * Manages the Logback-access [AccessContext] lifecycle.
 *
 * Resolves the configuration file, initializes the Joran configurator
 * with Spring environment support, and provides the [emit] entry point
 * for server integrations.
 */
public class LogbackAccessContext(
    /** Configuration properties for this context. */
    public val properties: LogbackAccessProperties,
    resourceLoader: ResourceLoader,
    environment: Environment,
) : AutoCloseable {
    /** The underlying Logback-access context. */
    public val accessContext: AccessContext = AccessContext()

    /** Compiled regex patterns for URL filtering, cached for performance. */
    private val includePatterns: List<Regex>? =
        properties.filter.includeUrlPatterns?.map { Regex(it) }

    /** Compiled regex patterns for URL exclusion, cached for performance. */
    private val excludePatterns: List<Regex>? =
        properties.filter.excludeUrlPatterns?.map { Regex(it) }

    init {
        val (name, resource) = resolveConfig(properties, resourceLoader)
        accessContext.name = name
        val configurator = AccessJoranConfigurator(environment)
        configurator.context = accessContext
        configurator.doConfigure(resource.url)
        accessContext.start()
        logger.debug { "Initialized LogbackAccessContext: $this" }
    }

    /**
     * Emits an access event through the filter chain and appenders.
     *
     * The processing pipeline is:
     * 1. URL filtering (include/exclude patterns via [shouldLog])
     * 2. Logback filter chain evaluation
     * 3. Appender invocation
     *
     * Only [Exception] subclasses are caught and logged at ERROR level.
     * Fatal errors ([Error]) are propagated to the caller.
     */
    @Suppress("TooGenericExceptionCaught")
    public fun emit(event: LogbackAccessEvent) {
        try {
            event
                .takeIf { shouldLog(it.requestURI) }
                ?.let { accessContext.getFilterChainDecision(it) }
                ?.takeIf { it != FilterReply.DENY }
                ?.let { accessContext.callAppenders(event) }
        } catch (e: Exception) {
            logger.error(e) { "Failed to emit access event: ${event.requestURI}" }
        }
    }

    /**
     * Determines whether the request URI should be logged based on include/exclude patterns.
     *
     * @param uri the request URI to check
     * @return true if the URI should be logged, false otherwise
     */
    private fun shouldLog(uri: String): Boolean = matchesIncludePatterns(uri) && !matchesExcludePatterns(uri)

    /** Returns true if URI matches at least one include pattern, or no include patterns are specified. */
    private fun matchesIncludePatterns(uri: String): Boolean = includePatterns?.any { it.containsMatchIn(uri) } ?: true

    /** Returns true if URI matches any exclude pattern. */
    private fun matchesExcludePatterns(uri: String): Boolean = excludePatterns?.any { it.containsMatchIn(uri) } ?: false

    override fun close(): Unit =
        logger.debug { "Closing LogbackAccessContext: $this" }.also {
            accessContext.run {
                stop()
                reset()
                detachAndStopAllAppenders()
                copyOfAttachedFiltersList.forEach { it.stop() }
                clearAllFilters()
            }
        }

    override fun toString(): String = "LogbackAccessContext(${accessContext.name})"

    private companion object {
        private val logger = KotlinLogging.logger {}

        private fun resolveConfig(
            properties: LogbackAccessProperties,
            resourceLoader: ResourceLoader,
        ): Pair<String, Resource> =
            properties.configLocation
                ?.let { it to resourceLoader.getResource("${getURL(it)}") }
                ?: DEFAULT_CONFIGS
                    .asSequence()
                    .map { it to resourceLoader.getResource(it) }
                    .firstOrNull { (_, resource) -> resource.exists() }
                ?: (FALLBACK_CONFIG to resourceLoader.getResource(FALLBACK_CONFIG))
    }
}
