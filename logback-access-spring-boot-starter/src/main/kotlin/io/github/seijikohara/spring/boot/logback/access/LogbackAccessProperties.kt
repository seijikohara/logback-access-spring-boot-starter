package io.github.seijikohara.spring.boot.logback.access

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding
import org.springframework.boot.context.properties.bind.DefaultValue

/**
 * Configuration properties for Logback-access.
 *
 * @property enabled Whether to enable auto-configuration.
 * @property configLocation Location of the Logback-access configuration file.
 *           Supports "classpath:" and "file:" URL prefixes.
 *           Auto-detected from [DEFAULT_CONFIGS] when not specified.
 * @property localPortStrategy Strategy for resolving the local port in access log events.
 * @property tomcat Tomcat-specific properties.
 * @property teeFilter TeeFilter properties for capturing request/response bodies.
 * @property filter URL filtering properties.
 */
@ConfigurationProperties("logback.access")
data class LogbackAccessProperties
    @ConstructorBinding
    constructor(
        @DefaultValue("true")
        val enabled: Boolean,
        val configLocation: String?,
        @DefaultValue("SERVER")
        val localPortStrategy: LocalPortStrategy,
        @DefaultValue
        val tomcat: TomcatProperties,
        @DefaultValue
        val teeFilter: TeeFilterProperties,
        @DefaultValue
        val filter: FilterProperties,
    ) {
        /**
         * Tomcat-specific properties.
         *
         * @property requestAttributesEnabled Whether to enable request attributes for use with RemoteIpValve.
         *           Defaults to the presence of RemoteIpValve when not specified.
         */
        data class TomcatProperties(
            val requestAttributesEnabled: Boolean?,
        )

        /**
         * TeeFilter properties for capturing request/response bodies.
         *
         * @property enabled Whether to enable the TeeFilter.
         * @property includeHosts Comma-separated host names to activate. All hosts when not specified.
         * @property excludeHosts Comma-separated host names to deactivate.
         */
        data class TeeFilterProperties
            @ConstructorBinding
            constructor(
                @DefaultValue("false")
                val enabled: Boolean,
                val includeHosts: String?,
                val excludeHosts: String?,
            )

        /**
         * URL pattern filtering properties for access logging.
         *
         * @property includeUrlPatterns Regex patterns for URLs to include in access logging.
         *           All URLs when not specified.
         * @property excludeUrlPatterns Regex patterns for URLs to exclude from access logging.
         */
        data class FilterProperties(
            val includeUrlPatterns: List<String>?,
            val excludeUrlPatterns: List<String>?,
        )

        companion object {
            /** Default configuration file locations searched in order. */
            @JvmField
            val DEFAULT_CONFIGS: List<String> =
                listOf(
                    "classpath:logback-access-test.xml",
                    "classpath:logback-access.xml",
                    "classpath:logback-access-test-spring.xml",
                    "classpath:logback-access-spring.xml",
                )

            /** Built-in fallback configuration file location. */
            const val FALLBACK_CONFIG: String =
                "classpath:io/github/seijikohara/spring/boot/logback/access/logback-access-spring.xml"
        }
    }
