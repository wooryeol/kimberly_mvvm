pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        flatDir {
            dirs("app/libs")
        }
        flatDir {
            dirs("app/libs")
        }
    }
}

rootProject.name = "Kimberly_mvvm"
include(":app")
 