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
            connection = "scm:git:https://github.com/seijikohara/logback-access-spring-boot-starter.git"
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

// Dokka and the Kotlin plugin resolve their own tool configurations in each published module.
// These floors lift vulnerable transitive dependencies there until the plugins ship patched
// versions. Neither configuration feeds the published artifacts or the SBOM. Precompiled script
// plugins get no `libs` accessor, so the catalog is looked up by name.
val catalog = the<VersionCatalogsExtension>().named("libs")
dependencies.constraints {
    add("dokkaHtmlGeneratorRuntime", catalog.findLibrary("jackson-core-dokka").get()) {
        because("Dokka 2.2.0 runs with Jackson 2.15.3 (GHSA-7hhh-6rmp-j9qf, GHSA-r7wm-3cxj-wff9)")
    }
    add("dokkaHtmlGeneratorRuntime", catalog.findLibrary("jackson-databind-dokka").get()) {
        because("Dokka 2.2.0 runs with Jackson 2.15.3 (GHSA-wv8q-qhhj-9h54, GHSA-cxp5-3px4-pw24)")
    }
    add("dokkaHtmlGeneratorRuntime", catalog.findLibrary("freemarker").get()) {
        because("Dokka 2.2.0 runs with FreeMarker 2.3.32 (GHSA-27j2-h3m2-8237)")
    }
    add("dokkaHtmlGeneratorRuntime", catalog.findLibrary("jsoup").get()) {
        because("Dokka 2.2.0 runs with jsoup 1.16.1 (GHSA-65r4-943x-97jj, GHSA-pmhh-3w7g-xqp8)")
    }
    add("kotlinBouncyCastleConfiguration", catalog.findLibrary("bouncycastle-bcprov").get()) {
        because("the Kotlin plugin's PGP helper tasks use bcprov 1.84 (GHSA-9pwp-9qqc-pr26, GHSA-qp49-qgx5-5m26)")
    }
}
