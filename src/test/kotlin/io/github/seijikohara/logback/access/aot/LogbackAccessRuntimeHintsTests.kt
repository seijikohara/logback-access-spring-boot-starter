package io.github.seijikohara.logback.access.aot

import io.github.seijikohara.logback.access.LogbackAccessEvent
import io.github.seijikohara.logback.access.LogbackAccessProperties
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates

/**
 * Tests the [LogbackAccessRuntimeHints].
 */
class LogbackAccessRuntimeHintsTests {

    private val hints = RuntimeHints().also {
        LogbackAccessRuntimeHints().registerHints(it, javaClass.classLoader)
    }

    @Test
    fun `Registers reflection hints for LogbackAccessProperties`() {
        RuntimeHintsPredicates.reflection()
            .onType(LogbackAccessProperties::class.java)
            .test(hints) shouldBe true
    }

    @Test
    fun `Registers reflection hints for LogbackAccessEvent`() {
        RuntimeHintsPredicates.reflection()
            .onType(LogbackAccessEvent::class.java)
            .test(hints) shouldBe true
    }

    @Test
    fun `Registers resource hints for default configuration`() {
        RuntimeHintsPredicates.resource()
            .forResource("io/github/seijikohara/logback/access/logback-access-spring.xml")
            .test(hints) shouldBe true
    }

    @Test
    fun `Registers resource hints for metadata`() {
        RuntimeHintsPredicates.resource()
            .forResource("META-INF/spring-configuration-metadata.json")
            .test(hints) shouldBe true
    }

    @Test
    fun `Registers serialization hints for LogbackAccessEvent`() {
        RuntimeHintsPredicates.serialization()
            .onType(LogbackAccessEvent::class.java)
            .test(hints) shouldBe true
    }
}
