// ============================================================================
// AccountKitHelper.kt — JEMBATAN ke Account Kit HMS (sign-in Huawei ID)
// ----------------------------------------------------------------------------
// SEJARAH SINGKAT (mengapa dulu error "Class Account Kit tidak ditemukan"):
//   * Versi awal file ini memakai REFLECTION ke class yang sebenarnya TIDAK
//     PERNAH ada di compile classpath maupun runtime:
//       - com.huawei.hms.support.api.client.HuaweiIdAuthParams$Builder
//       - com.huawei.hms.support.api.entity.auth.HuaweiIdAuthParams$Builder
//     Keduanya selalu ClassNotFoundException -> requestSignIn langsung jatuh
//     ke SignInRequest.Error("Class Account Kit tidak ditemukan...") padahal
//     HMS Core di perangkat sehat (kode 0).
//   * Akar masalah lain: artefak Gradle Account Kit lama ("availableaccount")
//     memang sudah dihapus dari Maven repo Huawei, tapi ternyata Account Kit
//     yang MASIH tersedia resmi adalah "com.huawei.hms:hwid" (6.15.0.300 —
//     diverifikasi langsung dari developer.huawei.com/repo). AAR hwid memuat
//     class API yang benar:
//       - com.huawei.hms.support.hwid.HuaweiIdAuthManager
//       - com.huawei.hms.support.hwid.service.HuaweiIdAuthService
//       - com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
//       - com.huawei.hms.support.hwid.result.AuthHuaweiId
//     plus activity hub internal (HwIdSignInHubActivity) yang dibutuhkan alur
//     getSignInIntent().
//   * Solusi: tambahkan dependency libs.hms.hwid dan tulis ulang helper ini
//     memakai API KONKRET (tanpa reflection) — lebih sederhana, stabil, dan
//     mengikuti contoh resmi dokumentasi Account Kit.
//
// Alur sign-in standar Account Kit:
//   1. Bangun parameter scope -> HuaweiIdAuthParamsHelper(...).setOpenId()
//      .setProfile().createParams()
//   2. Ambil service         -> HuaweiIdAuthManager.getService(activity, params)
//   3. Coba silentSignIn()   -> Task<AuthHuaweiId>; sukses = sudah login.
//   4. Kalau belum           -> getSignInIntent() -> startActivityForResult.
//   5. Hasil                 -> HuaweiIdAuthManager.parseAuthResultFromIntent(data)
// ============================================================================
package com.kinyo.huaweiecosystem.hms

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.huawei.hmf.tasks.Task
import com.huawei.hms.common.ApiException
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.hms.support.hwid.result.AuthHuaweiId
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

// ---------------------------------------------------------------------------
// TIPE HASIL — model data lokal kita sendiri (mudah ditampilkan di Compose).
// ---------------------------------------------------------------------------
data class HmsProfile(
    val displayName: String = "",   // nama tampilan akun (bisa kosong)
    val email: String = "",         // email jika scope email diberikan & tersedia
    val photoUri: String = "",      // URL foto profil (string, bukan Uri, biar mudah)
    val openId: String = "",        // identifier unik akun utk app kita (paling stabil)
    val unionId: String = "",       // identifier lintas-app milik developer yg sama
    val uid: Long = 0L              // user id numerik Huawei ID
) {
    // Helper: apakah profil ini punya isi berarti (artinya sudah login)?
    fun isReal(): Boolean = openId.isNotBlank() || displayName.isNotBlank()
}

// ---------------------------------------------------------------------------
// PERMOHONAN SIGN-IN — hasil pembungkus agar UI tahu harus melakukan apa.
// Dipakai sealed class supaya `when` di UI wajib menangani semua kasus
// (AlreadySignedIn / NeedsUi / Error) — tidak ada cabang yang terlewat.
// ---------------------------------------------------------------------------
sealed class SignInRequest {
    // Langsung dapat profil (user ternyata sudah login sebelumnya -> silent).
    data class AlreadySignedIn(val profile: HmsProfile) : SignInRequest()

    // Harus tampilkan UI login HMS Core: activity menaruh signInIntent ke
    // ActivityResultLauncher; setelah hasil tiba, onResult dipakai utk
    // mengekstrak profil dari Intent data.
    data class NeedsUi(
        val signInIntent: Intent,
        val onResult: (resultCode: Int, data: Intent?) -> HmsProfile?
    ) : SignInRequest()

    // Kegagalan dengan pesan manusiawi (mis. HMS Core tidak terpasang).
    data class Error(val message: String) : SignInRequest()
}

object AccountKitHelper {

    // =========================================================================
    // BAGIAN 1 — PARAMETER SCOPE (API KONKRET Account Kit / hwid)
    // =========================================================================
    // Kita minta OPEN_ID + PROFILE: cukup untuk menampilkan identitas akun
    // tanpa butuh persetujuan sensitif tambahan (email/phone sengaja tidak).
    private fun buildAuthParams(): HuaweiIdAuthParams =
        HuaweiIdAuthParamsHelper(HuaweiIdAuthParams())
            .setId()          // scope openid -> openId & unionId (API hwid 6.x: setId(), bukan setOpenId())
            .setProfile()     // scope profile -> display name & foto
            .createParams()

    // =========================================================================
    // BAGIAN 2 — MENGAMBIL SERVICE AUTH
    // =========================================================================
    // HuaweiIdAuthManager.getService(activity, authParams) -> service utama
    // tempat semua aksi auth berada (silentSignIn, getSignInIntent, signOut).
    private fun getAuthService(activity: Activity): HuaweiIdAuthService =
        HuaweiIdAuthManager.getService(activity, buildAuthParams())

    // =========================================================================
    // BAGIAN 3 — KONVERSI AuthHuaweiId -> HmsProfile
    // =========================================================================
    // Catatan versi hwid 6.x: getter bernama getOpenId() (huruf D kapital),
    // avatar lewat getAvatarUriString(), dan uid berupa String (perlu toLong).
    private fun toProfile(account: AuthHuaweiId?): HmsProfile {
        if (account == null) return HmsProfile()
        return HmsProfile(
            displayName = account.displayName.orEmpty(),
            email = account.email.orEmpty(),
            photoUri = account.avatarUriString.orEmpty(),
            openId = account.openId.orEmpty(),
            unionId = account.unionId.orEmpty(),
            uid = account.uid?.toLongOrNull() ?: 0L
        )
    }

    // =========================================================================
    // BAGIAN 4 — CEK KETERSEDIAAN HMS CORE DI PERANGKAT
    // =========================================================================
    // Memakai API normal dari hms-base. Kode 0/1 = SUCCESS (HMS Core terpasang
    // & siap), kode lain = perlu update/diaktifkan.
    suspend fun checkHmsAvailability(context: Context): String = withContext(Dispatchers.IO) {
        try {
            val status = com.huawei.hms.api.HuaweiApiAvailability.getInstance()
                .isHuaweiMobileServicesAvailable(context.applicationContext)
            when (status) {
                0, 1 -> "Siap ✅ (kode $status)"
                else -> "HMS Core perlu diperbarui/dicek (kode $status)"
            }
        } catch (e: Exception) {
            "Gagal cek: ${e.message}"
        }
    }

    // =========================================================================
    // BAGIAN 5 — ALUR SIGN-IN LENGKAP (dipanggil dari Compose; hasil Task
    //             dikonversi menjadi coroutine biasa lewat suspendCancellable)
    // =========================================================================
    // PENTING: silentSignIn() adalah panggilan REMOTE ke HMS Core. Kalau kita
    // menunggu task-nya di Dispatchers.Main, ada risiko deadlock saat HMS Core
    // mengirim balasan yang juga butuh main thread. Karena itu langkah remote
    // (cek ketersediaan + silent sign-in) dikerjakan di Dispatchers.IO;
    // hanya pembuatan Intent login yang dilakukan di Main (syarat API HMS).
    suspend fun requestSignIn(activity: Activity): SignInRequest {
        // ---- Langkah A+B (di IO): service & silent sign-in ----
        val silentProfile = withContext(Dispatchers.IO) {
            try {
                toProfile(awaitTask(getAuthService(activity).silentSignIn()))
            } catch (_: Exception) {
                HmsProfile() // gagal silent = anggap belum login, lanjut ke UI
            }
        }
        if (silentProfile.isReal()) {
            return SignInRequest.AlreadySignedIn(silentProfile)
        }

        // ---- Langkah C (di Main): minta intent UI login dari service ----
        // (getSignInIntent() WAJIB dipanggil dari main thread.)
        return withContext(Dispatchers.Main) {
            try {
                val service = getAuthService(activity)
                val signInIntent = service.getSignInIntent()
                    ?: return@withContext SignInRequest.Error("getSignInIntent() mengembalikan null.")

                // Kembalikan closure onResult: nanti setelah ActivityResult tiba,
                // UI memanggil closure ini untuk mengekstrak profil dari data.
                // parseAuthResultFromIntent = penerjemah resmi hasil Activity login.
                SignInRequest.NeedsUi(signInIntent) { _, data ->
                    if (data == null) null
                    else {
                        val parsed = runCatching {
                            HuaweiIdAuthManager.parseAuthResultFromIntent(data)
                        }.getOrNull()
                        val profile = toProfile(parsed)
                        if (profile.isReal()) profile else null
                    }
                }
            } catch (e: ApiException) {
                // HMS Core menolak (tidak terpasang / versi lama / disabled).
                SignInRequest.Error(
                    "Akun Huawei tidak dapat diakses (kode HMS ${e.statusCode}). " +
                        "Pastikan HMS Core aktif lalu coba lagi."
                )
            } catch (e: Exception) {
                SignInRequest.Error("Terjadi kesalahan tak terduga: ${e.message}")
            }
        }
    }

    // =========================================================================
    // BAGIAN 6 — SIGN OUT
    // =========================================================================
    // cancelAuthorization() melepas consent app terhadap akun Huawei user, lalu
    // signOut() membersihkan sesi lokal. Keduanya dipanggil berurutan agar
    // silentSignIn berikutnya benar-benar mengembalikan "belum login".
    suspend fun signOut(activity: Activity): Boolean = withContext(Dispatchers.Main) {
        try {
            val service = getAuthService(activity)
            awaitTaskVoid(service.cancelAuthorization())
            awaitTaskVoid(service.signOut())
            true
        } catch (_: Exception) {
            false
        }
    }

    // =========================================================================
    // BAGIAN 7 — MENUNGGU TASK HMS (async pattern bawaan HMS)
    // =========================================================================
    // Semua API HMS mengembalikan Task<T> dengan listener. Fungsi ini mengubah
    // gaya callback itu menjadi nilai suspend biasa (await) memakai
    // suspendCancellableCoroutine — inti interoperabilitas coroutine + SDK lama.
    private suspend fun awaitTask(task: Task<AuthHuaweiId>): AuthHuaweiId? =
        suspendCancellableCoroutine { cont ->
            task.addOnSuccessListener { result ->
                if (!cont.isCompleted) cont.resume(result)
            }
            task.addOnFailureListener { _ ->
                // Silent sign-in gagal = user belum pernah login; anggap null.
                if (!cont.isCompleted) cont.resume(null)
            }
        }

    // Varian untuk Task<Void> (signOut/cancelAuthorization) — kita hanya butuh
    // tahu selesai atau tidak, nilainya tidak dipakai.
    private suspend fun awaitTaskVoid(task: Task<Void>) {
        suspendCancellableCoroutine<Unit> { cont ->
            task.addOnSuccessListener { if (!cont.isCompleted) cont.resume(Unit) }
            task.addOnFailureListener { if (!cont.isCompleted) cont.resume(Unit) }
        }
    }
}
