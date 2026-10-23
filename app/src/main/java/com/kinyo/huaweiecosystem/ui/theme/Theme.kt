// ============================================================================
// package: com.kinyo.huaweiecosystem — akar aplikasi (root source set)
// ============================================================================
package com.kinyo.huaweiecosystem.ui.theme

// Import Material 3 (M3): sistem desain yang dipakai Jetpack Compose.
import androidx.compose.foundation.isSystemInDarkTheme // deteksi mode gelap perangkat
import androidx.compose.material3.MaterialTheme        // "tema" tempat warna/tipografi disimpan
import androidx.compose.material3.darkColorScheme      // palet untuk dark mode
import androidx.compose.material3.lightColorScheme     // palet untuk light mode
import androidx.compose.runtime.Composable             // penanda fungsi UI Compose
import androidx.compose.ui.graphics.Color              // representasi warna ARGB

// ----------------------------------------------------------------------------
// PALET WARNA BERTema HUAWEI
// merah khas Huawei (#C7000B) sebagai aksen utama; netral lainnya turunan M3.
// Color hex ditulis 0xFFAARRGGBB (AA = alpha/opacity, FF = penuh).
// ----------------------------------------------------------------------------
private val HuaweiRed = Color(0xFFC7000B)
private val HuaweiRedDark = Color(0xFF8E0008)
private val DarkSurface = Color(0xFF121212)
private val OnDarkSurface = Color(0xFFECECEC)

// skema warna TERANG: primary = merah Huawei, latar putih bersih.
private val LightColors = lightColorScheme(
    primary = HuaweiRed,          // tombol utama, ikon aktif, top bar
    onPrimary = Color.White,      // teks/icon DI ATAS warna primary
    secondary = HuaweiRedDark,    // aksen sekunder
    onSecondary = Color.White,
    background = Color(0xFFF7F5F4), // latar seluruh layar (abu sangat terang)
    surface = Color.White,        // latar "kartu" (card dashboard)
    onBackground = Color(0xFF1C1B1F), // teks di atas background
    onSurface = Color(0xFF1C1B1F)     // teks di atas card
)

// skema warna GELAP: latar hitam, kartu abu gelap, teks terang.
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB3AC),   // merah muda terang agar kontras di gelap
    onPrimary = Color(0xFF690005),
    secondary = HuaweiRed,
    onSecondary = Color.White,
    background = DarkSurface,
    surface = Color(0xFF1E1E1E),
    onBackground = OnDarkSurface,
    onSurface = OnDarkSurface
)

// ----------------------------------------------------------------------------
// Fungsi tema utama. Pola standar Android Studio: satu @Composable pembungkus
// MaterialTheme yang memilih skema berdasarkan mode gelap sistem.
// `@Composable` = fungsi yang menghasilkan bagian UI; hanya boleh dipanggil
// dari fungsi @Composable lain.
// ----------------------------------------------------------------------------
@Composable
fun HuaweiEcosystemTheme(
    // Default null artinya "ikuti pengaturan perangkat"; bisa dipaksa true/false.
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit // isi layar yang akan dibungkus tema ini
) {
    // Pilih skema warna sesuai mode; `if..else` biasa karena ini kode Kotlin,
    // bukan bagian deklaratif UI.
    val colorScheme = if (darkTheme) DarkColors else LightColors

    // Berikan skema ke seluruh subtree `content{}` lewat CompositionLocal
    // MaterialTheme — semua komposisi di dalamnya otomatis mewarisi warna ini.
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
