# Aturan ProGuard/R8 aplikasi KasBulanan.
# Minify (R8) + shrink resources aktif di build debug dan release
# (lihat build.gradle.kts).

# Room: KSP menghasilkan AppDatabase_Impl dan KasDao_Impl di paket data,
# dan Room memuatnya lewat refleksi berdasarkan nama kelas (kelas + "_Impl").
# Paket data dipertahankan utuh: entitas, DAO, database, dan helper-nya.
-keep class id.kasbulan.app.data.** { *; }

# Atribut yang dibutuhkan refleksi dan debugging stack trace.
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*
