package id.kasbulan.app

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import id.kasbulan.app.data.BulanKas
import id.kasbulan.app.data.TransaksiKas
import java.time.LocalDate
import java.time.YearMonth

fun saluranLabel(saluran: String): String = if (saluran == SALURAN_QRIS) "QRIS" else "Tunai"

/** Label saluran untuk tampilan (mengikuti bahasa aktif). */
@Composable
fun saluranTampil(saluran: String): String =
    if (saluran == SALURAN_QRIS) "QRIS" else tr("Tunai", "Cash")

@Composable
fun judulTransaksi(t: TransaksiKas): String =
    if (t.jenis == JENIS_TRANSFER) {
        "Transfer " + saluranTampil(t.saluran) + " \u2192 " + saluranTampil(t.saluranTujuan ?: SALURAN_TUNAI)
    } else {
        tampilKategori(t.kategori)
    }

@Composable
fun subTransaksi(t: TransaksiKas): String {
    val sb = StringBuilder()
    sb.append(formatJam(t.epoch))
    if (t.jenis != JENIS_TRANSFER) sb.append(" \u00b7 ").append(saluranTampil(t.saluran))
    if (t.catatan.isNotBlank()) sb.append(" \u00b7 ").append(t.catatan)
    return sb.toString()
}

/** Kolom isian nominal: tampilan berkelompok ribuan, nilai mentah digit saja. */
@Composable
fun MoneyField(label: String, rawDigits: String, onRawChange: (String) -> Unit) {
    OutlinedTextField(
        value = groupThousands(rawDigits),
        onValueChange = { onRawChange(it.filter { c -> c.isDigit() }.take(15)) },
        label = { Text(label) },
        prefix = { Text("Rp ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun BulanSelector(bulanList: List<BulanKas>, selected: BulanKas, onSelect: (BulanKas) -> Unit) {
    val index = bulanList.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (index > 0) onSelect(bulanList[index - 1]) },
            enabled = index > 0
        ) { Icon(Icons.Default.ChevronLeft, contentDescription = tr("Bulan sebelumnya", "Previous month")) }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                bulanLabel(selected.tahun, selected.bulan, LocalBahasa.current),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (selected.status == STATUS_DITUTUP) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        tr("Ditutup", "Closed"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
        IconButton(
            onClick = { if (index < bulanList.size - 1) onSelect(bulanList[index + 1]) },
            enabled = index < bulanList.size - 1
        ) { Icon(Icons.Default.ChevronRight, contentDescription = tr("Bulan berikutnya", "Next month")) }
    }
}

/**
 * Hijau khusus untuk nominal uang masuk (penjualan, total masuk,
 * pendapatan): hijau terang di mode gelap, hijau tegas di mode terang.
 * Sengaja bukan warna utama tema agar pemasukan selalu terbaca hijau.
 */
@Composable
fun warnaHijauMasuk(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
        Color(0xFF4ADE80)
    } else {
        Color(0xFF1E9E4A)
    }

@Composable
fun TransaksiRow(t: TransaksiKas, clickable: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val masuk = t.jenis == JENIS_MASUK
            val transfer = t.jenis == JENIS_TRANSFER
            val warna = when {
                transfer -> MaterialTheme.colorScheme.onSurfaceVariant
                masuk -> warnaHijauMasuk()
                else -> MaterialTheme.colorScheme.error
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Box(modifier = Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            when {
                                transfer -> Icons.Default.SwapHoriz
                                masuk -> Icons.Default.ArrowDownward
                                else -> Icons.Default.ArrowUpward
                            },
                            contentDescription = null,
                            tint = warna,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    judulTransaksi(t),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    subTransaksi(t),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                when {
                    transfer -> formatRupiah(t.nominal)
                    masuk -> "+" + formatRupiah(t.nominal)
                    else -> formatRupiah(-t.nominal)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = warna
            )
        }
    }
}

@Composable
fun PitaHeader(namaApotek: String, bulan: BulanKas?) {
    val bulanTeks = bulan?.let { bulanLabel(it.tahun, it.bulan, LocalBahasa.current) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                Box(modifier = Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "K",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "KasBulanan",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (namaApotek.isNotBlank()) namaApotek else tr("Nama usaha belum diatur", "Business name not set"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (bulanTeks != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f)
                ) {
                    Text(
                        bulanTeks,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

// ---------------- Beranda ----------------

/**
 * Teks rupiah yang angkanya berjalan (menghitung naik/turun) setiap kali
 * nilainya berubah, misalnya setelah transaksi disimpan. Interpolasi
 * memakai Long sehingga angka akhir selalu persis, tidak meleset karena
 * pembulatan float.
 */
@Composable
fun TeksRupiahAnimasi(
    nilai: Long,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight? = null,
    awalan: String = ""
) {
    var tampil by remember { mutableLongStateOf(nilai) }
    val gerak = remember { Animatable(0f) }
    LaunchedEffect(nilai) {
        val dari = tampil
        if (dari != nilai) {
            gerak.snapTo(0f)
            gerak.animateTo(1f, tween(550, easing = FastOutSlowInEasing)) {
                tampil = dari + ((nilai - dari).toDouble() * value).toLong()
            }
            tampil = nilai
        }
    }
    Text(
        awalan + formatRupiah(tampil),
        style = style,
        color = color,
        fontWeight = fontWeight
    )
}

/** Keadaan sinkronisasi online untuk ikon status di Beranda. */
enum class StatusSync { NONAKTIF, BELUM_MASUK, PROSES, BERHASIL, GAGAL }

@Composable
fun BerandaTab(
    bulan: BulanKas?,
    bulanTerakhir: BulanKas?,
    labelBulanBerikutnya: String?,
    transaksi: List<TransaksiKas>,
    namaApotek: String,
    statusSync: StatusSync,
    onBukaOnline: () -> Unit,
    onMulaiBulanIni: () -> Unit,
    onBukaBulanBerikutnya: () -> Unit,
    onTransfer: () -> Unit,
    onRekapHariIni: () -> Unit,
    onTransaksiClick: (TransaksiKas) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PitaHeader(namaApotek, bulan ?: bulanTerakhir)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item { Spacer(Modifier.height(4.dp)) }
        if (bulan == null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (bulanTerakhir == null) {
                            Text(tr("Mulai buku kas pertamamu", "Start your first cash book"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                tr("Catat pemasukan dan pengeluaranmu mulai hari ini. Saldo dihitung otomatis dari catatanmu - tidak ada saldo awal yang perlu diisi.", "Record your income and expenses starting today. The balance is computed automatically from your records - there is no opening balance to fill in."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onMulaiBulanIni, modifier = Modifier.fillMaxWidth()) {
                                Text(tr("Mulai Bulan Ini", "Start This Month"))
                            }
                        } else {
                            Text(tr("Semua bulan sudah ditutup", "All months are closed"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                tr("Buka bulan berikutnya untuk melanjutkan pencatatan. Saldo berjalan terus dari seluruh catatanmu.", "Open the next month to keep recording. The balance keeps running from all your records."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onBukaBulanBerikutnya, modifier = Modifier.fillMaxWidth()) {
                                Text(tr("Buka ", "Open ") + (labelBulanBerikutnya ?: tr("Bulan Berikutnya", "Next Month")))
                            }
                        }
                    }
                }
            }
        } else {
            val saldo = hitungSaldo(transaksi)
            val ringkasan = ringkasanBulan(transaksi, bulan.id)
            val terakhir = transaksi
                .filter { it.bulanId == bulan.id }
                .sortedByDescending { it.epoch }
                .take(5)
            val kalenderBerbeda = YearMonth.now() != YearMonth.of(bulan.tahun, bulan.bulan)
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        bulanLabel(bulan.tahun, bulan.bulan, LocalBahasa.current),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            tr("Berjalan", "Open"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tr("Saldo Kas", "Cash Balance"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            if (statusSync != StatusSync.NONAKTIF) {
                                val ikonStatus = when (statusSync) {
                                    StatusSync.BERHASIL -> Icons.Default.CloudDone
                                    StatusSync.PROSES -> Icons.Default.CloudSync
                                    else -> Icons.Default.CloudOff
                                }
                                val warnaStatus = when (statusSync) {
                                    StatusSync.GAGAL -> MaterialTheme.colorScheme.error
                                    StatusSync.BERHASIL -> Color.White
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                                val labelStatus = when (statusSync) {
                                    StatusSync.BERHASIL -> tr("Sudah tersinkron", "Synced")
                                    StatusSync.PROSES -> tr("Sedang menyinkronkan", "Syncing")
                                    StatusSync.GAGAL -> tr("Sinkron gagal", "Sync failed")
                                    else -> tr("Belum masuk", "Not signed in")
                                }
                                IconButton(
                                    onClick = onBukaOnline,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        ikonStatus,
                                        contentDescription = labelStatus,
                                        tint = warnaStatus,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                        }
                        TeksRupiahAnimasi(
                            saldo.total,
                            MaterialTheme.typography.headlineMedium,
                            Color.White,
                            FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f))
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tr("Tunai (laci)", "Cash (drawer)"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                TeksRupiahAnimasi(saldo.tunai, MaterialTheme.typography.titleSmall, Color.White, FontWeight.Bold)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(tr("QRIS (rekening)", "QRIS (account)"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                TeksRupiahAnimasi(saldo.qris, MaterialTheme.typography.titleSmall, Color.White, FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(tr("Masuk bulan ini", "Income this month"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TeksRupiahAnimasi(
                                ringkasan.totalMasuk,
                                MaterialTheme.typography.titleSmall,
                                warnaHijauMasuk(),
                                FontWeight.Bold,
                                awalan = "+"
                            )
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(tr("Keluar bulan ini", "Expenses this month"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TeksRupiahAnimasi(
                                -ringkasan.totalKeluar,
                                MaterialTheme.typography.titleSmall,
                                MaterialTheme.colorScheme.error,
                                FontWeight.Bold
                            )
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        tr("Saldo berjalan dari seluruh catatan", "Running balance from all records"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = onTransfer) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Transfer", "Transfer"))
                    }
                }
            }
            item {
                val hariIni = LocalDate.now()
                val rHariIni = ringkasanHari(hariIni, transaksi)
                val rKemarin = ringkasanHari(hariIni.minusDays(1), transaksi)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tr("KAS HARI INI", "TODAY'S CASH"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatTanggal(hariIni, LocalBahasa.current),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                tr("Masuk ", "In "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "+" + formatRupiah(rHariIni.totalMasuk),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = warnaHijauMasuk()
                            )
                            Text(
                                "  \u00b7  " + tr("Keluar ", "Out "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                formatRupiah(-rHariIni.totalKeluar),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Button(onClick = onRekapHariIni, modifier = Modifier.fillMaxWidth()) {
                            Text(tr("Rekap Hari Ini", "Today's Recap"))
                        }
                        Text(
                            if (rKemarin.jumlahTransaksi > 0) {
                                tr("Kemarin: masuk ", "Yesterday: in ") + formatRupiah(rKemarin.totalMasuk) +
                                    " \u00b7 " + tr("keluar ", "out ") + formatRupiah(rKemarin.totalKeluar)
                            } else {
                                tr("Kemarin: tidak ada catatan.", "Yesterday: no records.")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (kalenderBerbeda) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Text(
                            tr("Bulan kalender sudah berganti, tetapi ", "The calendar month has moved on, but ") + bulanLabel(bulan.tahun, bulan.bulan, LocalBahasa.current) +
                                tr(" masih berjalan. Tutup buku dari tab Laporan untuk membuka bulan baru.", " is still open. Close the book from the Report tab to open a new month."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            item {
                Text(tr("Transaksi Terakhir", "Latest Transactions"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (terakhir.isEmpty()) {
                item {
                    Text(
                        tr("Belum ada transaksi bulan ini.", "No transactions this month yet."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(terakhir, key = { it.id }) { t ->
                    TransaksiRow(t, clickable = true) { onTransaksiClick(t) }
                }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
    }
    }
}

// ---------------- Rekap Hari Ini ----------------

@Composable
fun RekapHariIniScreen(
    bulan: BulanKas,
    transaksi: List<TransaksiKas>,
    namaApotek: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hariIni = LocalDate.now()
    val ringkasan = ringkasanHari(hariIni, transaksi)
    val saldoAkhir = saldoAkhirHari(transaksi, hariIni)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = tr("Kembali", "Back"))
                }
                Column {
                    Text(tr("Rekap Hari Ini", "Today's Recap"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        formatTanggal(hariIni, LocalBahasa.current),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (ringkasan.jumlahTransaksi == 0) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Text(
                        tr("Belum ada catatan hari ini. Catat penjualan dan pengeluaran seperti biasa, rekapnya terisi otomatis.", "No records today yet. Record sales and expenses as usual, and the recap fills itself in."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(tr("Pemasukan", "Income"), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            Text(
                                "+" + formatRupiah(ringkasan.totalMasuk),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        for (r in ringkasan.rincianMasuk) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "   " + tampilKategori(r.kategori) + " \u00b7 " + saluranTampil(r.saluran),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.weight(1f))
                                Text(formatRupiah(r.nominal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(tr("Pengeluaran", "Expenses"), style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatRupiah(-ringkasan.totalKeluar),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        for (r in ringkasan.rincianKeluar) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    "   " + tampilKategori(r.kategori) + " \u00b7 " + saluranTampil(r.saluran),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.weight(1f))
                                Text(formatRupiah(r.nominal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (tr in ringkasan.transfer) {
                        Text(
                            "Transfer " + saluranTampil(tr.dari) + " \u2192 " + saluranTampil(tr.ke) +
                                ": " + formatRupiah(tr.nominal) + tr(" (netral)", " (neutral)"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (ringkasan.transfer.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f))
                    }
                    Text(
                        tr("SALDO AKHIR HARI INI", "TODAY'S CLOSING BALANCE"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(tr("Tunai", "Cash"), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatRupiah(saldoAkhir.tunai),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("QRIS", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.weight(1f))
                        Text(
                            formatRupiah(saldoAkhir.qris),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row {
                    Text(
                        tr("Total kas akhir: ", "Final cash total: "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatRupiah(saldoAkhir.total),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Text(
                    ringkasan.jumlahTransaksi.toString() + tr(" transaksi tercatat hari ini \u00b7 bulan ", " transactions recorded today \u00b7 month ") +
                        bulanLabel(bulan.tahun, bulan.bulan, LocalBahasa.current) + tr(" berjalan", " is open"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Button(
                onClick = {
                    val teks = rekapHariText(namaApotek, hariIni, ringkasan, saldoAkhir)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, teks)
                    }
                    context.startActivity(Intent.createChooser(intent, tr("Bagikan Rekap", "Share Recap")))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(tr("Bagikan Rekap", "Share Recap"))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        tr("Rekap ini rangkuman hari berjalan.", "This recap summarises the current day."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        tr("Angka resmi bulan tetap ditetapkan saat Tutup Buku.", "The month's official figures are set at book closing."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        tr("Detail per transaksi ada di tab Riwayat.", "Per-transaction details are in the History tab."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------- Riwayat ----------------

@Composable
fun RiwayatTab(
    bulanList: List<BulanKas>,
    transaksi: List<TransaksiKas>,
    namaApotek: String,
    onTransaksiClick: (TransaksiKas) -> Unit
) {
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = bulanList.firstOrNull { it.id == selectedId }
        ?: bulanList.firstOrNull { it.status == STATUS_BERJALAN }
        ?: bulanList.lastOrNull()
    Column(modifier = Modifier.fillMaxSize()) {
        PitaHeader(namaApotek, selected)
        if (selected == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tr("Belum ada data. Mulai bulan ini dari tab Beranda.", "No data yet. Start this month from the Home tab."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val milikBulan = transaksi
                .filter { it.bulanId == selected.id }
                .sortedByDescending { it.epoch }
            val perHari = milikBulan.groupBy { epochToLocalDate(it.epoch) }
            val hariIni = LocalDate.now()
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            BulanSelector(bulanList, selected) { selectedId = it.id }
        }
        if (milikBulan.isEmpty()) {
            item {
                Text(
                    tr("Belum ada transaksi pada bulan ini.", "No transactions in this month yet."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        for ((tanggal, daftar) in perHari) {
            item {
                val masukHari = daftar.filter { it.jenis == JENIS_MASUK }.sumOf { it.nominal }
                val keluarHari = daftar.filter { it.jenis == JENIS_KELUAR }.sumOf { it.nominal }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(labelHari(tanggal, hariIni, LocalBahasa.current), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        tr("Masuk ", "In ") + groupThousands(masukHari.toString()) + " - " + tr("Keluar ", "Out ") + groupThousands(keluarHari.toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(daftar, key = { it.id }) { t ->
                TransaksiRow(t, clickable = selected.status == STATUS_BERJALAN) { onTransaksiClick(t) }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }            }
        }
    }
}

// ---------------- Laporan ----------------

@Composable
fun LaporanTab(
    bulanList: List<BulanKas>,
    transaksi: List<TransaksiKas>,
    namaApotek: String,
    onTutupBuku: (BulanKas) -> Unit
) {
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = bulanList.firstOrNull { it.id == selectedId }
        ?: bulanList.firstOrNull { it.status == STATUS_BERJALAN }
        ?: bulanList.lastOrNull()
    Column(modifier = Modifier.fillMaxSize()) {
        PitaHeader(namaApotek, selected)
        if (selected == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tr("Belum ada data. Mulai bulan ini dari tab Beranda.", "No data yet. Start this month from the Home tab."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val saldoAwal = saldoAwalBulan(selected, bulanList, transaksi)
            val saldo = saldoAkhirBulan(selected, bulanList, transaksi)
            val ringkasan = ringkasanBulan(transaksi, selected.id)
            val pendapatanHarian = pendapatanPerHari(transaksi, selected.id)
            val ditutup = selected.status == STATUS_DITUTUP
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { BulanSelector(bulanList, selected) { selectedId = it.id } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (ditutup) tr("Saldo Akhir", "Closing Balance") else tr("Saldo Saat Ini", "Current Balance"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TeksRupiahAnimasi(
                        saldo.total,
                        MaterialTheme.typography.headlineMedium,
                        Color.White,
                        FontWeight.ExtraBold
                    )
                    Text(
                        tr("Saldo Awal: ", "Opening Balance: ") + formatRupiah(saldoAwal.total),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.25f))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TeksRupiahAnimasi(
                            saldo.tunai,
                            MaterialTheme.typography.bodySmall,
                            Color.White,
                            awalan = tr("Tunai - ", "Cash - ")
                        )
                        Spacer(Modifier.weight(1f))
                        TeksRupiahAnimasi(
                            saldo.qris,
                            MaterialTheme.typography.bodySmall,
                            Color.White,
                            awalan = "QRIS - "
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            tr("Total Masuk", "Total Income"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "+" + formatRupiah(ringkasan.totalMasuk),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = warnaHijauMasuk()
                        )
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            tr("Total Keluar", "Total Expenses"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatRupiah(-ringkasan.totalKeluar),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        if (ringkasan.perKategoriKeluar.isNotEmpty()) {
            item {
                Text(tr("Rincian Keluar per Kategori", "Expense Breakdown by Category"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            item {
                val maks = ringkasan.perKategoriKeluar.values.maxOrNull() ?: 1L
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for ((kategori, nominal) in ringkasan.perKategoriKeluar) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(tampilKategori(kategori), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.weight(1f))
                                    Text(formatRupiah(nominal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (nominal.toFloat() / maks.toFloat()).coerceIn(0f, 1f) },
                                    color = MaterialTheme.colorScheme.error,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
        if (ringkasan.perKategoriMasuk.isNotEmpty()) {
            item {
                Text(tr("Rincian Masuk per Kategori", "Income Breakdown by Category"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            item {
                val maks = ringkasan.perKategoriMasuk.values.maxOrNull() ?: 1L
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for ((kategori, nominal) in ringkasan.perKategoriMasuk) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(tampilKategori(kategori), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.weight(1f))
                                    Text(formatRupiah(nominal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (nominal.toFloat() / maks.toFloat()).coerceIn(0f, 1f) },
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
        if (pendapatanHarian.isNotEmpty()) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Pendapatan per Hari", "Income per Day"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        tr("kategori Penjualan saja", "Sales category only"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item {
                val maksPendapatan = pendapatanHarian.maxOf { it.total }
                val totalPendapatan = pendapatanHarian.sumOf { it.total }
                val hariIniLaporan = LocalDate.now()
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (p in pendapatanHarian) {
                            val tertinggi = p.total == maksPendapatan && pendapatanHarian.size > 1
                            val hariIniBaris = p.tanggal == hariIniLaporan
                            val sorot = hariIniBaris || tertinggi
                            Column {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        formatTanggalSingkat(p.tanggal, LocalBahasa.current) +
                                            (if (hariIniBaris) tr(" \u00b7 hari ini", " \u00b7 today") else "") +
                                            (if (tertinggi) tr(" \u00b7 tertinggi", " \u00b7 highest") else ""),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (sorot) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        formatRupiah(p.total),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (sorot) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (p.total.toFloat() / maksPendapatan.toFloat()).coerceIn(0f, 1f) },
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                tr("TOTAL PENDAPATAN", "TOTAL REVENUE"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatRupiah(totalPendapatan),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = warnaHijauMasuk()
                            )
                        }
                        Text(
                            pendapatanHarian.size.toString() +
                                tr(" hari tercatat \u2014 jumlah semua baris di atas. Tambahan Modal tidak dihitung sebagai pendapatan.", " days recorded \u2014 the sum of all rows above. Capital Injection does not count as revenue."),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            if (ditutup) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        tr("Bulan ini sudah ditutup", "This month is closed") + (selected.ditutupPada?.let { tr(" pada ", " on ") + formatTanggal(it, LocalBahasa.current) } ?: "") + tr(" dan terkunci.", " and locked."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onTutupBuku(selected) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Tutup Buku ", "Close the Book: ") + bulanLabel(selected.tahun, selected.bulan, LocalBahasa.current))
                    }
                    Text(
                        tr("Menutup buku mengunci bulan ini dan membuka bulan berikutnya. Saldo berjalan terus dari seluruh catatan.", "Closing the book locks this month and opens the next one. The balance keeps running from all records."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }            }
        }
    }
}

// ---------------- Lainnya ----------------

@Composable
fun LainnyaTab(
    namaApotek: String,
    onNamaApotekChange: (String) -> Unit,
    dark: Boolean,
    onToggleDark: (Boolean) -> Unit,
    lastBackupAt: Long,
    lastBackupName: String,
    peringatanBackup: String?,
    onBackup: () -> Unit,
    onEksporExcel: () -> Unit,
    onRestore: () -> Unit,
    onTentang: () -> Unit,
    statusOnline: String,
    onModeOnline: () -> Unit,
    bahasa: BahasaApp,
    onGantiBahasa: () -> Unit
) {
    var editNama by remember { mutableStateOf(false) }
    var namaDraft by remember { mutableStateOf("") }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(Modifier.height(28.dp)) }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Box(modifier = Modifier.size(76.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("KasBulanan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (namaApotek.isNotBlank()) namaApotek else tr("Nama usaha belum diatur", "Business name not set"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Text(
                        tr("Versi 1.0.0", "Version 1.0.0"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                Column {
                    LainnyaMenuRow(
                        icon = Icons.Default.Store,
                        tint = Color(0xFF6FA8DC),
                        title = tr("Nama Usaha", "Business Name"),
                        subtitle = if (namaApotek.isNotBlank()) namaApotek else tr("Ketuk untuk mengisi - tampil di laporan", "Tap to fill in - shown on reports"),
                        onClick = {
                            namaDraft = namaApotek
                            editNama = true
                        },
                        trailing = {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                    LainnyaMenuRow(
                        icon = if (dark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        tint = Color(0xFFE3B341),
                        title = tr("Mode Gelap", "Dark Mode"),
                        subtitle = if (dark) tr("Aktif", "On") else tr("Nonaktif - mode terang", "Off - light mode"),
                        onClick = null,
                        trailing = { Switch(checked = dark, onCheckedChange = onToggleDark) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                    LainnyaMenuRow(
                        icon = Icons.Default.Language,
                        tint = Color(0xFF7FB3D5),
                        title = tr("Bahasa", "Language"),
                        subtitle = if (bahasa == BahasaApp.ID) "Bahasa Indonesia" else "English",
                        onClick = onGantiBahasa,
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                    LainnyaMenuRow(
                        icon = Icons.Default.Upload,
                        tint = MaterialTheme.colorScheme.primary,
                        title = tr("Backup Data", "Back Up Data"),
                        subtitle = if (lastBackupAt > 0L) {
                            tr("Terakhir: ", "Last: ") + formatTanggalWaktu(lastBackupAt, LocalBahasa.current) +
                                (if (lastBackupName.isNotBlank()) " - " + lastBackupName else "")
                        } else {
                            tr("Simpan semua data ke satu file JSON", "Save all data to a single JSON file")
                        },
                        onClick = onBackup,
                        peringatan = peringatanBackup,
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                    LainnyaMenuRow(
                        icon = Icons.Default.Download,
                        tint = Color(0xFFD18BC0),
                        title = tr("Restore Data", "Restore Data"),
                        subtitle = tr("Pulihkan dari file backup - mengganti semua data saat ini", "Restore from a backup file - replaces all current data"),
                        onClick = onRestore,
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                    LainnyaMenuRow(
                        icon = Icons.Default.TableChart,
                        tint = MaterialTheme.colorScheme.primary,
                        title = tr("Ekspor Excel", "Excel Export"),
                        subtitle = tr("Semua bulan jadi satu file .xlsx", "All months into one .xlsx file"),
                        onClick = onEksporExcel,
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 68.dp, end = 16.dp))
                    LainnyaMenuRow(
                        icon = Icons.Default.CloudSync,
                        tint = MaterialTheme.colorScheme.primary,
                        title = tr("Mode Online", "Online Mode"),
                        subtitle = statusOnline,
                        onClick = onModeOnline,
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                LainnyaMenuRow(
                    icon = Icons.Default.Info,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = tr("Tentang", "About"),
                    subtitle = tr("Buku kas bulanan, Tunai & QRIS terpisah", "A monthly cash book, Cash & QRIS kept separate"),
                    onClick = onTentang,
                    trailing = {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (editNama) {
        AlertDialog(
            onDismissRequest = { editNama = false },
            title = { Text(tr("Nama Usaha", "Business Name")) },
            text = {
                OutlinedTextField(
                    value = namaDraft,
                    onValueChange = { namaDraft = it },
                    label = { Text(tr("Nama usaha", "Business name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onNamaApotekChange(namaDraft.trim())
                    editNama = false
                }) { Text(tr("Simpan", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { editNama = false }) { Text(tr("Batal", "Cancel")) }
            }
        )
    }
}

@Composable
private fun LainnyaMenuRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    peringatan: String? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (peringatan != null) {
                Text(
                    peringatan,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE3B341)
                )
            }
        }
        trailing()
    }
}

/**
 * Layar Mode Online (rancangan A): masuk/daftar dengan email, status
 * koneksi, dan sinkron manual. Bila Firebase belum dikonfigurasi
 * (google-services.json belum ada), layar hanya menjelaskan langkah
 * pengaturannya - aplikasi tetap berjalan offline.
 */
@Composable
fun ModeOnlineScreen(
    terkonfigurasi: Boolean,
    emailMasuk: String?,
    sinkronTerakhir: Long,
    sibuk: Boolean,
    pesanStatus: String?,
    googleSiap: Boolean,
    onMasukGoogle: () -> Unit,
    onResetSandi: (String) -> Unit,
    onMasuk: (String, String) -> Unit,
    onDaftar: (String, String) -> Unit,
    onKeluar: () -> Unit,
    onSinkron: () -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var kataSandi by remember { mutableStateOf("") }
    val masukanSah = email.contains("@") && kataSandi.length >= 6
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = tr("Kembali", "Back"))
                }
                Text(
                    tr("Mode Online", "Online Mode"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (!terkonfigurasi) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                tr("Belum dikonfigurasi", "Not configured"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                tr("Mode online memakai Firebase. Pemilik aplikasi perlu " +
                                    "membuat project Firebase, menaruh file " +
                                    "google-services.json ke dalam aplikasi, lalu " +
                                    "menyalakan login email dan database Firestore. " +
                                    "Langkah lengkapnya ada di dokumen ONLINE.md di repo. " +
                                    "Selama belum diatur, aplikasi berjalan offline " +
                                    "seperti biasa dan tidak ada yang berubah.",
                                    "Online mode uses Firebase. The app owner needs to " +
                                    "create a Firebase project, place the " +
                                    "google-services.json file in the app, then enable " +
                                    "email sign-in and the Firestore database. The full " +
                                    "steps are in the ONLINE.md document in the repo. " +
                                    "Until it is set up, the app runs offline as usual " +
                                    "and nothing changes."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (emailMasuk == null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                tr("Masuk untuk menyalakan sinkronisasi", "Sign in to turn on sync"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                tr("Data tetap tersimpan di HP ini; salinannya " +
                                    "tersinkron ke akunmu supaya tidak hilang " +
                                    "saat ganti HP.",
                                    "Data stays on this phone; a copy syncs to your " +
                                    "account so it is not lost when you change phones."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            if (googleSiap) {
                                Button(
                                    onClick = onMasukGoogle,
                                    enabled = !sibuk,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF1F1F1F)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LogoGoogle(modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(tr("Masuk dengan Google", "Sign in with Google"))
                                }
                                Spacer(Modifier.height(16.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                    Text(
                                        tr("  atau pakai email  ", "  or use email  "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(16.dp))
                            }
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = kataSandi,
                                onValueChange = { kataSandi = it },
                                label = { Text(tr("Kata sandi (min. 6 karakter)", "Password (min. 6 characters)")) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = { onMasuk(email, kataSandi) },
                                enabled = !sibuk && masukanSah,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (sibuk) tr("Memproses...", "Working...") else tr("Masuk", "Sign In")) }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onDaftar(email, kataSandi) },
                                enabled = !sibuk && masukanSah,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(tr("Daftar Akun Baru", "Create Account")) }
                            TextButton(
                                onClick = { onResetSandi(email) },
                                enabled = !sibuk && email.contains("@"),
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(tr("Lupa kata sandi?", "Forgot password?")) }
                        }
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                tr("Terhubung", "Connected"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                emailMasuk,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (sinkronTerakhir > 0L) {
                                    tr("Sinkron terakhir: ", "Last synced: ") + formatTanggalWaktu(sinkronTerakhir, LocalBahasa.current)
                                } else {
                                    tr("Belum pernah sinkron", "Never synced")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = onSinkron,
                                enabled = !sibuk,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (sibuk) tr("Menyinkronkan...", "Syncing...") else tr("Sinkronkan Sekarang", "Sync Now")) }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onKeluar,
                                enabled = !sibuk,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(tr("Keluar", "Sign Out")) }
                        }
                    }
                }
                if (pesanStatus != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        pesanStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    tr("Sinkron berjalan otomatis setiap ada perubahan dan berkala " +
                        "tiap beberapa jam saat ada internet. Konflik data " +
                        "diselesaikan dengan aturan: yang terakhir diubah yang menang.",
                        "Sync runs automatically whenever data changes and " +
                        "periodically every few hours while online. Data " +
                        "conflicts resolve by one rule: the last change wins."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Gerbang masuk (pilihan G2): layar pertama saat mode online aktif dan
 * belum ada yang masuk. Menawarkan masuk/daftar (email atau Google),
 * reset kata sandi, dan jalan tamu "Lewati" untuk memakai aplikasi
 * offline lokal tanpa akun. Hanya tampil bila Firebase terkonfigurasi.
 */
@Composable
fun LayarLogin(
    sibuk: Boolean,
    pesanStatus: String?,
    googleSiap: Boolean,
    onMasuk: (String, String) -> Unit,
    onDaftar: (String, String) -> Unit,
    onMasukGoogle: () -> Unit,
    onResetSandi: (String) -> Unit,
    onLewati: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var kataSandi by remember { mutableStateOf("") }
    val masukanSah = email.contains("@") && kataSandi.length >= 6
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            // L1 Kartu Tengah dengan tata letak L4 (Google di urutan utama):
            // semua isi di dalam satu kartu, ikon dompet menumpang di tepi
            // atas kartu.
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 38.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = 20.dp, end = 20.dp, top = 52.dp, bottom = 24.dp
                        ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "KasBulanan",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            tr("Masuk untuk menyinkronkan buku kasmu", "Sign in to sync your cash book"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        if (googleSiap) {
                            Button(
                                onClick = onMasukGoogle,
                                enabled = !sibuk,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF1F1F1F)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LogoGoogle(modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(tr("Masuk dengan Google", "Sign in with Google"))
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HorizontalDivider(modifier = Modifier.weight(1f))
                                Text(
                                    tr("  atau pakai email  ", "  or use email  "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = kataSandi,
                            onValueChange = { kataSandi = it },
                            label = { Text(tr("Kata sandi (min. 6 karakter)", "Password (min. 6 characters)")) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onMasuk(email, kataSandi) },
                            enabled = !sibuk && masukanSah,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (sibuk) tr("Memproses...", "Working...") else tr("Masuk", "Sign In")) }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onDaftar(email, kataSandi) },
                            enabled = !sibuk && masukanSah,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(tr("Daftar Akun Baru", "Create Account")) }
                        TextButton(
                            onClick = { onResetSandi(email) },
                            enabled = !sibuk && email.contains("@")
                        ) { Text(tr("Lupa kata sandi?", "Forgot password?")) }
                        if (pesanStatus != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                pesanStatus,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onLewati, enabled = !sibuk) {
                            Text(tr("Lewati - Pakai Tanpa Akun", "Skip - Use Without an Account"))
                        }
                        Text(
                            tr("Mode tamu: data hanya di HP ini, tanpa sinkronisasi.", "Guest mode: data stays on this phone, with no sync."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(76.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** Logo "G" Google: empat busur warna resmi + palang tengah, di atas lingkaran putih. */
@Composable
private fun LogoGoogle(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.17f
        val pad = stroke / 2 + size.minDimension * 0.03f
        drawCircle(color = Color.White, radius = size.minDimension / 2)
        val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
        val sudut = Offset(pad, pad)
        val gaya = Stroke(width = stroke, cap = StrokeCap.Butt)
        drawArc(Color(0xFF4285F4), -45f, 90f, false, topLeft = sudut, size = arcSize, style = gaya)
        drawArc(Color(0xFF34A853), 45f, 90f, false, topLeft = sudut, size = arcSize, style = gaya)
        drawArc(Color(0xFFFBBC05), 135f, 90f, false, topLeft = sudut, size = arcSize, style = gaya)
        drawArc(Color(0xFFEA4335), 225f, 90f, false, topLeft = sudut, size = arcSize, style = gaya)
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(size.width / 2, size.height / 2 - stroke / 2),
            size = Size(size.width / 2 - pad + stroke / 2, stroke)
        )
    }
}
