plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.errorprone)
    alias(libs.plugins.nullaway)
    `java-library`
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
    implementation(project(":logback-access-spring-boot-starter"))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.jspecify)

    testImplementation(project(":examples:common"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.logstash.logback.encoder)

    errorprone(libs.error.prone.core)
    errorprone(libs.nullaway)
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:all,-processing,-serial")
}
