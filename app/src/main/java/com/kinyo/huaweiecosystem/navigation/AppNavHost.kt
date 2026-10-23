// ============================================================================
// AppNavHost.kt — GRAFIK NAVIGASI (peta route -> layar)
// ----------------------------------------------------------------------------
// Navigation-Compose bekerja dengan "route string":
//   - "dashboard"        -> DashboardScreen
//   - "{kitRoute}"       -> KitDetailScreen (argumen dinamis dari path)
// Ketika DashboardScreen memanggil onKitClick("map"), NavController mencari
// route yang cocok, mengisi argumen kitRoute="map", lalu menampilkan
// KitDetailScreen dengan data KitMenu.MAP.
// ============================================================================
package com.kinyo.huaweiecosystem.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController   // pengendali graph navigasi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost     // komposisi tempat graph dipasang
import androidx.navigation.compose.composable  // satu entri route
import androidx.navigation.navArgument         // definisi argumen dinamis
import com.kinyo.huaweiecosystem.model.KitMenu
import com.kinyo.huaweiecosystem.ui.screens.DashboardScreen
import com.kinyo.huaweiecosystem.ui.screens.KitDetailScreen

// Nama route dipakai bersama agar tidak typo di dua tempat.
object Routes {
    const val DASHBOARD = "dashboard"                 // route awal (start)
    const val KIT_DETAIL = "kit/{kitRoute}"           // {kitRoute} = placeholder argumen

    // Helper pembangun URL detail, misal build("map") -> "kit/map".
    fun kitDetail(route: String) = "kit/$route"
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        // startDestination: layar pertama yang dirender saat aplikasi dibuka.
        startDestination = Routes.DASHBOARD
    ) {
        // ---------- ROUTE 1: DASHBOARD ----------
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onKitClick = { route ->
                    // User tap kartu kit -> push halaman detail ke back stack.
                    navController.navigate(Routes.kitDetail(route))
                }
            )
        }

        // ---------- ROUTE 2: DETAIL SATU KIT ----------
        composable(
            route = Routes.KIT_DETAIL,
            arguments = listOf(
                // Deklarasikan argumen path {kitRoute}; tipe string, wajib ada.
                navArgument("kitRoute") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Ambil nilai argumen ("account", "map", ...) dari entry stack.
            val kitRoute = backStackEntry.arguments?.getString("kitRoute").orEmpty()
            // Konversi string -> enum; jika tak dikenal, fallback aman: tampilkan
            // dashboard saja (elvis else di bawah mencegah crash).
            val kit = KitMenu.fromRoute(kitRoute)
            if (kit != null) {
                KitDetailScreen(
                    kit = kit,
                    onBack = {
                        // popBackStack = keluar dari halaman ini kembali ke
                        // dashboard (membuang entry teratas dari back stack).
                        navController.popBackStack()
                    }
                )
            } else {
                // Route tidak valid (harusnya tak terjadi) -> kembali ke grid.
                DashboardScreen(onKitClick = { })
            }
        }
    }
}
