package id.kasbulan.app.data

import id.kasbulan.app.JENIS_KELUAR
import id.kasbulan.app.JENIS_MASUK
import id.kasbulan.app.JENIS_TRANSFER
import id.kasbulan.app.SALURAN_QRIS
import id.kasbulan.app.SALURAN_TUNAI
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup satu file JSON: { app: "kasbulanan", version: 1, exportedAtMillis,
 * bulan: [...], transaksi: [...] }. Id bulan dipertahankan apa adanya,
 * dan transaksi divalidasi menunjuk ke bulan yang ikut terbawa.
 */
object BackupHelper {

    data class BackupData(
        val bulan: List<BulanKas>,
        val transaksi: List<TransaksiKas>,
        val exportedAtMillis: Long
    )

    fun buildBackupJson(
        bulan: List<BulanKas>,
        transaksi: List<TransaksiKas>,
        exportedAtMillis: Long
    ): String {
        val root = JSONObject()
        root.put("app", "kasbulanan")
        root.put("version", 1)
        root.put("exportedAtMillis", exportedAtMillis)
        val arrBulan = JSONArray()
        for (b in bulan) {
            val o = JSONObject()
            o.put("id", b.id)
            o.put("tahun", b.tahun)
            o.put("bulan", b.bulan)
            o.put("saldoAwalTunai", b.saldoAwalTunai)
            o.put("saldoAwalQris", b.saldoAwalQris)
            o.put("status", b.status)
            o.put("ditutupPada", b.ditutupPada ?: -1L)
            o.put("totalMasukFinal", b.totalMasukFinal)
            o.put("totalKeluarFinal", b.totalKeluarFinal)
            o.put("saldoAkhirTunaiFinal", b.saldoAkhirTunaiFinal)
            o.put("saldoAkhirQrisFinal", b.saldoAkhirQrisFinal)
            arrBulan.put(o)
        }
        root.put("bulan", arrBulan)
        val arrTransaksi = JSONArray()
        for (t in transaksi) {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("bulanId", t.bulanId)
            o.put("jenis", t.jenis)
            o.put("saluran", t.saluran)
            o.put("saluranTujuan", t.saluranTujuan ?: "")
            o.put("kategori", t.kategori)
            o.put("nominal", t.nominal)
            o.put("catatan", t.catatan)
            o.put("epoch", t.epoch)
            arrTransaksi.put(o)
        }
        root.put("transaksi", arrTransaksi)
        return root.toString()
    }

    /** Mengembalikan null untuk file yang bukan backup KasBulanan / rusak. */
    fun parseBackupJson(text: String): BackupData? {
        return try {
            val root = JSONObject(text)
            if (root.optString("app") != "kasbulanan") return null
            if (root.optInt("version", -1) != 1) return null
            val arrBulan = root.optJSONArray("bulan") ?: return null
            val arrTransaksi = root.optJSONArray("transaksi") ?: return null
            val bulan = ArrayList<BulanKas>()
            val idBulan = HashSet<Long>()
            for (i in 0 until arrBulan.length()) {
                val o = arrBulan.getJSONObject(i)
                val b = BulanKas(
                    id = o.getLong("id"),
                    tahun = o.getInt("tahun"),
                    bulan = o.getInt("bulan"),
                    saldoAwalTunai = o.getLong("saldoAwalTunai"),
                    saldoAwalQris = o.getLong("saldoAwalQris"),
                    status = o.optString("status", "BERJALAN"),
                    ditutupPada = o.optLong("ditutupPada", -1L).takeIf { it > 0 },
                    totalMasukFinal = o.optLong("totalMasukFinal", 0),
                    totalKeluarFinal = o.optLong("totalKeluarFinal", 0),
                    saldoAkhirTunaiFinal = o.optLong("saldoAkhirTunaiFinal", 0),
                    saldoAkhirQrisFinal = o.optLong("saldoAkhirQrisFinal", 0)
                )
                if (b.bulan !in 1..12) return null
                bulan.add(b)
                idBulan.add(b.id)
            }
            val transaksi = ArrayList<TransaksiKas>()
            val jenisSah = setOf(JENIS_MASUK, JENIS_KELUAR, JENIS_TRANSFER)
            val saluranSah = setOf(SALURAN_TUNAI, SALURAN_QRIS)
            for (i in 0 until arrTransaksi.length()) {
                val o = arrTransaksi.getJSONObject(i)
                val jenis = o.getString("jenis")
                val saluran = o.getString("saluran")
                val tujuanMentah = o.optString("saluranTujuan", "")
                val tujuan = tujuanMentah.takeIf { it.isNotEmpty() }
                if (jenis !in jenisSah) return null
                if (saluran !in saluranSah) return null
                if (tujuan != null && tujuan !in saluranSah) return null
                if (!o.has("bulanId") || o.getLong("bulanId") !in idBulan) return null
                transaksi.add(
                    TransaksiKas(
                        id = o.getLong("id"),
                        bulanId = o.getLong("bulanId"),
                        jenis = jenis,
                        saluran = saluran,
                        saluranTujuan = tujuan,
                        kategori = o.getString("kategori"),
                        nominal = o.getLong("nominal"),
                        catatan = o.optString("catatan", ""),
                        epoch = o.getLong("epoch")
                    )
                )
            }
            BackupData(bulan, transaksi, root.optLong("exportedAtMillis", 0))
        } catch (e: Exception) {
            null
        }
    }
}
