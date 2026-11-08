package id.kasbulan.app

import id.kasbulan.app.data.BulanKas
import id.kasbulan.app.data.TransaksiKas
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KasLogicTest {

    private val bulan = BulanKas(
        id = 1,
        tahun = 2026,
        bulan = 11,
        saldoAwalTunai = 1_000_000,
        saldoAwalQris = 500_000
    )

    private fun transaksi(
        jenis: String,
        saluran: String,
        kategori: String,
        nominal: Long,
        tujuan: String? = null,
        bulanId: Long = 1
    ) = TransaksiKas(
        bulanId = bulanId,
        jenis = jenis,
        saluran = saluran,
        saluranTujuan = tujuan,
        kategori = kategori,
        nominal = nominal,
        epoch = 1_000L
    )

    private val daftar = listOf(
        transaksi(JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 300_000),
        transaksi(JENIS_MASUK, SALURAN_QRIS, "Penjualan", 400_000),
        transaksi(JENIS_KELUAR, SALURAN_TUNAI, "Pembelian Obat/PBF", 250_000),
        transaksi(JENIS_TRANSFER, SALURAN_QRIS, KATEGORI_TRANSFER, 200_000, tujuan = SALURAN_TUNAI),
        // Transaksi bulan lain harus diabaikan untuk bulan ini.
        transaksi(JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 999_000, bulanId = 2)
    )

    @Test
    fun saldoDihitungPerSaluranDanTransferMenggeserDompet() {
        val saldo = hitungSaldo(bulan, daftar)
        assertEquals(1_250_000L, saldo.tunai)
        assertEquals(700_000L, saldo.qris)
        assertEquals(1_950_000L, saldo.total)
    }

    @Test
    fun ringkasanTidakMenghitungTransfer() {
        val ringkasan = ringkasanBulan(daftar, bulan.id)
        assertEquals(700_000L, ringkasan.totalMasuk)
        assertEquals(250_000L, ringkasan.totalKeluar)
        assertEquals(700_000L, ringkasan.perKategoriMasuk["Penjualan"])
        assertEquals(250_000L, ringkasan.perKategoriKeluar["Pembelian Obat/PBF"])
    }

    @Test
    fun formatRupiahDanKelompokRibuan() {
        assertEquals("Rp 7.950.000", formatRupiah(7_950_000))
        assertEquals("-Rp 150.000", formatRupiah(-150_000))
        assertEquals("Rp 0", formatRupiah(0))
        assertEquals("1.250.000", groupThousands("1250000"))
        assertEquals("100", groupThousands("100"))
        assertEquals("", groupThousands(""))
        assertEquals("0", groupThousands("0"))
    }

    @Test
    fun teksLaporanMemuatAngkaPerSaluran() {
        val teks = laporanText(bulan, "Apotek Lestari", daftar)
        assertTrue(teks.contains("November 2026"))
        assertTrue(teks.contains("Apotek Lestari"))
        assertTrue(teks.contains("Rp 1.950.000"))
        assertTrue(teks.contains("Rp 1.250.000"))
        assertTrue(teks.contains("Rp 700.000"))
    }

    @Test
    fun labelBulanIndonesia() {
        assertEquals("November 2026", bulanLabel(2026, 11))
        assertEquals("Januari 2027", bulanLabel(2027, 1))
    }
    @Test
    fun saldoTanpaTransaksiSamaDenganSaldoAwal() {
        val saldo = hitungSaldo(bulan, emptyList())
        assertEquals(1_000_000L, saldo.tunai)
        assertEquals(500_000L, saldo.qris)
        assertEquals(1_500_000L, saldo.total)
    }

    @Test
    fun transferTunaiKeQrisMenggeserSebaliknya() {
        val saldo = hitungSaldo(
            bulan,
            listOf(transaksi(JENIS_TRANSFER, SALURAN_TUNAI, KATEGORI_TRANSFER, 150_000, tujuan = SALURAN_QRIS))
        )
        assertEquals(850_000L, saldo.tunai)
        assertEquals(650_000L, saldo.qris)
        assertEquals(1_500_000L, saldo.total)
    }

    @Test
    fun keluarDariQrisMengurangiDompetQris() {
        val saldo = hitungSaldo(
            bulan,
            listOf(transaksi(JENIS_KELUAR, SALURAN_QRIS, "Operasional", 125_000))
        )
        assertEquals(1_000_000L, saldo.tunai)
        assertEquals(375_000L, saldo.qris)
    }

    @Test
    fun saldoBolehNegatifTanpaDijepit() {
        val saldo = hitungSaldo(
            bulan,
            listOf(transaksi(JENIS_KELUAR, SALURAN_TUNAI, "Pembelian Obat/PBF", 1_500_000))
        )
        assertEquals(-500_000L, saldo.tunai)
        assertEquals(0L, saldo.total)
    }

    @Test
    fun ringkasanMengakumulasiKategoriYangSama() {
        val data = listOf(
            transaksi(JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 100_000),
            transaksi(JENIS_MASUK, SALURAN_QRIS, "Penjualan", 50_000),
            transaksi(JENIS_MASUK, SALURAN_TUNAI, "Tambahan Modal", 200_000),
            transaksi(JENIS_KELUAR, SALURAN_TUNAI, "Operasional", 30_000),
            transaksi(JENIS_KELUAR, SALURAN_QRIS, "Operasional", 20_000)
        )
        val ringkasan = ringkasanBulan(data, bulan.id)
        assertEquals(350_000L, ringkasan.totalMasuk)
        assertEquals(50_000L, ringkasan.totalKeluar)
        assertEquals(150_000L, ringkasan.perKategoriMasuk["Penjualan"])
        assertEquals(200_000L, ringkasan.perKategoriMasuk["Tambahan Modal"])
        assertEquals(50_000L, ringkasan.perKategoriKeluar["Operasional"])
        assertEquals(2, ringkasan.perKategoriMasuk.size)
        assertEquals(1, ringkasan.perKategoriKeluar.size)
    }

    @Test
    fun ringkasanDaftarKosongNolSemua() {
        val ringkasan = ringkasanBulan(emptyList(), bulan.id)
        assertEquals(0L, ringkasan.totalMasuk)
        assertEquals(0L, ringkasan.totalKeluar)
        assertTrue(ringkasan.perKategoriMasuk.isEmpty())
        assertTrue(ringkasan.perKategoriKeluar.isEmpty())
    }

    @Test
    fun groupThousandsMenyaringKarakterDanNolDiDepan() {
        assertEquals("15.000", groupThousands("Rp 15000"))
        assertEquals("7", groupThousands("007"))
        assertEquals("0", groupThousands("000"))
        assertEquals("", groupThousands("abc"))
        assertEquals("1.000", groupThousands("1000"))
        assertEquals("12.345.678", groupThousands("12345678"))
    }

    @Test
    fun formatRupiahNilaiBatas() {
        assertEquals("Rp 999", formatRupiah(999))
        assertEquals("Rp 1.000", formatRupiah(1_000))
        assertEquals("-Rp 1", formatRupiah(-1))
        assertEquals("Rp 1.000.000.000", formatRupiah(1_000_000_000))
    }

    @Test
    fun labelBulanDijepitKeRentangKalender() {
        assertEquals("Desember 2026", bulanLabel(2026, 13))
        assertEquals("Januari 2026", bulanLabel(2026, 0))
    }

    @Test
    fun labelHariUntukHariIniKemarinDanHariLain() {
        val hariIni = LocalDate.of(2026, 11, 9)
        assertEquals("Hari ini - Senin, 9 Nov 2026", labelHari(hariIni, hariIni))
        assertEquals("Kemarin - Minggu, 8 Nov 2026", labelHari(hariIni.minusDays(1), hariIni))
        assertEquals("Kamis, 1 Jan 2026", labelHari(LocalDate.of(2026, 1, 1), hariIni))
    }

    @Test
    fun teksLaporanBulanBerjalanTanpaTransaksi() {
        val teks = laporanText(bulan, "", emptyList())
        assertTrue(teks.contains("Status: Berjalan"))
        assertTrue(teks.contains("Saldo Awal: Rp 1.500.000"))
        assertTrue(teks.contains("Total Masuk: Rp 0"))
        assertTrue(teks.contains("Saldo Akhir: Rp 1.500.000"))
        assertFalse(teks.contains("per kategori"))
        assertTrue(teks.endsWith("Dibuat dengan KasBulanan"))
    }

    @Test
    fun teksLaporanMemuatRincianPerKategori() {
        val teks = laporanText(bulan, "Apotek Lestari", daftar)
        assertTrue(teks.contains("- Penjualan: Rp 700.000"))
        assertTrue(teks.contains("- Pembelian Obat/PBF: Rp 250.000"))
        assertTrue(teks.contains("Total Masuk: Rp 700.000"))
        assertTrue(teks.contains("Total Keluar: Rp 250.000"))
    }
}
