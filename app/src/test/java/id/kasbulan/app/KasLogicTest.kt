package id.kasbulan.app

import id.kasbulan.app.data.BulanKas
import id.kasbulan.app.data.TransaksiKas
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KasLogicTest {

    private val bulan = BulanKas(
        id = 1,
        tahun = 2026,
        bulan = 11
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
    fun saldoGlobalMenghitungSemuaBulanDanTransferMenggeserDompet() {
        val saldo = hitungSaldo(daftar)
        // Tunai: 300rb masuk - 250rb keluar + 200rb transfer masuk + 999rb bulan lain.
        assertEquals(1_249_000L, saldo.tunai)
        // QRIS: 400rb masuk - 200rb transfer keluar.
        assertEquals(200_000L, saldo.qris)
        assertEquals(1_449_000L, saldo.total)
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
        val teks = laporanText(bulan, "Apotek Lestari", listOf(bulan), daftar)
        assertTrue(teks.contains("November 2026"))
        assertTrue(teks.contains("Apotek Lestari"))
        assertTrue(teks.contains("Saldo Awal: Rp 0"))
        assertTrue(teks.contains("Rp 450.000"))
        assertTrue(teks.contains("Rp 250.000"))
        assertTrue(teks.contains("Rp 200.000"))
    }

    @Test
    fun labelBulanIndonesia() {
        assertEquals("November 2026", bulanLabel(2026, 11))
        assertEquals("Januari 2027", bulanLabel(2027, 1))
    }
    @Test
    fun saldoTanpaTransaksiNol() {
        val saldo = hitungSaldo(emptyList())
        assertEquals(0L, saldo.tunai)
        assertEquals(0L, saldo.qris)
        assertEquals(0L, saldo.total)
    }

    @Test
    fun transferTunaiKeQrisMenggeserSebaliknya() {
        val saldo = hitungSaldo(
            listOf(transaksi(JENIS_TRANSFER, SALURAN_TUNAI, KATEGORI_TRANSFER, 150_000, tujuan = SALURAN_QRIS))
        )
        assertEquals(-150_000L, saldo.tunai)
        assertEquals(150_000L, saldo.qris)
        assertEquals(0L, saldo.total)
    }

    @Test
    fun keluarDariQrisMengurangiDompetQris() {
        val saldo = hitungSaldo(
            listOf(transaksi(JENIS_KELUAR, SALURAN_QRIS, "Operasional", 125_000))
        )
        assertEquals(0L, saldo.tunai)
        assertEquals(-125_000L, saldo.qris)
    }

    @Test
    fun saldoBolehNegatifTanpaDijepit() {
        val saldo = hitungSaldo(
            listOf(transaksi(JENIS_KELUAR, SALURAN_TUNAI, "Pembelian Obat/PBF", 1_500_000))
        )
        assertEquals(-1_500_000L, saldo.tunai)
        assertEquals(-1_500_000L, saldo.total)
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
        val teks = laporanText(bulan, "", listOf(bulan), emptyList())
        assertTrue(teks.contains("Status: Berjalan"))
        assertTrue(teks.contains("Saldo Awal: Rp 0"))
        assertTrue(teks.contains("Total Masuk: Rp 0"))
        assertTrue(teks.contains("Saldo Akhir: Rp 0"))
        assertFalse(teks.contains("per kategori"))
        assertTrue(teks.endsWith("Dibuat dengan KasBulanan"))
    }

    @Test
    fun teksLaporanMemuatRincianPerKategori() {
        val teks = laporanText(bulan, "Apotek Lestari", listOf(bulan), daftar)
        assertTrue(teks.contains("- Penjualan: Rp 700.000"))
        assertTrue(teks.contains("- Pembelian Obat/PBF: Rp 250.000"))
        assertTrue(teks.contains("Total Masuk: Rp 700.000"))
        assertTrue(teks.contains("Total Keluar: Rp 250.000"))
    }

    private fun transaksiPada(
        tanggal: LocalDate,
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
        epoch = tanggal.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )

    @Test
    fun ringkasanHariMengelompokkanPerKategoriDanSaluran() {
        val tanggal = LocalDate.of(2026, 11, 9)
        val data = listOf(
            transaksiPada(tanggal, JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 300_000),
            transaksiPada(tanggal, JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 185_000),
            transaksiPada(tanggal, JENIS_MASUK, SALURAN_QRIS, "Penjualan", 372_000),
            transaksiPada(tanggal, JENIS_KELUAR, SALURAN_TUNAI, "Pembelian Obat/PBF", 150_000),
            transaksiPada(tanggal, JENIS_KELUAR, SALURAN_TUNAI, "Operasional", 60_000),
            transaksiPada(tanggal, JENIS_TRANSFER, SALURAN_QRIS, KATEGORI_TRANSFER, 300_000, tujuan = SALURAN_TUNAI),
            transaksiPada(tanggal.minusDays(1), JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 999_000)
        )
        val r = ringkasanHari(tanggal, data)
        assertEquals(857_000L, r.totalMasuk)
        assertEquals(210_000L, r.totalKeluar)
        assertEquals(2, r.rincianMasuk.size)
        assertEquals(485_000L, r.rincianMasuk.first { it.saluran == SALURAN_TUNAI }.nominal)
        assertEquals(372_000L, r.rincianMasuk.first { it.saluran == SALURAN_QRIS }.nominal)
        assertEquals(2, r.rincianKeluar.size)
        assertEquals(1, r.transfer.size)
        assertEquals(300_000L, r.transfer[0].nominal)
        assertEquals(6, r.jumlahTransaksi)
    }

    @Test
    fun ringkasanHariTanpaCatatanNolSemua() {
        val r = ringkasanHari(LocalDate.of(2026, 11, 9), emptyList())
        assertEquals(0L, r.totalMasuk)
        assertEquals(0L, r.totalKeluar)
        assertTrue(r.rincianMasuk.isEmpty())
        assertTrue(r.rincianKeluar.isEmpty())
        assertTrue(r.transfer.isEmpty())
        assertEquals(0, r.jumlahTransaksi)
    }

    @Test
    fun pendapatanPerHariHanyaPenjualanDanTerurut() {
        val data = listOf(
            transaksiPada(LocalDate.of(2026, 11, 3), JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 100_000),
            transaksiPada(LocalDate.of(2026, 11, 1), JENIS_MASUK, SALURAN_QRIS, "Penjualan", 200_000),
            transaksiPada(LocalDate.of(2026, 11, 3), JENIS_MASUK, SALURAN_QRIS, "Penjualan", 50_000),
            transaksiPada(LocalDate.of(2026, 11, 2), JENIS_MASUK, SALURAN_TUNAI, "Tambahan Modal", 500_000),
            transaksiPada(LocalDate.of(2026, 11, 2), JENIS_KELUAR, SALURAN_TUNAI, "Operasional", 10_000),
            transaksiPada(LocalDate.of(2026, 11, 4), JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 700_000, bulanId = 2)
        )
        val hasil = pendapatanPerHari(data, bulan.id)
        assertEquals(2, hasil.size)
        assertEquals(LocalDate.of(2026, 11, 1), hasil[0].tanggal)
        assertEquals(200_000L, hasil[0].total)
        assertEquals(LocalDate.of(2026, 11, 3), hasil[1].tanggal)
        assertEquals(150_000L, hasil[1].total)
        assertEquals(350_000L, hasil.sumOf { it.total })
    }

    @Test
    fun saldoAkhirHariBerhentiPadaTanggalDiminta() {
        val d1 = LocalDate.of(2026, 11, 1)
        val d3 = LocalDate.of(2026, 11, 3)
        val data = listOf(
            transaksiPada(d1, JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 100_000),
            // Catatan bulan lain pada tanggal yang sama ikut terhitung - saldo kontinu.
            transaksiPada(d1, JENIS_MASUK, SALURAN_QRIS, "Penjualan", 25_000, bulanId = 2),
            transaksiPada(d3, JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 900_000),
            transaksiPada(d3, JENIS_KELUAR, SALURAN_QRIS, "Operasional", 50_000)
        )
        val saldo1 = saldoAkhirHari(data, d1)
        assertEquals(100_000L, saldo1.tunai)
        assertEquals(25_000L, saldo1.qris)
        val saldo3 = saldoAkhirHari(data, d3)
        assertEquals(1_000_000L, saldo3.tunai)
        assertEquals(-25_000L, saldo3.qris)
    }

    @Test
    fun saldoAwalBulanSamaDenganSaldoAkhirBulanSebelumnya() {
        val oktober = BulanKas(id = 2, tahun = 2026, bulan = 10)
        val semuaBulan = listOf(oktober, bulan)
        val data = listOf(
            transaksiPada(LocalDate.of(2026, 10, 20), JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 500_000, bulanId = 2),
            transaksiPada(LocalDate.of(2026, 10, 21), JENIS_KELUAR, SALURAN_QRIS, "Operasional", 50_000, bulanId = 2),
            transaksiPada(LocalDate.of(2026, 11, 2), JENIS_MASUK, SALURAN_QRIS, "Penjualan", 200_000)
        )
        val awalNov = saldoAwalBulan(bulan, semuaBulan, data)
        assertEquals(500_000L, awalNov.tunai)
        assertEquals(-50_000L, awalNov.qris)
        assertEquals(awalNov, saldoAkhirBulan(oktober, semuaBulan, data))
        val akhirNov = saldoAkhirBulan(bulan, semuaBulan, data)
        assertEquals(500_000L, akhirNov.tunai)
        assertEquals(150_000L, akhirNov.qris)
        assertEquals(SaldoKas(0, 0), saldoAwalBulan(oktober, semuaBulan, data))
    }

    @Test
    fun teksRekapHariMemuatRincianDanSaldoAkhir() {
        val tanggal = LocalDate.of(2026, 11, 9)
        val data = listOf(
            transaksiPada(tanggal, JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 485_000),
            transaksiPada(tanggal, JENIS_MASUK, SALURAN_QRIS, "Penjualan", 372_000),
            transaksiPada(tanggal, JENIS_KELUAR, SALURAN_TUNAI, "Operasional", 60_000),
            transaksiPada(tanggal, JENIS_TRANSFER, SALURAN_QRIS, KATEGORI_TRANSFER, 300_000, tujuan = SALURAN_TUNAI)
        )
        val teks = rekapHariText(
            "Apotek Lestari",
            tanggal,
            ringkasanHari(tanggal, data),
            saldoAkhirHari(data, tanggal)
        )
        assertTrue(teks.contains("Senin, 9 Nov 2026"))
        assertTrue(teks.contains("Pemasukan: +Rp 857.000"))
        assertTrue(teks.contains("- Penjualan (Tunai): Rp 485.000"))
        assertTrue(teks.contains("- Penjualan (QRIS): Rp 372.000"))
        assertTrue(teks.contains("Pengeluaran: -Rp 60.000"))
        assertTrue(teks.contains("Transfer QRIS"))
        assertTrue(teks.contains("Saldo Akhir: Rp 797.000"))
        assertTrue(teks.endsWith("Dibuat dengan KasBulanan"))
    }

    @Test
    fun formatTanggalSingkatIndonesia() {
        assertEquals("Sen, 9 Nov", formatTanggalSingkat(LocalDate.of(2026, 11, 9)))
        assertEquals("Jum, 6 Nov", formatTanggalSingkat(LocalDate.of(2026, 11, 6)))
    }
}
