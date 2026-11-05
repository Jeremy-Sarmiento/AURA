// ARCHIVO: settings.gradle.kts
// FUNCIÓN: Configuración del proyecto Gradle: repositorios de plugins/dependencias, nombre del
//          proyecto y módulos incluidos (:app). Se carga antes que cualquier build.gradle.kts.
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

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

// Repositorios desde los que se resuelven las dependencias (solo se declaran aquí).
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

// Nombre del proyecto y módulos incluidos.
rootProject.name = "AURA"

include(":app")
