import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("com.vanniktech.maven.publish")
    `maven-publish`
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Dokka("dokkaGenerateHtml"),
            sourcesJar = SourcesJar.Sources(),
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

publishing {
    publications.withType<MavenPublication>().configureEach {
        versionMapping {
            allVariants {
                fromResolutionResult()
            }
        }
    }
}
