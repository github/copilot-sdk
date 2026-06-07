pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "koog-copilot-agent"

providers.environmentVariable("KOOG_INCLUDE_BUILD").orNull
    ?.takeIf { it.isNotBlank() }
    ?.let { includeBuild(file(it)) }
