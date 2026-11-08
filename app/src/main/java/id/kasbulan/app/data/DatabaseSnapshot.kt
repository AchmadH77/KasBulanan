package id.kasbulan.app.data

import android.content.Context
import java.io.File

/**
 * Jaring pengaman data: tepat sebelum database dibuka setelah aplikasi
 * DIPERBARUI, file database disalin utuh ke folder internal "db-snapshots"
 * (maks. 3 salinan terbaru). Migrasi database adalah momen paling berisiko
 * bagi data; salinan ini jalan pulang bila sebuah pembaruan merusak
 * database. Salinan ikut hilang bila aplikasi di-uninstall, jadi ini
 * BUKAN pengganti backup JSON manual.
 *
 * Semua kegagalan ditelan: penyalinan tidak boleh pernah menghalangi
 * aplikasi untuk menyala.
 */
object DatabaseSnapshot {

    private const val PREFS = "kasbulan_db_guard"
    private const val KEY_LAST_UPDATE = "last_update_time"
    private const val MAX_SNAPSHOTS = 3
    private const val SNAPSHOT_DIR = "db-snapshots"

    fun snapshotBeforeUpdate(context: Context, dbName: String) {
        try {
            val lastUpdate = context.packageManager
                .getPackageInfo(context.packageName, 0).lastUpdateTime
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val previous = prefs.getLong(KEY_LAST_UPDATE, 0L)
            if (previous != 0L && previous != lastUpdate) {
                val dbFile = context.getDatabasePath(dbName)
                if (dbFile.exists()) {
                    val dir = File(context.filesDir, SNAPSHOT_DIR)
                    dir.mkdirs()
                    val stamp = System.currentTimeMillis().toString()
                    for (suffix in listOf("", "-wal", "-shm")) {
                        val src = File(dbFile.path + suffix)
                        if (src.exists()) {
                            src.copyTo(File(dir, stamp + "-" + dbName + suffix), overwrite = true)
                        }
                    }
                    snapshotsToPrune(dir.list()?.toList() ?: emptyList(), dbName, MAX_SNAPSHOTS)
                        .forEach { File(dir, it).delete() }
                }
            }
            if (previous != lastUpdate) {
                prefs.edit().putLong(KEY_LAST_UPDATE, lastUpdate).apply()
            }
        } catch (e: Exception) {
            // Jaring pengaman tidak boleh pernah menjatuhkan aplikasi.
        }
    }

    /**
     * Nama file snapshot yang harus dihapus supaya hanya [keep] salinan
     * terbaru yang tersisa. Setiap salinan adalah sekelompok file yang
     * berbagi awalan stempel waktu epoch (nama file = "<stamp>-<db>",
     * "<stamp>-<db>-wal", "<stamp>-<db>-shm"), jadi pengelompokan memakai
     * teks sebelum tanda minus pertama dan urutan leksikografis stempel
     * sama dengan urutan waktu. File milik database lain diabaikan.
     */
    fun snapshotsToPrune(fileNames: List<String>, dbName: String, keep: Int): List<String> {
        val groups = fileNames.filter { it.contains("-" + dbName) }
            .groupBy { it.substringBefore("-") }
        val stamps = groups.keys.sorted()
        val excess = stamps.size - keep.coerceAtLeast(0)
        if (excess <= 0) return emptyList()
        return stamps.take(excess).flatMap { groups.getValue(it) }
    }
}
