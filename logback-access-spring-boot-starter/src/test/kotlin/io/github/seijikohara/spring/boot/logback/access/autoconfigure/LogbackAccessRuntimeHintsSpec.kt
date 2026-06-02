package io.github.seijikohara.spring.boot.logback.access.autoconfigure

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.TypeReference
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates

class LogbackAccessRuntimeHintsSpec :
    FunSpec({
        val hints = RuntimeHints().also { LogbackAccessRuntimeHints().registerHints(it, javaClass.classLoader) }

        test("registers reflection hints for the Joran springProperty types") {
            RuntimeHintsPredicates
                .reflection()
                .onType(TypeReference.of("io.github.seijikohara.spring.boot.logback.access.joran.SpringPropertyModel"))
                .test(hints) shouldBe true
        }

        test("registers a resource hint for the bundled fallback configuration") {
            RuntimeHintsPredicates
                .resource()
                .forResource("io/github/seijikohara/spring/boot/logback/access/logback-access-spring.xml")
                .test(hints) shouldBe true
        }
    })
