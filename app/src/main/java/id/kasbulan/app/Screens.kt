package id.kasbulan.app

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.kasbulan.app.data.BulanKas
import id.kasbulan.app.data.TransaksiKas
import java.time.LocalDate
import java.time.YearMonth

fun saluranLabel(saluran: String): String = if (saluran == SALURAN_QRIS) "QRIS" else "Tunai"

fun judulTransaksi(t: TransaksiKas): String =
    if (t.jenis == JENIS_TRANSFER) {
        "Transfer " + saluranLabel(t.saluran) + " \u2192 " + saluranLabel(t.saluranTujuan ?: SALURAN_TUNAI)
    } else {
        t.kategori
    }

fun subTransaksi(t: TransaksiKas): String {
    val sb = StringBuilder()
    sb.append(formatJam(t.epoch))
    if (t.jenis != JENIS_TRANSFER) sb.append(" \u00b7 ").append(saluranLabel(t.saluran))
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
        ) { Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan sebelumnya") }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                bulanLabel(selected.tahun, selected.bulan),
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
                        "Ditutup",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
        IconButton(
            onClick = { if (index < bulanList.size - 1) onSelect(bulanList[index + 1]) },
            enabled = index < bulanList.size - 1
        ) { Icon(Icons.Default.ChevronRight, contentDescription = "Bulan berikutnya") }
    }
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
                masuk -> MaterialTheme.colorScheme.primary
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

// ---------------- Beranda ----------------

@Composable
fun BerandaTab(
    bulan: BulanKas?,
    bulanTerakhir: BulanKas?,
    labelBulanBerikutnya: String?,
    transaksi: List<TransaksiKas>,
    namaApotek: String,
    onIsiSaldoAwal: () -> Unit,
    onBukaBulanBerikutnya: () -> Unit,
    onEditSaldoAwal: () -> Unit,
    onTransfer: () -> Unit,
    onRekapHariIni: () -> Unit,
    onTransaksiClick: (TransaksiKas) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text("KasBulanan", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                if (namaApotek.isNotBlank()) {
                    Text(
                        namaApotek,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (bulan == null) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (bulanTerakhir == null) {
                            Text("Mulai buku kas pertamamu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Isi saldo awal bulan ini - uang tunai di laci dan saldo QRIS di rekening - lalu mulai catat pemasukan dan pengeluaran apotekmu.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onIsiSaldoAwal, modifier = Modifier.fillMaxWidth()) {
                                Text("Isi Saldo Awal Bulan Ini")
                            }
                        } else {
                            Text("Semua bulan sudah ditutup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Buka bulan berikutnya untuk melanjutkan pencatatan. Saldo awalnya otomatis dari saldo akhir " +
                                    bulanLabel(bulanTerakhir.tahun, bulanTerakhir.bulan) + ".",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(onClick = onBukaBulanBerikutnya, modifier = Modifier.fillMaxWidth()) {
                                Text("Buka " + (labelBulanBerikutnya ?: "Bulan Berikutnya"))
                            }
                        }
                    }
                }
            }
        } else {
            val saldo = hitungSaldo(bulan, transaksi)
            val ringkasan = ringkasanBulan(transaksi, bulan.id)
            val terakhir = transaksi
                .filter { it.bulanId == bulan.id }
                .sortedByDescending { it.epoch }
                .take(5)
            val kalenderBerbeda = YearMonth.now() != YearMonth.of(bulan.tahun, bulan.bulan)
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        bulanLabel(bulan.tahun, bulan.bulan),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "Berjalan",
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
                        Text(
                            "Saldo Kas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            formatRupiah(saldo.total),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f))
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Tunai (laci)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(formatRupiah(saldo.tunai), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text("QRIS (rekening)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(formatRupiah(saldo.qris), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                            Text("Masuk bulan ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "+" + formatRupiah(ringkasan.totalMasuk),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Keluar bulan ini", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                formatRupiah(-ringkasan.totalKeluar),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Saldo awal: " + formatRupiah(bulan.saldoAwalTunai + bulan.saldoAwalQris),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onEditSaldoAwal) { Text("Ubah") }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = onTransfer) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Transfer")
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
                                "KAS HARI INI",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatTanggal(hariIni),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Masuk ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "+" + formatRupiah(rHariIni.totalMasuk),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "  \u00b7  Keluar ",
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
                            Text("Rekap Hari Ini")
                        }
                        Text(
                            if (rKemarin.jumlahTransaksi > 0) {
                                "Kemarin: masuk " + formatRupiah(rKemarin.totalMasuk) +
                                    " \u00b7 keluar " + formatRupiah(rKemarin.totalKeluar)
                            } else {
                                "Kemarin: tidak ada catatan."
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
                            "Bulan kalender sudah berganti, tetapi " + bulanLabel(bulan.tahun, bulan.bulan) +
                                " masih berjalan. Tutup buku dari tab Laporan untuk membuka bulan baru.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
            item {
                Text("Transaksi Terakhir", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (terakhir.isEmpty()) {
                item {
                    Text(
                        "Belum ada transaksi bulan ini.",
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
    val saldoAkhir = saldoAkhirHari(bulan, transaksi, hariIni)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                }
                Column {
                    Text("Rekap Hari Ini", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        formatTanggal(hariIni),
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
                        "Belum ada catatan hari ini. Catat penjualan dan pengeluaran seperti biasa, rekapnya terisi otomatis.",
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
                            Text("Pemasukan", style = MaterialTheme.typography.bodyMedium)
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
                                    "   " + r.kategori + " \u00b7 " + saluranLabel(r.saluran),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.weight(1f))
                                Text(formatRupiah(r.nominal), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        HorizontalDivider()
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Pengeluaran", style = MaterialTheme.typography.bodyMedium)
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
                                    "   " + r.kategori + " \u00b7 " + saluranLabel(r.saluran),
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
                            "Transfer " + saluranLabel(tr.dari) + " \u2192 " + saluranLabel(tr.ke) +
                                ": " + formatRupiah(tr.nominal) + " (netral)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (ringkasan.transfer.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f))
                    }
                    Text(
                        "SALDO AKHIR HARI INI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Tunai", color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                        "Total kas akhir: ",
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
                    ringkasan.jumlahTransaksi.toString() + " transaksi tercatat hari ini \u00b7 bulan " +
                        bulanLabel(bulan.tahun, bulan.bulan) + " berjalan",
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
                    context.startActivity(Intent.createChooser(intent, "Bagikan Rekap"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bagikan Rekap")
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Rekap ini rangkuman hari berjalan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Angka resmi bulan tetap ditetapkan saat Tutup Buku.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Detail per transaksi ada di tab Riwayat.",
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
    onTransaksiClick: (TransaksiKas) -> Unit
) {
    if (bulanList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Belum ada data. Isi saldo awal dari tab Beranda.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = bulanList.firstOrNull { it.id == selectedId }
        ?: bulanList.firstOrNull { it.status == STATUS_BERJALAN }
        ?: bulanList.last()
    val milikBulan = transaksi
        .filter { it.bulanId == selected.id }
        .sortedByDescending { it.epoch }
    val perHari = milikBulan.groupBy { epochToLocalDate(it.epoch) }
    val hariIni = LocalDate.now()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            BulanSelector(bulanList, selected) { selectedId = it.id }
        }
        if (milikBulan.isEmpty()) {
            item {
                Text(
                    "Belum ada transaksi pada bulan ini.",
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
                    Text(labelHari(tanggal, hariIni), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Masuk " + groupThousands(masukHari.toString()) + " - Keluar " + groupThousands(keluarHari.toString()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(daftar, key = { it.id }) { t ->
                TransaksiRow(t, clickable = selected.status == STATUS_BERJALAN) { onTransaksiClick(t) }
            }
        }
        item { Spacer(Modifier.height(72.dp)) }
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
    if (bulanList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Belum ada data. Isi saldo awal dari tab Beranda.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf<Long?>(null) }
    val selected = bulanList.firstOrNull { it.id == selectedId }
        ?: bulanList.firstOrNull { it.status == STATUS_BERJALAN }
        ?: bulanList.last()
    val saldo = hitungSaldo(selected, transaksi)
    val ringkasan = ringkasanBulan(transaksi, selected.id)
    val pendapatanHarian = pendapatanPerHari(transaksi, selected.id)
    val ditutup = selected.status == STATUS_DITUTUP
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item { BulanSelector(bulanList, selected) { selectedId = it.id } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Saldo Awal", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        Text(formatRupiah(selected.saldoAwalTunai + selected.saldoAwalQris), fontWeight = FontWeight.SemiBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Total Masuk", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        Text("+" + formatRupiah(ringkasan.totalMasuk), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Total Keluar", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.weight(1f))
                        Text(formatRupiah(-ringkasan.totalKeluar), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                    }
                    HorizontalDivider()
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(if (ditutup) "Saldo Akhir" else "Saldo Saat Ini", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text(formatRupiah(saldo.total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Tunai - " + formatRupiah(saldo.tunai),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "QRIS - " + formatRupiah(saldo.qris),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (ringkasan.perKategoriKeluar.isNotEmpty()) {
            item {
                Text("Rincian Keluar per Kategori", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            item {
                val maks = ringkasan.perKategoriKeluar.values.maxOrNull() ?: 1L
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for ((kategori, nominal) in ringkasan.perKategoriKeluar) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(kategori, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("Rincian Masuk per Kategori", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            item {
                val maks = ringkasan.perKategoriMasuk.values.maxOrNull() ?: 1L
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for ((kategori, nominal) in ringkasan.perKategoriMasuk) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(kategori, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Pendapatan per Hari", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "kategori Penjualan saja",
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
                                        formatTanggalSingkat(p.tanggal) +
                                            (if (hariIniBaris) " \u00b7 hari ini" else "") +
                                            (if (tertinggi) " \u00b7 tertinggi" else ""),
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
                                "TOTAL PENDAPATAN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                formatRupiah(totalPendapatan),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            pendapatanHarian.size.toString() +
                                " hari tercatat \u2014 jumlah semua baris di atas. Tambahan Modal tidak dihitung sebagai pendapatan.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    val teks = laporanText(selected, namaApotek, transaksi)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, teks)
                    }
                    context.startActivity(Intent.createChooser(intent, "Bagikan Laporan"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bagikan Laporan")
            }
        }
        item {
            if (ditutup) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Bulan ini sudah ditutup" + (selected.ditutupPada?.let { " pada " + formatTanggal(it) } ?: "") + " dan terkunci.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { onTutupBuku(selected) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tutup Buku " + bulanLabel(selected.tahun, selected.bulan))
                    }
                    Text(
                        "Menutup buku mengunci bulan ini dan membuka bulan berikutnya dengan saldo awal dari saldo akhir bulan ini.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

// ---------------- Lainnya ----------------

@Composable
fun LainnyaTab(
    namaApotek: String,
    onNamaApotekChange: (String) -> Unit,
    dark: Boolean,
    onToggleDark: (Boolean) -> Unit,
    bulanBerjalan: BulanKas?,
    onEditSaldoAwal: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onTentang: () -> Unit
) {
    var editNama by remember { mutableStateOf(false) }
    var namaDraft by remember { mutableStateOf("") }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("KasBulanan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (namaApotek.isNotBlank()) namaApotek else "Nama apotek belum diatur",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Versi 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            ListItem(
                headlineContent = { Text("Nama Apotek") },
                supportingContent = { Text(if (namaApotek.isNotBlank()) namaApotek else "Ketuk untuk mengisi - tampil di laporan") },
                leadingContent = { Icon(Icons.Default.Store, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier.clickable {
                    namaDraft = namaApotek
                    editNama = true
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Saldo Awal Bulan Berjalan") },
                supportingContent = {
                    Text(
                        if (bulanBerjalan != null) {
                            bulanLabel(bulanBerjalan.tahun, bulanBerjalan.bulan) +
                                " - Tunai " + formatRupiah(bulanBerjalan.saldoAwalTunai) +
                                " - QRIS " + formatRupiah(bulanBerjalan.saldoAwalQris)
                        } else {
                            "Tidak ada bulan berjalan"
                        }
                    )
                },
                leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = if (bulanBerjalan != null) Modifier.clickable(onClick = onEditSaldoAwal) else Modifier
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Mode Gelap") },
                supportingContent = { Text(if (dark) "Aktif" else "Nonaktif - mode terang") },
                leadingContent = {
                    Icon(if (dark) Icons.Default.DarkMode else Icons.Default.LightMode, contentDescription = null)
                },
                trailingContent = { Switch(checked = dark, onCheckedChange = onToggleDark) }
            )
        }
        item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
        item {
            ListItem(
                headlineContent = { Text("Backup Data") },
                supportingContent = { Text("Simpan semua data ke satu file JSON") },
                leadingContent = { Icon(Icons.Default.Upload, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onBackup)
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Restore Data") },
                supportingContent = { Text("Pulihkan dari file backup - mengganti semua data saat ini") },
                leadingContent = { Icon(Icons.Default.Download, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onRestore)
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Tentang") },
                supportingContent = { Text("Buku kas bulanan khusus apotek, Tunai & QRIS terpisah") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onTentang)
            )
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    if (editNama) {
        AlertDialog(
            onDismissRequest = { editNama = false },
            title = { Text("Nama Apotek") },
            text = {
                OutlinedTextField(
                    value = namaDraft,
                    onValueChange = { namaDraft = it },
                    label = { Text("Nama apotek") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onNamaApotekChange(namaDraft.trim())
                    editNama = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { editNama = false }) { Text("Batal") }
            }
        )
    }
}
