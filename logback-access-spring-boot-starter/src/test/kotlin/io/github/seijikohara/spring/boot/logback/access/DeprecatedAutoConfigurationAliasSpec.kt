package io.github.seijikohara.spring.boot.logback.access

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.github.seijikohara.spring.boot.logback.access.autoconfigure.LogbackAccessAutoConfiguration as AutoConfig

/**
 * Guards the backward-compatibility surface kept for consumers that referenced the pre-rename
 * auto-configuration. Both the deprecated typealias and the `.replacements` remap must keep
 * pointing at the current autoconfigure class until they are intentionally removed in v2.0.0.
 */
class DeprecatedAutoConfigurationAliasSpec :
    FunSpec({
        test("deprecated root-package alias resolves to the autoconfigure class") {
            @Suppress("DEPRECATION")
            LogbackAccessAutoConfiguration::class.java shouldBe AutoConfig::class.java
        }

        test(".replacements remaps the old FQN to the autoconfigure class") {
            val replacements =
                DeprecatedAutoConfigurationAliasSpec::class.java
                    .getResource("/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.replacements")
                    ?.readText()
                    .orEmpty()

            replacements shouldContain
                "io.github.seijikohara.spring.boot.logback.access.LogbackAccessAutoConfiguration=" +
                "io.github.seijikohara.spring.boot.logback.access.autoconfigure.LogbackAccessAutoConfiguration"
        }
    })
