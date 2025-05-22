pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    maven(url = "https://www.jitpack.io")
    maven {
      url = uri("https://androidx.dev/snapshots/builds/13536396/artifacts/repository")
    }
    mavenCentral()
  }
}

rootProject.name = "mpvKt"
include(":app")
