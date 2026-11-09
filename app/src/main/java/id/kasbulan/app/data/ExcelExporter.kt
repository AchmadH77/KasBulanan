package id.kasbulan.app.data

import id.kasbulan.app.JENIS_KELUAR
import id.kasbulan.app.JENIS_TRANSFER
import id.kasbulan.app.SALURAN_QRIS
import id.kasbulan.app.bulanLabel
import id.kasbulan.app.formatTanggal
import id.kasbulan.app.saldoAkhirBulan
import java.io.ByteArrayOutputStream
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFWorkbook

/** Satu baris data pada lembar Excel satu bulan. */
data class BarisExcel(
    val tanggal: String,
    val jenis: String,
    val kategori: String,
    val saluran: String,
    val keterangan: String,
    val masuk: Long?,
    val keluar: Long?
)

private fun labelSaluran(saluran: String): String = if (saluran == SALURAN_QRIS) "QRIS" else "Tunai"

/**
 * Baris-baris untuk satu bulan, urut tanggal menaik. Transfer antar saluran
 * tidak masuk kolom Masuk/Keluar karena bukan pemasukan atau pengeluaran.
 */
fun barisExcel(bulan: BulanKas, transaksi: List<TransaksiKas>): List<BarisExcel> =
    transaksi.filter { it.bulanId == bulan.id }
        .sortedBy { it.epoch }
        .map { t ->
            when (t.jenis) {
                JENIS_TRANSFER -> BarisExcel(
                    tanggal = formatTanggal(t.epoch),
                    jenis = "Transfer",
                    kategori = "Transfer antar saluran",
                    saluran = labelSaluran(t.saluran) +
                        (t.saluranTujuan?.let { " ke " + labelSaluran(it) } ?: ""),
                    keterangan = t.catatan,
                    masuk = null,
                    keluar = null
                )
                JENIS_KELUAR -> BarisExcel(
                    tanggal = formatTanggal(t.epoch),
                    jenis = "Keluar",
                    kategori = t.kategori,
                    saluran = labelSaluran(t.saluran),
                    keterangan = t.catatan,
                    masuk = null,
                    keluar = t.nominal
                )
                else -> BarisExcel(
                    tanggal = formatTanggal(t.epoch),
                    jenis = "Masuk",
                    kategori = t.kategori,
                    saluran = labelSaluran(t.saluran),
                    keterangan = t.catatan,
                    masuk = t.nominal,
                    keluar = null
                )
            }
        }

/**
 * Ekspor seluruh buku kas ke satu berkas .xlsx, satu lembar per bulan.
 * Tampilan T4: judul kolom hijau dengan teks putih, baris data selang-seling,
 * nominal berformat desimal (#,##0.00 - tampil 1.500.000,00 di Excel/WPS
 * berbahasa Indonesia, nilai sel tetap angka), baris TOTAL dan SALDO AKHIR.
 */
object ExcelExporter {

    private fun warna(r: Int, g: Int, b: Int): XSSFColor =
        XSSFColor(byteArrayOf(r.toByte(), g.toByte(), b.toByte()))

    fun buildWorkbook(bulanList: List<BulanKas>, transaksi: List<TransaksiKas>): ByteArray {
        val wb = XSSFWorkbook()

        val fontBold = wb.createFont()
        fontBold.bold = true
        val fontHeader = wb.createFont()
        fontHeader.bold = true
        fontHeader.color = IndexedColors.WHITE.index

        val gayaJudul = wb.createCellStyle() as XSSFCellStyle
        gayaJudul.setFont(fontHeader)
        gayaJudul.fillForegroundColor = warna(0x0E, 0x5C, 0x3A)
        gayaJudul.fillPattern = FillPatternType.SOLID_FOREGROUND

        val gayaData = wb.createCellStyle()
        val gayaZebra = wb.createCellStyle() as XSSFCellStyle
        gayaZebra.fillForegroundColor = warna(0xF2, 0xF7, 0xF3)
        gayaZebra.fillPattern = FillPatternType.SOLID_FOREGROUND

        val formatUang = wb.createDataFormat().getFormat("#,##0.00")
        val gayaUang = wb.createCellStyle()
        gayaUang.dataFormat = formatUang
        val gayaUangZebra = wb.createCellStyle() as XSSFCellStyle
        gayaUangZebra.dataFormat = formatUang
        gayaUangZebra.fillForegroundColor = warna(0xF2, 0xF7, 0xF3)
        gayaUangZebra.fillPattern = FillPatternType.SOLID_FOREGROUND

        val gayaTotal = wb.createCellStyle() as XSSFCellStyle
        gayaTotal.setFont(fontBold)
        gayaTotal.fillForegroundColor = warna(0xE1, 0xEF, 0xE6)
        gayaTotal.fillPattern = FillPatternType.SOLID_FOREGROUND
        val gayaTotalUang = wb.createCellStyle() as XSSFCellStyle
        gayaTotalUang.setFont(fontBold)
        gayaTotalUang.dataFormat = formatUang
        gayaTotalUang.fillForegroundColor = warna(0xE1, 0xEF, 0xE6)
        gayaTotalUang.fillPattern = FillPatternType.SOLID_FOREGROUND

        val urut = bulanList.sortedWith(compareBy({ it.tahun }, { it.bulan }))
        if (urut.isEmpty()) {
            wb.createSheet("KasBulanan")
        }
        for (bulan in urut) {
            val sheet = wb.createSheet(bulanLabel(bulan.tahun, bulan.bulan))
            sheet.createFreezePane(0, 1)
            val header = sheet.createRow(0)
            listOf("Tanggal", "Jenis", "Kategori", "Saluran", "Keterangan", "Masuk (Rp)", "Keluar (Rp)")
                .forEachIndexed { i, judul ->
                    val cell = header.createCell(i)
                    cell.setCellValue(judul)
                    cell.cellStyle = gayaJudul
                }
            var r = 1
            var totalMasuk = 0L
            var totalKeluar = 0L
            for ((i, baris) in barisExcel(bulan, transaksi).withIndex()) {
                val zebra = i % 2 == 1
                val gayaTeks = if (zebra) gayaZebra else gayaData
                val gayaNominal = if (zebra) gayaUangZebra else gayaUang
                val row = sheet.createRow(r++)
                listOf(baris.tanggal, baris.jenis, baris.kategori, baris.saluran, baris.keterangan)
                    .forEachIndexed { c, nilai ->
                        val cell = row.createCell(c)
                        cell.setCellValue(nilai)
                        cell.cellStyle = gayaTeks
                    }
                val cellMasuk = row.createCell(5)
                cellMasuk.cellStyle = gayaNominal
                baris.masuk?.let {
                    cellMasuk.setCellValue(it.toDouble())
                    totalMasuk += it
                }
                val cellKeluar = row.createCell(6)
                cellKeluar.cellStyle = gayaNominal
                baris.keluar?.let {
                    cellKeluar.setCellValue(it.toDouble())
                    totalKeluar += it
                }
            }
            val total = sheet.createRow(r++)
            for (c in 0..6) {
                val cell = total.createCell(c)
                cell.cellStyle = if (c >= 5) gayaTotalUang else gayaTotal
            }
            total.getCell(0).setCellValue("TOTAL")
            total.getCell(5).setCellValue(totalMasuk.toDouble())
            total.getCell(6).setCellValue(totalKeluar.toDouble())

            val saldoAkhir = sheet.createRow(r)
            for (c in 0..6) {
                val cell = saldoAkhir.createCell(c)
                cell.cellStyle = if (c >= 5) gayaTotalUang else gayaTotal
            }
            saldoAkhir.getCell(0).setCellValue("SALDO AKHIR")
            val saldo = saldoAkhirBulan(bulan, bulanList, transaksi)
            saldoAkhir.getCell(6).setCellValue(saldo.total.toDouble())

            intArrayOf(22, 10, 22, 20, 40, 16, 16).forEachIndexed { i, w ->
                sheet.setColumnWidth(i, w * 256)
            }
        }
        val out = ByteArrayOutputStream()
        wb.write(out)
        wb.close()
        return out.toByteArray()
    }
}
