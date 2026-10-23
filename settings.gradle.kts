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
    // FAIL_ON_PROJECT_REPOS: seluruh repository harus dideklarasikan di sini;
    // modul (app/build.gradle.kts) TIDAK boleh menambah repositories() sendiri
    // — kalau melanggar, sync gagal dengan error "Build was configured to
    // prefer settings repositories over project repositories but repository
    // ... was added by build file". Karena itu Account Kit (hwid) kini
    // dikonsumsi sebagai AAR lokal app/libs/ via implementation(files(...))
    // yang tidak menyentuh repositori sama sekali.
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
