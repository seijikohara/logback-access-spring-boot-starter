package io.github.seijikohara.spring.boot.logback.access.autoconfigure

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.TypeReference

/**
 * Registers GraalVM native-image hints for the reflective and resource access paths used by the
 * Joran-based access-log configuration, so the starter works under AOT / native compilation.
 *
 * The Joran model, action, and handler classes are referenced by type during rule registration and
 * model processing, and the bundled fallback configuration is loaded as a classpath resource. Types
 * are registered by name because the Joran extension types are `internal` to the core module.
 */
internal class LogbackAccessRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(
        hints: RuntimeHints,
        classLoader: ClassLoader?,
    ) {
        JORAN_TYPES.forEach { type ->
            hints.reflection().registerType(
                TypeReference.of(type),
                MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
            )
        }
        hints.resources().registerPattern(FALLBACK_CONFIG_RESOURCE)
    }

    private companion object {
        private const val JORAN_PACKAGE = "io.github.seijikohara.spring.boot.logback.access.joran"

        private val JORAN_TYPES =
            listOf(
                "$JORAN_PACKAGE.AccessJoranConfigurator",
                "$JORAN_PACKAGE.SpringPropertyAction",
                "$JORAN_PACKAGE.SpringPropertyModel",
                "$JORAN_PACKAGE.SpringPropertyModelHandler",
                "$JORAN_PACKAGE.SpringProfileAction",
                "$JORAN_PACKAGE.SpringProfileModel",
                "$JORAN_PACKAGE.SpringProfileModelHandler",
            )

        private const val FALLBACK_CONFIG_RESOURCE =
            "io/github/seijikohara/spring/boot/logback/access/logback-access-spring.xml"
    }
}
