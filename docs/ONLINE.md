# Mode Online (Firebase) — Setup Guide

KasBulanan works fully offline by default. Online mode is an optional
sync layer (design A): sign in with email, and the app keeps a synced
copy of the cash book in Cloud Firestore, so the data survives a lost
or replaced phone. Local data (Room) stays the source of truth on the
device; sync runs automatically after changes and periodically.

Without the configuration below, the app builds and behaves exactly
like the offline version — the Mode Online screen in Lainnya simply
says "Belum dikonfigurasi".

## Owner setup (one time, about 10 minutes)

1. Open the [Firebase Console](https://console.firebase.google.com) and
   create a project (any name, e.g. `kasbulanan`). The free Spark plan
   is enough.
2. In the project, choose **Add app → Android**:
   - Android package name: `id.kasbulan.app`
   - App nickname: KasBulanan
   - Debug signing SHA-1: skip for now (only needed for Google
     Sign-In later).
3. Download **`google-services.json`** and put it at
   **`app/google-services.json`** in this repository, then commit it.
   The file identifies the Firebase project; it is not a password.
   CI only applies the google-services plugin when this file exists.
4. In the console, open **Build → Authentication → Sign-in method**
   and enable **Email/Password**.
5. Open **Build → Firestore Database → Create database** (production
   mode).
6. In Firestore, open the **Rules** tab, replace the content with the
   `firestore.rules` file from this repository, and publish.
7. Run the **Build APK** workflow, install the APK, open
   **Lainnya → Mode Online**, and create an account with
   **Daftar Akun Baru**. The first sync uploads the whole cash book.

## How sync works

- Structure on the server: `users/{uid}/bulan/{remoteId}` and
  `users/{uid}/transaksi/{remoteId}` — a user can only access their
  own tree (see the rules).
- Every row carries a `remoteId`, an `updatedAt` timestamp, a soft
  delete flag, and a dirty flag (database v2). Sync pushes dirty rows,
  then pulls everything and applies rows whose `updatedAt` is newer —
  **last write wins**.
- Deleting a transaction is a soft delete so the deletion syncs too;
  synced tombstones are purged automatically.
- Backup JSON files carry the same sync fields and stay compatible:
  old backups still restore, and restored data is pushed on the next
  sync.
- Sign out stops syncing; nothing on the device changes.

## Enabling Google Sign-In (optional)

The sign-in screen shows a "Masuk dengan Google" button only when the
configuration supports it. To enable it:

1. Firebase Console → Project settings → the KasBulanan app →
   **Add fingerprint**: SHA-1
   `84:9C:E8:75:B1:DA:BD:D8:B9:E2:5E:37:C0:1B:60:79:40:22:A8:EB`
   (the kasbulan.p12 signing certificate; the SHA-256 can be added too:
   `03:1F:8C:FF:03:0A:FD:89:DA:2C:04:7A:A8:F6:8F:41:43:B5:EF:FD:9B:3F:FA:63:E1:45:10:C4:2C:F3:23:3F`).
2. Authentication → Sign-in method → enable **Google**.
3. Download a fresh **google-services.json** (it now contains the web
   client id) and replace `app/google-services.json` in this repo.
4. Rebuild. The Google button appears on the sign-in form; sign-in uses
   the system Credential Manager and exchanges the Google ID token for
   a Firebase session.

Forgot-password e-mail reset is built into the sign-in form
("Lupa kata sandi?") through Firebase Auth.

## Not included yet

- Account deletion screen.
