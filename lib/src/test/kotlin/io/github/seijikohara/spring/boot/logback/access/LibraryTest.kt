package io.github.seijikohara.spring.boot.logback.access

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LibraryTest :
    FunSpec({
        test("someLibraryMethod should return true") {
            val classUnderTest = Library()
            classUnderTest.someLibraryMethod() shouldBe true
        }
    })
