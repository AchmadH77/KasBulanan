package id.kasbulan.app.data

import androidx.room.withTransaction
import id.kasbulan.app.STATUS_DITUTUP
import id.kasbulan.app.ringkasanBulan
import id.kasbulan.app.saldoAkhirBulan
import kotlinx.coroutines.flow.Flow

class KasRepository(private val db: AppDatabase) {
    private val dao: KasDao = db.kasDao()

    val bulanList: Flow<List<BulanKas>> = dao.bulanFlow()
    val transaksiList: Flow<List<TransaksiKas>> = dao.transaksiFlow()

    suspend fun insertBulan(bulan: BulanKas): Long = dao.insertBulan(bulan)

    suspend fun insertTransaksi(transaksi: TransaksiKas): Long = dao.insertTransaksi(transaksi)

    suspend fun updateTransaksi(transaksi: TransaksiKas) = dao.updateTransaksi(transaksi)

    suspend fun deleteTransaksi(transaksi: TransaksiKas) = dao.deleteTransaksi(transaksi)

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
                )
            )
        }
    }

    /** Ganti seluruh data (restore backup) dalam satu transaksi. */
    suspend fun replaceAll(bulan: List<BulanKas>, transaksi: List<TransaksiKas>) {
        db.withTransaction {
            dao.clearTransaksi()
            dao.clearBulan()
            for (b in bulan) dao.insertBulan(b)
            for (t in transaksi) dao.insertTransaksi(t)
        }
    }
}
