// ============================================================================
// DashboardScreen.kt — HALAMAN UTAMA (dashboard 8 menu HMS Kit)
// ----------------------------------------------------------------------------
// Flow logika:
//   MainActivity.setContent -> HuaweiEcosystemTheme -> AppNavHost
//     -> composable("dashboard") -> DashboardScreen(onKitClick = nav ke kit)
// User melihat grid 2 kolom berisi kartu; tap kartu => onKitClick(route)
// dipanggil => NavController.navigate(route) => pindah ke halaman detail.
// ============================================================================
package com.kinyo.huaweiecosystem.ui.screens

import androidx.compose.foundation.background           // latar warna pada Modifier (card ikon/badge)
import androidx.compose.foundation.clickable            // membuat card bisa di-tap
import androidx.compose.foundation.layout.Arrangement   // pengaturan jarak antar item
import androidx.compose.foundation.layout.Box           // kontainer sederhana (badge & sel grid)
import androidx.compose.foundation.layout.Column        // layout vertikal (isi kartu)
import androidx.compose.foundation.layout.Row           // layout horizontal (baris ikon+badge)
import androidx.compose.foundation.layout.Spacer        // ruang kosong pengatur jarak
import androidx.compose.foundation.layout.fillMaxSize   // isi penuh lebar+tinggi
import androidx.compose.foundation.layout.fillMaxWidth  // isi penuh lebar
import androidx.compose.foundation.layout.height        // tinggi tetap
import androidx.compose.foundation.layout.heightIn       // batas min/max tinggi (menyeragamkan kartu)
import androidx.compose.foundation.layout.padding       // padding komposable
import androidx.compose.foundation.layout.size          // ukuran ikon/box
import androidx.compose.foundation.lazy.grid.GridCells  // konfigurasi kolom/spacing LazyGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid // grid yang bisa di-scroll + recycle item
import androidx.compose.foundation.lazy.grid.items      // fungsi scope: List -> sel grid
import androidx.compose.foundation.shape.RoundedCornerShape   // sudut membulat
import androidx.compose.material3.Card                  // komponen kartu dashboard
import androidx.compose.material3.CardDefaults          // default elevasi/warna card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme         // akses warna/tipografi tema
import androidx.compose.material3.Scaffold              // kerangka layar (topBar+isi)
// PENTING: SmallTopAppBar SUDAH DIHAPUS dari Material3 versi terbaru.
// Nama resminya sekarang TopAppBar (stabil, tidak eksperimen lagi).
// Kalau kamu membaca tutorial lama yang memakai SmallTopAppBar -> ganti TopAppBar.
import androidx.compose.material3.TopAppBar             // judul atas layar (pengganti SmallTopAppBar)
import androidx.compose.material3.Text                  // komponen teks
import androidx.compose.runtime.Composable              // penanda fungsi UI
import androidx.compose.ui.Alignment                    // perataan (tengah dll)
import androidx.compose.ui.Modifier                     // "rangkaian" aturan layout
import androidx.compose.ui.text.font.FontWeight         // ketebalan huruf
import androidx.compose.ui.unit.dp                      // density-independent pixel
import androidx.compose.ui.unit.sp                      // ukuran font
import com.kinyo.huaweiecosystem.model.KitMenu          // data menu enum

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar M3 masih ditandai eksperimental di sebagian versi
@Composable
fun DashboardScreen(
    // Callback: halaman ini TIDAK tahu soal navigasi — ia hanya melaporkan
    // "user memilih route X". Ini pola pemisahan tanggung jawab (loosely coupled).
    onKitClick: (String) -> Unit
) {
    // Scaffold = template layar Material: menyediakan area topBar + content
    // padding yang benar (menghindari tabrakan dengan status bar dsb).
    Scaffold(
        topBar = {
            // Judul aplikasi di bagian atas.
            // TopAppBar (Material3) = pengganti SmallTopAppBar dari versi M3 lama.
            TopAppBar(title = { Text("Huawei Eco", fontWeight = FontWeight.Bold) })
        }
    ) { innerPadding ->
        // innerPadding dari Scaffold WAJIB dipakai agar konten tidak tertutup
        // top bar — kita terapkan sebagai contentPadding grid.
        // --------------------------------------------------------------------
        // PERBAIKAN LAYOUT (kenapa sebelumnya "berantakan / mepet"):
        // 1. BUG UTAMA: Spacer antar-baris dulu diletakkan DI DALAM Row. Anak Row
        //    disusun HORIZONTAL, jadi Spacer tinggi-12dp itu malah jadi "kolom
        //    ketiga" dan menggeser kartu kedua => jarak & lebar tidak simetris.
        // 2. Tinggi kartu murni mengikuti panjang subtitle; tiap kit beda panjang
        //    => ada kartu lebih pendek/tinggi, baris terlihat tidak rata.
        // SOLUSI FINAL: LazyVerticalGrid dengan `items(list)` BIASA (bukan named
        // argumen). Catatan penting: error kompilasi sebelumnya
        // ("No parameter with name 'items' found") terjadi karena pemanggilan
        // memakai NAMED argument `items = ...` yang bentrok dengan aturan
        // Kotlin untuk extension function di scope interface. Pemanggilan
        // posisional `items(kits) { ... }` adalah bentuk standar yang selalu
        // resolve. Ditambah:
        //   - GridCells.Fixed(2)  -> kolom selalu pas 2, lebar sel identik.
        //   - horizontal/verticalArrangement spacedBy -> jarak antar kartu
        //     konsisten ke semua arah (inilah pengganti Spacer yang salah tadi).
        //   - heightIn seragam di KitCard -> semua kartu tingginya sama.
        // LazyVerticalGrid juga otomatis bisa di-scroll — dashboard 8+ menu
        // tetap rapi di layar kecil.
        // --------------------------------------------------------------------
        val kits = KitMenu.entries.toList()   // semua enum menu jadi List
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),     // grid 2 kolom SIMETRIS permanen
            modifier = Modifier
                .fillMaxSize()                // isi penuh area di bawah top bar
                .padding(innerPadding),       // hormati padding dari Scaffold
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp
            ),                                // tepi konten + ruang ekstra di bawah
            horizontalArrangement = Arrangement.spacedBy(12.dp), // jarak antar KOLOM
            verticalArrangement = Arrangement.spacedBy(12.dp)    // jarak antar BARIS
        ) {
            // Bentuk POSISIONAL items(List) { item -> ... } : setiap elemen list
            // menjadi satu sel grid; lambda berisi UI sel-nya.
            items(kits) { kit ->
                KitCard(
                    kit = kit,
                    onClick = { onKitClick(kit.route) } // tap -> navigate ke halaman kit
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// KIT CARD — satu kartu menu (ikon emoji, judul, subtitle, badge "COMING SOON")
// ----------------------------------------------------------------------------
@Composable
private fun KitCard(
    kit: KitMenu,
    onClick: () -> Unit // aksi saat kartu ditekan (menjalankan navigate)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()   // kartu melebar mengikuti kolom grid
            // heightIn(min) = tinggi MINIMUM seragam untuk semua kartu.
            // Ini yang membuat baris terlihat rapi: kartu dengan subtitle 1 baris
            // tidak lagi lebih pendek dari kartu dengan subtitle 2 baris.
            // max 200.dp menjaga kartu tidak melar berlebihan jika teks panjang.
            .heightIn(min = 150.dp, max = 200.dp)
            // clickable memberi efek ripple + memanggil onClick saat ditekan.
            // Inilah titik tempat "tap user" berubah menjadi "navigasi".
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),      // sudut membulat modern
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // warna latar ikut tema
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // bayangan tipis
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Baris pertama: lingkaran berisi emoji ikon + badge status di kanan.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)  // bulatan 44dp
                        .background(
                            MaterialTheme.colorScheme.primaryContainer, // warna lembut khas tema
                            shape = RoundedCornerShape(50)              // 50% radius = lingkaran
                        ),
                    contentAlignment = Alignment.Center // emoji rata tengah
                ) {
                    Text(text = kit.icon, fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.weight(1f)) // dorong badge ke ujung kanan
                // Badge kecil penanda status implementasi.
                Text(
                    text = "SOON",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp)) // jarak ikon <-> teks

            // Judul kit, contoh: "Map Kit".
            Text(
                text = kit.title,
                style = MaterialTheme.typography.titleMedium, // gaya bawaan M3
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Deskripsi singkat fungsi kit.
            Text(
                text = kit.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant // abu-abu redup
            )
        }
    }
}
