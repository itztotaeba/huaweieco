// ============================================================================
// KitMenu.kt — MODEL DATA untuk daftar menu HMS Kit di dashboard
// ============================================================================
package com.kinyo.huaweiecosystem.model

// Enum adalah tipe data dengan jumlah nilai TETAP — cocok untuk daftar kit
// yang tidak berubah-ubah. Setiap konstanta menyimpan metadata menunya.
enum class KitMenu(
    val route: String,      // id navigasi (diketik saat user tap kartu)
    val title: String,      // judul yang tampil di kartu
    val subtitle: String,   // deskripsi singkat fungsi kit
    val icon: String,       // emoji sederhana sbg ikon (tanpa resource gambar)
    val docsUrl: String     // link dokumentasi resmi utk belajar
) {
    ACCOUNT(
        route = "account",
        title = "Account Kit",
        subtitle = "Sign-in dengan Huawei ID",
        icon = "👤",
        docsUrl = "https://developer.huawei.com/consumer/en/doc/HMS-Core-SDK/account-kit-sdk"
    ),
    LOCATION(
        route = "location",
        title = "Location Kit",
        subtitle = "Fused location: GPS + jaringan",
        icon = "📍",
        docsUrl = "https://developer.huawei.com/consumer/en/doc/HMS-Core-SDK/location-kit-sdk"
    ),
    MAP(
        route = "map",
        title = "Map Kit",
        subtitle = "Peta, marker & kamera HMS Maps",
        icon = "🗺️",
        docsUrl = "https://developer.huawei.com/consumer/en/doc/HMS-Core-SDK/map-kit-sdk"
    ),
    PUSH(
        route = "push",
        title = "Push Kit",
        subtitle = "Token & pesan push dari server",
        icon = "🔔",
        docsUrl = "https://developer.huawei.com/consumer/en/doc/HMS-Core-SDK/push-kit-server-api"
    ),
    SCAN(
        route = "scan",
        title = "Scan Kit",
        subtitle = "Pemindai barcode / QR code",
        icon = "📷",
        docsUrl = "https://developer.huawei.com/consumer/en/doc/HMS-Core-SDK/scan-kit-sdk"
    ),
    TEXT_RECOGNITION(
        route = "text_recognition",
        title = "Text Recognition",
        subtitle = "OCR: baca teks dari gambar",
        icon = "🔤",
        docsUrl = "https://developer.huawei.com/consumer/en/doc/HMS-Cloud-ML-Vision-reference/text-recognition"
    ),
    SAFETY_DETECT(
        route = "safety_detect",
        title = "Safety Detect",
        subtitle = "Deteksi root, emulator, SIM palsu",
        icon = "🛡️",
        docsUrl = "https://developer.huawei.com/consumer/en/doc/HMS-Core-SDK/safety-detect"
    ),
    FIDO(
        route = "fido",
        title = "FIDO",
        subtitle = "Login biometrik standar FIDO2",
        icon = "🔑",
        docsUrl = "https://developer.huawei.com/consumer/en/doc/HMS-Core-SDK/fido"
    );

    companion object {
        // Konversi route string -> enum (dipakai halaman detail utk mencari
        // metadata dari argumen navigasi). `.entries` = daftar semua nilai.
        fun fromRoute(route: String): KitMenu? = entries.firstOrNull { it.route == route }
    }
}
