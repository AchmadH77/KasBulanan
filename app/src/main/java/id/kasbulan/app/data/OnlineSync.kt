package id.kasbulan.app.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await

/**
 * Lapisan online opsional (rancangan A, docs/ONLINE.md). Semua akses
 * Firebase dijaga: tanpa google-services.json Firebase tidak pernah
 * terinisialisasi dan lapisan ini diam - aplikasi berjalan offline
 * persis seperti sebelumnya.
 */
object FirebaseKit {
    fun siap(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    fun auth(): FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (e: Exception) {
        null
    }

    fun firestore(): FirebaseFirestore? = try {
        FirebaseFirestore.getInstance()
    } catch (e: Exception) {
        null
    }

    fun emailMasuk(): String? = auth()?.currentUser?.email

    suspend fun masukDenganEmail(email: String, kataSandi: String): String {
        val auth = auth() ?: throw IllegalStateException("Firebase belum dikonfigurasi")
        val hasil = auth.signInWithEmailAndPassword(email, kataSandi).await()
        return hasil.user?.email ?: email
    }

    suspend fun daftarDenganEmail(email: String, kataSandi: String): String {
        val auth = auth() ?: throw IllegalStateException("Firebase belum dikonfigurasi")
        val hasil = auth.createUserWithEmailAndPassword(email, kataSandi).await()
        return hasil.user?.email ?: email
    }

    fun keluar() {
        auth()?.signOut()
    }
}

sealed interface HasilSync {
    data object BelumDikonfigurasi : HasilSync
    data object BelumMasuk : HasilSync
    data class Berhasil(val didorong: Int, val ditarik: Int) : HasilSync
    data class Gagal(val pesan: String) : HasilSync
}

/**
 * Sinkronisasi dua arah Room <-> Firestore:
 * dorong baris kotor (bulan dulu, baru transaksi), lalu tarik semua
 * dokumen dan terapkan yang lebih baru (aturan terakhir-diubah-menang).
 * Struktur server: users/{uid}/bulan/{remoteId} dan
 * users/{uid}/transaksi/{remoteId}.
 */
class SyncManager(private val repository: KasRepository) {

    suspend fun sinkronSekarang(context: Context): HasilSync {
        if (!FirebaseKit.siap(context)) return HasilSync.BelumDikonfigurasi
        val uid = FirebaseKit.auth()?.currentUser?.uid ?: return HasilSync.BelumMasuk
        val fs = FirebaseKit.firestore() ?: return HasilSync.BelumDikonfigurasi
        return try {
            var didorong = 0
            var ditarik = 0
            val akar = fs.collection("users").document(uid)

            // 1) Dorong: bulan lebih dulu karena transaksi menunjuk bulan.
            val petaBulan = repository.bulanMentah().associate { it.id to it.remoteId }
            for (b in repository.bulanKotor()) {
                if (b.remoteId.isBlank()) continue
                akar.collection("bulan").document(b.remoteId)
                    .set(SyncLogic.kePeta(SyncLogic.bulanKeSync(b))).await()
                repository.tandaiBulanBersih(b.id, b.updatedAt)
                didorong++
            }
            for (t in repository.transaksiKotor()) {
                if (t.remoteId.isBlank()) continue
                val bulanRemoteId = petaBulan[t.bulanId] ?: continue
                akar.collection("transaksi").document(t.remoteId)
                    .set(SyncLogic.kePeta(SyncLogic.transaksiKeSync(t, bulanRemoteId))).await()
                repository.tandaiTransaksiBersih(t.id, t.updatedAt)
                didorong++
            }

            // 2) Tarik: bulan dulu, bangun peta remoteId -> id lokal,
            // lalu transaksi diterjemahkan ke id bulan lokalnya.
            val snapBulan = akar.collection("bulan").get().await()
            for (doc in snapBulan.documents) {
                val remote = SyncLogic.bulanDariPeta(doc.data ?: continue) ?: continue
                val lokal = repository.bulanByRemoteId(remote.remoteId)
                if (lokal == null || SyncLogic.remoteMenang(lokal.updatedAt, remote.updatedAt)) {
                    repository.terapkanBulanRemote(SyncLogic.bulanDariSync(remote))
                    ditarik++
                }
            }
            val petaRemoteKeLokal = repository.bulanMentah().associate { it.remoteId to it.id }
            val snapTransaksi = akar.collection("transaksi").get().await()
            for (doc in snapTransaksi.documents) {
                val remote = SyncLogic.transaksiDariPeta(doc.data ?: continue) ?: continue
                val bulanLokal = petaRemoteKeLokal[remote.bulanRemoteId] ?: continue
                val lokal = repository.transaksiByRemoteId(remote.remoteId)
                if (lokal == null || SyncLogic.remoteMenang(lokal.updatedAt, remote.updatedAt)) {
                    repository.terapkanTransaksiRemote(
                        SyncLogic.transaksiDariSync(remote, bulanLokal)
                    )
                    ditarik++
                }
            }

            repository.bersihkanSampah()
            HasilSync.Berhasil(didorong, ditarik)
        } catch (e: Exception) {
            HasilSync.Gagal(e.message ?: "Sinkronisasi gagal")
        }
    }
}

/** Sync berkala di latar belakang; diam bila belum dikonfigurasi/masuk. */
class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!FirebaseKit.siap(applicationContext)) return Result.success()
        if (FirebaseKit.auth()?.currentUser == null) return Result.success()
        val repository = KasRepository(AppDatabase.create(applicationContext))
        return when (val hasil = SyncManager(repository).sinkronSekarang(applicationContext)) {
            is HasilSync.Berhasil -> {
                applicationContext
                    .getSharedPreferences("kasbulan_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putLong("online_sync_terakhir", System.currentTimeMillis())
                    .apply()
                Result.success()
            }
            is HasilSync.Gagal -> Result.retry()
            else -> Result.success()
        }
    }
}

/** Jadwalkan sync berkala 6 jam sekali (hanya berjalan saat ada jaringan). */
fun jadwalkanSyncBerkala(context: Context) {
    val syarat = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val permintaan = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
        .setConstraints(syarat)
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "kasbulanan-sync",
        ExistingPeriodicWorkPolicy.KEEP,
        permintaan
    )
}
