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
