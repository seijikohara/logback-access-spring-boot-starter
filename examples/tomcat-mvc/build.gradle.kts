plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.errorprone)
    alias(libs.plugins.nullaway)
    `java-library`
}

group = rootProject.group
version = rootProject.version

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

nullaway {
    annotatedPackages.add("examples")
    jspecifyMode = true
}

dependencies {
    implementation(project(":logback-access-spring-boot-starter"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.jspecify)

    errorprone(libs.error.prone.core)
    errorprone(libs.nullaway)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            dependencies {
                implementation(project(":examples:common"))
                implementation(libs.spring.boot.starter.test)
                implementation(libs.logstash.logback.encoder)
            }
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:all,-processing,-serial")
}
