# KasBulanan

[![Build APK](https://github.com/AchmadH77/KasBulanan/actions/workflows/build.yml/badge.svg)](https://github.com/AchmadH77/KasBulanan/actions/workflows/build.yml)

A universal monthly cash book — an offline-first Android app for recording
cash on a monthly rhythm: **open the month → record daily → close the book**.
For a small shop, a food stall, a service business, or personal cash.

Money is split per channel, **Cash** and **QRIS**, because QRIS money is not
in the drawer: it sits in the bank account until it is settled. A settlement
is recorded as a **Transfer between balances**, not income, so reports never
double-count it.

The book lives on the device. An optional account-based sync can mirror it
to the owner's own Firebase project as a cloud backup and for moving
between devices — the app stays fully usable without an account or a
connection.

## Features

- No opening balance, ever: the balance is one continuous sum of all
  records across all months, per channel. Months group the records;
  closing the book locks a month (it can no longer be edited —
  corrections go through entries in the running month), and the next
  month continues the same running balance.
- The balance is always computed from the transaction records — no figure is
  ever typed in directly, so the balance can never disagree with the history.
- Running-month home: total cash balance + the Cash/QRIS breakdown, this
  month's income and expenses, latest transactions.
- A daily layer: a "Kas Hari Ini" card on Home with today's totals and a
  "Rekap Hari Ini" recap screen (per category and channel, closing balance
  per channel, shareable as text).
- History grouped per day with daily subtotals; a monthly Report with a
  per-category breakdown and an Income-per-Day section (sales only,
  summing to the month's total revenue).
- Fast entry: a full-screen form with a "Save & Record Another" button
  for repeated entries, and custom categories you can type yourself next
  to the built-in lists.
- Excel export: one .xlsx workbook with a sheet per month, formatted
  headers and zebra rows, decimal number format, and TOTAL plus final
  balance rows (Apache POI).
- Backup & restore as a single JSON file, with a preview of the file's
  contents before any data is replaced. The More tab shows when the last
  backup was saved and warns when the data has changed since it.
- Optional online mode: sign in with email or Google, and entries sync
  both ways with Cloud Firestore (conflicts resolve last-write-wins),
  automatically after changes, periodically, or on demand from the Mode
  Online screen. A sync-status icon on the Home balance card shows the
  current state.
- When online mode is configured, the app opens on a sign-in gate with a
  guest pass ("Lewati — Pakai Tanpa Akun"), so the offline book is never
  locked behind an account.
- Database safety net: an automatic copy of the database file right before
  an updated app is opened for the first time (max. 3 copies), on top of the
  manual backup.

## Technology

Kotlin + Jetpack Compose, a single `:app` module. Official Material 3
Expressive (1.5.0-rc01, `MaterialExpressiveTheme` +
`MotionScheme.expressive()`), Compose BOM 2026.09.00, Room 2.7.2. Online
mode uses Firebase Authentication and Cloud Firestore (Firebase BOM
33.5.1) behind a configuration check — without a `google-services.json`
the online layer stays dormant and the app is purely offline. R8 is
enabled for both debug and release builds. Requires Android 8.0 (API 26)
or newer. 45 unit tests run as a CI step before the APK is built. The project follows the Kasir pattern:
builds run on GitHub Actions (`workflow_dispatch`), with no Gradle wrapper —
Gradle 9.6.0 is provided by `gradle/actions/setup-gradle`.

## Project Structure

```
app/src/main/java/id/kasbulan/app/
├── MainActivity.kt   App shell, entry form, and dialogs
├── Screens.kt        Home, History, Report, and More tabs,
│                     the sign-in gate, and the Mode Online screen
├── KasLogic.kt       Pure cash logic (balances, summaries, reports)
├── Theme.kt          Midnight-blue Material 3 Expressive theme
└── data/             Room (Models, KasRepository), BackupHelper,
                      ExcelExporter, DatabaseSnapshot (update safety net),
                      SyncLogic and OnlineSync (Firebase sync)
```

Full explanation: [docs/ARSITEKTUR.md](docs/ARSITEKTUR.md) ·
Online mode setup: [docs/ONLINE.md](docs/ONLINE.md) ·
Change history: [CHANGELOG.md](CHANGELOG.md)

## Build

1. Run the **Build APK** workflow from the Actions tab (the *Run workflow*
   button).
2. The `kasbulan-debug-apk` artifact is always produced. The
   `kasbulan-release-apk` artifact appears when the
   `KASBULAN_KEYSTORE_BASE64` secret is set (its value = the base64 of the
   `kasbulan.p12` file, alias/password `kasbulan`); without the secret, the
   debug build uses the built-in debug key.
3. Online mode activates when `app/google-services.json` is present. This
   repository carries the owner's Firebase Android config; to run the app
   against your own Firebase project, replace that file, enable
   Email/Password (and Google) in Firebase Authentication, and publish
   `firestore.rules` — see [docs/ONLINE.md](docs/ONLINE.md).

## License

All Rights Reserved — see [LICENSE](LICENSE).
