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
import kotlinx.coroutines.flow.Flow

/**
 * Satu bulan kas. Saat status BERJALAN, transaksi bulan ini masih bisa
 * diubah; setelah DITUTUP, angka final di kolom terakhir menjadi arsip
 * terkunci dan tidak pernah dihitung ulang dari transaksi.
 */
@Entity(tableName = "bulan_kas")
data class BulanKas(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tahun: Int,
    val bulan: Int,
    val saldoAwalTunai: Long,
    val saldoAwalQris: Long,
    val status: String = "BERJALAN",
    val ditutupPada: Long? = null,
    val totalMasukFinal: Long = 0,
    val totalKeluarFinal: Long = 0,
    val saldoAkhirTunaiFinal: Long = 0,
    val saldoAkhirQrisFinal: Long = 0
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
    val epoch: Long
)

@Dao
interface KasDao {
    @Query("SELECT * FROM bulan_kas ORDER BY tahun, bulan")
    fun bulanFlow(): Flow<List<BulanKas>>

    @Query("SELECT * FROM transaksi_kas ORDER BY epoch, id")
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

    @Query("SELECT * FROM bulan_kas ORDER BY tahun, bulan")
    suspend fun allBulanOnce(): List<BulanKas>

    @Query("SELECT * FROM transaksi_kas ORDER BY epoch, id")
    suspend fun allTransaksiOnce(): List<TransaksiKas>

    @Query("DELETE FROM bulan_kas")
    suspend fun clearBulan()

    @Query("DELETE FROM transaksi_kas")
    suspend fun clearTransaksi()
}

@Database(entities = [BulanKas::class, TransaksiKas::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kasDao(): KasDao

    companion object {
        fun create(context: Context): AppDatabase {
            // Jaring pengaman dulu, SEBELUM Room membuka database: bila
            // aplikasi baru saja diperbarui, salin file database apa
            // adanya supaya migrasi yang gagal tidak menghilangkan data.
            DatabaseSnapshot.snapshotBeforeUpdate(context.applicationContext, "kasbulan.db")
            return Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "kasbulan.db")
                .build()
        }
    }
}
