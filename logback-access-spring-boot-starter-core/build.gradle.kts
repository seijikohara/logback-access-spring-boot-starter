plugins {
    id("maven-publish-conventions")
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    `java-library`
}

group = rootProject.group
version = rootProject.version

mavenPublishing {
    pom {
        name = "Logback Access Spring Boot Starter Core"
        description = "Core API for Logback Access Spring Boot Starter"
    }
}

dependencies {
    api(platform(libs.spring.boot.dependencies))
    api(libs.logback.access.common)
    api(libs.kotlin.reflect)

    implementation(libs.spring.boot.starter)
    implementation(libs.kotlin.logging)
}

kotlin {
    explicitApi()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.seijikohara.logback.access.core")
    }
}

spotless {
    kotlin {
        ktlint()
    }
    kotlinGradle {
        ktlint()
    }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(platform(libs.kotest.bom))
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotest.assertions.core)
                implementation(libs.mockk)
                implementation(libs.spring.test)
            }
        }
    }
}
