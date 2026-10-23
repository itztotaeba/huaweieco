// ============================================================================
// AccountScreen.kt — HALAMAN DEMO ACCOUNT KIT (sign-in Huawei ID)
// ----------------------------------------------------------------------------
// ALUR BESAR LAYAR INI (baca berurutan):
//   1. Saat pertama dibuka -> LaunchedEffect memanggil cek ketersediaan HMS
//      Core + silentSignIn via AccountKitHelper (semua logic SDK ada di helper,
//      layar ini murni UI + state).
//   2. State `ui` menentukan tampilan:
//        Idle          -> tombol "Sign in with Huawei ID"
//        SignedIn      -> kartu profil (nama, email, openId, uid, foto)
//        NeedsUi       -> (transisi) intent login dikirim ke launcher
//        Error         -> pesan kegagalan yang jelas
//   3. ActivityResultLauncher (registerForActivityResult) menangkap hasil
//      halaman login HMS Core; closure onResult dari helper yang mengubah
//      Intent -> HmsProfile.
//   4. Sign out -> helper.signOut() lalu state kembali ke Idle.
//
// POLA PENTING Compose yang dipakai: `remember`+`mutableStateOf` untuk state
// yang bertahan saat recomposition, dan coroutine scope untuk memanggil
// fungsi suspend helper tanpa memblokir UI thread.
// ============================================================================
package com.kinyo.huaweiecosystem.ui.screens

import android.app.Activity                  // helper butuh Activity utk UI login HMS
import androidx.activity.compose.rememberLauncherForActivityResult // launcher modern Compose
import androidx.activity.result.contract.ActivityResultContracts   // kontrak StartActivityForResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinyo.huaweiecosystem.hms.AccountKitHelper
import com.kinyo.huaweiecosystem.hms.HmsProfile
import com.kinyo.huaweiecosystem.hms.SignInRequest
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// STATE TAMPILAN — satu sealed class sederhana agar UI mudah bercabang.
// ---------------------------------------------------------------------------
private sealed interface AccountUi {
    data object Loading : AccountUi                      // sedang cek/proses (spinner)
    data object Idle : AccountUi                         // belum login -> tampilkan tombol
    data class SignedIn(val profile: HmsProfile) : AccountUi  // sudah login -> kartu profil
    data class Failed(val message: String) : AccountUi   // error dengan pesan jelas
}

@Composable
fun AccountScreen(onBack: () -> Unit) {
    // ---- Akses konteks & activity ----
    // LocalContext.current di dalam ComponentActivity selalu berupa Activity,
    // jadi cast aman; kalau suatu saat bukan Activity -> fallback state error.
    val context = LocalContext.current
    val activity = context as? Activity

    // ---- State utama layar ----
    var ui by remember { mutableStateOf<AccountUi>(AccountUi.Loading) }

    // Info ketersediaan HMS Core (hasil cek awal; tampil di bawah tombol).
    var hmsInfo by remember { mutableStateOf("") }

    // Scope coroutine milik komposisi ini (otomatis dibatalkan saat keluar).
    val scope = rememberCoroutineScope()

    // Closure sementara: menyimpan callback onResult dari SignInRequest.NeedsUi
    // supaya bisa dipanggil ketika ActivityResult datang (launcher dibuat sekali
    // di awal, sedangkan closure baru tersedia setelah requestSignIn selesai).
    var pendingParser by remember {
        mutableStateOf<((Int, android.content.Intent?) -> HmsProfile?)?>(null)
    }

    // ---- Launcher hasil halaman login HMS Core ----
    // registerForActivityResult WAJIB dipanggil saat composition pertama
    // (bukan di dalam onClick) — inilah pola resmi Activity Result API.
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Callback ini terpanggil setelah user selesai/batal di UI login HMS.
        val parser = pendingParser
        pendingParser = null
        if (parser != null) {
            val profile = parser(result.resultCode, result.data)
            ui = if (profile != null) AccountUi.SignedIn(profile)
                 else AccountUi.Failed("Login dibatalkan atau profil tidak terbaca.")
        } else {
            // Parser hilang (mis. proses dipanggil ulang sebelum hasil tiba).
            ui = AccountUi.Failed("Hasil login tidak dikenali. Coba sign-in lagi.")
        }
    }

    // ---- Efek awal: cek ketersediaan HMS + coba silent sign-in ----
    LaunchedEffect(Unit) {
        // Unit = kunci efek; dijalankan sekali saat layar pertama compose.
        if (activity == null) {
            ui = AccountUi.Failed("Konteks bukan Activity — Account Kit butuh Activity.")
            return@LaunchedEffect
        }
        // Cek dulu apakah HMS Core tersedia; simpan sbg teks info di state Idle.
        hmsInfo = AccountKitHelper.checkHmsAvailability(context)
        // requestSignIn melakukan silentSignIn internal; kalau user ternyata
        // sudah pernah login, kita langsung dapat profil TANPA membuka UI.
        when (val req = AccountKitHelper.requestSignIn(activity)) {
            is SignInRequest.AlreadySignedIn ->
                ui = AccountUi.SignedIn(req.profile)
            is SignInRequest.NeedsUi -> {
                // Simpan parser DULU, baru luncurkan intent -> saat hasil tiba,
                // pendingParser pasti sudah terisi (urutan penting!).
                pendingParser = req.onResult
                signInLauncher.launch(req.signInIntent)
                ui = AccountUi.Idle // tombol tetap siap bila user membatalkan
            }
            is SignInRequest.Error ->
                ui = AccountUi.Failed("${req.message}\n(Status HMS: $hmsInfo)")
        }
    }

    // =========================================================================
    // RENDER — Scaffold standar sama seperti halaman lain (konsistensi visual)
    // =========================================================================
    Scaffold(topBar = { DetailTopBar("Account Kit", onBack) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Keterangan singkat fitur (eduksi: apa yang sedang dicoba).
            Text(
                "Demo Account Kit: sign-in memakai Huawei ID lewat HMS Core. "+
                "Tekan tombol, setujui akses, dan profil akun akan tampil di bawah.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (val state = ui) {
                AccountUi.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.size(10.dp))
                    Text("Memeriksa akun tersimpan (silent sign-in)...", fontSize = 13.sp)
                }

                AccountUi.Idle -> Button(
                    // onClick -> panggil requestSignIn lagi (async) lalu tangani
                    // hasilnya persis seperti LaunchedEffect di atas.
                    onClick = {
                        if (activity == null) return@Button
                        scope.launch {
                            ui = AccountUi.Loading
                            when (val req = AccountKitHelper.requestSignIn(activity)) {
                                is SignInRequest.AlreadySignedIn ->
                                    ui = AccountUi.SignedIn(req.profile)
                                is SignInRequest.NeedsUi -> {
                                    pendingParser = req.onResult
                                    signInLauncher.launch(req.signInIntent)
                                    ui = AccountUi.Idle
                                }
                                is SignInRequest.Error ->
                                    ui = AccountUi.Failed(req.message)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Sign in with Huawei ID")
                }

                // Status HMS Core — membantu debug kalau sign-in gagal.
                if (hmsInfo.isNotBlank()) {
                    Text("Status HMS Core: $hmsInfo", fontSize = 12.sp,
                         color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                is AccountUi.SignedIn -> ProfileCard(
                    profile = state.profile,
                    onSignOut = {
                        // Sign-out: revoke consent lalu balik ke Idle.
                        if (activity != null) scope.launch {
                            ui = AccountUi.Loading
                            val ok = AccountKitHelper.signOut(activity)
                            ui = if (ok) AccountUi.Idle
                                 else AccountUi.Failed("Sign-out gagal (coba lagi).")
                        }
                    }
                )

                is AccountUi.Failed -> Surface(
                    // Kartu merah lembut khusus pesan error.
                    color = Color(0xFFFFF3F0),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("⚠️ Belum berhasil", fontWeight = FontWeight.Bold, color = Color(0xFFB3261E))
                        Spacer(Modifier.height(6.dp))
                        Text(state.message, fontSize = 13.sp, color = Color(0xFF5F1F1B))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// KARTU PROFIL — menampilkan field HmsProfile satu per satu.
// Field kosong disembunyikan (kondisi `if`) supaya UI tetap bersih.
// ---------------------------------------------------------------------------
@Composable
private fun ProfileCard(profile: HmsProfile, onSignOut: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar: lingkaran dengan ikon Person (foto asli butuh Coil —
                // sengaja tidak ditambah biar APK ramping; photoUri tetap tampil).
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(
                        profile.displayName.ifBlank { "Huawei User" },
                        fontSize = 18.sp, fontWeight = FontWeight.Bold
                    )
                    if (profile.email.isNotBlank()) {
                        Text(profile.email, fontSize = 13.sp,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider()
            // Baris identitas teknis — font mono biar terlihat seperti debug info.
            if (profile.openId.isNotBlank()) InfoRow("Open ID", profile.openId)
            if (profile.unionId.isNotBlank()) InfoRow("Union ID", profile.unionId)
            if (profile.uid != 0L) InfoRow("UID", profile.uid.toString())
            if (profile.photoUri.isNotBlank()) InfoRow("Foto", profile.photoUri)
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out / Cabut akses")
            }
        }
    }
}

// Satu baris label -> nilai (dipakai berulang di kartu profil).
@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
             modifier = Modifier.padding(start = 12.dp))
    }
}

// TopBar ringkas ala halaman detail lain (judul + tombol back teks).
@Composable
private fun DetailTopBar(title: String, onBack: () -> Unit) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onBack) { Text("← Kembali") }
            Spacer(Modifier.size(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}
