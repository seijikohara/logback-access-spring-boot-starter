plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "logback-access-spring-boot-starter-root"
include("logback-access-spring-boot-starter")
include("examples:common")
include("examples:tomcat-mvc")
include("examples:tomcat-webflux")
include("examples:jetty-mvc")
include("examples:jetty-webflux")

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}
