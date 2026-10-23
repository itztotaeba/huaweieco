// ============================================================================
// app/build.gradle.kts — konfigurasi modul aplikasi (bahasa: Kotlin DSL)
// ----------------------------------------------------------------------------
// File ini memberitahu Gradle: plugin apa yang dipakai, SDK & versi apa yang
// ditargetkan, fitur apa yang diaktifkan, dan library apa yang dibutuhkan.
// ============================================================================

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
        // Placeholder untuk AppGallery Connect (AGC). Beberapa kit (Push/
        // Account) membaca APP ID dari metadata manifest. Isi nilainya dengan
        // App ID dari konsol Huawei Developer, atau taruh file
        // agconnect-services.json hasil download dari AGC ke folder app/.
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
    //    DULU kita menambahkan "com.huawei.hms:availableaccount", tetapi artefak itu
    //    TIDAK tersedia di Maven repo Huawei (diverifikasi: HTTP 404), sehingga
    //    gradle sync gagal "Failed to resolve". API Account Kit (HuaweiIdAuthService,
    //    Scopes, dll.) sebenarnya sudah tercakup lewat hms-base di atas, jadi tidak
    //    perlu dependensi tambahan. Komentar ini ditinggalkan supaya alurnya jelas.
    // implementation("com.huawei.hms:availableaccount:<versi>")  // <- nonaktif

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
