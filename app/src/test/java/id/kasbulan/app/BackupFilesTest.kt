package id.kasbulan.app

import id.kasbulan.app.data.BackupHelper
import id.kasbulan.app.data.BulanKas
import id.kasbulan.app.data.TransaksiKas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupFilesTest {

    private val bulan = listOf(
        BulanKas(
            id = 1,
            tahun = 2026,
            bulan = 10,
            saldoAwalTunai = 3_150_000,
            saldoAwalQris = 0,
            status = STATUS_DITUTUP,
            ditutupPada = 1_730_000_000_000,
            totalMasukFinal = 14_200_000,
            totalKeluarFinal = 13_450_000,
            saldoAkhirTunaiFinal = 2_600_000,
            saldoAkhirQrisFinal = 1_300_000
        ),
        BulanKas(id = 2, tahun = 2026, bulan = 11, saldoAwalTunai = 2_600_000, saldoAwalQris = 1_300_000)
    )

    private val transaksi = listOf(
        TransaksiKas(
            id = 1,
            bulanId = 2,
            jenis = JENIS_MASUK,
            saluran = SALURAN_QRIS,
            kategori = "Penjualan",
            nominal = 1_250_000,
            catatan = "Penjualan pagi",
            epoch = 1_731_000_000_000
        ),
        TransaksiKas(
            id = 2,
            bulanId = 2,
            jenis = JENIS_TRANSFER,
            saluran = SALURAN_QRIS,
            saluranTujuan = SALURAN_TUNAI,
            kategori = KATEGORI_TRANSFER,
            nominal = 2_000_000,
            catatan = "Hasil QRIS dicairkan",
            epoch = 1_731_100_000_000
        )
    )

    @Test
    fun backupRoundTripMenjagaSemuaKolom() {
        val json = BackupHelper.buildBackupJson(bulan, transaksi, 1_731_200_000_000)
        val parsed = BackupHelper.parseBackupJson(json)
        assertEquals(bulan, parsed?.bulan)
        assertEquals(transaksi, parsed?.transaksi)
        assertEquals(1_731_200_000_000L, parsed?.exportedAtMillis)
    }

    @Test
    fun parseMenolakFileBukanBackupKasBulanan() {
        assertNull(BackupHelper.parseBackupJson("{}"))
        assertNull(BackupHelper.parseBackupJson("{\"app\":\"kasir\",\"version\":1,\"bulan\":[],\"transaksi\":[]}"))
        assertNull(BackupHelper.parseBackupJson("bukan json"))
    }

    @Test
    fun parseMenolakTransaksiTanpaBulanYangValid() {
        val json = BackupHelper.buildBackupJson(bulan, transaksi, 0)
            .replace("\"bulanId\":2", "\"bulanId\":99")
        assertNull(BackupHelper.parseBackupJson(json))
    }

    @Test
    fun parseMenolakJenisTidakDikenal() {
        val json = BackupHelper.buildBackupJson(bulan, transaksi, 0)
            .replace("\"jenis\":\"MASUK\"", "\"jenis\":\"APA\"")
        assertNull(BackupHelper.parseBackupJson(json))
    }
    @Test
    fun parseMenolakVersiSelainSatu() {
        val json = BackupHelper.buildBackupJson(bulan, transaksi, 0)
            .replace("\"version\":1", "\"version\":2")
        assertNull(BackupHelper.parseBackupJson(json))
    }

    @Test
    fun parseMenolakSaluranTidakDikenal() {
        val json = BackupHelper.buildBackupJson(bulan, transaksi, 0)
            .replace("\"saluran\":\"QRIS\"", "\"saluran\":\"EDC\"")
        assertNull(BackupHelper.parseBackupJson(json))
    }

    @Test
    fun parseMenolakSaluranTujuanTidakDikenal() {
        val json = BackupHelper.buildBackupJson(bulan, transaksi, 0)
            .replace("\"saluranTujuan\":\"TUNAI\"", "\"saluranTujuan\":\"EDC\"")
        assertNull(BackupHelper.parseBackupJson(json))
    }

    @Test
    fun parseMenolakBulanDiLuarKalender() {
        val bulanAneh = listOf(
            BulanKas(id = 9, tahun = 2026, bulan = 13, saldoAwalTunai = 0, saldoAwalQris = 0)
        )
        val json = BackupHelper.buildBackupJson(bulanAneh, emptyList(), 0)
        assertNull(BackupHelper.parseBackupJson(json))
    }

    @Test
    fun backupKosongBerhasilDiparse() {
        val json = BackupHelper.buildBackupJson(emptyList(), emptyList(), 42)
        val parsed = BackupHelper.parseBackupJson(json)
        assertNotNull(parsed)
        assertEquals(0, parsed?.bulan?.size)
        assertEquals(0, parsed?.transaksi?.size)
        assertEquals(42L, parsed?.exportedAtMillis)
    }
}
