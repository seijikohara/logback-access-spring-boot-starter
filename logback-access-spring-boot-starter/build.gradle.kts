plugins {
    id("maven-publish-conventions")
    // Applied per published module (not at the root) so the SBOM reflects only the
    // released artifact's dependencies, not the example apps' test dependencies.
    alias(libs.plugins.cyclonedx)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    `java-library`
}

group = rootProject.group
version = rootProject.version

mavenPublishing {
    pom {
        name = "Logback Access Spring Boot Starter"
        description = "Spring Boot Starter for Logback Access with auto-configuration support for Tomcat and Jetty"
    }
}

dependencies {
    api(project(":logback-access-spring-boot-starter-core"))
    api(platform(libs.spring.boot.dependencies))

    implementation(libs.spring.boot.starter)
    implementation(libs.kotlin.logging)

    compileOnly(libs.spring.boot.starter.tomcat)
    compileOnly(libs.spring.boot.starter.jetty)
    compileOnly(libs.spring.boot.starter.security)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

// The SBOM describes what a consumer of the published artifact pulls in, so restrict it
// to the runtime dependency graph; the default includes test configurations. The
// aggregate cyclonedxBom task builds on this task's output.
tasks.cyclonedxDirectBom {
    includeConfigs = listOf("runtimeClasspath")
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "io.github.seijikohara.logback.access.spring")
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
        named<JvmTestSuite>("test") {
            useJUnitJupiter()
            dependencies {
                implementation(platform(libs.kotest.bom))
                implementation(libs.kotest.runner.junit5)
                implementation(libs.kotest.assertions.core)
                implementation(libs.mockk)
                implementation(libs.spring.test)
                implementation(libs.spring.boot.test)
                implementation(libs.assertj.core)
                implementation(libs.spring.boot.starter.tomcat)
                implementation(libs.spring.boot.starter.jetty)
                implementation(libs.spring.boot.starter.webflux)
                implementation(libs.spring.boot.starter.security)
            }
        }
    }
}
