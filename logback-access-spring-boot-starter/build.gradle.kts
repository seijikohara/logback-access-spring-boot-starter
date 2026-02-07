plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.spotless)
    `java-library`
}

mavenPublishing {
    pom {
        name = "Logback Access Spring Boot Starter"
        description = "Spring Boot Starter for Logback Access with auto-configuration support for Tomcat and Jetty"
    }
}

dependencies {
    api(platform(libs.spring.boot.dependencies))

    implementation(libs.spring.boot.starter)
    implementation(libs.logback.access.common)
    implementation(libs.kotlin.logging)

    api(libs.kotlin.reflect)

    compileOnly(libs.spring.boot.starter.webmvc)
    compileOnly(libs.spring.boot.starter.webflux)
    compileOnly(libs.spring.boot.starter.tomcat)
    compileOnly(libs.spring.boot.starter.jetty)
    compileOnly(libs.spring.boot.starter.security)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
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
                implementation("org.springframework:spring-test")
            }
        }
    }
}
