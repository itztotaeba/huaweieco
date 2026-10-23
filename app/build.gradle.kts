// ============================================================================
// app/build.gradle.kts — konfigurasi modul aplikasi (bahasa: Kotlin DSL)
// ----------------------------------------------------------------------------
// File ini memberitahu Gradle: plugin apa yang dipakai, SDK & versi apa yang
// ditargetkan, fitur apa yang diaktifkan, dan library apa yang dibutuhkan.
// ============================================================================

import java.net.URI

plugins {
    // Alias dari gradle/libs.versions.toml -> com.android.application v9.4.1.
    // NB: AGP 9 sudah menyertakan dukungan Kotlin secara built-in, jadi kita
    // TIDAK menambahkan plugin "org.jetbrains.kotlin.android" (bila ditambah
    // justru error: tidak kompatibel dengan built-in Kotlin).
    alias(libs.plugins.android.application)

    // Compose Compiler plugin (org.jetbrains.kotlin.plugin.compose) — WAJIB
    // sejak Kotlin 2.x saat `compose = true`. Plugin ini yang menerjemahkan
    // anotasi @Composable menjadi kode Java bytecode.
    alias(libs.plugins.kotlin.compose.compiler)
}

// ------------------------------------------------------------------------
// PLUGIN APPGALLERY CONNECT (AGC) — membaca file app/agconnect-services.json
// Fungsi plugin: menyuntikkan App ID, API key, dan metadata lain ke
// AndroidManifest + BuildConfig saat build, jadi kit seperti Push/Account
// langsung punya konfigurasi tanpa tulis manual di kode.
// Classpath plugin dideklarasikan di root build.gradle.kts (buildscript).
// PENTING: plugin hanya aktif bila file agconnect-services.json ADA. Kalau
// belum ada, build dilewati dengan pesan jelas (aplikasi tetap bisa jalan
// karena App ID dibaca dari placeholder manifest HMS_APP_ID di bawah).
// ------------------------------------------------------------------------
if (file("agconnect-services.json").exists()) {
    apply(plugin = "com.huawei.agconnect")
} else {
    logger.warn(
        "[AGC] app/agconnect-services.json belum ditemukan -> plugin AGC " +
        "dilewati. Taruh file hasil download dari AppGallery Connect ke " +
        "folder app/, atau isi App ID manual pada manifestPlaceholders " +
        "[\"HMS_APP_ID\"] di file ini."
    )
}

android {
    // Namespace = paket R class & BuildConfig. applicationId bisa beda, tapi
    // di sini kita samakan saja.
    namespace = "com.kinyo.huaweiecosystem"

    compileSdk {
        // Kompilasi terhadap API 37 (Android terbaru saat ini). Kode tetap
        // bisa berjalan di perangkat lama karena ada minSdk di bawah.
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.kinyo.huaweiecosystem"  // ID unik di Play/AppGallery
        minSdk = 24      // HMS Core umumnya butuh minimal Android 7.0 (API 24)
        targetSdk = 37   // Perilaku runtime mengikuti aturan API 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // --------------------------------------------------------------------
        // 👉👉 MASUKKAN APP ID DI SINI (satu-satunya tempat yang perlu diubah)
        // Ambil dari AppGallery Connect -> Project Settings -> General Info,
        // field "App ID" (angka panjang, contoh: 110812345).
        // Ganti string "REPLACE_WITH_YOUR_APP_ID" dengan angka tsb (tetap dalam
        // tanda kutip). Nilai ini disuntikkan ke AndroidManifest.xml sebagai
        // metadata "push_service_appid" — dipakai HMS Core utk mengenali app.
        // ALTERNATIF: kalau kamu menaruh file agconnect-services.json di folder
        // app/, sebagian kit bisa membaca App ID otomatis dari file itu; tapi
        // placeholder manifest di bawah tetap sebaiknya diisi juga.
        // --------------------------------------------------------------------
        manifestPlaceholders["HMS_APP_ID"] = "REPLACE_WITH_YOUR_APP_ID"
    }

    buildTypes {
        release {
            optimization {
                enable = true                     // R8/minify aktif utk rilis
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }

    // ------------------------------------------------------------------------
    // Compose harus diaktifkan lewat blok `buildFeatures`. Setelah true,
    // AGP menyiapkan Compose compiler (dari Kotlin built-in AGP 9) sehingga
    // fungsi @Composable bisa dikompilasi.
    // ------------------------------------------------------------------------
    buildFeatures {
        compose = true
    }

    compileOptions {
        // Versi bytecode Java yang dihasilkan (11 = standar modern aman).
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // ------------------------------------------------------------------------
    // DUKUNGAN 16 KB PAGE SIZE (Android 15+ / syarat Google Play Nov 2025)
    // ------------------------------------------------------------------------
    // Beberapa SDK Huawei masih membawa native library lama yang LOAD
    // segment-nya hanya ber-align 4 KB — contoh: Scan Kit menarik dependency
    // "com.huawei.hms:ml-computer-sdkbase-inner" yang berisi lib/arm64-v8a/
    // libml.so. Karena kita TIDAK memakai fitur scan lokal berbasis ML itu
    // (mode default Remote-Scan sudah cukup), kita buang file .so tersebut
    // dari APK lewat packagingOptions agar warning 16KB hilang sepenuhnya.
    packaging {
        jniLibs {
            // Entri di bawah = path relatif terhadap isi APK (bukan AAR).
            // '**' memastikan semua ABI tercakup, meski praktis hanya
            // arm64-v8a yang bermasalah dengan alignment 4 KB.
            excludes += "**/libml.so"
        }
    }
}

dependencies {
    // ================= JETPACK COMPOSE =================
    // BOM = Bill Of Materials: sekali import, semua lib Compose di bawahnya
    // otomatis memakai versi yang saling cocok (tanpa tulis versi satu-satu).
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)

    implementation(libs.androidx.compose.ui)                    // inti UI Compose
    implementation(libs.androidx.compose.ui.graphics)           // warna, kanvas, dll
    implementation(libs.androidx.compose.ui.tooling.preview)    // dukungan @Preview
    implementation(libs.androidx.compose.foundation)            // layout & gesture
    implementation(libs.androidx.compose.material3)             // tema Material You
    // Ikon vektor Material (Icons.AutoMirrored.Filled.ArrowBack, dll).
    implementation("androidx.compose.material:material-icons-extended")
    androidTestImplementation(composeBom)                       // BOM juga utk tes
    debugImplementation(libs.androidx.compose.ui.tooling)       // tooling hanya debug

    // ================= ANDROIDX CORE =================
    implementation(libs.androidx.core.ktx)              // ekstensi Kotlin (Context, dsb)
    implementation(libs.androidx.appcompat)             // dipakai sebagian remote-UI HMS
    implementation(libs.androidx.activity.compose)      // setContent{} di ComponentActivity
    implementation(libs.androidx.lifecycle.runtime.ktx) // lifecycleScope, repeatOnLifecycle
    implementation(libs.androidx.navigation.compose)    // NavHost + composable() routes

    // ================= HUAWEI HMS CORE KITS =================
    // hms-base = fondasi semua kit: berisi GRS (Gradle Runtime Service),
    // pengecekan ketersediaan HMS di perangkat (ApiAvailability), dan
    // sistem Task<> async. Semua kit lain bergantung padanya.
    implementation(libs.hms.base)

    // 1) Account Kit — sign-in dengan Huawei ID.
    //    Sejarah: artefak lama "com.huawei.hms:availableaccount" memang sudah
    //    DIHAPUS dari Maven repo Huawei (HTTP 404). Tetapi Account Kit yang
    //    masih tersedia resmi adalah "com.huawei.hms:hwid" (rilis 6.15.0.300,
    //    diverifikasi langsung dari developer.huawei.com/repo). AAR-nya memuat
    //    class lengkap: HuaweiIdAuthManager, HuaweiIdAuthService,
    //    HuaweiIdAuthParamsHelper, AuthHuaweiId + activity hub internal yang
    //    dibutuhkan alur getSignInIntent(). Tanpa dependensi ini, reflection ke
    //    class Account Kit selalu gagal -> pesan "Class Account Kit tidak
    //    ditemukan" meski HMS Core sehat.
    //
    //    CATATAN PENTING: artefak hwid hanya menyediakan file .aar di repo
    //    Huawei (file .pom-nya HTTP 404 — diverifikasi langsung), sehingga
    //    koordinat Maven "com.huawei.hms:hwid" TIDAK bisa di-resolve Gradle.
    //    Solusi: AAR diunduh oleh task downloadHwidAar (lihat akhir file ini)
    //    ke app/libs/ dan di-import sebagai file lokal. Jika file belum ada
    //    saat pertama sync, jalankan: ./gradlew :app:downloadHwidAar
    implementation(files("libs/hwid-${libs.versions.hmsHwid.get()}.aar"))

    // 2) Location Kit — lokasi terfusi (GPS + jaringan + sensor Huawei)
    implementation(libs.hms.location)

    // 3) Map Kit — peta Huawei/HMS Maps (MapView, marker, camera)
    implementation(libs.hms.maps)

    // 4) Push Kit — token push & pesan data/notifikasi dari server Huawei
    implementation(libs.hms.push)

    // 5) Scan Kit — pemindai barcode/QR (remote & local SDK)
    implementation(libs.hms.scan)

    // 6+7) Safety Detect — deteksi root, emulator, SIM palsu, attestation.
    //      (Text Recognition memakai ML Kit — akan ditambahkan saat halaman
    //       Text Recognition diimplementasikan, agar APK tetap ramping.)
    implementation(libs.hms.safetydetect)

    // 8) FIDO — autentikasi biometrik berbasis standar FIDO2/Huawei ID
    implementation(libs.hms.fido)

    // Dependensi internal yang diminta beberapa kit di atas (site planning
    // untuk Map, open device id untuk Push/Account):
    implementation(libs.hms.site)
    implementation(libs.hms.opendevice)

    // ================= TESTING =================
    testImplementation(libs.junit)                          // unit test JVM
    androidTestImplementation(libs.androidx.junit)          // instrumented test
    androidTestImplementation(libs.androidx.espresso.core)  // UI automator klasik
}

// ============================================================================
// downloadHwidAar — mengoreksi resolve "com.huawei.hms:hwid" (Account Kit)
// ----------------------------------------------------------------------------
// Fakta terverifikasi (2026-10): repo Maven resmi Huawei hanya menyimpan file
// .aar untuk artefak hwid; file .pom-nya HTTP 404. Karena Gradle mewajibkan
// POM saat me-resolve dependensi Maven, koordinat "com.huawei.hms:hwid" TIDAK
// bisa dipakai langsung (sync gagal "Could not find hwid-<v>.pom"). Solusi:
// unduh .aar sekali ke app/libs/ lewat task di bawah, lalu deklarasikan
// implementasinya sebagai file lokal (lihat blok dependencies{}).
// ===========================================================================
// Catatan verifikasi (Gradle 8.x + Kotlin DSL): ekspresi `java.net.URI(...)`
// TIDAK bisa dipakai langsung di dalam closure `doLast {}` — script compiler
// Gradle gagal resolve paket `java.*` di scope tersebut ("Unresolved reference
// 'net'"). Karena itu kelas URI/URL diimpor secara eksplisit di header file
// dan hanya nama pendeknya yang digunakan di dalam task.
val hwidVersion = libs.versions.hmsHwid.get()
val hwidAarFile = file("libs/hwid-$hwidVersion.aar")

tasks.register("downloadHwidAar") {
    group = "huawei"
    description = "Mengunduh AAR Account Kit (hwid) dari repo resmi Huawei ke app/libs/"
    outputs.file(hwidAarFile)
    doLast {
        if (!hwidAarFile.exists()) {
            hwidAarFile.parentFile.mkdirs()
            val url = URI(
                "https://developer.huawei.com/repo/com/huawei/hms/hwid/$hwidVersion/hwid-$hwidVersion.aar"
            ).toURL()
            url.openStream().use { input ->
                hwidAarFile.outputStream().use { output -> input.copyTo(output) }
            }
            logger.lifecycle("downloadHwidAar: tersimpan di ${hwidAarFile.absolutePath}")
        } else {
            logger.lifecycle("downloadHwidAar: AAR sudah ada, lewati unduhan.")
        }
    }
}

// Unduhan berjalan otomatis HANYA saat file belum ada (mis. setelah clone
// bersih atau folder app/libs dihapus). Pengecekan dilakukan di configuration
// phase agar tidak ada I/O jaringan tak perlu saat build reguler.
if (!hwidAarFile.exists()) {
    tasks.matching { it.name == "preBuild" }.configureEach { dependsOn("downloadHwidAar") }
}

