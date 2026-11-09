package id.kasbulan.app

import id.kasbulan.app.data.BulanKas
import id.kasbulan.app.data.SyncLogic
import id.kasbulan.app.data.TransaksiKas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncLogicTest {

    private val bulan = BulanKas(
        id = 7, tahun = 2026, bulan = 11, status = STATUS_DITUTUP,
        ditutupPada = 1_731_000_000_000, totalMasukFinal = 5_000_000,
        totalKeluarFinal = 3_000_000, saldoAkhirTunaiFinal = 1_500_000,
        saldoAkhirQrisFinal = 500_000, remoteId = "bulan-7",
        updatedAt = 1_731_500_000_000, terhapus = false, kotor = true
    )

    private val transaksi = TransaksiKas(
        id = 9, bulanId = 7, jenis = JENIS_TRANSFER, saluran = SALURAN_QRIS,
        saluranTujuan = SALURAN_TUNAI, kategori = KATEGORI_TRANSFER,
        nominal = 2_000_000, catatan = "Cairkan QRIS", epoch = 1_731_100_000_000,
        remoteId = "transaksi-9", updatedAt = 1_731_600_000_000,
        terhapus = true, kotor = true
    )

    @Test
    fun bulanBolakBalikLewatPeta() {
        val peta = SyncLogic.kePeta(SyncLogic.bulanKeSync(bulan))
        val kembali = SyncLogic.bulanDariPeta(peta)!!
        val entitas = SyncLogic.bulanDariSync(kembali)
        assertEquals(bulan.tahun, entitas.tahun)
        assertEquals(bulan.bulan, entitas.bulan)
        assertEquals(bulan.status, entitas.status)
        assertEquals(bulan.ditutupPada, entitas.ditutupPada)
        assertEquals(bulan.totalMasukFinal, entitas.totalMasukFinal)
        assertEquals(bulan.saldoAkhirQrisFinal, entitas.saldoAkhirQrisFinal)
        assertEquals("bulan-7", entitas.remoteId)
        assertEquals(bulan.updatedAt, entitas.updatedAt)
        assertFalse(entitas.kotor)
    }

    @Test
    fun transaksiBolakBalikLewatPeta() {
        val peta = SyncLogic.kePeta(SyncLogic.transaksiKeSync(transaksi, "bulan-7"))
        val kembali = SyncLogic.transaksiDariPeta(peta)!!
        assertEquals("bulan-7", kembali.bulanRemoteId)
        val entitas = SyncLogic.transaksiDariSync(kembali, 42)
        assertEquals(42L, entitas.bulanId)
        assertEquals(JENIS_TRANSFER, entitas.jenis)
        assertEquals(SALURAN_TUNAI, entitas.saluranTujuan)
        assertEquals(2_000_000L, entitas.nominal)
        assertTrue(entitas.terhapus)
        assertFalse(entitas.kotor)
    }

    @Test
    fun remoteMenangHanyaBilaLebihBaru() {
        assertTrue(SyncLogic.remoteMenang(100, 101))
        assertFalse(SyncLogic.remoteMenang(100, 100))
        assertFalse(SyncLogic.remoteMenang(101, 100))
    }

    @Test
    fun petaTidakSahDitolak() {
        assertNull(SyncLogic.bulanDariPeta(mapOf("remoteId" to "")))
        assertNull(SyncLogic.transaksiDariPeta(mapOf("remoteId" to "x")))
    }
}
