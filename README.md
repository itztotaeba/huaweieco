# Huawei Eco 🇨🇳🤖

Aplikasi **belajar** berisi contoh implementasi **Huawei Mobile Services (HMS) Core Kits**
dalam satu aplikasi (all-in-one), dibangun dengan:

- **Android Gradle Plugin (AGP) 9.4.1** + Gradle 9.6.0
- **Kotlin** (built-in Kotlin pada AGP 9 — tanpa plugin `kotlin-android`)
- **Jetpack Compose** (Material 3)
- **Version Catalog** (`gradle/libs.versions.toml`)

> ⚠️ Semua kode di repo ini diberi komentar penjelasan baris-demi-baris dan catatan
> alur logika, karena tujuan utamanya adalah media belajar.

## Struktur Aplikasi

```
Aplikasi dibuka
   └── MainActivity (single-activity)
         └── HuaweiEcosystemTheme (tema M3 merah Huawei)
               └── AppNavHost (Navigation Compose)
                     ├── route "dashboard"      → DashboardScreen (grid 8 kartu kit)
                     └── route "kit/{kitRoute}" → KitDetailScreen (placeholder per kit)
```

## 8 Menu HMS Kit (Dashboard)

| # | Kit | Route | Status |
|---|-----|-------|--------|
| 1 | Account Kit | `account` | 🔜 implementasi berikutnya |
| 2 | Location Kit | `location` | 🔜 |
| 3 | Map Kit | `map` | 🔜 |
| 4 | Push Kit | `push` | 🔜 |
| 5 | Scan Kit | `scan` | 🔜 |
| 6 | Text Recognition | `text_recognition` | 🔜 |
| 7 | Safety Detect | `safety_detect` | 🔜 |
| 8 | FIDO | `fido` | 🔜 |

Fase ini fokus pada **kerangka**: dashboard grid kartu + navigasi ke halaman tiap kit.

## Setup Wajib Sebelum Menjalankan

1. **Buat proyek di AppGallery Connect** → download `agconnect-services.json`
   taruh di folder `app/`.
2. Salin **App ID** dari konsol, lalu ganti di `app/build.gradle.kts`:
   ```kotlin
   manifestPlaceholders["HMS_APP_ID"] = "100xxxxxx" // <-- App ID kamu
   ```
3. Jalankan di **perangkat Huawei/HMS** (atau emulator dengan HMS Core).
   Di perangkat non-Huawei, `ApiAvailability` akan memberi tahu HMS tidak tersedia.

## Catatan Versi (Kompatibilitas AGP 9.4.1)

- AGP 9 butuh **Gradle 9.x** dan sudah menyertakan dukungan Kotlin built-in;
  menambahkan `org.jetbrains.kotlin.android` justru menimbulkan error konflik.
- Compose diaktifkan via `buildFeatures { compose = true }` + `compose-bom`.
- SDK HMS ditambahkan dari repo Maven resmi Huawei
  (`https://developer.huawei.com/repo/`) yang dideklarasikan di `settings.gradle.kts`.

## Cara Build

```bash
./gradlew assembleDebug
```
