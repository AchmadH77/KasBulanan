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

val NAMA_BULAN_EN = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

/** Terjemahan tampilan kategori bawaan; nilai tersimpan tidak berubah. */
val KATEGORI_EN = mapOf(
    "Penjualan" to "Sales",
    "Tambahan Modal" to "Capital Injection",
    "Pembelian Obat/PBF" to "Medicine/Supplier Purchases",
    "Operasional" to "Operations",
    "Gaji Pegawai" to "Staff Salaries",
    "Setoran Bank" to "Bank Deposit",
    "Pengambilan Pemilik" to "Owner Withdrawal",
    "Lainnya" to "Other"
)

fun bulanLabel(tahun: Int, bulan: Int, bahasa: BahasaApp = BahasaApp.ID): String {
    val daftar = if (bahasa == BahasaApp.EN) NAMA_BULAN_EN else NAMA_BULAN
    return "${daftar[(bulan - 1).coerceIn(0, 11)]} $tahun"
}

/** Saldo dua dompet: uang fisik di laci dan uang QRIS di rekening. */
data class SaldoKas(val tunai: Long, val qris: Long) {
    val total: Long get() = tunai + qris
}

/**
 * Saldo selalu HASIL HITUNG dari SELURUH catatan semua bulan - tidak ada
 * saldo awal yang diisi. Transfer menggeser uang antar dompet, total
 * tidak berubah.
 */
fun hitungSaldo(transaksi: List<TransaksiKas>): SaldoKas {
    var saldo = SaldoKas(0, 0)
    for (t in transaksi) {
        saldo = terapkanSaldo(saldo, t)
    }
    return saldo
}

/** Saldo pada akhir bulan-bulan SEBELUM bulan ini (= "saldo awal" bulan ini, murni hasil hitung). */
fun saldoAwalBulan(bulan: BulanKas, bulanList: List<BulanKas>, transaksi: List<TransaksiKas>): SaldoKas {
    val idSebelumnya = bulanList
        .filter { it.tahun < bulan.tahun || (it.tahun == bulan.tahun && it.bulan < bulan.bulan) }
        .map { it.id }
        .toSet()
    var saldo = SaldoKas(0, 0)
    for (t in transaksi) {
        if (t.bulanId !in idSebelumnya) continue
        saldo = terapkanSaldo(saldo, t)
    }
    return saldo
}

/** Saldo pada akhir bulan ini: saldo awal bulan ditambah catatan bulan ini sendiri. */
fun saldoAkhirBulan(bulan: BulanKas, bulanList: List<BulanKas>, transaksi: List<TransaksiKas>): SaldoKas {
    var saldo = saldoAwalBulan(bulan, bulanList, transaksi)
    for (t in transaksi) {
        if (t.bulanId != bulan.id) continue
        saldo = terapkanSaldo(saldo, t)
    }
    return saldo
}

/** Saldo pada AKHIR satu tanggal: seluruh catatan sampai tanggal tersebut, lintas bulan. */
fun saldoAkhirHari(transaksi: List<TransaksiKas>, tanggal: LocalDate): SaldoKas {
    var saldo = SaldoKas(0, 0)
    for (t in transaksi) {
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
fun laporanText(bulan: BulanKas, namaApotek: String, bulanList: List<BulanKas>, transaksi: List<TransaksiKas>): String {
    val saldoAwal = saldoAwalBulan(bulan, bulanList, transaksi)
    val saldo = saldoAkhirBulan(bulan, bulanList, transaksi)
    val ringkasan = ringkasanBulan(transaksi, bulan.id)
    val sb = StringBuilder()
    sb.append("Laporan Kas Bulanan - ").append(bulanLabel(bulan.tahun, bulan.bulan)).append('\n')
    if (namaApotek.isNotBlank()) sb.append(namaApotek).append('\n')
    sb.append("Status: ").append(if (bulan.status == STATUS_DITUTUP) "Ditutup" else "Berjalan").append("\n\n")
    sb.append("Saldo Awal: ").append(formatRupiah(saldoAwal.total))
        .append(" (Tunai ").append(formatRupiah(saldoAwal.tunai))
        .append(" - QRIS ").append(formatRupiah(saldoAwal.qris)).append(")\n")
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

private fun labelSaluran(saluran: String, bahasa: BahasaApp = BahasaApp.ID): String =
    if (saluran == SALURAN_QRIS) "QRIS" else if (bahasa == BahasaApp.EN) "Cash" else "Tunai"

/** Teks rekap satu hari untuk dibagikan (WhatsApp dan lain-lain). */
fun rekapHariText(namaApotek: String, tanggal: LocalDate, ringkasan: RingkasanHari, saldoAkhir: SaldoKas, bahasa: BahasaApp = BahasaApp.ID): String {
    val en = bahasa == BahasaApp.EN
    fun kat(k: String): String = if (en) (KATEGORI_EN[k] ?: k) else k
    val sb = StringBuilder()
    sb.append(if (en) "Daily Cash Recap - " else "Rekap Kas Harian - ").append(formatTanggal(tanggal, bahasa)).append('\n')
    if (namaApotek.isNotBlank()) sb.append(namaApotek).append('\n')
    sb.append('\n')
    sb.append(if (en) "Income: +" else "Pemasukan: +").append(formatRupiah(ringkasan.totalMasuk)).append('\n')
    for (r in ringkasan.rincianMasuk) {
        sb.append("- ").append(kat(r.kategori)).append(" (").append(labelSaluran(r.saluran, bahasa))
            .append("): ").append(formatRupiah(r.nominal)).append('\n')
    }
    sb.append(if (en) "Expenses: " else "Pengeluaran: ").append(formatRupiah(-ringkasan.totalKeluar)).append('\n')
    for (r in ringkasan.rincianKeluar) {
        sb.append("- ").append(kat(r.kategori)).append(" (").append(labelSaluran(r.saluran, bahasa))
            .append("): ").append(formatRupiah(r.nominal)).append('\n')
    }
    for (t in ringkasan.transfer) {
        sb.append("Transfer ").append(labelSaluran(t.dari, bahasa)).append(" \u2192 ").append(labelSaluran(t.ke, bahasa))
            .append(": ").append(formatRupiah(t.nominal)).append('\n')
    }
    sb.append('\n')
    sb.append(if (en) "Closing Balance: " else "Saldo Akhir: ").append(formatRupiah(saldoAkhir.total))
        .append(if (en) " (Cash " else " (Tunai ").append(formatRupiah(saldoAkhir.tunai))
        .append(" - QRIS ").append(formatRupiah(saldoAkhir.qris)).append(")\n")
    sb.append(if (en) "\nMade with KasBulanan" else "\nDibuat dengan KasBulanan")
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
private val FORMAT_TANGGAL_EN: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale.ENGLISH)
private val FORMAT_JAM: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH.mm", Locale("id", "ID"))

private fun formatTanggalUntuk(bahasa: BahasaApp): DateTimeFormatter =
    if (bahasa == BahasaApp.EN) FORMAT_TANGGAL_EN else FORMAT_TANGGAL

fun formatTanggal(epoch: Long, bahasa: BahasaApp = BahasaApp.ID): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(formatTanggalUntuk(bahasa))

fun formatTanggal(date: LocalDate, bahasa: BahasaApp = BahasaApp.ID): String =
    date.format(formatTanggalUntuk(bahasa))

fun formatJam(epoch: Long): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).format(FORMAT_JAM)

private val FORMAT_TANGGAL_WAKTU: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", Locale("id", "ID"))
private val FORMAT_TANGGAL_WAKTU_EN: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH.mm", Locale.ENGLISH)

/** Bentuk tanggal + jam: "9 Nov 2026, 21.10". */
fun formatTanggalWaktu(epoch: Long, bahasa: BahasaApp = BahasaApp.ID): String =
    Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault())
        .format(if (bahasa == BahasaApp.EN) FORMAT_TANGGAL_WAKTU_EN else FORMAT_TANGGAL_WAKTU)

private val FORMAT_TANGGAL_SINGKAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM", Locale("id", "ID"))
private val FORMAT_TANGGAL_SINGKAT_EN: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)

/** Bentuk pendek tanggal: "Sen, 9 Nov". */
fun formatTanggalSingkat(date: LocalDate, bahasa: BahasaApp = BahasaApp.ID): String =
    date.format(if (bahasa == BahasaApp.EN) FORMAT_TANGGAL_SINGKAT_EN else FORMAT_TANGGAL_SINGKAT)

/** Label kepala kelompok riwayat: "Hari ini - ...", "Kemarin - ...", atau tanggal. */
fun labelHari(date: LocalDate, hariIni: LocalDate, bahasa: BahasaApp = BahasaApp.ID): String = when (date) {
    hariIni -> (if (bahasa == BahasaApp.EN) "Today - " else "Hari ini - ") + formatTanggal(date, bahasa)
    hariIni.minusDays(1) -> (if (bahasa == BahasaApp.EN) "Yesterday - " else "Kemarin - ") + formatTanggal(date, bahasa)
    else -> formatTanggal(date, bahasa)
}
