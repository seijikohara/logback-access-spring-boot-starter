package io.github.seijikohara.spring.boot.logback.access

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class LocalPortStrategySpec :
    FunSpec({
        test("enum has LOCAL and SERVER values") {
            LocalPortStrategy.entries shouldContainExactlyInAnyOrder
                listOf(
                    LocalPortStrategy.LOCAL,
                    LocalPortStrategy.SERVER,
                )
        }

        test("name returns expected string for configuration binding") {
            LocalPortStrategy.LOCAL.name shouldBe "LOCAL"
            LocalPortStrategy.SERVER.name shouldBe "SERVER"
        }

        test("valueOf resolves from string") {
            LocalPortStrategy.valueOf("LOCAL") shouldBe LocalPortStrategy.LOCAL
            LocalPortStrategy.valueOf("SERVER") shouldBe LocalPortStrategy.SERVER
        }
    })
