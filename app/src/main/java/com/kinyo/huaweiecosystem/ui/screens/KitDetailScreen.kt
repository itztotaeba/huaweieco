// ============================================================================
// KitDetailScreen.kt — HALAMAN SEMENTARA untuk tiap menu kit
// ----------------------------------------------------------------------------
// Tujuan fase ini: membuktikan alur navigasi Dashboard -> halaman kit works.
// Nantinya, isi layar ini akan kita ganti dengan implementasi nyata per kit
// (misal MapScreen berisi MapView HMS, LocationScreen meminta izin lokasi...).
// ============================================================================
package com.kinyo.huaweiecosystem.ui.screens

import android.content.Intent             // Intent utk membuka browser (docsUrl)
import android.net.Uri                    // pengemas URL
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement    // jarak antar item kolom
import androidx.compose.foundation.layout.Box            // kontainer (bulatan ikon)
import androidx.compose.foundation.layout.Column         // layout vertikal
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons             // kumpulan ikon Material
import androidx.compose.material.icons.automirrored.filled.ArrowBack // ikon back (auto RTL)
import androidx.compose.material3.Button                 // tombol aksi utama
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon                   // wadah ikon
import androidx.compose.material3.IconButton            // ikon yang bisa ditekan
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
// NB: TopAppBar di bawah ditulis dengan nama lengkap (FQN) karena "TopAppBar" plain adalah Experimental API M3.
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext        // akses Context di Compose
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinyo.huaweiecosystem.model.KitMenu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitDetailScreen(
    kit: KitMenu,          // metadata kit yang ditampilkan (dari route argumen)
    onBack: () -> Unit     // callback saat user menekan tombol kembali
) {
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                // Judul = nama kit, misal "Map Kit".
                title = { Text(kit.title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    // Tombol back: memanggil onBack() => navController.popBackStack()
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke dashboard"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp), // jarak tepi konten
            horizontalAlignment = Alignment.CenterHorizontally, // semua anak rata tengah
            verticalArrangement = Arrangement.Center            // blok konten di tengah layar
        ) {
            // Bulatan besar berisi emoji ikon kit.
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = kit.icon, fontSize = 48.sp)
            }

            Text(
                text = kit.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = kit.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Penanda bahwa halaman implementasi belum dibuat (fase belajar).
            Text(
                text = "🚧 Halaman implementasi \"" + kit.title + "\" akan dibuat pada langkah berikutnya.\n" +
                        "Saat ini kamu berada di halaman contoh navigasi Dashboard → Kit.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp)
            )

            // LocalContext = cara idiomatik mengambil Context di Compose
            // (dibanding Activity context manual). Kita pakai utk buka browser.
            val context = LocalContext.current
            Button(
                onClick = {
                    // ACTION_VIEW + Uri => sistem memilih browser default
                    // dan membuka dokumentasi resmi kit terkait.
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(kit.docsUrl))
                    )
                },
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Text("Buka Dokumentasi Resmi")
            }
        }
    }
}
