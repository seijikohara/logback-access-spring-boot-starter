package io.github.seijikohara.spring.boot.logback.access

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

class LocalPortStrategySpec :
    FunSpec({
        test("enum has LOCAL and SERVER values") {
            LocalPortStrategy.entries shouldContainExactlyInAnyOrder
                listOf(
                    LocalPortStrategy.LOCAL,
                    LocalPortStrategy.SERVER,
                )
        }

        test("enum values have expected ordinals") {
            LocalPortStrategy.LOCAL.ordinal
            LocalPortStrategy.SERVER.ordinal
        }
    })
