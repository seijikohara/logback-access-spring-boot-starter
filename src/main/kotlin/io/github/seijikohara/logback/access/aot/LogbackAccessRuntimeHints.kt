package io.github.seijikohara.logback.access.aot

import org.slf4j.LoggerFactory
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.TypeReference

/**
 * [RuntimeHintsRegistrar] for Logback-access Spring Boot Starter.
 * Registers reflection, resource, and serialization hints for GraalVM native image support.
 */
class LogbackAccessRuntimeHints : RuntimeHintsRegistrar {

    private val logger = LoggerFactory.getLogger(LogbackAccessRuntimeHints::class.java)

    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        registerReflectionHints(hints)
        registerResourceHints(hints)
        registerSerializationHints(hints)
    }

    private fun registerReflectionHints(hints: RuntimeHints) {
        val memberCategories = arrayOf(
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
            MemberCategory.INVOKE_DECLARED_METHODS,
            MemberCategory.DECLARED_FIELDS,
        )

        REFLECTION_CLASSES.forEach { className ->
            runCatching {
                hints.reflection().registerType(
                    TypeReference.of(className),
                    *memberCategories,
                )
            }.onFailure { e ->
                logger.debug("Failed to register reflection hint for class: {}", className, e)
            }
        }
    }

    private fun registerResourceHints(hints: RuntimeHints) {
        RESOURCE_PATTERNS.forEach { pattern ->
            hints.resources().registerPattern(pattern)
        }
    }

    private fun registerSerializationHints(hints: RuntimeHints) {
        SERIALIZABLE_CLASSES.forEach { className ->
            runCatching {
                hints.serialization().registerType(TypeReference.of(className))
            }.onFailure { e ->
                logger.debug("Failed to register serialization hint for class: {}", className, e)
            }
        }
    }

    companion object {

        /**
         * Classes requiring reflection hints.
         */
        private val REFLECTION_CLASSES = listOf(
            // Configuration Properties
            "io.github.seijikohara.logback.access.LogbackAccessProperties",
            "io.github.seijikohara.logback.access.LogbackAccessProperties\$Tomcat",
            "io.github.seijikohara.logback.access.LogbackAccessProperties\$TeeFilter",
            "io.github.seijikohara.logback.access.value.LogbackAccessLocalPortStrategy",
            // Event classes
            "io.github.seijikohara.logback.access.LogbackAccessEvent",
            "io.github.seijikohara.logback.access.LogbackAccessEventSource",
            "io.github.seijikohara.logback.access.LogbackAccessEventSource\$Fixed",
            "io.github.seijikohara.logback.access.tomcat.LogbackAccessTomcatEventSource",
            "io.github.seijikohara.logback.access.jetty.LogbackAccessJettyEventSource",
            "io.github.seijikohara.logback.access.netty.LogbackAccessNettyEventSource",
            // Joran configuration classes
            "io.github.seijikohara.logback.access.joran.LogbackAccessJoranConfigurator",
            "io.github.seijikohara.logback.access.joran.LogbackAccessJoranSpringProfileModel",
            "io.github.seijikohara.logback.access.joran.LogbackAccessJoranSpringPropertyModel",
            "io.github.seijikohara.logback.access.joran.LogbackAccessJoranSpringProfileModelHandler",
            "io.github.seijikohara.logback.access.joran.LogbackAccessJoranSpringPropertyModelHandler",
            "io.github.seijikohara.logback.access.joran.LogbackAccessJoranSpringProfileAction",
            "io.github.seijikohara.logback.access.joran.LogbackAccessJoranSpringPropertyAction",
            // Logback Core classes
            "ch.qos.logback.access.common.spi.AccessContext",
            "ch.qos.logback.access.common.spi.AccessEvent",
            "ch.qos.logback.core.ConsoleAppender",
            "ch.qos.logback.core.encoder.LayoutWrappingEncoder",
            "ch.qos.logback.access.common.PatternLayout",
            "ch.qos.logback.access.common.PatternLayoutEncoder",
            "ch.qos.logback.core.pattern.PatternLayoutEncoderBase",
        )

        /**
         * Resource patterns requiring hints.
         */
        private val RESOURCE_PATTERNS = listOf(
            "io/github/seijikohara/logback/access/logback-access-spring.xml",
            "META-INF/spring-configuration-metadata.json",
            "logback-access.xml",
            "logback-access-spring.xml",
            "logback-access-test.xml",
            "logback-access-test-spring.xml",
        )

        /**
         * Serializable classes requiring hints.
         */
        private val SERIALIZABLE_CLASSES = listOf(
            "io.github.seijikohara.logback.access.LogbackAccessEvent",
            "io.github.seijikohara.logback.access.LogbackAccessEventSource\$Fixed",
        )
    }
}
