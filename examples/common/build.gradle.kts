plugins {
    `java-library`
    alias(libs.plugins.errorprone)
    alias(libs.plugins.nullaway)
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
    api(project(":logback-access-spring-boot-starter"))
    api(platform(libs.spring.boot.dependencies))
    api(libs.logback.access.common)
    api(libs.jspecify)
    api(libs.logstash.logback.encoder)
    api(libs.junit.jupiter.api)
    api(libs.assertj.core)
    api(libs.spring.boot.test)
    api(libs.spring.test)

    // MVC common code dependencies (compileOnly to avoid forcing MVC on all consumers)
    compileOnly(libs.spring.boot.starter.webmvc)
    compileOnly(libs.spring.boot.starter.security)

    // WebFlux common code dependencies (compileOnly to avoid forcing WebFlux on all consumers)
    compileOnly(libs.spring.boot.starter.webflux)

    errorprone(libs.error.prone.core)
    errorprone(libs.nullaway)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:all,-processing,-serial")
    options.compilerArgs.add("-parameters")
}
