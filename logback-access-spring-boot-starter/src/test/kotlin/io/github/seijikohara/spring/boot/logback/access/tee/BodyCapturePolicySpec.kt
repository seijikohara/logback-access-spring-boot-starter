package io.github.seijikohara.spring.boot.logback.access.tee

import io.github.seijikohara.spring.boot.logback.access.LogbackAccessProperties.TeeFilterProperties
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BodyCapturePolicySpec :
    FunSpec({
        val defaultProperties =
            TeeFilterProperties(
                enabled = true,
                includeHosts = null,
                excludeHosts = null,
                maxPayloadSize = 65536L,
                allowedContentTypes = null,
            )

        context("evaluate — text content types") {
            test("allows text/plain") {
                BodyCapturePolicy.evaluate("text/plain", 100, defaultProperties) shouldBe null
            }

            test("allows text/html with charset") {
                BodyCapturePolicy.evaluate("text/html; charset=UTF-8", 100, defaultProperties) shouldBe null
            }

            test("allows application/json") {
                BodyCapturePolicy.evaluate("application/json", 100, defaultProperties) shouldBe null
            }

            test("allows application/xml") {
                BodyCapturePolicy.evaluate("application/xml", 100, defaultProperties) shouldBe null
            }

            test("allows application/vnd.api+json") {
                BodyCapturePolicy.evaluate("application/vnd.api+json", 100, defaultProperties) shouldBe null
            }

            test("allows application/atom+xml") {
                BodyCapturePolicy.evaluate("application/atom+xml", 100, defaultProperties) shouldBe null
            }

            test("allows application/x-www-form-urlencoded") {
                BodyCapturePolicy.evaluate("application/x-www-form-urlencoded", 100, defaultProperties) shouldBe null
            }
        }

        context("evaluate — binary content types") {
            test("suppresses image/png with IMAGE sentinel") {
                BodyCapturePolicy.evaluate("image/png", 100, defaultProperties) shouldBe
                    "[IMAGE CONTENTS SUPPRESSED]"
            }

            test("suppresses image/jpeg with IMAGE sentinel") {
                BodyCapturePolicy.evaluate("image/jpeg", 100, defaultProperties) shouldBe
                    "[IMAGE CONTENTS SUPPRESSED]"
            }

            test("suppresses video/mp4 with BINARY sentinel") {
                BodyCapturePolicy.evaluate("video/mp4", 100, defaultProperties) shouldBe
                    "[BINARY CONTENT SUPPRESSED]"
            }

            test("suppresses application/octet-stream with BINARY sentinel") {
                BodyCapturePolicy.evaluate("application/octet-stream", 100, defaultProperties) shouldBe
                    "[BINARY CONTENT SUPPRESSED]"
            }

            test("suppresses audio/mpeg with BINARY sentinel") {
                BodyCapturePolicy.evaluate("audio/mpeg", 100, defaultProperties) shouldBe
                    "[BINARY CONTENT SUPPRESSED]"
            }
        }

        context("evaluate — size limit") {
            test("suppresses payload exceeding maxPayloadSize") {
                BodyCapturePolicy.evaluate("text/plain", 70000, defaultProperties) shouldBe
                    "[CONTENT TOO LARGE]"
            }

            test("allows payload at exactly maxPayloadSize") {
                BodyCapturePolicy.evaluate("text/plain", 65536, defaultProperties) shouldBe null
            }

            test("size check takes precedence over content type") {
                BodyCapturePolicy.evaluate("application/octet-stream", 70000, defaultProperties) shouldBe
                    "[CONTENT TOO LARGE]"
            }
        }

        context("evaluate — custom allowed content types") {
            test("uses custom list when specified (override mode)") {
                val customProperties =
                    defaultProperties.copy(
                        allowedContentTypes = listOf("application/pdf"),
                    )
                BodyCapturePolicy.evaluate("application/pdf", 100, customProperties) shouldBe null
            }

            test("rejects previously allowed type when custom list excludes it") {
                val customProperties =
                    defaultProperties.copy(
                        allowedContentTypes = listOf("application/pdf"),
                    )
                BodyCapturePolicy.evaluate("text/plain", 100, customProperties) shouldBe
                    "[BINARY CONTENT SUPPRESSED]"
            }
        }

        context("evaluate — null content type") {
            test("allows null content type (treated as text)") {
                BodyCapturePolicy.evaluate(null, 100, defaultProperties) shouldBe null
            }
        }

        context("resolveCharset") {
            test("resolves Shift_JIS") {
                BodyCapturePolicy.resolveCharset("Shift_JIS").name() shouldBe "Shift_JIS"
            }

            test("falls back to UTF-8 for null encoding") {
                BodyCapturePolicy.resolveCharset(null) shouldBe Charsets.UTF_8
            }

            test("falls back to UTF-8 for invalid encoding") {
                BodyCapturePolicy.resolveCharset("INVALID-CHARSET") shouldBe Charsets.UTF_8
            }
        }
    })
