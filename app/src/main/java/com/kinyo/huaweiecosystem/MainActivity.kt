// ============================================================================
// MainActivity.kt — SATU-SATUNYA Activity aplikasi ini (single-activity app)
// ----------------------------------------------------------------------------
// Arsitektur modern Android memakai 1 Activity + Compose Navigation:
//   Activity hanya "jendela" ke sistem Android; pergantian layar dilakukan
//   oleh NavHost di dalam Compose, bukan oleh banyak Activity.
//
// Flow saat aplikasi dibuka:
//   1. Sistem membuat instance MainActivity -> onCreate() dipanggil.
//   2. enableEdgeToEdge() membuat konten menembus area status/nav bar.
//   3. setContent { } memulai pohon Compose:
//        HuaweiEcosystemTheme { AppNavHost(navController) }
//      Tema membungkus warna; NavHost merender route "dashboard".
// ============================================================================
package com.kinyo.huaweiecosystem

import android.os.Bundle                                  // data state yg disimpan sistem
import androidx.activity.ComponentActivity                // base class utk apps non-XML UI
import androidx.activity.compose.setContent               // Jembatan Activity -> Compose
import androidx.activity.enableEdgeToEdge                 // tampilan fullscreen modern
import androidx.navigation.compose.rememberNavController // controller graph navigasi
import com.kinyo.huaweiecosystem.navigation.AppNavHost   // graph route dashboard/kit
import com.kinyo.huaweiecosystem.ui.theme.HuaweiEcosystemTheme // tema warna M3 kita

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // WAJIB: biarkan superclass merestore state

        // Konten melebar sampai tepi layar (status bar transparan). Pada mode
        // lama hal ini butuh banyak workaround window flags; helper ini ringkas.
        enableEdgeToEdge()

        // setContent = titik masuk deklaratif UI Compose. Blok lambda di
        // dalamnya adalah "root composable" aplikasi.
        setContent {
            // 1) Terapkan tema (palet merah Huawei + dark/light otomatis).
            HuaweiEcosystemTheme {
                // 2) Buat & ingat NavController. `remember` memastikan objek
                //    yang sama dipakai walau recomposition terjadi berulang.
                val navController = rememberNavController()

                // 3) Pasang graph navigasi: start = DashboardScreen berisi
                //    grid kartu 8 HMS Kit (Account, Location, Map, Push, Scan,
                //    Text Recognition, Safety Detect, FIDO).
                AppNavHost(navController = navController)
            }
        }
    }
}
