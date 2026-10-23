// Top-level build file where you can add configuration options common to all sub-projects/modules.

// ============================================================================
// BUILDSCRIPT — tempat Gradle mencari CLASS plugin sebelum modul di-evaluasi.
// Plugin AppGallery Connect (agcp) TIDAK ada di Maven Central / Google repo,
// jadi kita daftarkan Maven repo resmi Huawei + koordinat artifact plugin-nya
// di sini. Setelah ini, modul app bisa memanggil apply("com.huawei.agconnect").
// Versi 1.9.5.300 diverifikasi tersedia (HTTP 200) di developer.huawei.com/repo.
// ============================================================================
buildscript {
    repositories {
        google()
        mavenCentral()
        // Repo resmi SDK & toolchain Huawei — WAJIB untuk plugin agcp.
        maven { url = uri("https://developer.huawei.com/repo/") }
    }
    dependencies {
        // Classpath plugin AGC: memproses agconnect-services.json saat build.
        classpath("com.huawei.agconnect:agcp:1.9.5.300")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}