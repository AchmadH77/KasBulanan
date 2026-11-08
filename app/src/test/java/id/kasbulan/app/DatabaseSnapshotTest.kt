package id.kasbulan.app

import id.kasbulan.app.data.DatabaseSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseSnapshotTest {

    /** Satu salinan snapshot utuh = db + wal + shm dengan stempel sama. */
    private fun names(vararg stamps: String): List<String> =
        stamps.flatMap { s -> listOf("$s-kasbulan.db", "$s-kasbulan.db-wal", "$s-kasbulan.db-shm") }

    @Test
    fun pruneMenghapusKelompokPalingLamaDiAtasBatas() {
        val hapus = DatabaseSnapshot.snapshotsToPrune(
            names("1000", "2000", "3000", "4000"), "kasbulan.db", keep = 3
        )
        assertEquals(3, hapus.size)
        assertTrue(hapus.all { it.startsWith("1000-") })
    }

    @Test
    fun pruneTidakMenghapusBilaMasihDiDalamBatas() {
        assertTrue(
            DatabaseSnapshot.snapshotsToPrune(names("1000", "2000"), "kasbulan.db", keep = 3).isEmpty()
        )
        assertTrue(
            DatabaseSnapshot.snapshotsToPrune(emptyList(), "kasbulan.db", keep = 3).isEmpty()
        )
    }

    @Test
    fun pruneMengabaikanDatabaseLainDanUrutanAcak() {
        val files = listOf(
            "9000-kasbulan.db", "5000-kasbulan.db", "7000-lain.db",
            "1000-kasbulan.db", "3000-kasbulan.db", "7000-kasbulan.db"
        )
        val hapus = DatabaseSnapshot.snapshotsToPrune(files, "kasbulan.db", keep = 3)
        assertEquals(setOf("1000-kasbulan.db", "3000-kasbulan.db"), hapus.toSet())
    }

    @Test
    fun pruneKeepNolMenghapusSemuaSalinan() {
        val hapus = DatabaseSnapshot.snapshotsToPrune(names("1000"), "kasbulan.db", keep = 0)
        assertEquals(3, hapus.size)
    }
}
