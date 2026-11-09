# KasBulanan Architecture

This document explains how the KasBulanan app is put together, for whoever
continues its development (including future me).

## Summary

KasBulanan is a **fully offline** Android app: a single `:app` module, no
server, no account, no network. All data lives in a Room database on the
phone. The unit of business is the **cash month**: one `BulanKas` row with
its list of `TransaksiKas` entries.

Domain rules that must not be broken:

1. **The balance is always computed**, never typed in directly - and
   there is no opening balance at all: balance = the sum of ALL records
   across all months, per channel (`hitungSaldo`). A month's opening
   figure in the Report is itself computed as the previous month's
   closing balance (`saldoAwalBulan` / `saldoAkhirBulan`).
2. **Money is separated per channel** — TUNAI (Cash, the drawer) and QRIS
   (the bank account) — in the running balance, the closing balance, and
   the close-book archive.
3. **A Transfer between balances is neither income nor expense**; it only
   moves money between wallets and never enters the category summary.
4. **A closed month is locked.** Its final figures are frozen on the month
   row; corrections happen only through entries in the running month.

## File Map

```
app/src/main/java/id/kasbulan/app/
├── MainActivity.kt   Activity + app shell (4 tabs) and every dialog:
│                     transaction form, transfer, close book, about,
│                     restore confirmation; the backup file pickers.
├── Screens.kt        The four tabs: BerandaTab (Home), RiwayatTab
│                     (History), LaporanTab (Report), LainnyaTab (More),
│                     plus the transaction row and MoneyField components.
├── KasLogic.kt       Pure logic (no Android): domain constants,
│                     hitungSaldo, ringkasanBulan, laporanText,
│                     formatRupiah/groupThousands, date helpers.
├── Theme.kt          Pharmacy-green MaterialExpressiveTheme
│                     (dark-first, plus light), MotionScheme.expressive().
└── data/
    ├── Models.kt         The BulanKas and TransaksiKas entities, KasDao,
    │                     AppDatabase ("kasbulan.db" v1).
    ├── KasRepository.kt  Data access; tutupBulan() freezes the final
    │                     figures and opens the next month inside ONE
    │                     Room transaction; replaceAll() for restore.
    ├── BackupHelper.kt   The backup JSON format and its validation.
    └── DatabaseSnapshot.kt  The pre-update database copy safety net.
```

Screen and type names stay Indonesian in the code (`BerandaTab`,
`tutupBulan`, …) because they mirror the app's Indonesian UI.

## The Close-Book Cycle

`tutupBulan()` runs inside a single database transaction:

1. Compute the closing balance and the summary of the running month.
2. Freeze them onto the month row (`status = DITUTUP`, `ditutupPada`,
   `totalMasukFinal`, `totalKeluarFinal`, `saldoAkhirTunaiFinal`,
   `saldoAkhirQrisFinal`).
3. Insert the next month (empty, `status = BERJALAN`). The balance is
   continuous, so nothing is carried over - the new month simply
   continues the same running balance.

Because steps 1–3 are atomic, the app dying mid-process can never leave a
half-closed month behind.

## Backup & Restore

A single JSON file: `{ "app": "kasbulanan", "version": 1,
"exportedAtMillis": ..., "bulan": [...], "transaksi": [...] }`, exchanged
through the Storage Access Framework. The parser rejects any file that is
not a KasBulanan backup: wrong app/version, a calendar month outside
1–12, an unknown type or channel, or a transaction pointing at a month
that is not carried in the file. Before a restore, a dialog shows the
file's contents (month/transaction counts and its creation date) next to
the current data counts; the restore then replaces all data in one
transaction.

## Update Safety Net

`AppDatabase.create()` calls `DatabaseSnapshot.snapshotBeforeUpdate()`
BEFORE Room opens the database. When the package's `lastUpdateTime` has
changed (the app was just updated) and this is not the first install, the
`kasbulan.db` file (plus `-wal`/`-shm`) is copied untouched into
`filesDir/db-snapshots/`, keeping at most 3 snapshot groups. Copy failures
are swallowed — the app must never fail to start because of its own safety
net. The copies live in internal storage and disappear on uninstall; the
manual JSON backup remains the primary backup.

## Build & CI

- No Gradle wrapper: CI uses `gradle/actions/setup-gradle` 9.3.1, JDK 17
  (Temurin), SDK platform 37. The `Build APK` workflow is
  `workflow_dispatch` only.
- CI steps: unit tests (`:app:testDebugUnitTest`) → `assembleDebug` →
  (when the `KASBULAN_KEYSTORE_BASE64` secret exists) restore
  `kasbulan.p12` + `assembleRelease`.
- R8 (minify + resource shrinking) is on for debug and release, so
  shrinking problems surface in everyday builds, not at release time.
- Signing uses one fixed keystore (`kasbulan.p12`, kept outside the repo)
  so every build installs over the previous one; without the secret,
  builds fall back to the debug key.

## Testing

30 JVM unit tests for the pure logic, under `app/src/test/`:

- `KasLogicTest` (17) — per-channel balances, transfers in both
  directions, summaries and categories, rupiah formatting, date labels,
  report text.
- `BackupFilesTest` (9) — backup round-trip and every parser rejection.
- `DatabaseSnapshotTest` (4) — the snapshot pruning logic
  (`snapshotsToPrune`).

Logic that touches Android/Room (close book, restore) is not yet covered
automatically; it is verified manually on a device after each build.
