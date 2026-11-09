package id.kasbulan.app

import id.kasbulan.app.data.BulanKas
import id.kasbulan.app.data.ExcelExporter
import id.kasbulan.app.data.TransaksiKas
import id.kasbulan.app.data.barisExcel
import java.time.LocalDate
import java.time.ZoneId
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExcelExportTest {

    private val bulan = BulanKas(id = 1, tahun = 2026, bulan = 11)
    private val bulanLain = BulanKas(id = 2, tahun = 2026, bulan = 12)

    private fun epoch(hari: Int): Long =
        LocalDate.of(2026, 11, hari).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun tx(
        jenis: String,
        saluran: String,
        kategori: String,
        nominal: Long,
        hari: Int,
        tujuan: String? = null,
        bulanId: Long = 1,
        catatan: String = ""
    ) = TransaksiKas(
        bulanId = bulanId,
        jenis = jenis,
        saluran = saluran,
        saluranTujuan = tujuan,
        kategori = kategori,
        nominal = nominal,
        catatan = catatan,
        epoch = epoch(hari)
    )

    @Test
    fun barisExcelMengurutkanDanMemisahkanKolom() {
        val daftar = listOf(
            tx(JENIS_KELUAR, SALURAN_TUNAI, "Operasional", 50_000, 3),
            tx(JENIS_MASUK, SALURAN_QRIS, "Penjualan", 200_000, 1),
            tx(JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 999_000, 2, bulanId = 2)
        )
        val baris = barisExcel(bulan, daftar)
        assertEquals(2, baris.size)
        assertEquals("Masuk", baris[0].jenis)
        assertEquals(200_000L, baris[0].masuk)
        assertNull(baris[0].keluar)
        assertEquals("QRIS", baris[0].saluran)
        assertEquals("Keluar", baris[1].jenis)
        assertEquals(50_000L, baris[1].keluar)
        assertNull(baris[1].masuk)
    }

    @Test
    fun barisExcelTransferTidakMasukKolomUang() {
        val daftar = listOf(
            tx(JENIS_TRANSFER, SALURAN_QRIS, "Transfer", 300_000, 5, tujuan = SALURAN_TUNAI)
        )
        val baris = barisExcel(bulan, daftar)
        assertEquals(1, baris.size)
        assertEquals("Transfer", baris[0].jenis)
        assertEquals("QRIS ke Tunai", baris[0].saluran)
        assertNull(baris[0].masuk)
        assertNull(baris[0].keluar)
    }

    @Test
    fun workbookSatuLembarPerBulanDenganTotal() {
        val daftar = listOf(
            tx(JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 100_000, 1),
            tx(JENIS_KELUAR, SALURAN_TUNAI, "Operasional", 40_000, 2),
            tx(JENIS_MASUK, SALURAN_TUNAI, "Penjualan", 70_000, 1, bulanId = 2)
        )
        val bytes = ExcelExporter.buildWorkbook(listOf(bulan, bulanLain), daftar)
        val wb = XSSFWorkbook(bytes.inputStream())
        assertEquals(2, wb.numberOfSheets)
        val sheet = wb.getSheet("November 2026")
        assertTrue(sheet != null)
        assertEquals("Tanggal", sheet.getRow(0).getCell(0).stringCellValue)
        // 2 data rows + header + total row = last row index 3
        val total = sheet.getRow(3)
        assertEquals("TOTAL", total.getCell(0).stringCellValue)
        assertEquals(100_000.0, total.getCell(5).numericCellValue, 0.01)
        assertEquals(40_000.0, total.getCell(6).numericCellValue, 0.01)
        wb.close()
    }

    @Test
    fun formatTanggalWaktuSesuaiPola() {
        val epoch = LocalDate.of(2026, 11, 9).atTime(21, 10)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals("9 Nov 2026, 21.10", formatTanggalWaktu(epoch))
    }
}
