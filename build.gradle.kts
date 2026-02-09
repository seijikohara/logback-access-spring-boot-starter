import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.axion.release)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.cyclonedx)
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.version.catalog.update)
}

apiValidation {
    ignoredProjects += listOf(
        "logback-access-spring-boot-starter",
        "common",
        "tomcat-mvc",
        "jetty-mvc",
        "tomcat-webflux",
        "jetty-webflux",
    )
}

group = "io.github.seijikohara"

// Configure version management with axion-release-plugin
// Version is derived from Git tags (e.g., v1.2.0 -> 1.2.0)
scmVersion {
    useHighestVersion = true
    tag {
        prefix = "v"
        versionSeparator = ""
    }
    versionCreator("simple")
    repository {
        pushTagsOnly = true
    }
    checks {
        uncommittedChanges = false
        aheadOfRemote = false
    }
}

version = scmVersion.version

versionCatalogUpdate {
    sortByKey = true
}

allprojects {
    group = rootProject.group
    version = rootProject.version
}

// Configure reproducible builds for all archive tasks
subprojects {
    tasks.withType<AbstractArchiveTask>().configureEach {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}

// Common maven-publish configuration for all publishable subprojects
subprojects {
    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        // Resolve BOM-managed dependency versions in the generated POM
        pluginManager.withPlugin("maven-publish") {
            extensions.configure<PublishingExtension> {
                publications.withType<MavenPublication>().configureEach {
                    versionMapping {
                        allVariants {
                            fromResolutionResult()
                        }
                    }
                }
            }
        }

        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()

            // Configure to use Dokka for javadoc (Dokka V2)
            configure(
                JavaLibrary(
                    javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"),
                    sourcesJar = true,
                ),
            )

            pom {
                url = "https://github.com/seijikohara/logback-access-spring-boot-starter"
                inceptionYear = "2026"
                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
                developers {
                    developer {
                        id = "seijikohara"
                        name = "Seiji Kohara"
                        email = "seiji.kohara@gmail.com"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/seijikohara/logback-access-spring-boot-starter.git"
                    developerConnection = "scm:git:ssh://github.com/seijikohara/logback-access-spring-boot-starter.git"
                    url = "https://github.com/seijikohara/logback-access-spring-boot-starter"
                }
            }
        }
    }
}
