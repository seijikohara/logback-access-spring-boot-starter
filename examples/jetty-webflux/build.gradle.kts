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

configurations.all {
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
}

dependencies {
    implementation(project(":logback-access-spring-boot-starter"))
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.jetty)
    implementation(libs.jspecify)

    testImplementation(project(":examples:common"))
    testImplementation(libs.spring.boot.starter.test)

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
