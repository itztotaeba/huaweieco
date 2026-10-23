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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ====================================================================
        // REPO MAVEN RESMI HUAWEI — WAJIB!
        // Seluruh artefak "com.huawei.hms:*" (Account/Location/Map/Push/Scan/
        // SafetyDetect/FIDO) TIDAK ada di Google/Maven Central. Tanpa baris ini
        // build akan gagal dengan error "Could not find com.huawei.hms:...".
        // ====================================================================
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
}

rootProject.name = "Huawei Ecosystem"
include(":app")
