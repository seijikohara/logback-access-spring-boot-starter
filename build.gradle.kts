plugins {
    alias(libs.plugins.axion.release)
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.cyclonedx)
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
}
