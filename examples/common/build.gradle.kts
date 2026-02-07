plugins {
    `java-library`
    alias(libs.plugins.errorprone)
    alias(libs.plugins.nullaway)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

nullaway {
    annotatedPackages.add("io.github.seijikohara")
    jspecifyMode = true
}

dependencies {
    api(project(":logback-access-spring-boot-starter"))
    api(platform(libs.spring.boot.dependencies))
    api(libs.logback.access.common)
    api(libs.jspecify)
    api(libs.logstash.logback.encoder)
    api("org.junit.jupiter:junit-jupiter-api")
    api("org.assertj:assertj-core")
    api("org.springframework.boot:spring-boot-test")
    api("org.springframework:spring-test")

    // MVC common code dependencies (compileOnly to avoid forcing MVC on all consumers)
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc")
    compileOnly("org.springframework.boot:spring-boot-starter-security")

    // WebFlux common code dependencies (compileOnly to avoid forcing WebFlux on all consumers)
    compileOnly("org.springframework.boot:spring-boot-starter-webflux")

    errorprone(libs.error.prone.core)
    errorprone(libs.nullaway)
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:all,-processing,-serial")
    options.compilerArgs.add("-parameters")
}
