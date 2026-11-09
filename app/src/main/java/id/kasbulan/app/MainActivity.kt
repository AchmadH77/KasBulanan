package id.kasbulan.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import id.kasbulan.app.data.FirebaseKit
import id.kasbulan.app.data.HasilSync
import id.kasbulan.app.data.SyncManager
import id.kasbulan.app.data.jadwalkanSyncBerkala
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
import kotlinx.coroutines.delay
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
            var bahasa by remember {
                mutableStateOf(if (prefs.getString("bahasa", "id") == "en") BahasaApp.EN else BahasaApp.ID)
            }
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
                    },
                    bahasa = bahasa,
                    onBahasaChange = {
                        bahasa = if (bahasa == BahasaApp.ID) BahasaApp.EN else BahasaApp.ID
                        prefs.edit().putString("bahasa", if (bahasa == BahasaApp.ID) "id" else "en").apply()
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
    onNamaApotekChange: (String) -> Unit,
    bahasa: BahasaApp,
    onBahasaChange: () -> Unit
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
        bulanLabel(ym.year, ym.monthValue, bahasa)
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
        lastBackupAt == 0L && transaksiList.isNotEmpty() -> tr("Belum pernah dibackup", "Never backed up")
        lastBackupAt > 0L && lastBackupTanda != tandaData -> tr("Ada perubahan sejak backup terakhir", "Data changed since the last backup")
        else -> null
    }

    // ---- Mode Online (lapisan opsional, docs/ONLINE.md) ----
    var showInfoOnline by remember { mutableStateOf(false) }
    var syncManual by remember { mutableStateOf(false) }
    val onlineSiap = remember { FirebaseKit.siap(context) }
    var emailOnline by remember {
        mutableStateOf(if (onlineSiap) FirebaseKit.emailMasuk() else null)
    }
    var syncTerakhir by remember {
        mutableLongStateOf(prefs.getLong("online_sync_terakhir", 0L))
    }
    var onlineSibuk by remember { mutableStateOf(false) }
    var pesanOnline by remember { mutableStateOf<String?>(null) }
    val googleSiap = remember { onlineSiap && FirebaseKit.googleSiap(context) }
    var lewatiLogin by remember { mutableStateOf(false) }
    var statusSync by remember {
        mutableStateOf(
            when {
                !onlineSiap -> StatusSync.NONAKTIF
                emailOnline == null -> StatusSync.BELUM_MASUK
                syncTerakhir > 0L -> StatusSync.BERHASIL
                else -> StatusSync.PROSES
            }
        )
    }

    fun jalankanSync() {
        if (onlineSibuk) return
        onlineSibuk = true
        statusSync = StatusSync.PROSES
        scope.launch {
            when (val hasil = SyncManager(repository).sinkronSekarang(context)) {
                is HasilSync.Berhasil -> {
                    val kini = System.currentTimeMillis()
                    prefs.edit().putLong("online_sync_terakhir", kini).apply()
                    syncTerakhir = kini
                    statusSync = StatusSync.BERHASIL
                    pesanOnline = teks(bahasa, "Sinkron berhasil: ", "Sync complete: ") + hasil.didorong +
                        teks(bahasa, " terkirim, ", " sent, ") + hasil.ditarik + teks(bahasa, " diterima", " received")
                }
                is HasilSync.Gagal -> {
                    statusSync = StatusSync.GAGAL
                    pesanOnline = teks(bahasa, "Sinkron gagal: ", "Sync failed: ") + hasil.pesan
                }
                HasilSync.BelumMasuk -> pesanOnline = teks(bahasa, "Belum masuk", "Not signed in")
                HasilSync.BelumDikonfigurasi -> pesanOnline = teks(bahasa, "Firebase belum dikonfigurasi", "Firebase is not configured")
            }
            if (syncManual) {
                pesan = pesanOnline
                syncManual = false
            }
            onlineSibuk = false
        }
    }

    // Handler masuk/daftar dipakai gerbang login dan layar Mode Online.
    fun aksiMasuk(email: String, sandi: String) {
        onlineSibuk = true
        scope.launch {
            try {
                emailOnline = FirebaseKit.masukDenganEmail(email.trim(), sandi)
                pesanOnline = teks(bahasa, "Berhasil masuk", "Signed in")
                onlineSibuk = false
                jalankanSync()
            } catch (e: Exception) {
                pesanOnline = teks(bahasa, "Gagal masuk: ", "Sign-in failed: ") + (e.message ?: teks(bahasa, "periksa email dan kata sandi", "check your email and password"))
                onlineSibuk = false
            }
        }
    }

    fun aksiDaftar(email: String, sandi: String) {
        onlineSibuk = true
        scope.launch {
            try {
                emailOnline = FirebaseKit.daftarDenganEmail(email.trim(), sandi)
                pesanOnline = teks(bahasa, "Akun dibuat dan sudah masuk", "Account created and signed in")
                onlineSibuk = false
                jalankanSync()
            } catch (e: Exception) {
                pesanOnline = teks(bahasa, "Gagal daftar: ", "Sign-up failed: ") + (e.message ?: teks(bahasa, "coba lagi", "try again"))
                onlineSibuk = false
            }
        }
    }

    fun aksiMasukGoogle() {
        onlineSibuk = true
        scope.launch {
            try {
                emailOnline = FirebaseKit.masukDenganGoogle(context)
                pesanOnline = teks(bahasa, "Berhasil masuk dengan Google", "Signed in with Google")
                statusSync = StatusSync.PROSES
                onlineSibuk = false
                jalankanSync()
            } catch (e: Exception) {
                pesanOnline = teks(bahasa, "Gagal masuk dengan Google: ", "Google sign-in failed: ") + (e.message ?: teks(bahasa, "dibatalkan", "cancelled"))
                onlineSibuk = false
            }
        }
    }

    fun aksiResetSandi(email: String) {
        onlineSibuk = true
        scope.launch {
            try {
                FirebaseKit.kirimResetSandi(email.trim())
                pesanOnline = teks(bahasa, "Email reset kata sandi terkirim ke ", "Password reset email sent to ") + email.trim()
            } catch (e: Exception) {
                pesanOnline = teks(bahasa, "Gagal mengirim reset: ", "Could not send the reset: ") + (e.message ?: teks(bahasa, "periksa alamat email", "check the email address"))
            }
            onlineSibuk = false
        }
    }

    LaunchedEffect(Unit) {
        if (onlineSiap) jadwalkanSyncBerkala(context)
    }
    // Sync otomatis beberapa detik setelah data berubah (saat sudah masuk).
    LaunchedEffect(transaksiList, bulanList, emailOnline) {
        if (onlineSiap && emailOnline != null) {
            delay(5000)
            jalankanSync()
        }
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
                pesan = if (ok) teks(bahasa, "Ekspor Excel tersimpan", "Excel export saved") else teks(bahasa, "Ekspor Excel gagal", "Excel export failed")
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
                pesan = if (ok) teks(bahasa, "Backup tersimpan", "Backup saved") else teks(bahasa, "Backup gagal", "Backup failed")
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
                else pesan = teks(bahasa, "File bukan backup KasBulanan atau rusak", "The file is not a KasBulanan backup or is damaged")
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

    CompositionLocalProvider(LocalBahasa provides bahasa) {
    // Gerbang login (G2): hanya saat Firebase terkonfigurasi, belum
    // ada yang masuk, dan tamu belum memilih Lewati pada sesi ini.
    val perluLogin = onlineSiap && emailOnline == null && !lewatiLogin
    if (perluLogin) {
        LayarLogin(
            sibuk = onlineSibuk,
            pesanStatus = pesanOnline,
            googleSiap = googleSiap,
            onMasuk = { email, sandi -> aksiMasuk(email, sandi) },
            onDaftar = { email, sandi -> aksiDaftar(email, sandi) },
            onMasukGoogle = { aksiMasukGoogle() },
            onResetSandi = { aksiResetSandi(it) },
            onLewati = { lewatiLogin = true }
        )
    } else {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!showForm) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ItemPillNav(Icons.Default.Home, tr("Beranda", "Home"), tab == 0) {
                                showRekapHariIni = false; tab = 0
                            }
                            ItemPillNav(Icons.Default.History, tr("Riwayat", "History"), tab == 1) {
                                showRekapHariIni = false; tab = 1
                            }
                            ItemPillNav(Icons.Default.Assessment, tr("Laporan", "Report"), tab == 2) {
                                showRekapHariIni = false; tab = 2
                            }
                            ItemPillNav(Icons.Default.MoreHoriz, tr("Lainnya", "More"), tab == 3) {
                                showRekapHariIni = false; tab = 3
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (tab == 0 && bulanBerjalan != null && !showRekapHariIni && !showForm) {
                ExtendedFloatingActionButton(
                    onClick = { bukaForm(null) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(tr("Catat", "Record")) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    val arah = if (targetState > initialState) 1 else -1
                    (slideInHorizontally(tween(300)) { lebar -> arah * lebar / 5 } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(tween(300)) { lebar -> -arah * lebar / 5 } + fadeOut(tween(300)))
                },
                label = "pindahTab"
            ) { tabTampil ->
            when (tabTampil) {
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
                    statusSync = statusSync,
                    onBukaOnline = {
                        when {
                            emailOnline != null -> { syncManual = true; jalankanSync() }
                            onlineSiap -> lewatiLogin = false
                        }
                    },
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
                            pesan = teks(bahasa, "Belum ada data untuk diekspor", "No data to export yet")
                        } else {
                            eksporLauncher.launch("kasbulanan-ekspor.xlsx")
                        }
                    },
                    onRestore = { restoreLauncher.launch(arrayOf("application/json")) },
                    onTentang = { showTentang = true },
                    subjudulSync = when {
                        !onlineSiap -> tr("Belum dikonfigurasi", "Not configured")
                        emailOnline != null -> if (syncTerakhir > 0L) {
                            tr("Terhubung: ", "Connected: ") + emailOnline + " - " +
                                tr("sinkron ", "synced ") + formatTanggalWaktu(syncTerakhir)
                        } else {
                            tr("Terhubung: ", "Connected: ") + emailOnline
                        }
                        else -> tr("Masuk untuk sinkron otomatis", "Sign in for automatic sync")
                    },
                    onBarisSync = {
                        when {
                            emailOnline != null -> { syncManual = true; jalankanSync() }
                            onlineSiap -> lewatiLogin = false
                            else -> showInfoOnline = true
                        }
                    },
                    emailAkun = emailOnline,
                    onlineSiap = onlineSiap,
                    syncTerakhir = syncTerakhir,
                    onKeluarAkun = {
                        FirebaseKit.keluar()
                        emailOnline = null
                        statusSync = StatusSync.BELUM_MASUK
                        pesan = teks(bahasa, "Sudah keluar - data di HP ini tidak berubah", "Signed out - the data on this phone is unchanged")
                    },
                    bahasa = bahasa,
                    onGantiBahasa = onBahasaChange
                )
            }
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
                pesan = teks(bahasa, "Buku ", "Book ") + bulanLabel(target.tahun, target.bulan, bahasa) + teks(bahasa, " ditutup", " closed")
            }
        )
    }
    if (showTentang) {
        TentangDialog(onDismiss = { showTentang = false })
    }
    if (showInfoOnline) {
        AlertDialog(
            onDismissRequest = { showInfoOnline = false },
            title = { Text(tr("Sinkronisasi Online", "Online Sync")) },
            text = {
                Text(
                    tr(
                        "Sinkronisasi memakai Firebase. Pemilik aplikasi perlu membuat project Firebase, menaruh file google-services.json ke dalam aplikasi, lalu menyalakan login email dan database Firestore. Selama belum diatur, aplikasi berjalan offline seperti biasa dan tidak ada yang berubah.",
                        "Sync uses Firebase. The app owner needs to create a Firebase project, place the google-services.json file in the app, then enable email sign-in and the Firestore database. Until it is set up, the app runs offline as usual and nothing changes."
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoOnline = false }) { Text(tr("Tutup", "Close")) }
            }
        )
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
                pesan = teks(bahasa, "Data dipulihkan dari backup", "Data restored from backup")
            }
        )
    }
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
            TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) }
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
    var showKategoriDialog by remember { mutableStateOf(false) }
    var teksKategori by remember { mutableStateOf("") }
    var jumlahTersimpanLagi by remember { mutableIntStateOf(0) }

    LaunchedEffect(jenis) {
        val daftar = if (jenis == JENIS_MASUK) KATEGORI_MASUK else KATEGORI_KELUAR
        val daftarLawan = if (jenis == JENIS_MASUK) KATEGORI_KELUAR else KATEGORI_MASUK
        // Kategori bawaan mengikuti jenis; kategori ketik manual dipertahankan.
        if (kategori in daftarLawan || kategori.isBlank()) kategori = daftar.first()
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
                Icon(Icons.Default.ArrowBack, contentDescription = tr("Kembali", "Back"))
            }
            Text(
                if (initial == null) tr("Catat Transaksi", "Record Transaction") else tr("Ubah Transaksi", "Edit Transaction"),
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
                listOf(JENIS_MASUK to tr("Masuk", "Income"), JENIS_KELUAR to tr("Keluar", "Expense")).forEachIndexed { i, (nilai, label) ->
                    SegmentedButton(
                        selected = jenis == nilai,
                        onClick = { jenis = nilai },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = 2)
                    ) { Text(label) }
                }
            }
            Text(tr("Saluran Uang", "Money Channel"), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                listOf(SALURAN_TUNAI to tr("Tunai", "Cash"), SALURAN_QRIS to "QRIS").forEachIndexed { i, (nilai, label) ->
                    SegmentedButton(
                        selected = saluran == nilai,
                        onClick = { saluran = nilai },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = 2)
                    ) { Text(label) }
                }
            }
            MoneyField(tr("Nominal", "Amount"), nominalRaw) { nominalRaw = it }
            Text(tr("Kategori", "Category"), style = MaterialTheme.typography.labelLarge)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                daftarKategori.forEach { k ->
                    FilterChip(
                        selected = kategori == k,
                        onClick = { kategori = k },
                        label = { Text(tampilKategori(k)) }
                    )
                }
                if (kategori !in daftarKategori) {
                    FilterChip(
                        selected = true,
                        onClick = {
                            teksKategori = kategori
                            showKategoriDialog = true
                        },
                        label = { Text(kategori) }
                    )
                }
                val warnaEmas = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) {
                    Color(0xFFD4AF37)
                } else {
                    Color(0xFF96761C)
                }
                FilterChip(
                    selected = false,
                    onClick = {
                        teksKategori = if (kategori !in daftarKategori) kategori else ""
                        showKategoriDialog = true
                    },
                    label = { Text(tr("+ Buat Kategori", "+ New Category")) },
                    colors = FilterChipDefaults.filterChipColors(labelColor = warnaEmas),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = warnaEmas,
                        borderWidth = 1.dp
                    )
                )
            }
            Text(tr("Tanggal", "Date"), style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatTanggal(tanggal, LocalBahasa.current))
            }
            Text(tr("Catatan (boleh kosong)", "Note (optional)"), style = MaterialTheme.typography.labelLarge)
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
                Text(tr("Simpan", "Save"), style = MaterialTheme.typography.titleMedium)
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
                    Text(tr("Simpan & Catat Lagi", "Save & Record Another"))
                }
                if (jumlahTersimpanLagi > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        jumlahTersimpanLagi.toString() + tr(" catatan tersimpan - lanjutkan berikutnya", " records saved - keep going"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            TextButton(onClick = onBack) { Text(tr("Batal", "Cancel")) }
            if (initial != null) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text(tr("Hapus", "Delete"), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (showKategoriDialog) {
        AlertDialog(
            onDismissRequest = { showKategoriDialog = false },
            title = { Text(tr("Kategori Baru", "New Category")) },
            text = {
                OutlinedTextField(
                    value = teksKategori,
                    onValueChange = { teksKategori = it },
                    label = { Text(tr("Nama kategori", "Category name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = teksKategori.isNotBlank(),
                    onClick = {
                        kategori = teksKategori.trim()
                        showKategoriDialog = false
                    }
                ) { Text(tr("Pakai", "Use")) }
            },
            dismissButton = {
                TextButton(onClick = { showKategoriDialog = false }) { Text(tr("Batal", "Cancel")) }
            }
        )
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
            title = { Text(tr("Hapus transaksi ini?", "Delete this transaction?")) },
            text = { Text(tr("Catatan akan dihapus permanen dan saldo dihitung ulang.", "The record will be permanently deleted and the balance recomputed.")) },
            confirmButton = {
                TextButton(onClick = { onDelete(initial) }) {
                    Text(tr("Hapus", "Delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(tr("Batal", "Cancel")) }
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
                    if (initial == null) tr("Transfer Antar Saldo", "Transfer Between Balances") else tr("Ubah Transfer", "Edit Transfer"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    tr("Perpindahan uang antar dompet - bukan pemasukan atau pengeluaran, jadi total kas tidak berubah.", "Moving money between balances - not income or an expense, so the cash total does not change."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(true to "QRIS \u2192 " + tr("Tunai", "Cash"), false to tr("Tunai", "Cash") + " \u2192 QRIS").forEachIndexed { i, (nilai, label) ->
                        SegmentedButton(
                            selected = dariQris == nilai,
                            onClick = { dariQris = nilai },
                            shape = SegmentedButtonDefaults.itemShape(index = i, count = 2)
                        ) { Text(label) }
                    }
                }
                MoneyField(tr("Nominal yang dipindahkan", "Amount to move"), nominalRaw) { nominalRaw = it }
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Tanggal: ", "Date: ") + formatTanggal(tanggal, LocalBahasa.current))
                }
                OutlinedTextField(
                    value = catatan,
                    onValueChange = { catatan = it },
                    label = { Text(tr("Catatan (boleh kosong)", "Note (optional)")) },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    if (initial != null) {
                        TextButton(onClick = { confirmDelete = true }) {
                            Text(tr("Hapus", "Delete"), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) }
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
                    ) { Text(tr("Simpan", "Save")) }
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
            title = { Text(tr("Hapus transfer ini?", "Delete this transfer?")) },
            text = { Text(tr("Catatan akan dihapus permanen dan saldo dihitung ulang.", "The record will be permanently deleted and the balance recomputed.")) },
            confirmButton = {
                TextButton(onClick = { onDelete(initial) }) {
                    Text(tr("Hapus", "Delete"), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(tr("Batal", "Cancel")) }
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
        title = { Text(tr("Tutup Buku ", "Close the Book: ") + bulanLabel(bulan.tahun, bulan.bulan, LocalBahasa.current) + "?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Total Masuk: ", "Total Income: ") + formatRupiah(ringkasan.totalMasuk))
                Text(tr("Total Keluar: ", "Total Expenses: ") + formatRupiah(ringkasan.totalKeluar))
                Text(
                    tr("Saldo Akhir: ", "Closing Balance: ") + formatRupiah(saldo.total),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    tr("Tunai ", "Cash ") + formatRupiah(saldo.tunai) + " - QRIS " + formatRupiah(saldo.qris),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    tr("Setelah ditutup, bulan ini terkunci dan tidak bisa diubah. Bulan berikutnya dibuka; saldo berjalan terus dari seluruh catatan.", "Once closed, this month is locked and cannot be changed. The next month opens; the balance keeps running from all records."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(tr("Tutup Buku", "Close Book")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) }
        }
    )
}

// ---------- Dialog tentang & restore ----------

@Composable
fun TentangDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF0A3A24), Color(0xFF0E7A46))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "K",
                                    color = Color(0xFF5ED4A0),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD4AF37))
                                ) {
                                    Text(
                                        "Rp",
                                        color = Color(0xFF1E1804),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "KasBulanan",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    tr("Buku Kas Bulanan", "Monthly Cash Book"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                tr("VERSI 1.0.0", "VERSION 1.0.0"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                tr("SEJAK 2026", "SINCE 2026"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Text(tr("Buku kas bulanan untuk usaha dan pemakaian sehari-hari, dengan pemisahan uang Tunai dan QRIS, siklus tutup buku bulanan, backup JSON, dan ekspor Excel.", "A monthly cash book for businesses and everyday use, with Cash and QRIS kept separate, a monthly book-closing cycle, JSON backup, and Excel export."))
                Text(tr("Semua data tersimpan di HP ini, tanpa akun dan tanpa server.", "All data is stored on this phone, with no account and no server."))
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Pengembang", "Developer"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text("Achmad Hidayatullah", fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Lisensi", "License"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(tr("Hak cipta dilindungi", "All rights reserved"), fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "\u00a9 2026 Achmad Hidayatullah",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(tr("Tutup", "Close")) }
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
        title = { Text(tr("Pulihkan Data dari Backup?", "Restore Data from Backup?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    tr("File berisi ", "The file contains ") + data.bulan.size + tr(" bulan dan ", " months and ") + data.transaksi.size + tr(" transaksi", " transactions") +
                        (if (data.exportedAtMillis > 0) tr(" (dibuat ", " (created ") + formatTanggal(data.exportedAtMillis, LocalBahasa.current) + ")" else "") + "."
                )
                Text(
                    tr("Data saat ini (", "Current data (") + jumlahBulanSaatIni + tr(" bulan, ", " months, ") + jumlahTransaksiSaatIni +
                        tr(" transaksi) akan DIGANTI seluruhnya.", " transactions) will be REPLACED entirely."),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(tr("Pulihkan Data", "Restore Data"), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) }
        }
    )
}

/**
 * Satu tab pada menu bawah pill melayang (B1): tab aktif melebar menjadi
 * pill warna utama berisi ikon + label; tab lain hanya ikon. Lebar pill
 * dianimasikan dengan animateContentSize.
 */
@Composable
private fun ItemPillNav(
    ikon: ImageVector,
    label: String,
    aktif: Boolean,
    onClick: () -> Unit
) {
    val skema = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (aktif) skema.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .animateContentSize()
            .padding(horizontal = if (aktif) 16.dp else 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            ikon,
            contentDescription = label,
            tint = if (aktif) skema.onPrimary else skema.onSurfaceVariant
        )
        if (aktif) {
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = skema.onPrimary
            )
        }
    }
}
