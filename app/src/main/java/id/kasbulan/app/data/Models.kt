package id.kasbulan.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * Satu bulan kas. Bulan hanya mengelompokkan catatan dan menjadi unit
 * tutup buku - bulan TIDAK membawa saldo awal: saldo kas selalu kontinu,
 * yaitu jumlah seluruh catatan dari semua bulan. Saat status BERJALAN,
 * transaksi bulan ini masih bisa diubah; setelah DITUTUP, angka final di
 * kolom terakhir menjadi arsip terkunci.
 */
@Entity(tableName = "bulan_kas")
data class BulanKas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tahun: Int,
    val bulan: Int,
    val status: String = "BERJALAN",
    val ditutupPada: Long? = null,
    val totalMasukFinal: Long = 0,
    val totalKeluarFinal: Long = 0,
    val saldoAkhirTunaiFinal: Long = 0,
    val saldoAkhirQrisFinal: Long = 0,
    // Kolom sinkronisasi online (docs/ONLINE.md): identitas baris di
    // server, cap waktu ubah terakhir untuk aturan konflik, tanda hapus
    // lunak supaya penghapusan ikut tersinkron, dan penanda baris kotor
    // (belum terkirim ke server).
    val remoteId: String = "",
    val updatedAt: Long = 0,
    val terhapus: Boolean = false,
    val kotor: Boolean = true
)

/**
 * Satu catatan transaksi. jenis: MASUK, KELUAR, atau TRANSFER.
 * saluran: dompet asal (TUNAI/QRIS); saluranTujuan hanya untuk TRANSFER.
 */
@Entity(tableName = "transaksi_kas")
data class TransaksiKas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bulanId: Long,
    val jenis: String,
    val saluran: String,
    val saluranTujuan: String? = null,
    val kategori: String,
    val nominal: Long,
    val catatan: String = "",
    val epoch: Long,
    // Kolom sinkronisasi online - lihat BulanKas.
    val remoteId: String = "",
    val updatedAt: Long = 0,
    val terhapus: Boolean = false,
    val kotor: Boolean = true
)

@Dao
interface KasDao {
    // Semua query baca biasa menyembunyikan baris yang terhapus lunak.
    @Query("SELECT * FROM bulan_kas WHERE terhapus = 0 ORDER BY tahun, bulan")
    fun bulanFlow(): Flow<List<BulanKas>>

    @Query("SELECT * FROM transaksi_kas WHERE terhapus = 0 ORDER BY epoch, id")
    fun transaksiFlow(): Flow<List<TransaksiKas>>

    @Insert
    suspend fun insertBulan(bulan: BulanKas): Long

    @Update
    suspend fun updateBulan(bulan: BulanKas)

    @Insert
    suspend fun insertTransaksi(transaksi: TransaksiKas): Long

    @Update
    suspend fun updateTransaksi(transaksi: TransaksiKas)

    @Delete
    suspend fun deleteTransaksi(transaksi: TransaksiKas)

    @Query("SELECT * FROM bulan_kas WHERE terhapus = 0 ORDER BY tahun, bulan")
    suspend fun allBulanOnce(): List<BulanKas>

    @Query("SELECT * FROM transaksi_kas WHERE terhapus = 0 ORDER BY epoch, id")
    suspend fun allTransaksiOnce(): List<TransaksiKas>

    // ---- Akses lapisan sync (tanpa saringan hapus lunak) ----
    @Query("SELECT * FROM bulan_kas ORDER BY tahun, bulan")
    suspend fun semuaBulanMentah(): List<BulanKas>

    @Query("SELECT * FROM transaksi_kas ORDER BY epoch, id")
    suspend fun semuaTransaksiMentah(): List<TransaksiKas>

    @Query("SELECT * FROM bulan_kas WHERE kotor = 1 ORDER BY tahun, bulan")
    suspend fun bulanKotor(): List<BulanKas>

    @Query("SELECT * FROM transaksi_kas WHERE kotor = 1 ORDER BY epoch, id")
    suspend fun transaksiKotor(): List<TransaksiKas>

    @Query("SELECT * FROM bulan_kas WHERE remoteId = :remoteId LIMIT 1")
    suspend fun bulanByRemoteId(remoteId: String): BulanKas?

    @Query("SELECT * FROM transaksi_kas WHERE remoteId = :remoteId LIMIT 1")
    suspend fun transaksiByRemoteId(remoteId: String): TransaksiKas?

    // Tandai bersih hanya bila baris tidak berubah sejak didorong
    // (updatedAt masih sama dengan yang terkirim).
    @Query("UPDATE bulan_kas SET kotor = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun tandaiBulanBersih(id: Long, updatedAt: Long)

    @Query("UPDATE transaksi_kas SET kotor = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun tandaiTransaksiBersih(id: Long, updatedAt: Long)

    // Baris terhapus lunak yang sudah terkirim boleh dibuang permanen.
    @Query("DELETE FROM transaksi_kas WHERE terhapus = 1 AND kotor = 0")
    suspend fun buangTransaksiTerhapus()

    @Query("DELETE FROM bulan_kas WHERE terhapus = 1 AND kotor = 0")
    suspend fun buangBulanTerhapus()

    @Query("DELETE FROM bulan_kas")
    suspend fun clearBulan()

    @Query("DELETE FROM transaksi_kas")
    suspend fun clearTransaksi()
}

/** v1 -> v2: kolom sinkronisasi online; baris lama diberi remoteId turunan id. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE bulan_kas ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE bulan_kas ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE bulan_kas ADD COLUMN terhapus INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE bulan_kas ADD COLUMN kotor INTEGER NOT NULL DEFAULT 1")
        db.execSQL("UPDATE bulan_kas SET remoteId = 'bulan-' || id")
        db.execSQL("ALTER TABLE transaksi_kas ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE transaksi_kas ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transaksi_kas ADD COLUMN terhapus INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE transaksi_kas ADD COLUMN kotor INTEGER NOT NULL DEFAULT 1")
        db.execSQL("UPDATE transaksi_kas SET remoteId = 'transaksi-' || id")
    }
}

@Database(entities = [BulanKas::class, TransaksiKas::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kasDao(): KasDao

    companion object {
        fun create(context: Context): AppDatabase {
            // Jaring pengaman dulu, SEBELUM Room membuka database: bila
            // aplikasi baru saja diperbarui, salin file database apa
            // adanya supaya migrasi yang gagal tidak menghilangkan data.
            DatabaseSnapshot.snapshotBeforeUpdate(context.applicationContext, "kasbulan.db")
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "kasbulan.db")
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
