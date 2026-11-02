plugins {
    alias(libs.plugins.axion.release)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.version.catalog.update)
}

apiValidation {
    // The published starter module is tracked so its public surface (the auto-configuration
    // entry point and the deprecated compatibility alias) is enforced against drift.
    ignoredProjects += listOf(
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
    // buildSrc-only libraries that the root update task sees as unused are retained via `# @keep`
    // comments in gradle/libs.versions.toml, since the keep {} block only controls versions.
}

// Every module pins its compile toolchain to Java 21, so without an override the CI
// matrix would run tests on 21 regardless of the JDK installed on the runner. CI passes
// -PtestJavaVersion so the compiled artifacts are exercised on each supported runtime.
val testJavaVersion = providers.gradleProperty("testJavaVersion").map(JavaLanguageVersion::of)

// Each subproject requests its plugins in its own plugins {} block and resolves its own build
// classpath, so a root buildscript constraint would not reach those classpaths. These floors lift
// vulnerable transitive dependencies of the plugins until the plugins ship patched versions; a
// constraint has no effect on a classpath that does not contain the module. The root catalog is
// captured first because `libs` inside subprojects {} would look up each subproject's extension.
val catalog = libs
subprojects {
    buildscript.dependencies.constraints {
        add("classpath", catalog.guava) {
            because("the CycloneDX plugin pulls Guava 33.6.0 (GHSA-xxph-c9ww-hj94)")
        }
        add("classpath", catalog.jackson.core.cyclonedx) {
            because("the CycloneDX plugin pulls Jackson 2.22.1 (GHSA-p6pp-m3f8-5c89, GHSA-7hhh-6rmp-j9qf)")
        }
        add("classpath", catalog.jackson.databind.cyclonedx) {
            because("the CycloneDX plugin pulls Jackson 2.22.1 (GHSA-wv8q-qhhj-9h54, GHSA-cxp5-3px4-pw24)")
        }
        add("classpath", catalog.commons.lang3) {
            because("the Spring Boot plugin pulls commons-lang3 3.16.0 (GHSA-j288-q9x7-2f5v)")
        }
    }
}

subprojects {
    plugins.withType<JavaBasePlugin> {
        val javaToolchains = extensions.getByType<JavaToolchainService>()
        tasks.withType<Test>().configureEach {
            javaLauncher = javaToolchains.launcherFor {
                languageVersion = testJavaVersion.orElse(JavaLanguageVersion.of(21))
            }
        }
    }
}
