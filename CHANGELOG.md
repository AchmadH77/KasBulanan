# Changelog

All notable changes to KasBulanan are documented in this file.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

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
