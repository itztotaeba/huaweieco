// ============================================================================
// AccountKitHelper.kt — JEMBATAN (bridge) ke Account Kit HMS Core
// ----------------------------------------------------------------------------
// KENAPA FILE INI ADA / MENGAPA PAKAI REFLECTION?
// ----------------------------------------------------------------------------
// Secara historis Account Kit dipaketkan sebagai artefak Gradle terpisah
// ("com.huawei.hms:availableaccount"). Saat kita cek langsung ke Maven repo
// resmi Huawei (https://developer.huawei.com/repo), artefak itu sudah TIDAK
// tersedia lagi (HTTP 404 untuk semua varian versi). Karena itu satu-satunya
// cara yang dijamin bisa dikompilasi adalah pola yang sama yang dipakai SDK
// HMS di baliknya: MEMANGGIL API Account Kit milik aplikasi "HMS Core" lewat
// Java Reflection terhadap class-class yang disediakan oleh modul `hms-base`
// (com.huawei.hms:*:base) yang SUDAH kita import di build.gradle.kts.
//
// Alur logika sign-in Account Kit (standar HMS):
//   1. Buat objek parameter scope       -> HuaweiIdAuthParams (butuh scope
//      profile/openid dari enum Scopes).
//   2. Minta service auth               -> HuaweiIdAuthService (diambil dari
//      HuaweiIdAuthManager.getService(activity, params)).
//   3. Panggil getSignInIntent()        -> Intent; kalau user belum login ke
//      Huawei ID, HMS Core menampilkan halaman konsent/halaman login.
//   4. startActivityForResult(intent)   -> hasil datang di ActivityResult.
//   5. silentSignIn()/getHuaweiAccountFromIntent() -> service mengubah data
//      hasil menjadi objek HuaweiIdAccount (display name, photo, openId...).
//
// Semua langkah di atas dibungkus fungsi suspend agar UI Compose tinggal
// menunggu dengan coroutine — tidak ada callback berantakan.
// ============================================================================
package com.kinyo.huaweiecosystem.hms

import android.app.Activity                 // dibutuhkan HMS utk menampilkan UI login
import android.content.Context              // konteks aplikasi (dipakai cek ketersediaan)
import android.content.Intent               // intent sign-in dari HMS Core
import com.huawei.hms.api.HuaweiApiAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume              // utk menyelesaikan suspendCancellableCoroutine

// ---------------------------------------------------------------------------
// TIPE HASIL — model data lokal kita sendiri.
// Kita sengaja TIDAK mengimpor class HuaweiIdAccount secara langsung karena
// class itu tidak ada di compile classpath (tidak ada artefak account kit);
// nilainya kita ekstrak via reflection ke bentuk data class sederhana ini.
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
    // BAGIAN 1 — UTILITAS REFLECTION
    // =========================================================================

    // Varian "aman" untuk class yang opsional: kalau tidak ketemu, kembalikan
    // null TANPA exception — dipakai utk mendeteksi Account Kit modern vs legacy.
    private fun loadClassOrNull(name: String): Class<*>? = try {
        Class.forName(name)
    } catch (_: Throwable) {
        null
    }

    // Muat class berdasarkan NAMA LENGKAP-nya (fully qualified name).
    // Kenapa pakai string? Karena class-nya hanya ada saat runtime (disediakan
    // hms-base/HMS Core), jadi compiler Kotlin tak boleh melihatnya langsung.
    private fun loadClass(name: String): Class<*>? = try {
        Class.forName(name)
    } catch (_: ClassNotFoundException) {
        null // class benar-benar tidak ada di perangkat ini
    }

    // Cari method public berdasarkan nama + jumlah argumen (praktik aman untuk
    // API yang overload-nya banyak; kita pilih yang cocok arity-nya).
    private fun Any.method(name: String, argCount: Int) =
        this.javaClass.methods.firstOrNull { it.name == name && it.parameterCount == argCount }

    // Panggil method tanpa argumen dan kembalikan hasilnya apa adanya.
    private fun Any.callNoArg(name: String): Any? =
        method(name, 0)?.invoke(this)

    // Konversi objek HuaweiIdAccount (reflection) -> HmsProfile (data lokal).
    // Setiap getter dibungkus runCatching kecil supaya satu field gagal tidak
    // menjatuhkan seluruh pembacaan profil.
    private fun toProfile(account: Any?): HmsProfile {
        if (account == null) return HmsProfile()
        // str() = helper ambil getter String yang aman (null-safe).
        fun str(getter: String): String = runCatching {
            account.javaClass.getMethod(getter).invoke(account) as? String
        }.getOrNull().orEmpty()
        return HmsProfile(
            displayName = str("getDisplayName"),
            email = str("getEmail"),
            photoUri = str("getPhotoUriString"),
            openId = str("getOpenid"),
            unionId = str("getUnionId"),
            uid = runCatching {
                account.javaClass.getMethod("getUid").invoke(account) as? Long
            }.getOrNull() ?: 0L
        )
    }

    // =========================================================================
    // BAGIAN 2 — MEMBANGUN OBJEK PARAMETER SCOPE (HuaweiIdAuthParams)
    // =========================================================================
    // HuaweiIdAuthParams dibuat lewat builder:
    //   new HuaweiIdAuthParams.Builder().addScope(...).build();
    // Scope diambil dari konstanta statis enum Scopes (PROFILE, OPEN_ID, EMAIL).
    // Semua langkah ditulis dalam reflection (lihat komentar per baris).
    private fun buildAuthParams(context: Context): Any? {
        // ---- JALUR 1: Account Kit MODERN (class tersedia di runtime HMS Core) ----
        val modern = buildModernAuthParams()
        if (modern != null) return modern

        // ---- JALUR 2: FALLBACK ke koordinat class Account Kit LEGACY
        //      (...support.api.client.HuaweiIdAuthParams). Class ini tidak ada
        //      di compile classpath, tapi bisa muncul saat runtime lewat jar
        //      internal HMS Core / GRS. Kalau dua-duanya gagal, requestSignIn
        //      mengembalikan Error dengan pesan jelas (bukan crash).
        val authParamsBuilderCls = loadClass("com.huawei.hms.support.api.client.HuaweiIdAuthParams$Builder")
            ?: return null
        val scopesCls = loadClass("com.huawei.hms.support.api.client.Scopes") ?: return null

        // Ambil nilai konstanta scope dari static field enum Scopes.
        // PROFILE -> display name & foto; OPEN_ID -> openId/unionId.
        fun scopeValue(fieldName: String): String? = runCatching {
            scopesCls.getField(fieldName).get(null)?.toString()
        }.getOrNull()

        // Bangun instance Builder lewat constructor no-arg.
        val builder = runCatching {
            authParamsBuilderCls.getDeclaredConstructor().newInstance()
        }.getOrNull() ?: return null

        // Rangkai daftar scope yang tersedia (skip yang null biar tidak crash).
        listOfNotNull(scopeValue("PROFILE"), scopeValue("OPEN_ID")).forEach { scope ->
            // addScope(String) dipanggil satu per satu lebih aman daripada
            // setRequestedScopes(List) yang signature-nya bervariasi antar versi.
            runCatching { builder.method("addScope", 1)?.invoke(builder, scope) }
        }

        // build() menghasilkan objek HuaweiIdAuthParams final.
        return builder.callNoArg("build")
    }

    // ------------------------------------------------------------------------
    // Pembuat parameter utk Account Kit versi BARU (post-migrasi AGC).
    // Class target: com.huawei.hms.support.api.entity.auth.HuaweiIdAuthParams
    // dan Scopes ada di paket ...api.client pada SDK modern. Semua dibungkus
    // loadClassOrNull agar kalau class-nya tidak ada, kita balik null dan
    // pemanggil jatuh ke jalur fallback tanpa crash.
    // ------------------------------------------------------------------------
    private fun buildModernAuthParams(): Any? {
        val builderCls = loadClassOrNull(
            "com.huawei.hms.support.api.entity.auth.HuaweiIdAuthParams$Builder"
        ) ?: return null
        val scopesCls = loadClassOrNull(
            "com.huawei.hms.support.api.entity.auth.Scopes"
        ) ?: loadClassOrNull("com.huawei.hms.support.api.client.Scopes") ?: return null

        // Ambil konstanta scope (OPEN_ID & PROFILE) dari static field enum Scopes.
        fun scopeValue(fieldName: String): String? = runCatching {
            scopesCls.getField(fieldName).get(null)?.toString()
        }.getOrNull()

        val builder = runCatching {
            builderCls.getDeclaredConstructor().newInstance()
        }.getOrNull() ?: return null

        listOfNotNull(scopeValue("OPEN_ID"), scopeValue("PROFILE")).forEach { scope ->
            runCatching { builder.method("addScope", 1)?.invoke(builder, scope) }
        }
        return builder.callNoArg("build")
    }

    // =========================================================================
    // BAGIAN 3 — MENGAMBIL SERVICE AUTH (HuaweiIdAuthService)
    // =========================================================================
    // HuaweiIdAuthManager.getService(activity, authParams) -> service utama
    // tempat semua aksi auth berada (silentSignIn, getSignInIntent, dll.).
    private fun getAuthService(activity: Activity, authParams: Any?): Any? {
        // Cari manager auth: coba paket MODERN dulu (entity.auth), baru LEGACY
        // (support.api.client). firstNotNullOfOrNull = ambil yang pertama ketemu.
        val managerCls = listOf(
            "com.huawei.hms.support.api.entity.auth.HuaweiIdAuthManager",
            "com.huawei.hms.support.api.client.HuaweiIdAuthManager"
        ).firstNotNullOfOrNull { loadClassOrNull(it) }
            ?: return null
        // getService punya beberapa overload; cari yang parameternya
        // (Activity/Context + AuthParams) dan cocok dengan tipe aktivitas kita.
        val getService = managerCls.methods.firstOrNull { m ->
            m.name == "getService" && m.parameterCount == 2 &&
                m.parameterTypes[0].isAssignableFrom(activity.javaClass)
        } ?: return null
        // getService bersifat static -> receiver invoke-nya null.
        return runCatching { getService.invoke(null, activity, authParams) }.getOrNull()
    }

    // =========================================================================
    // BAGIAN 4 — CEK KETERSEDIAAN HMS CORE DI PERANGKAT
    // =========================================================================
    // Ini memakai API NORMAL dari hms-base (tanpa reflection) — bukti bahwa
    // sebagian besar fondasi HMS memang sudah tercakup dependency tersebut.
    suspend fun checkHmsAvailability(context: Context): String = withContext(Dispatchers.IO) {
        try {
            // Kode 0/1 = SUCCESS (HMS Core terpasang & siap), kode lain = masalah.
            val status = HuaweiApiAvailability.getInstance()
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
    // BAGIAN 5 — ALUR SIGN-IN LENGKAP (suspend, dipanggil dari Compose)
    // =========================================================================
    suspend fun requestSignIn(activity: Activity): SignInRequest = withContext(Dispatchers.IO) {
        try {
            // Langkah A: bangun parameter scope. Kalau gagal -> class Account
            // Kit tidak tersedia di perangkat (HMS Core lama/tidak ada).
            val authParams = buildAuthParams(activity)
                ?: return@withContext SignInRequest.Error(
                    "Class Account Kit tidak ditemukan. Pastikan HMS Core terpasang & update."
                )

            // Langkah B: ambil service auth.
            val service = getAuthService(activity, authParams)
                ?: return@withContext SignInRequest.Error("Gagal mengambil HuaweiIdAuthService.")

            // Langkah C: coba SILENT sign-in dulu.
            // silentSignIn() mengembalikan Task<HuaweiIdAccount>. Kalau task
            // selesai sukses (result non-null), user sudah pernah login ->
            // kita tidak perlu menampilkan UI apa pun (pengalaman lebih cepat).
            val silentTask = service.callNoArg("silentSignIn")
            val silentResult = silentTask?.let { t -> awaitTaskResult(t) }
            val silentProfile = toProfile(silentResult)
            if (silentProfile.isReal()) {
                return@withContext SignInRequest.AlreadySignedIn(silentProfile)
            }

            // Langkah D: belum login -> minta intent UI login dari service.
            val signInIntent = service.callNoArg("getSignInIntent") as? Intent
                ?: return@withContext SignInRequest.Error("getSignInIntent() mengembalikan null.")

            // Kembalikan closure onResult: nanti setelah ActivityResult tiba,
            // UI memanggil closure ini untuk mengekstrak profil dari data.
            return@withContext SignInRequest.NeedsUi(signInIntent) { _, data ->
                parseSignInResult(service, data)
            }
        } catch (e: Exception) {
            SignInRequest.Error("Terjadi kesalahan tak terduga: ${e.message}")
        }
    }

    // =========================================================================
    // BAGIAN 6 — PARSE HASIL SETELAH UI LOGIN SELESAI
    // =========================================================================
    // Service punya getHuaweiAccountFromIntent(Intent) -> HuaweiIdAccount.
    // Inilah penerjemah data hasil Activity login menjadi objek profil.
    private fun parseSignInResult(service: Any?, data: Intent?): HmsProfile? {
        if (service == null || data == null) return null
        // Jalur utama: parse dari Intent hasil.
        val fromIntent = runCatching {
            service.javaClass
                .getMethod("getHuaweiAccountFromIntent", Intent::class.java)
                .invoke(service, data)
        }.getOrNull()
        // Fallback: beberapa versi menyediakan getHuaweiAccount() (cache sesi).
        val account = fromIntent ?: runCatching {
            service.callNoArg("getHuaweiAccount")
        }.getOrNull()
        val profile = toProfile(account)
        return if (profile.isReal()) profile else null
    }

    // =========================================================================
    // BAGIAN 7 — SIGN OUT
    // =========================================================================
    // revokeAccess() melepas consent app terhadap akun Huawei user, lalu
    // signOut() membersihkan sesi lokal. Keduanya dipanggil berurutan agar
    // silentSignIn berikutnya benar-benar mengembalikan "belum login".
    suspend fun signOut(activity: Activity): Boolean = withContext(Dispatchers.IO) {
        try {
            val authParams = buildAuthParams(activity) ?: return@withContext false
            val service = getAuthService(activity, authParams) ?: return@withContext false
            service.callNoArg("revokeAccess")
            service.callNoArg("signOut")
            true
        } catch (_: Exception) {
            false
        }
    }

    // =========================================================================
    // BAGIAN 8 — MENUNGGU TASK HMS (async pattern bawaan HMS)
    // =========================================================================
    // Semua API HMS mengembalikan Task<T> dengan listener. Fungsi ini mengubah
    // gaya callback itu menjadi nilai suspend biasa (await) memakai
    // suspendCancellableCoroutine — inti interoperabilitas coroutine + SDK lama.
    private suspend fun awaitTaskResult(task: Any): Any? =
        suspendCancellableCoroutine { cont ->
            try {
                // Interface listener standar HMS (satu method onSuccess/onFailure).
                val okCls = loadClass("com.huawei.hms.tasks.OnSuccessListener")
                val failCls = loadClass("com.huawei.hms.tasks.OnFailureListener")
                if (okCls == null || failCls == null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }

                // Proxy dinamis: implementasikan interface listener TANPA
                // membuat class konkret — cukup tangani pemanggilan metodenya.
                val successHandler = java.lang.reflect.Proxy.newProxyInstance(
                    okCls.classLoader, arrayOf(okCls)
                ) { _, m, args ->
                    // onSuccess(T result) -> lanjutkan coroutine dengan result.
                    if (m.name == "onSuccess" && !cont.isCompleted) cont.resume(args?.firstOrNull())
                    null
                }
                val failureHandler = java.lang.reflect.Proxy.newProxyInstance(
                    failCls.classLoader, arrayOf(failCls)
                ) { _, m, _ ->
                    // onFailure(Exception) -> anggap tidak ada hasil (null).
                    if (m.name == "onFailure" && !cont.isCompleted) cont.resume(null)
                    null
                }
                // Daftarkan kedua listener ke Task.
                task.method("addOnSuccessListener", 1)?.invoke(task, successHandler)
                task.method("addOnFailureListener", 1)?.invoke(task, failureHandler)
            } catch (_: Exception) {
                if (!cont.isCompleted) cont.resume(null)
            }
        }
}
