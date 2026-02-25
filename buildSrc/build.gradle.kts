plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.gradle.dokka.plugin)
    implementation(libs.gradle.kotlin.plugin)
    implementation(libs.gradle.maven.publish.plugin)
}
