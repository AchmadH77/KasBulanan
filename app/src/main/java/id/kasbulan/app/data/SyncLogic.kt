package id.kasbulan.app.data

/**
 * Bentuk datar BulanKas untuk dokumen Firestore. Transaksi menunjuk
 * bulan lewat bulanRemoteId (bukan id lokal), karena id lokal Long
 * berbeda di setiap perangkat.
 */
data class BulanSync(
    val remoteId: String,
    val tahun: Int,
    val bulan: Int,
    val status: String,
    val ditutupPada: Long?,
    val totalMasukFinal: Long,
    val totalKeluarFinal: Long,
    val saldoAkhirTunaiFinal: Long,
    val saldoAkhirQrisFinal: Long,
    val updatedAt: Long,
    val terhapus: Boolean
)

/** Bentuk datar TransaksiKas untuk dokumen Firestore. */
data class TransaksiSync(
    val remoteId: String,
    val bulanRemoteId: String,
    val jenis: String,
    val saluran: String,
    val saluranTujuan: String?,
    val kategori: String,
    val nominal: Long,
    val catatan: String,
    val epoch: Long,
    val updatedAt: Long,
    val terhapus: Boolean
)

/**
 * Logika sync murni (tanpa Firebase, tanpa Android) supaya bisa diuji:
 * pemetaan entitas <-> bentuk sync <-> peta dokumen, dan aturan konflik.
 */
object SyncLogic {

    fun bulanKeSync(b: BulanKas): BulanSync = BulanSync(
        remoteId = b.remoteId,
        tahun = b.tahun,
        bulan = b.bulan,
        status = b.status,
        ditutupPada = b.ditutupPada,
        totalMasukFinal = b.totalMasukFinal,
        totalKeluarFinal = b.totalKeluarFinal,
        saldoAkhirTunaiFinal = b.saldoAkhirTunaiFinal,
        saldoAkhirQrisFinal = b.saldoAkhirQrisFinal,
        updatedAt = b.updatedAt,
        terhapus = b.terhapus
    )

    fun transaksiKeSync(t: TransaksiKas, bulanRemoteId: String): TransaksiSync = TransaksiSync(
        remoteId = t.remoteId,
        bulanRemoteId = bulanRemoteId,
        jenis = t.jenis,
        saluran = t.saluran,
        saluranTujuan = t.saluranTujuan,
        kategori = t.kategori,
        nominal = t.nominal,
        catatan = t.catatan,
        epoch = t.epoch,
        updatedAt = t.updatedAt,
        terhapus = t.terhapus
    )

    /** Hasil dari server selalu disimpan bersih (kotor = false). */
    fun bulanDariSync(s: BulanSync): BulanKas = BulanKas(
        id = 0,
        tahun = s.tahun,
        bulan = s.bulan,
        status = s.status,
        ditutupPada = s.ditutupPada,
        totalMasukFinal = s.totalMasukFinal,
        totalKeluarFinal = s.totalKeluarFinal,
        saldoAkhirTunaiFinal = s.saldoAkhirTunaiFinal,
        saldoAkhirQrisFinal = s.saldoAkhirQrisFinal,
        remoteId = s.remoteId,
        updatedAt = s.updatedAt,
        terhapus = s.terhapus,
        kotor = false
    )

    fun transaksiDariSync(s: TransaksiSync, bulanIdLokal: Long): TransaksiKas = TransaksiKas(
        id = 0,
        bulanId = bulanIdLokal,
        jenis = s.jenis,
        saluran = s.saluran,
        saluranTujuan = s.saluranTujuan,
        kategori = s.kategori,
        nominal = s.nominal,
        catatan = s.catatan,
        epoch = s.epoch,
        remoteId = s.remoteId,
        updatedAt = s.updatedAt,
        terhapus = s.terhapus,
        kotor = false
    )

    /** Aturan konflik: salinan server hanya menang bila lebih baru. */
    fun remoteMenang(updatedAtLokal: Long, updatedAtRemote: Long): Boolean =
        updatedAtRemote > updatedAtLokal

    fun kePeta(s: BulanSync): Map<String, Any?> = mapOf(
        "remoteId" to s.remoteId,
        "tahun" to s.tahun.toLong(),
        "bulan" to s.bulan.toLong(),
        "status" to s.status,
        "ditutupPada" to s.ditutupPada,
        "totalMasukFinal" to s.totalMasukFinal,
        "totalKeluarFinal" to s.totalKeluarFinal,
        "saldoAkhirTunaiFinal" to s.saldoAkhirTunaiFinal,
        "saldoAkhirQrisFinal" to s.saldoAkhirQrisFinal,
        "updatedAt" to s.updatedAt,
        "terhapus" to s.terhapus
    )

    fun kePeta(s: TransaksiSync): Map<String, Any?> = mapOf(
        "remoteId" to s.remoteId,
        "bulanRemoteId" to s.bulanRemoteId,
        "jenis" to s.jenis,
        "saluran" to s.saluran,
        "saluranTujuan" to s.saluranTujuan,
        "kategori" to s.kategori,
        "nominal" to s.nominal,
        "catatan" to s.catatan,
        "epoch" to s.epoch,
        "updatedAt" to s.updatedAt,
        "terhapus" to s.terhapus
    )

    private fun Map<String, Any?>.teks(kunci: String): String? = this[kunci] as? String
    private fun Map<String, Any?>.angka(kunci: String): Long? =
        (this[kunci] as? Number)?.toLong()

    /** Null bila peta tidak lengkap / bukan dokumen sync yang sah. */
    fun bulanDariPeta(m: Map<String, Any?>): BulanSync? {
        val remoteId = m.teks("remoteId")?.takeIf { it.isNotBlank() } ?: return null
        return BulanSync(
            remoteId = remoteId,
            tahun = m.angka("tahun")?.toInt() ?: return null,
            bulan = m.angka("bulan")?.toInt() ?: return null,
            status = m.teks("status") ?: return null,
            ditutupPada = m.angka("ditutupPada"),
            totalMasukFinal = m.angka("totalMasukFinal") ?: 0,
            totalKeluarFinal = m.angka("totalKeluarFinal") ?: 0,
            saldoAkhirTunaiFinal = m.angka("saldoAkhirTunaiFinal") ?: 0,
            saldoAkhirQrisFinal = m.angka("saldoAkhirQrisFinal") ?: 0,
            updatedAt = m.angka("updatedAt") ?: 0,
            terhapus = m["terhapus"] as? Boolean ?: false
        )
    }

    fun transaksiDariPeta(m: Map<String, Any?>): TransaksiSync? {
        val remoteId = m.teks("remoteId")?.takeIf { it.isNotBlank() } ?: return null
        return TransaksiSync(
            remoteId = remoteId,
            bulanRemoteId = m.teks("bulanRemoteId")?.takeIf { it.isNotBlank() } ?: return null,
            jenis = m.teks("jenis") ?: return null,
            saluran = m.teks("saluran") ?: return null,
            saluranTujuan = m.teks("saluranTujuan"),
            kategori = m.teks("kategori") ?: return null,
            nominal = m.angka("nominal") ?: return null,
            catatan = m.teks("catatan") ?: "",
            epoch = m.angka("epoch") ?: return null,
            updatedAt = m.angka("updatedAt") ?: 0,
            terhapus = m["terhapus"] as? Boolean ?: false
        )
    }
}
