plugins {
    id("maven-publish-conventions")
    id("org.jetbrains.kotlin.kapt")
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

    implementation(libs.spring.boot.starter)
    implementation(libs.kotlin.logging)
    // Runtime requirement for Spring Boot constructor binding of the @ConfigurationProperties
    // data class. Not part of the public compile surface, so it is not an api dependency.
    implementation(libs.kotlin.reflect)

    // Generates META-INF/spring-configuration-metadata.json for logback.access.* so consumers get
    // IDE auto-completion and property documentation. The Spring Boot configuration processor is a
    // Java annotation processor, so it runs through kapt for this Kotlin module. The kapt
    // configuration needs the BOM applied directly to resolve the processor version.
    kapt(platform(libs.spring.boot.dependencies))
    kapt(libs.spring.boot.configuration.processor)
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
