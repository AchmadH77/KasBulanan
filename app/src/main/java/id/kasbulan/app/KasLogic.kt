package id.kasbulan.app

import id.kasbulan.app.data.BulanKas
import id.kasbulan.app.data.TransaksiKas
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Konstanta domain. Nilai disimpan sebagai teks di database supaya file
// backup tetap terbaca antar versi.
const val JENIS_MASUK = "MASUK"
const val JENIS_KELUAR = "KELUAR"
const val JENIS_TRANSFER = "TRANSFER"

const val SALURAN_TUNAI = "TUNAI"
const val SALURAN_QRIS = "QRIS"

const val STATUS_BERJALAN = "BERJALAN"
const val STATUS_DITUTUP = "DITUTUP"

val KATEGORI_MASUK = listOf("Penjualan", "Tambahan Modal", "Lainnya")
val KATEGORI_KELUAR = listOf(
    "Pembelian Obat/PBF", "Operasional", "Gaji Pegawai",
    "Setoran Bank", "Pengambilan Pemilik", "Lainnya"
)
const val KATEGORI_TRANSFER = "Transfer"

val NAMA_BULAN = listOf(
    "Januari", "Februari", "Maret", "April", "Mei", "Juni",
    "Juli", "Agustus", "September", "Oktober", "November", "Desember"
)

fun bulanLabel(tahun: Int, bulan: Int): String =
    "${NAMA_BULAN[(bulan - 1).coerceIn(0, 11)]} $tahun"

/** Saldo dua dompet: uang fisik di laci dan uang QRIS di rekening. */
data class SaldoKas(val tunai: Long, val qris: Long) {
    val total: Long get() = tunai + qris
}

/**
 * Saldo selalu HASIL HITUNG: saldo awal bulan ditambah semua catatan
 * bulan itu. Transfer menggeser uang antar dompet, total tidak berubah.
 */
fun hitungSaldo(bulan: BulanKas, transaksi: List<TransaksiKas>): SaldoKas {
    var saldo = SaldoKas(bulan.saldoAwalTunai, bulan.saldoAwalQris)
    for (t in transaksi) {
        if (t.bulanId != bulan.id) continue
        saldo = terapkanSaldo(saldo, t)
    }
    return saldo
}

/** Saldo pada AKHIR satu tanggal: hanya catatan bulan itu sampai tanggal tersebut yang dihitung. */
fun saldoAkhirHari(bulan: BulanKas, transaksi: List<TransaksiKas>, tanggal: LocalDate): SaldoKas {
    var saldo = SaldoKas(bulan.saldoAwalTunai, bulan.saldoAwalQris)
    for (t in transaksi) {
        if (t.bulanId != bulan.id) continue
        if (epochToLocalDate(t.epoch).isAfter(tanggal)) continue
        saldo = terapkanSaldo(saldo, t)
    }
    return saldo
}

/** Terapkan satu transaksi pada sepasang saldo dompet. */
private fun terapkanSaldo(saldo: SaldoKas, t: TransaksiKas): SaldoKas = when (t.jenis) {
    JENIS_MASUK ->
        if (t.saluran == SALURAN_QRIS) saldo.copy(qris = saldo.qris + t.nominal)
        else saldo.copy(tunai = saldo.tunai + t.nominal)
    JENIS_KELUAR ->
        if (t.saluran == SALURAN_QRIS) saldo.copy(qris = saldo.qris - t.nominal)
        else saldo.copy(tunai = saldo.tunai - t.nominal)
    JENIS_TRANSFER -> {
        var tunai = saldo.tunai
        var qris = saldo.qris
        if (t.saluran == SALURAN_QRIS) qris -= t.nominal else tunai -= t.nominal
        if (t.saluranTujuan == SALURAN_QRIS) qris += t.nominal else tunai += t.nominal
        SaldoKas(tunai, qris)
    }
    else -> saldo
}

/** Ringkasan satu bulan. Transfer TIDAK dihitung sebagai masuk/keluar. */
data class RingkasanBulan(
    val totalMasuk: Long,
    val totalKeluar: Long,
    val perKategoriMasuk: Map<String, Long>,
    val perKategoriKeluar: Map<String, Long>
)

fun ringkasanBulan(transaksi: List<TransaksiKas>, bulanId: Long): RingkasanBulan {
    var masuk = 0L
    var keluar = 0L
    val perMasuk = LinkedHashMap<String, Long>()
    val perKeluar = LinkedHashMap<String, Long>()
    for (t in transaksi) {
        if (t.bulanId != bulanId) continue
        when (t.jenis) {
            JENIS_MASUK -> {
                masuk += t.nominal
                perMasuk[t.kategori] = (perMasuk[t.kategori] ?: 0L) + t.nominal
            }
            JENIS_KELUAR -> {
                keluar += t.nominal
                perKeluar[t.kategori] = (perKeluar[t.kategori] ?: 0L) + t.nominal
            }
        }
    }
    return RingkasanBulan(masuk, keluar, perMasuk, perKeluar)
}

/** Teks laporan satu bulan untuk dibagikan (WhatsApp dan lain-lain). */
fun laporanText(bulan: BulanKas, namaApotek: String, transaksi: List<TransaksiKas>): String {
    val saldo = hitungSaldo(bulan, transaksi)
    val ringkasan = ringkasanBulan(transaksi, bulan.id)
    val sb = StringBuilder()
    sb.append("Laporan Kas Bulanan - ").append(bulanLabel(bulan.tahun, bulan.bulan)).append('\n')
    if (namaApotek.isNotBlank()) sb.append(namaApotek).append('\n')
    sb.append("Status: ").append(if (bulan.status == STATUS_DITUTUP) "Ditutup" else "Berjalan").append("\n\n")
    sb.append("Saldo Awal: ").append(formatRupiah(bulan.saldoAwalTunai + bulan.saldoAwalQris))
        .append(" (Tunai ").append(formatRupiah(bulan.saldoAwalTunai))
        .append(" - QRIS ").append(formatRupiah(bulan.saldoAwalQris)).append(")\n")
    sb.append("Total Masuk: ").append(formatRupiah(ringkasan.totalMasuk)).append('\n')
    sb.append("Total Keluar: ").append(formatRupiah(ringkasan.totalKeluar)).append('\n')
    sb.append("Saldo Akhir: ").append(formatRupiah(saldo.total))
        .append(" (Tunai ").append(formatRupiah(saldo.tunai))
        .append(" - QRIS ").append(formatRupiah(saldo.qris)).append(")\n")
    if (ringkasan.perKategoriMasuk.isNotEmpty()) {
        sb.append("\nMasuk per kategori:\n")
        for ((kategori, nominal) in ringkasan.perKategoriMasuk) {
            sb.append("- ").append(kategori).append(": ").append(formatRupiah(nominal)).append('\n')
        }
    }
    if (ringkasan.perKategoriKeluar.isNotEmpty()) {
        sb.append("\nKeluar per kategori:\n")
        for ((kategori, nominal) in ringkasan.perKategoriKeluar) {
            sb.append("- ").append(kategori).append(": ").append(formatRupiah(nominal)).append('\n')
        }
    }
    sb.append("\nDibuat dengan KasBulanan")
    return sb.toString()
}

// ---------- Harian ----------

const val KATEGORI_PENJUALAN = "Penjualan"

/** Satu baris rincian harian: total satu kategori pada satu saluran. */
data class RincianHari(val kategori: String, val saluran: String, val nominal: Long)

/** Satu perpindahan antar dompet pada satu hari. */
data class TransferHari(val dari: String, val ke: String, val nominal: Long)

/**
 * Ringkasan SATU hari kalender dari seluruh catatan (satu hari tidak
 * terikat satu bulan). Transfer dicatat terpisah - bukan masuk/keluar.
 */
data class RingkasanHari(
    val totalMasuk: Long,
    val totalKeluar: Long,
    val rincianMasuk: List<RincianHari>,
    val rincianKeluar: List<RincianHari>,
    val transfer: List<TransferHari>,
    val jumlahTransaksi: Int
)

fun ringkasanHari(tanggal: LocalDate, transaksi: List<TransaksiKas>): RingkasanHari {
    var masuk = 0L
    var keluar = 0L
    val petaMasuk = LinkedHashMap<Pair<String, String>, Long>()
    val petaKeluar = LinkedHashMap<Pair<String, String>, Long>()
    val daftarTransfer = ArrayList<TransferHari>()
    var jumlah = 0
    for (t in transaksi) {
        if (epochToLocalDate(t.epoch) != tanggal) continue
        jumlah++
        when (t.jenis) {
            JENIS_MASUK -> {
                masuk += t.nominal
                val kunci = t.kategori to t.saluran
                petaMasuk[kunci] = (petaMasuk[kunci] ?: 0L) + t.nominal
            }
            JENIS_KELUAR -> {
                keluar += t.nominal
                val kunci = t.kategori to t.saluran
                petaKeluar[kunci] = (petaKeluar[kunci] ?: 0L) + t.nominal
            }
            JENIS_TRANSFER -> daftarTransfer.add(
                TransferHari(t.saluran, t.saluranTujuan ?: SALURAN_TUNAI, t.nominal)
            )
        }
    }
    return RingkasanHari(
        totalMasuk = masuk,
        totalKeluar = keluar,
        rincianMasuk = petaMasuk.map { (kunci, nominal) -> RincianHari(kunci.first, kunci.second, nominal) },
        rincianKeluar = petaKeluar.map { (kunci, nominal) -> RincianHari(kunci.first, kunci.second, nominal) },
        transfer = daftarTransfer,
        jumlahTransaksi = jumlah
    )
}

/** Pendapatan (kategori Penjualan saja) pada satu tanggal. */
data class PendapatanHari(val tanggal: LocalDate, val total: Long)

/**
 * Pendapatan per hari dalam satu bulan, urut tanggal. Hanya hari yang
 * punya penjualan yang muncul; jumlah semua baris sama dengan total
 * Penjualan bulan itu. Tambahan Modal BUKAN pendapatan, tidak dihitung.
 */
fun pendapatanPerHari(transaksi: List<TransaksiKas>, bulanId: Long): List<PendapatanHari> {
    val perTanggal = sortedMapOf<LocalDate, Long>()
    for (t in transaksi) {
        if (t.bulanId != bulanId) continue
        if (t.jenis != JENIS_MASUK || t.kategori != KATEGORI_PENJUALAN) continue
        val tanggal = epochToLocalDate(t.epoch)
        perTanggal[tanggal] = (perTanggal[tanggal] ?: 0L) + t.nominal
    }
    return perTanggal.map { (tanggal, total) -> PendapatanHari(tanggal, total) }
}

private fun labelSaluran(saluran: String): String = if (saluran == SALURAN_QRIS) "QRIS" else "Tunai"

/** Teks rekap satu hari untuk dibagikan (WhatsApp dan lain-lain). */
fun rekapHariText(namaApotek: String, tanggal: LocalDate, ringkasan: RingkasanHari, saldoAkhir: SaldoKas): String {
    val sb = StringBuilder()
    sb.append("Rekap Kas Harian - ").append(formatTanggal(tanggal)).append('\n')
    if (namaApotek.isNotBlank()) sb.append(namaApotek).append('\n')
    sb.append('\n')
    sb.append("Pemasukan: +").append(formatRupiah(ringkasan.totalMasuk)).append('\n')
    for (r in ringkasan.rincianMasuk) {
        sb.append("- ").append(r.kategori).append(" (").append(labelSaluran(r.saluran))
            .append("): ").append(formatRupiah(r.nominal)).append('\n')
    }
    sb.append("Pengeluaran: ").append(formatRupiah(-ringkasan.totalKeluar)).append('\n')
    for (r in ringkasan.rincianKeluar) {
        sb.append("- ").append(r.kategori).append(" (").append(labelSaluran(r.saluran))
            .append("): ").append(formatRupiah(r.nominal)).append('\n')
    }
    for (tr in ringkasan.transfer) {
        sb.append("Transfer ").append(labelSaluran(tr.dari)).append(" \u2192 ").append(labelSaluran(tr.ke))
            .append(": ").append(formatRupiah(tr.nominal)).append('\n')
    }
    sb.append('\n')
    sb.append("Saldo Akhir: ").append(formatRupiah(saldoAkhir.total))
        .append(" (Tunai ").append(formatRupiah(saldoAkhir.tunai))
        .append(" - QRIS ").append(formatRupiah(saldoAkhir.qris)).append(")\n")
    sb.append("\nDibuat dengan KasBulanan")
    return sb.toString()
}

// ---------- Format & tanggal ----------

/** Mengelompokkan digit dengan titik ribuan: "1250000" menjadi "1.250.000". */
fun groupThousands(raw: String): String {
    val digits = raw.filter { it.isDigit() }.trimStart('0')
    val bersih = if (digits.isEmpty()) (if (raw.any { it.isDigit() }) "0" else "") else digits
    if (bersih.isEmpty()) return ""
    val sb = StringBuilder()
    var hitung = 0
    for (i in bersih.length - 1 downTo 0) {
        sb.append(bersih[i])
        hitung++
        if (hitung == 3 && i > 0) {
            sb.append('.')
            hitung = 0
        }
    }
    return sb.reverse().toString()
}

fun formatRupiah(n: Long): String {
    val negatif = n < 0
    val grup = groupThousands(kotlin.math.abs(n).toString())
    return (if (negatif) "-Rp " else "Rp ") + grup
}

fun epochToLocalDate(epoch: Long): LocalDate =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()

private val FORMAT_TANGGAL: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale("id", "ID"))
private val FORMAT_JAM: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH.mm", Locale("id", "ID"))

fun formatTanggal(epoch: Long): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(FORMAT_TANGGAL)

fun formatTanggal(date: LocalDate): String = date.format(FORMAT_TANGGAL)

fun formatJam(epoch: Long): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(FORMAT_JAM)

private val FORMAT_TANGGAL_SINGKAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM", Locale("id", "ID"))

/** Bentuk pendek tanggal: "Sen, 9 Nov". */
fun formatTanggalSingkat(date: LocalDate): String = date.format(FORMAT_TANGGAL_SINGKAT)

/** Label kepala kelompok riwayat: "Hari ini - ...", "Kemarin - ...", atau tanggal. */
fun labelHari(date: LocalDate, hariIni: LocalDate): String = when (date) {
    hariIni -> "Hari ini - " + formatTanggal(date)
    hariIni.minusDays(1) -> "Kemarin - " + formatTanggal(date)
    else -> formatTanggal(date)
}
