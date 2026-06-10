pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "FocusFarm"
include(":app")
include(":core:model")
include(":core:data")
include(":core:ui")
include(":core:monetization")
include(":feature-home")
include(":feature-session")
include(":feature-stats")
