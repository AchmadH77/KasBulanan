package id.kasbulan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import id.kasbulan.app.data.AppDatabase
import id.kasbulan.app.data.BackupHelper
import id.kasbulan.app.data.BulanKas
import id.kasbulan.app.data.ExcelExporter
import id.kasbulan.app.data.KasRepository
import id.kasbulan.app.data.TransaksiKas
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.create(applicationContext)
        val repository = KasRepository(database)
        setContent {
            val prefs = remember { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }
            var dark by remember { mutableStateOf(prefs.getBoolean("dark_theme", true)) }
            var namaApotek by remember { mutableStateOf(prefs.getString("nama_apotek", "") ?: "") }
            KasBulananTheme(darkTheme = dark) {
                KasBulananApp(
                    repository = repository,
                    dark = dark,
                    onToggleDark = { nilai ->
                        dark = nilai
                        prefs.edit().putBoolean("dark_theme", nilai).apply()
                    },
                    namaApotek = namaApotek,
                    onNamaApotekChange = { nilai ->
                        namaApotek = nilai
                        prefs.edit().putString("nama_apotek", nilai).apply()
                    }
                )
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "kasbulan_prefs"
    }
}

@Composable
fun KasBulananApp(
    repository: KasRepository,
    dark: Boolean,
    onToggleDark: (Boolean) -> Unit,
    namaApotek: String,
    onNamaApotekChange: (String) -> Unit
) {
    val bulanList by repository.bulanList.collectAsState(initial = emptyList())
    val transaksiList by repository.transaksiList.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var showRekapHariIni by remember { mutableStateOf(false) }
    val bulanBerjalan = bulanList.firstOrNull { it.status == STATUS_BERJALAN }
    val bulanTerakhir = bulanList.lastOrNull()
    val labelBerikutnya = bulanTerakhir?.let {
        val ym = YearMonth.of(it.tahun, it.bulan).plusMonths(1)
        bulanLabel(ym.year, ym.monthValue)
    }

    var showForm by remember { mutableStateOf(false) }
    var formInitial by remember { mutableStateOf<TransaksiKas?>(null) }
    var showTransfer by remember { mutableStateOf(false) }
    var transferInitial by remember { mutableStateOf<TransaksiKas?>(null) }
    var tutupTarget by remember { mutableStateOf<BulanKas?>(null) }
    var showTentang by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<BackupHelper.BackupData?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var pesan by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(pesan) {
        val teks = pesan
        if (teks != null) {
            snackbarHostState.showSnackbar(teks)
            pesan = null
        }
    }

    val prefs = remember { context.getSharedPreferences("kasbulan_prefs", android.content.Context.MODE_PRIVATE) }
    var lastBackupAt by remember { mutableLongStateOf(prefs.getLong("last_backup_at", 0L)) }
    var lastBackupName by remember { mutableStateOf(prefs.getString("last_backup_name", "") ?: "") }
    var lastBackupTanda by remember { mutableStateOf(prefs.getString("last_backup_tanda", "") ?: "") }
    val tandaData = transaksiList.size.toString() + ":" + transaksiList.sumOf { it.nominal } + ":" + bulanList.size
    val peringatanBackup = when {
        lastBackupAt == 0L && transaksiList.isNotEmpty() -> "Belum pernah dibackup"
        lastBackupAt > 0L && lastBackupTanda != tandaData -> "Ada perubahan sejak backup terakhir"
        else -> null
    }

    val eksporLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    try {
                        val bytes = ExcelExporter.buildWorkbook(bulanList, transaksiList)
                        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                pesan = if (ok) "Ekspor Excel tersimpan" else "Ekspor Excel gagal"
            }
        }
    }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val teks = BackupHelper.buildBackupJson(bulanList, transaksiList, System.currentTimeMillis())
                val ok = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use {
                            it.write(teks.toByteArray(Charsets.UTF_8))
                        }
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                if (ok) {
                    val namaFile = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                            }
                        } catch (e: Exception) {
                            null
                        }
                    } ?: "kasbulanan-backup.json"
                    val sekarang = System.currentTimeMillis()
                    prefs.edit()
                        .putLong("last_backup_at", sekarang)
                        .putString("last_backup_name", namaFile)
                        .putString("last_backup_tanda", tandaData)
                        .apply()
                    lastBackupAt = sekarang
                    lastBackupName = namaFile
                    lastBackupTanda = tandaData
                }
                pesan = if (ok) "Backup tersimpan" else "Backup gagal"
            }
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val teks = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    } catch (e: Exception) {
                        null
                    }
                }
                val data = teks?.let { BackupHelper.parseBackupJson(it) }
                if (data != null) pendingRestore = data
                else pesan = "File bukan backup KasBulanan atau rusak"
            }
        }
    }

    fun bukaForm(t: TransaksiKas?) {
        if (t?.jenis == JENIS_TRANSFER) {
            transferInitial = t
            showTransfer = true
        } else {
            formInitial = t
            showForm = true
        }
    }

    BackHandler(enabled = showRekapHariIni) { showRekapHariIni = false }
    BackHandler(enabled = showForm) { showForm = false }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!showForm) {
                NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { showRekapHariIni = false; tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Beranda") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { showRekapHariIni = false; tab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Riwayat") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { showRekapHariIni = false; tab = 2 },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label = { Text("Laporan") }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { showRekapHariIni = false; tab = 3 },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
                    label = { Text("Lainnya") }
                )
                }
            }
        },
        floatingActionButton = {
            if (tab == 0 && bulanBerjalan != null && !showRekapHariIni && !showForm) {
                ExtendedFloatingActionButton(
                    onClick = { bukaForm(null) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Catat") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                0 -> if (showRekapHariIni && bulanBerjalan != null) {
                    RekapHariIniScreen(
                        bulan = bulanBerjalan,
                        transaksi = transaksiList,
                        namaApotek = namaApotek,
                        onBack = { showRekapHariIni = false }
                    )
                } else BerandaTab(
                    bulan = bulanBerjalan,
                    bulanTerakhir = bulanTerakhir,
                    labelBulanBerikutnya = labelBerikutnya,
                    transaksi = transaksiList,
                    namaApotek = namaApotek,
                    onMulaiBulanIni = {
                        scope.launch {
                            val ym = YearMonth.now()
                            repository.insertBulan(
                                BulanKas(tahun = ym.year, bulan = ym.monthValue)
                            )
                        }
                    },
                    onBukaBulanBerikutnya = {
                        bulanTerakhir?.let { terakhir ->
                            scope.launch {
                                val ym = YearMonth.of(terakhir.tahun, terakhir.bulan).plusMonths(1)
                                repository.insertBulan(
                                    BulanKas(tahun = ym.year, bulan = ym.monthValue)
                                )
                            }
                        }
                    },
                    onTransfer = {
                        transferInitial = null
                        showTransfer = true
                    },
                    onRekapHariIni = { showRekapHariIni = true },
                    onTransaksiClick = { bukaForm(it) }
                )
                1 -> RiwayatTab(bulanList, transaksiList, namaApotek) { bukaForm(it) }
                2 -> LaporanTab(bulanList, transaksiList, namaApotek) { tutupTarget = it }
                else -> LainnyaTab(
                    namaApotek = namaApotek,
                    onNamaApotekChange = onNamaApotekChange,
                    dark = dark,
                    onToggleDark = onToggleDark,
                    lastBackupAt = lastBackupAt,
                    lastBackupName = lastBackupName,
                    peringatanBackup = peringatanBackup,
                    onBackup = { backupLauncher.launch("kasbulanan-backup.json") },
                    onEksporExcel = {
                        if (bulanList.isEmpty()) {
                            pesan = "Belum ada data untuk diekspor"
                        } else {
                            eksporLauncher.launch("kasbulanan-ekspor.xlsx")
                        }
                    },
                    onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
                    onTentang = { showTentang = true }
                )
            }
            if (showForm && bulanBerjalan != null) {
                CatatTransaksiScreen(
                    bulan = bulanBerjalan,
                    initial = formInitial,
                    onBack = { showForm = false },
                    onSave = { t ->
                        scope.launch {
                            if (t.id == 0L) repository.insertTransaksi(t) else repository.updateTransaksi(t)
                        }
                        showForm = false
                    },
                    onSaveAndAgain = { t ->
                        scope.launch { repository.insertTransaksi(t) }
                    },
                    onDelete = { t ->
                        scope.launch { repository.deleteTransaksi(t) }
                        showForm = false
                    }
                )
            }
        }
    }

    if (showTransfer && bulanBerjalan != null) {
        TransferDialog(
            bulan = bulanBerjalan,
            initial = transferInitial,
            onDismiss = { showTransfer = false },
            onSave = { t ->
                scope.launch {
                    if (t.id == 0L) repository.insertTransaksi(t) else repository.updateTransaksi(t)
                }
                showTransfer = false
            },
            onDelete = { t ->
                scope.launch { repository.deleteTransaksi(t) }
                showTransfer = false
            }
        )
    }
    tutupTarget?.let { target ->
        TutupBukuDialog(
            bulan = target,
            bulanList = bulanList,
            transaksi = transaksiList,
            onDismiss = { tutupTarget = null },
            onConfirm = {
                scope.launch { repository.tutupBulan(target) }
                tutupTarget = null
                pesan = "Buku " + bulanLabel(target.tahun, target.bulan) + " ditutup"
            }
        )
    }
    if (showTentang) {
        TentangDialog(onDismiss = { showTentang = false })
    }
    pendingRestore?.let { data ->
        RestoreConfirmDialog(
            data = data,
            jumlahBulanSaatIni = bulanList.size,
            jumlahTransaksiSaatIni = transaksiList.size,
            onDismiss = { pendingRestore = null },
            onConfirm = {
                scope.launch { repository.replaceAll(data.bulan, data.transaksi) }
                pendingRestore = null
                pesan = "Data dipulihkan dari backup"
            }
        )
    }
}

// ---------- Dialog tanggal bersama ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PilihTanggalDialog(
    bulan: BulanKas,
    tanggal: LocalDate,
    onDismiss: () -> Unit,
    onPilih: (LocalDate) -> Unit
) {
    val ym = YearMonth.of(bulan.tahun, bulan.bulan)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = tanggal.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return YearMonth.from(d) == ym
            }

            override fun isSelectableYear(year: Int): Boolean = year == bulan.tahun
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let {
                    onPilih(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    ) {
        DatePicker(state = state, showModeToggle = false)
    }
}

private fun tanggalAwal(initial: TransaksiKas?, bulan: BulanKas): LocalDate {
    if (initial != null) return epochToLocalDate(initial.epoch)
    val ym = YearMonth.of(bulan.tahun, bulan.bulan)
    return if (YearMonth.now() == ym) LocalDate.now() else ym.atEndOfMonth()
}

private fun epochDariTanggal(tanggal: LocalDate, initial: TransaksiKas?): Long {
    val waktu = initial?.let {
        Instant.ofEpochMilli(it.epoch).atZone(ZoneId.systemDefault()).toLocalTime()
    } ?: java.time.LocalTime.now()
    return tanggal.atTime(waktu).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

// ---------- Form transaksi masuk/keluar (halaman penuh, CT1) ----------

@Composable
fun CatatTransaksiScreen(
    bulan: BulanKas,
    initial: TransaksiKas?,
    onBack: () -> Unit,
    onSave: (TransaksiKas) -> Unit,
    onSaveAndAgain: ((TransaksiKas) -> Unit)? = null,
    onDelete: (TransaksiKas) -> Unit
) {
    var jenis by remember { mutableStateOf(initial?.jenis ?: JENIS_MASUK) }
    var saluran by remember { mutableStateOf(initial?.saluran ?: SALURAN_TUNAI) }
    var kategori by remember { mutableStateOf(initial?.kategori ?: KATEGORI_MASUK.first()) }
    var nominalRaw by remember { mutableStateOf(initial?.nominal?.toString() ?: "") }
    var catatan by remember { mutableStateOf(initial?.catatan ?: "") }
    var tanggal by remember { mutableStateOf(tanggalAwal(initial, bulan)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var jumlahTersimpanLagi by remember { mutableIntStateOf(0) }

    LaunchedEffect(jenis) {
        val daftar = if (jenis == JENIS_MASUK) KATEGORI_MASUK else KATEGORI_KELUAR
        if (kategori !in daftar) kategori = daftar.first()
    }
    val nominal = nominalRaw.toLongOrNull() ?: 0L
    val daftarKategori = if (jenis == JENIS_MASUK) KATEGORI_MASUK else KATEGORI_KELUAR
    fun buatTransaksi() = TransaksiKas(
        id = initial?.id ?: 0,
        bulanId = bulan.id,
        jenis = jenis,
        saluran = saluran,
        saluranTujuan = null,
        kategori = kategori,
        nominal = nominal,
        catatan = catatan.trim(),
        epoch = epochDariTanggal(tanggal, initial)
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text(
                if (initial == null) "Catat Transaksi" else "Ubah Transaksi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(JENIS_MASUK to "Masuk", JENIS_KELUAR to "Keluar").forEachIndexed { i, (nilai, label) ->
                    SegmentedButton(
                        selected = jenis == nilai,
                        onClick = { jenis = nilai },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = 2)
                    ) { Text(label) }
                }
            }
            Text("Saluran Uang", style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(SALURAN_TUNAI to "Tunai", SALURAN_QRIS to "QRIS").forEachIndexed { i, (nilai, label) ->
                    SegmentedButton(
                        selected = saluran == nilai,
                        onClick = { saluran = nilai },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = 2)
                    ) { Text(label) }
                }
            }
            MoneyField("Nominal", nominalRaw) { nominalRaw = it }
            Text("Kategori", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                daftarKategori.forEach { k ->
                    FilterChip(
                        selected = kategori == k,
                        onClick = { kategori = k },
                        label = { Text(k) }
                    )
                }
            }
            Text("Tanggal", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatTanggal(tanggal))
            }
            Text("Catatan (boleh kosong)", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = catatan,
                onValueChange = { catatan = it },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                enabled = nominal > 0,
                onClick = { onSave(buatTransaksi()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Simpan", style = MaterialTheme.typography.titleMedium)
            }
            if (initial == null && onSaveAndAgain != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    enabled = nominal > 0,
                    onClick = {
                        onSaveAndAgain(buatTransaksi())
                        jumlahTersimpanLagi++
                        nominalRaw = ""
                        catatan = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Simpan & Catat Lagi")
                }
                if (jumlahTersimpanLagi > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        jumlahTersimpanLagi.toString() + " catatan tersimpan - lanjutkan berikutnya",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            TextButton(onClick = onBack) { Text("Batal") }
            if (initial != null) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (showDatePicker) {
        PilihTanggalDialog(
            bulan = bulan,
            tanggal = tanggal,
            onDismiss = { showDatePicker = false },
            onPilih = { tanggal = it }
        )
    }
    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus transaksi ini?") },
            text = { Text("Catatan akan dihapus permanen dan saldo dihitung ulang.") },
            confirmButton = {
                TextButton(onClick = { onDelete(initial) }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Batal") }
            }
        )
    }
}

// ---------- Form transfer antar saldo ----------

@Composable
fun TransferDialog(
    bulan: BulanKas,
    initial: TransaksiKas?,
    onDismiss: () -> Unit,
    onSave: (TransaksiKas) -> Unit,
    onDelete: (TransaksiKas) -> Unit
) {
    var dariQris by remember { mutableStateOf(initial?.saluran != SALURAN_TUNAI) }
    var nominalRaw by remember { mutableStateOf(initial?.nominal?.toString() ?: "") }
    var catatan by remember { mutableStateOf(initial?.catatan ?: "") }
    var tanggal by remember { mutableStateOf(tanggalAwal(initial, bulan)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val nominal = nominalRaw.toLongOrNull() ?: 0L
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (initial == null) "Transfer Antar Saldo" else "Ubah Transfer",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Perpindahan uang antar dompet - bukan pemasukan atau pengeluaran, jadi total kas tidak berubah.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(true to "QRIS \u2192 Tunai", false to "Tunai \u2192 QRIS").forEachIndexed { i, (nilai, label) ->
                        SegmentedButton(
                            selected = dariQris == nilai,
                            onClick = { dariQris = nilai },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = 2)
                        ) { Text(label) }
                    }
                }
                MoneyField("Nominal yang dipindahkan", nominalRaw) { nominalRaw = it }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Tanggal: " + formatTanggal(tanggal))
                }
                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text("Catatan (boleh kosong)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (initial != null) {
                        TextButton(onClick = { confirmDelete = true }) {
                            Text("Hapus", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Batal") }
                    Button(
                        enabled = nominal > 0,
                        onClick = {
                            onSave(
                                TransaksiKas(
                                    id = initial?.id ?: 0,
                                    bulanId = bulan.id,
                                    jenis = JENIS_TRANSFER,
                                    saluran = if (dariQris) SALURAN_QRIS else SALURAN_TUNAI,
                                    saluranTujuan = if (dariQris) SALURAN_TUNAI else SALURAN_QRIS,
                                    kategori = KATEGORI_TRANSFER,
                                    nominal = nominal,
                                    catatan = catatan.trim(),
                                    epoch = epochDariTanggal(tanggal, initial)
                                )
                            )
                        }
                    ) { Text("Simpan") }
                }
            }
        }
    }
    if (showDatePicker) {
        PilihTanggalDialog(
            bulan = bulan,
            tanggal = tanggal,
            onDismiss = { showDatePicker = false },
            onPilih = { tanggal = it }
        )
    }
    if (confirmDelete && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus transfer ini?") },
            text = { Text("Catatan akan dihapus permanen dan saldo dihitung ulang.") },
            confirmButton = {
                TextButton(onClick = { onDelete(initial) }) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Batal") }
            }
        )
    }
}

// ---------- Dialog tutup buku ----------

@Composable
fun TutupBukuDialog(
    bulan: BulanKas,
    bulanList: List<BulanKas>,
    transaksi: List<TransaksiKas>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val saldo = saldoAkhirBulan(bulan, bulanList, transaksi)
    val ringkasan = ringkasanBulan(transaksi, bulan.id)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tutup Buku " + bulanLabel(bulan.tahun, bulan.bulan) + "?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Total Masuk: " + formatRupiah(ringkasan.totalMasuk))
                Text("Total Keluar: " + formatRupiah(ringkasan.totalKeluar))
                Text(
                    "Saldo Akhir: " + formatRupiah(saldo.total),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tunai " + formatRupiah(saldo.tunai) + " - QRIS " + formatRupiah(saldo.qris),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Setelah ditutup, bulan ini terkunci dan tidak bisa diubah. Bulan berikutnya dibuka; saldo berjalan terus dari seluruh catatan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Tutup Buku") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

// ---------- Dialog tentang & restore ----------

@Composable
fun TentangDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("KasBulanan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Buku kas bulanan khusus apotek dengan pemisahan uang Tunai dan QRIS, siklus tutup buku bulanan, dan backup JSON. Semua data tersimpan di HP ini, tanpa akun dan tanpa server.")
                Text("Versi 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("\u00a9 2026 Achmad Hidayatullah", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

@Composable
fun RestoreConfirmDialog(
    data: BackupHelper.BackupData,
    jumlahBulanSaatIni: Int,
    jumlahTransaksiSaatIni: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pulihkan Data dari Backup?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "File berisi " + data.bulan.size + " bulan dan " + data.transaksi.size + " transaksi" +
                        (if (data.exportedAtMillis > 0) " (dibuat " + formatTanggal(data.exportedAtMillis) + ")" else "") + "."
                )
                Text(
                    "Data saat ini (" + jumlahBulanSaatIni + " bulan, " + jumlahTransaksiSaatIni +
                        " transaksi) akan DIGANTI seluruhnya.",
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Pulihkan Data", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
