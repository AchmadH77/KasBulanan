# Aturan ProGuard/R8 aplikasi KasBulanan.
# Minify (R8) + shrink resources aktif di build debug dan release
# (lihat build.gradle.kts).

# Room: KSP menghasilkan AppDatabase_Impl dan KasDao_Impl di paket data,
# dan Room memuatnya lewat refleksi berdasarkan nama kelas (kelas + "_Impl").
# Paket data dipertahankan utuh: entitas, DAO, database, dan helper-nya.
-keep class id.kasbulan.app.data.** { *; }

# Atribut yang dibutuhkan refleksi dan debugging stack trace.
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*

# Apache POI (ekspor Excel .xlsx) memakai refleksi dan tidak membawa
# consumer rules yang memadai untuk Android - jaga paketnya beserta
# dependensi OOXML-nya supaya ekspor tidak rusak di perangkat.
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class org.apache.commons.io.** { *; }
-keep class org.apache.commons.compress.** { *; }
-keep class org.apache.commons.collections4.** { *; }
-keep class org.apache.commons.math3.** { *; }
-keep class org.apache.logging.log4j.** { *; }
-keep class com.zaxxer.sparsebits.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn org.apache.xmlbeans.**
-dontwarn org.apache.commons.io.**
-dontwarn org.apache.commons.compress.**
-dontwarn org.apache.commons.collections4.**
-dontwarn org.apache.commons.math3.**

# R8 gagal (minifyDebugWithR8) karena POI/Log4j/XMLBeans mereferensikan
# kelas desktop/server yang tidak ada di Android dan tidak pernah dipakai:
# java.awt (gambar desktop), OSGi (Log4j), Saxon (XPath opsional XMLBeans),
# StAX, serta anotasi bnd/FindBugs. Redam peringatannya supaya build lanjut.
-dontwarn java.awt.**
-dontwarn org.osgi.**
-dontwarn com.graphbuilder.**
-dontwarn org.apache.logging.log4j.**
-dontwarn net.sf.saxon.**
-dontwarn javax.xml.stream.**
-dontwarn aQute.bnd.**
-dontwarn edu.umd.cs.findbugs.**
