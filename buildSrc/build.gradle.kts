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

    // Lift the Jackson version that the Dokka Gradle plugin puts on the build classpath until a
    // Dokka release ships a patched one.
    constraints {
        implementation(libs.jackson.core.dokka) {
            because("Dokka 2.2.0 pulls Jackson 2.15.3 (GHSA-7hhh-6rmp-j9qf, GHSA-r7wm-3cxj-wff9)")
        }
        implementation(libs.jackson.databind.dokka) {
            because("Dokka 2.2.0 pulls Jackson 2.15.3 (GHSA-wv8q-qhhj-9h54, GHSA-cxp5-3px4-pw24)")
        }
    }
}
