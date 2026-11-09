package id.kasbulan.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Bahasa antarmuka aplikasi. Mengikuti pola Apotek Lestari: hanya tampilan
 * yang diterjemahkan - nilai yang tersimpan (nama kategori, dsb.) tidak
 * pernah berubah, dan dokumen yang dihasilkan dari data tersimpan
 * (Excel) tetap berbahasa Indonesia.
 */
enum class BahasaApp { ID, EN }

val LocalBahasa = staticCompositionLocalOf { BahasaApp.ID }

/** Pilih teks sesuai bahasa aktif: tr("Simpan", "Save"). */
@Composable
fun tr(id: String, en: String): String =
    if (LocalBahasa.current == BahasaApp.ID) id else en

/** Nama kategori untuk DITAMPILKAN (nilai tersimpan tetap bahasa Indonesia). */
@Composable
fun tampilKategori(kategori: String): String =
    if (LocalBahasa.current == BahasaApp.ID) kategori
    else KATEGORI_EN[kategori] ?: kategori

/** Pilih teks untuk konteks non-Composable (handler, coroutine, launcher). */
fun teks(bahasa: BahasaApp, id: String, en: String): String =
    if (bahasa == BahasaApp.ID) id else en
