# Changelog

All notable changes to KasBulanan are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Changed

- The transaction form (Catat/Ubah Transaksi) is now a full-screen page instead of a dialog (design CT1): a back-arrow header, a label above every field, category chips that wrap into a grid instead of scrolling sideways, and a large Save button pinned at the bottom with Cancel beneath it (plus Delete when editing).
- The Catat floating button now appears only on the Beranda tab, no longer on Riwayat, so the entry point is not duplicated.
- The Laporan summary is now a hero layout (design LR1): the current balance sits in a large primary hero card together with the computed opening balance and the per-channel amounts, and Total Masuk / Total Keluar are two side-by-side mini cards beneath it.
- The Lainnya tab is now a centered profile layout (design LY4): a large app icon and name in the middle with a version pill, the settings (Nama Apotek, Mode Gelap, Backup, Restore) grouped in one card with coloured icon chips, and Tentang in its own card.

- The theme is now Midnight Blue (design R2): deep navy surfaces with an electric cyan accent in dark mode, pale blue and azure in light mode, replacing the pharmacy green. The app is positioned as a universal cash book, not a pharmacy-only one: the name field is now "Nama Usaha" (Business Name), and the About text and placeholders no longer assume a pharmacy.
- The bottom navigation is now a floating pill bar (design B1): detached from the screen edges, with the active tab expanding into a filled pill that shows its label; inactive tabs are icon-only, and the switch is animated. Tab content also animates with a directional slide and fade.
- The header on Home, History, and Report is now a floating card (design H4) instead of a full-width band, with the month shown as a filled chip.
- The balance amounts in the Home and Report heroes are pure white, and income amounts are green everywhere else: transaction rows, the Kas Hari Ini card, the monthly income card, Total Masuk, and Total Pendapatan. Amounts count up or down with a short animation when they change.
- The About dialog is now an identity card (design T2): app icon, name, version and "since 2026" on a navy card, followed by the description and developer/licence rows.

- The balance model is now a continuous ledger: there is no opening
  balance to enter, ever. The cash balance is simply the sum of all
  records across all months, per channel; a month's "opening balance" in
  the Report is computed as the balance at the end of the previous month.
  Months still group the records and closing a month still locks it -
  the next month just continues the same running balance instead of
  carrying a typed-in opening figure. The opening-balance dialog and
  the edit-opening-balance entries are gone; the first month opens with
  a single tap. Backups written by earlier builds still parse (the two
  dropped JSON fields are ignored), and the database keeps version 1
  because no build has been released yet, so no device holds the old
  schema.

### Added

- A daily layer on top of the monthly book: a "Kas Hari Ini" (Today's Cash)
  card on Home showing today's income and expense totals, yesterday's
  totals for comparison, and a one-tap path to the day's recap.
- A "Rekap Hari Ini" (Today's Recap) screen: the day's income and expenses
  broken down per category and channel, the day's transfers, and the
  closing balance per channel at the end of the day - shareable as text,
  like the monthly report.
- "Pendapatan per Hari" (Income per Day) in the monthly Report: one row
  per day with that day's Penjualan (Sales) income and a proportional bar,
  the best day and today marked, summing to a Total Pendapatan row. Only
  sales count as income here; Tambahan Modal (Added Capital) is money in,
  but not revenue, and is excluded.
- Excel export in the Lainnya tab: one .xlsx workbook with a sheet per
  month, formatted with a dark header, zebra rows, a frozen header row,
  decimal number format, and TOTAL plus SALDO AKHIR (final balance) rows.
- A last-backup marker: the Backup row now shows when the last backup was
  saved and to which file, with an amber warning when the data has changed
  since that backup (or was never backed up).
- A "Simpan & Catat Lagi" (Save & Record Another) button in the entry form
  for fast repeated entry: it saves and clears only the amount and note.
- Custom categories: a gold "+ Buat Kategori" chip in the entry form opens
  a small dialog to type any category name (design M3); the typed name is
  stored as-is and shown as its own chip, for both income and expenses.
- A new launcher icon (design L6): a K on a green gradient with a gold
  "Rp" badge.

### Removed

- The "Bagikan Laporan" (Share Report) button in the Report tab; the
  report text builder remains in the code and its tests.

### Fixed

- The CI build failing at R8 minification because Apache POI references
  desktop/server classes that Android does not have (java.awt, OSGi,
  Saxon, StAX): the POI stack is now kept and those references are
  silenced with ProGuard rules, mirroring the pharmacy cashier app.
- Two compile errors that failed the unit-test step: a POI cell-fill
  colour assignment that bound to the wrong overload, and a missing
  TableChart icon import.

## [1.0.0] - 2026-11-09

### Added

- A monthly cash book built for pharmacies: open a month, record daily, then
  close the book to lock the archive; the next month's opening balance
  automatically comes from the previous month's closing balance.
- Money split per channel, Cash and QRIS, on every transaction, with
  balances and closing figures per channel.
- Transfer entries between balances (e.g. QRIS proceeds settled into the
  drawer) that are not counted as income or expenses.
- Pharmacy-specific categories — income: Penjualan (Sales), Tambahan Modal
  (Added Capital), Lainnya (Other); expenses: Pembelian Obat/PBF (Medicine
  Purchases), Operasional (Operations), Gaji Pegawai (Salaries), Setoran
  Bank (Bank Deposits), Pengambilan Pemilik (Owner Withdrawals), Lainnya
  (Other). Category names are stored in Indonesian, as the app's data.
- Four tabs: Home (the running month's balances), History (per day, per
  month), Report (summary + category breakdown + share + close book), More
  (pharmacy name, dark mode, backup/restore).
- Backup & restore as a single JSON file; pharmacy-green Material 3
  Expressive theme, dark-first.
- Data safety net: an automatic copy of the database file right before an
  updated app is opened for the first time (max. 3 copies kept), so the
  data can be recovered if an update ever damages the database.
