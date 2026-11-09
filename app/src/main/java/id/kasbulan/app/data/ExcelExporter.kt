package id.kasbulan.app.data

import id.kasbulan.app.JENIS_KELUAR
import id.kasbulan.app.JENIS_TRANSFER
import id.kasbulan.app.SALURAN_QRIS
import id.kasbulan.app.bulanLabel
import id.kasbulan.app.formatTanggal
import java.io.ByteArrayOutputStream
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

/** Ekspor seluruh buku kas ke satu berkas .xlsx, satu lembar per bulan. */
object ExcelExporter {

    fun buildWorkbook(bulanList: List<BulanKas>, transaksi: List<TransaksiKas>): ByteArray {
        val wb = XSSFWorkbook()
        val bold = wb.createCellStyle()
        val boldFont = wb.createFont()
        boldFont.bold = true
        bold.setFont(boldFont)
        val urut = bulanList.sortedWith(compareBy({ it.tahun }, { it.bulan }))
        if (urut.isEmpty()) {
            wb.createSheet("KasBulanan")
        }
        for (bulan in urut) {
            val sheet = wb.createSheet(bulanLabel(bulan.tahun, bulan.bulan))
            val header = sheet.createRow(0)
            listOf("Tanggal", "Jenis", "Kategori", "Saluran", "Keterangan", "Masuk (Rp)", "Keluar (Rp)")
                .forEachIndexed { i, judul ->
                    val cell = header.createCell(i)
                    cell.setCellValue(judul)
                    cell.cellStyle = bold
                }
            var r = 1
            var totalMasuk = 0L
            var totalKeluar = 0L
            for (baris in barisExcel(bulan, transaksi)) {
                val row = sheet.createRow(r++)
                row.createCell(0).setCellValue(baris.tanggal)
                row.createCell(1).setCellValue(baris.jenis)
                row.createCell(2).setCellValue(baris.kategori)
                row.createCell(3).setCellValue(baris.saluran)
                row.createCell(4).setCellValue(baris.keterangan)
                baris.masuk?.let {
                    row.createCell(5).setCellValue(it.toDouble())
                    totalMasuk += it
                }
                baris.keluar?.let {
                    row.createCell(6).setCellValue(it.toDouble())
                    totalKeluar += it
                }
            }
            val total = sheet.createRow(r)
            val labelCell = total.createCell(0)
            labelCell.setCellValue("TOTAL")
            labelCell.cellStyle = bold
            val masukCell = total.createCell(5)
            masukCell.setCellValue(totalMasuk.toDouble())
            masukCell.cellStyle = bold
            val keluarCell = total.createCell(6)
            keluarCell.setCellValue(totalKeluar.toDouble())
            keluarCell.cellStyle = bold
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
