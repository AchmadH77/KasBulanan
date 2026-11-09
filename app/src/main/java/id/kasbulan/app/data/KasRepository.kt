package id.kasbulan.app.data

import androidx.room.withTransaction
import id.kasbulan.app.STATUS_DITUTUP
import java.util.UUID
import id.kasbulan.app.ringkasanBulan
import id.kasbulan.app.saldoAkhirBulan
import kotlinx.coroutines.flow.Flow

class KasRepository(private val db: AppDatabase) {
    private val dao: KasDao = db.kasDao()

    val bulanList: Flow<List<BulanKas>> = dao.bulanFlow()
    val transaksiList: Flow<List<TransaksiKas>> = dao.transaksiFlow()

    /** Cap kolom sync untuk baris baru: remoteId acak bila kosong. */
    private fun BulanKas.dicap(): BulanKas = copy(
        remoteId = remoteId.ifBlank { UUID.randomUUID().toString() },
        updatedAt = System.currentTimeMillis(),
        terhapus = false,
        kotor = true
    )

    private fun TransaksiKas.dicap(): TransaksiKas = copy(
        remoteId = remoteId.ifBlank { UUID.randomUUID().toString() },
        updatedAt = System.currentTimeMillis(),
        terhapus = false,
        kotor = true
    )

    suspend fun insertBulan(bulan: BulanKas): Long = dao.insertBulan(bulan.dicap())

    suspend fun insertTransaksi(transaksi: TransaksiKas): Long = dao.insertTransaksi(transaksi.dicap())

    suspend fun updateTransaksi(transaksi: TransaksiKas) = dao.updateTransaksi(
        transaksi.copy(updatedAt = System.currentTimeMillis(), kotor = true)
    )

    /** Hapus lunak: baris ditandai terhapus supaya ikut tersinkron. */
    suspend fun deleteTransaksi(transaksi: TransaksiKas) = dao.updateTransaksi(
        transaksi.copy(
            terhapus = true,
            updatedAt = System.currentTimeMillis(),
            kotor = true
        )
    )

    /**
     * Tutup buku: bekukan ringkasan + saldo akhir bulan ini, lalu buka
     * bulan berikutnya. Saldo bersifat kontinu - bulan baru tidak membawa
     * saldo awal; saldo berjalan terus dari seluruh catatan.
     * Semua dalam satu transaksi database.
     */
    suspend fun tutupBulan(bulan: BulanKas) {
        db.withTransaction {
            val semua = dao.allTransaksiOnce()
            val semuaBulan = dao.allBulanOnce()
            val saldo = saldoAkhirBulan(bulan, semuaBulan, semua)
            val ringkasan = ringkasanBulan(semua, bulan.id)
            dao.updateBulan(
                bulan.copy(
                    updatedAt = System.currentTimeMillis(),
                    kotor = true,
                    status = STATUS_DITUTUP,
                    ditutupPada = System.currentTimeMillis(),
                    totalMasukFinal = ringkasan.totalMasuk,
                    totalKeluarFinal = ringkasan.totalKeluar,
                    saldoAkhirTunaiFinal = saldo.tunai,
                    saldoAkhirQrisFinal = saldo.qris
                )
            )
            val tahunBerikut = if (bulan.bulan == 12) bulan.tahun + 1 else bulan.tahun
            val bulanBerikut = if (bulan.bulan == 12) 1 else bulan.bulan + 1
            dao.insertBulan(
                BulanKas(
                    tahun = tahunBerikut,
                    bulan = bulanBerikut
                ).dicap()
            )
        }
    }

    /** Ganti seluruh data (restore backup) dalam satu transaksi. */
    suspend fun replaceAll(bulan: List<BulanKas>, transaksi: List<TransaksiKas>) {
        db.withTransaction {
            dao.clearTransaksi()
            dao.clearBulan()
            // Baris hasil restore wajib didorong ke server pada sync
            // berikutnya; yang belum punya remoteId diberi yang baru.
            for (b in bulan) dao.insertBulan(
                b.copy(
                    remoteId = b.remoteId.ifBlank { UUID.randomUUID().toString() },
                    terhapus = false,
                    kotor = true
                )
            )
            for (t in transaksi) dao.insertTransaksi(
                t.copy(
                    remoteId = t.remoteId.ifBlank { UUID.randomUUID().toString() },
                    terhapus = false,
                    kotor = true
                )
            )
        }
    }

    // ---- Primitif lapisan sync (dipakai SyncManager) ----
    suspend fun bulanMentah(): List<BulanKas> = dao.semuaBulanMentah()
    suspend fun transaksiMentah(): List<TransaksiKas> = dao.semuaTransaksiMentah()
    suspend fun bulanKotor(): List<BulanKas> = dao.bulanKotor()
    suspend fun transaksiKotor(): List<TransaksiKas> = dao.transaksiKotor()
    suspend fun bulanByRemoteId(remoteId: String): BulanKas? = dao.bulanByRemoteId(remoteId)
    suspend fun transaksiByRemoteId(remoteId: String): TransaksiKas? =
        dao.transaksiByRemoteId(remoteId)

    /** Terapkan baris dari server: cocokkan lewat remoteId, simpan bersih. */
    suspend fun terapkanBulanRemote(remote: BulanKas) {
        val lokal = dao.bulanByRemoteId(remote.remoteId)
        if (lokal == null) {
            dao.insertBulan(remote.copy(id = 0, kotor = false))
        } else {
            dao.updateBulan(remote.copy(id = lokal.id, kotor = false))
        }
    }

    suspend fun terapkanTransaksiRemote(remote: TransaksiKas) {
        val lokal = dao.transaksiByRemoteId(remote.remoteId)
        if (lokal == null) {
            dao.insertTransaksi(remote.copy(id = 0, kotor = false))
        } else {
            dao.updateTransaksi(remote.copy(id = lokal.id, kotor = false))
        }
    }

    suspend fun tandaiBulanBersih(id: Long, updatedAt: Long) =
        dao.tandaiBulanBersih(id, updatedAt)

    suspend fun tandaiTransaksiBersih(id: Long, updatedAt: Long) =
        dao.tandaiTransaksiBersih(id, updatedAt)

    /** Buang permanen baris terhapus lunak yang sudah tersinkron. */
    suspend fun bersihkanSampah() {
        dao.buangTransaksiTerhapus()
        dao.buangBulanTerhapus()
    }
}
