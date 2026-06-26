# MetricMind

Offline-first, privacy-first Android app: a **Life Stats Tracker** + a **Micro-Habit Automator**.
No accounts, no cloud, no servers. Encryption at rest. Minimal permissions.

> Full design rationale (MVP/v2 scope, architecture, data model, habit engine, security threat
> model, monetization, build/AAB instructions) lives in **[DESIGN.md](DESIGN.md)**.

## Stack
Kotlin · Jetpack Compose (Material 3) · MVVM + light Clean · Hilt · Room + **SQLCipher** ·
WorkManager · DataStore · Play Billing · (charts: Vico recommended — see DESIGN §4).

## Project layout
```
app/src/main/java/com/metricmind/
  core/crypto/   KeyManager (Keystore-wrapped DB passphrase)
  data/local/    Room entities, DAOs, DB, converters
  data/repository/ repo implementations
  domain/        models, repository interfaces, use-cases (pure Kotlin)
  di/            Hilt modules (DatabaseModule wires SQLCipher)
  habit/         HabitEngine, HabitWorker, scheduler, notifications, BootReceiver
  export/        CSV/JSON exporters (SAF, local-only)
  billing/       BillingManager (server-free entitlement)
  ui/            Compose screens + ViewModels (home, insights, habits, settings)
```

## Build & run (debug)
1. Open in Android Studio (Ladybug+), JDK 17, Android SDK with **compileSdk 34**.
2. Let Gradle sync (uses the version catalog in `gradle/libs.versions.toml`).
3. Run the `app` config on a device/emulator (minSdk 26).

A fresh checkout builds without signing secrets — the release build type falls back to debug signing
until you provide a keystore.

## Release AAB
See **DESIGN.md §13**. Summary:
```bash
# 1. one-time keystore
keytool -genkeypair -v -keystore metricmind-upload.jks -alias metricmind -keyalg RSA -keysize 4096 -validity 10000
# 2. put MM_STORE_FILE / MM_STORE_PASSWORD / MM_KEY_ALIAS / MM_KEY_PASSWORD in keystore.properties (gitignored)
# 3. build
./gradlew clean :app:bundleRelease   # -> app/build/outputs/bundle/release/app-release.aab
```

## Privacy posture
- No `INTERNET` permission in the MVP.
- `allowBackup=false`; encrypted DB + wrapped key excluded from backups.
- Screen-time (UsageStatsManager) is strictly opt-in and revocable.

## Tests
```bash
./gradlew test            # pure-Kotlin unit tests (e.g. correlation math)
./gradlew connectedCheck  # instrumented (DAO/migrations/WorkManager) — needs a device
```
