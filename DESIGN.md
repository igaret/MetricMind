# MetricMind — Design & Architecture

> Offline-first, privacy-first Android app combining a **Life Stats Tracker** (local-only personal analytics) and a **Micro-Habit Automator** (one tiny task per day).
>
> **Non-negotiables:** no accounts, no cloud sync, no servers. Encryption at rest. Minimal permissions. Smooth on mid-range devices.

---

## 1. Product scope

### 1.1 MVP (v1.0) — ship this first

**Tracker**
- Manual daily metrics: **Mood** (1–5 scale + optional note), **Sleep** (hours + quality 1–5), **Energy** (1–5), **Productivity** (1–5), **Caffeine** (mg or # of servings), **Symptoms** (tag-based, free multi-select).
- Quick "log today" flow from the home screen (one screen, < 15s to fill).
- Edit/delete past entries via a calendar/day view.
- Device-derived: **Screen time** (daily total + top categories) via `UsageStatsManager`, *opt-in only*.

**Insights / charts (fully on-device)**
- Line/area chart for any numeric metric over time (7/30/90-day ranges).
- Correlation view: scatter + a simple Pearson r between two chosen metrics (e.g., Sleep vs. Mood).
- Weekly summary cards (averages, deltas vs. previous period, streaks).

**Habits**
- Create a micro-habit (title, template/category, one reminder time or "smart time").
- Engine emits **one micro-task per habit per day** (configurable: one-per-habit vs. one-total).
- Daily local notification (single reminder per habit, smart-time option).
- Mark done / skip; track completion streaks.

**Cross-cutting**
- Encryption at rest (SQLCipher) with key in Android Keystore.
- Export: **CSV + JSON** (local only, share-sheet to user-chosen target).
- Light/dark theme; Material 3 dynamic color.
- Full offline. No network permission in MVP.

### 1.2 Explicitly OUT of MVP
- No PDF export (v1.1), no cloud, no accounts, no widgets, no wearables, no ML.

### 1.3 v1.1 (fast follow)
- **PDF export** (report with embedded charts).
- Home-screen **Glance widget** (today's habit + quick mood log).
- More chart types (bar, calendar heatmap), custom date ranges.
- Reminder snooze + adaptive "smart time" learning from completion history.

### 1.4 v2 roadmap
- **Premium tier** (Play Billing): premium chart packs, theme packs, unlimited custom habits, advanced export (PDF + templated reports), data-driven insights ("your mood is 18% higher on 7h+ sleep days").
- Health Connect read integration (steps, sleep) — still 100% local.
- Encrypted local **backup/restore** to user-chosen file (passphrase-wrapped).
- Tasker/Quick-Settings tile integrations.
- Multi-locale habit template library (on-device, bundled).
- Optional biometric app lock.

---

## 2. Architecture

**Stack:** Kotlin, Jetpack Compose (Material 3), MVVM + a light Clean layering, Hilt DI, Coroutines/Flow, Room + SQLCipher, WorkManager, Vico charts, DataStore (prefs), Play Billing.

```
:app
 ├── ui/            (Compose screens, ViewModels, navigation, theme)   <- presentation
 ├── domain/        (models, use-cases, repository interfaces)         <- pure Kotlin, no Android
 ├── data/          (Room entities/DAOs, repo impls, crypto, datastore) <- data
 ├── habit/         (engine + WorkManager workers + notifications)
 ├── export/        (CSV/JSON/PDF exporters)
 ├── billing/       (Play Billing wrapper + entitlement gate)
 └── core/          (crypto/KeyManager, time, result types, di)
```

**Why this shape (and not full multi-module Clean):**
- Single Gradle module keeps build times low and maintenance light — a stated constraint. The *package* boundaries give the Clean separation (domain has zero Android imports, so it's unit-testable) without the overhead of inter-module wiring. Promote `domain`/`data` to real modules in v2 only if the team grows.

**Data flow:** UI → ViewModel (StateFlow of UI state) → UseCase → Repository (interface in `domain`, impl in `data`) → DAO/DataStore/UsageStats. One-directional; ViewModels never touch Room directly.

**Threading:** All DB/IO on `Dispatchers.IO` via repository; DAOs return `Flow` for reactive screens and `suspend` for writes.

---

## 3. Persistence & encryption at rest

### 3.1 Library choice — **Room + SQLCipher**
- **Room** for the type-safe DAO/entity layer, migrations, and Flow support.
- **SQLCipher for Android** (`net.zetetic:sqlcipher-android`) as the `SupportSQLiteOpenHelper.Factory`, giving transparent full-database AES-256 encryption at rest. Room plugs into it via `openHelperFactory`.
- **DataStore (Preferences)** for non-sensitive settings (theme, selected ranges, onboarding flags). Anything sensitive lives in the encrypted DB.

### 3.2 Key handling (the important part)
We never hardcode or ship a passphrase. The DB passphrase is a random 256-bit value generated on first launch and **wrapped by an AES key stored in the Android Keystore** (hardware-backed where available, `StrongBox` if present).

```
First launch:
  1. Generate random 32-byte DB passphrase (SecureRandom).
  2. Generate/lookup AES key in AndroidKeyStore (alias "metricmind_master").
  3. Encrypt passphrase with that key (AES/GCM, random IV).
  4. Persist {iv, ciphertext} in EncryptedSharedPreferences (Jetpack Security).
Subsequent launches:
  - Load {iv, ciphertext}, unwrap with Keystore key -> plaintext passphrase -> open SQLCipher DB.
```

- Keystore key is non-exportable; passphrase only exists in memory while the app runs.
- Optional (v2): require biometric auth to use the Keystore key (`setUserAuthenticationRequired`).
- See `core/crypto/KeyManager.kt` and `di/DatabaseModule.kt` in the scaffold.

### 3.3 Backup posture
- `android:allowBackup="false"` and a custom `dataExtractionRules` that **excludes the DB and the wrapped key** — auto-backup of an encrypted DB whose key is device-bound would be useless and is a leak surface. Explicit user-driven encrypted export is the supported path (v2).

---

## 4. Charting — **Vico** (recommended)

| | Vico | MPAndroidChart |
|---|---|---|
| Compose-native | ✅ first-class `@Composable` | ❌ View + `AndroidView` wrapper |
| Maintenance | ✅ actively maintained | ⚠️ effectively unmaintained |
| Theming | ✅ Material 3-aware, easy | ⚠️ manual |
| License | Apache-2.0 | Apache-2.0 |
| Animations/perf | ✅ smooth, Compose-driven | ✅ but heavier |

**Pick Vico.** It is Compose-first (matches our UI), Material3-themable, actively maintained, and avoids `AndroidView` interop friction. MPAndroidChart is powerful but View-based and stale. All rendering is on-device; no network. For correlation scatter + Pearson r we compute stats locally (`domain/usecase/Correlation.kt`) and feed points to Vico.

Vico is wired into the Insights screen (`ui/insights/InsightsScreen.kt`): a `CartesianChartModelProducer` is fed the selected metric's daily series inside a `LaunchedEffect`, rendered by `CartesianChartHost` with a line layer and start/bottom axes. The trend chart updates reactively when the user changes metric or range.

---

## 5. Permissions & device-derived metrics

| Permission / API | Why | When requested |
|---|---|---|
| `PACKAGE_USAGE_STATS` (special access) | Screen-time metric via `UsageStatsManager` | Only if user enables Screen Time; deep-link to Settings |
| `POST_NOTIFICATIONS` (API 33+) | Daily habit reminder | When user creates first habit / enables reminders |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Only if exact-time reminders chosen (else WorkManager) | At reminder setup; prefer inexact to avoid the permission |
| `RECEIVE_BOOT_COMPLETED` | Reschedule reminders after reboot | Declared only; no runtime prompt |
| `BODY_SENSORS` (runtime) | On-demand heart-rate reading (`Sensor.TYPE_HEART_RATE`) | Only when the user taps *Measure* on the Vitals card |

**Vitals (beta) — body sensors, fully on-device:** the Home screen's Vitals card measures on demand only (nothing samples in the background):
- **Heart rate**: `TYPE_HEART_RATE` where the hardware exists (`uses-feature` optional, so phones without it can still install). Gracefully reports "no sensor" otherwise.
- **Temperature**: `TYPE_AMBIENT_TEMPERATURE`, labeled honestly as *ambient* — phones don't expose true body temperature.
- **Awake/asleep**: an offline heuristic (`SleepHeuristic`) combining accelerometer stillness (~3 s sample), screen interactivity, and hour-of-day into a confidence-scored estimate. Deliberately avoids the Google Sleep API, which would require Play Services — this keeps the no-network promise intact.
- **User verification loop**: every reading prompts "Was this accurate?" — *Looks right* / *It's off (enter correct value)*. Verifications are stored (`vital_reading.verification` = UNVERIFIED/CONFIRMED/CORRECTED) and the card reports measured per-sensor accuracy ("Heart rate: 80% correct, 10 of 12 readings verified"). Readings live in the same SQLCipher DB (schema v2, `MIGRATION_1_2`).

**No `INTERNET` permission in MVP** — strongest possible privacy signal.

**UsageStats UX:** It's a *special access*, not a runtime permission — you cannot request it via the normal dialog. UX: an explainer screen ("Screen time stays on your device; we read daily totals only") → button → `Settings.ACTION_USAGE_ACCESS_SETTINGS` → on return, check `AppOpsManager` `OPSTR_GET_USAGE_STATS` and reflect state. Always degrade gracefully if denied (hide the metric).

**Notifications UX:** Request `POST_NOTIFICATIONS` contextually with a pre-prompt rationale, not at cold start.

---

## 6. Data model

### 6.1 Entities (Room)

```
metric_entry
  id (PK, autogen)
  type        TEXT   -- MOOD|SLEEP|ENERGY|PRODUCTIVITY|CAFFEINE  (enum)
  value       REAL   -- normalized numeric (hours, mg, 1..5)
  note        TEXT?  -- optional
  recorded_at INTEGER (epochDay or epochMillis)  -- see note
  created_at  INTEGER
  INDEX(type, recorded_at)   -- range queries per metric
  UNIQUE(type, recorded_at)  -- one value per metric per day (upsert)

symptom_tag
  id (PK), name TEXT UNIQUE

symptom_log
  id (PK), recorded_at INTEGER, tag_id FK->symptom_tag(id)
  INDEX(recorded_at), INDEX(tag_id)
  -- many-to-many day<->symptom via this join-ish table

screen_time_daily
  day (PK, epochDay), total_minutes INTEGER, top_pkg TEXT?, top_minutes INTEGER
  -- cached snapshot pulled from UsageStatsManager

habit
  id (PK), title TEXT, template TEXT (enum), cadence TEXT,
  reminder_mode TEXT (FIXED|SMART|NONE), reminder_minute INTEGER?, -- minutes from midnight
  active INTEGER (bool), created_at INTEGER

habit_task        -- the generated "one tiny task per day"
  id (PK), habit_id FK->habit(id), for_day INTEGER (epochDay),
  prompt TEXT, status TEXT (PENDING|DONE|SKIPPED), completed_at INTEGER?
  INDEX(habit_id, for_day), UNIQUE(habit_id, for_day)
```

**Date strategy:** store user-entered metrics keyed by **epochDay** (`LocalDate.toEpochDay()`) for clean "one per day" upserts and fast range scans; store precise event timestamps in `created_at` (epochMillis) for auditing. Type converters handle enums and `LocalDate`/`Instant`.

### 6.2 Relationships
- `habit 1—* habit_task`
- `symptom_tag *—* day` through `symptom_log`
- Metrics are flat per (type, day).

### 6.3 Indexes (perf on mid-range)
- `metric_entry(type, recorded_at)` covers all charting range queries.
- `habit_task(habit_id, for_day)` for "today's task" and streaks.
- UNIQUE constraints enable `@Upsert` and prevent duplicate-day rows.

### 6.4 Migrations
- Ship `Migration(1,2)…` objects; **never** rely on `fallbackToDestructiveMigration` in release (would wipe user data). Each schema change = explicit migration + Room exported schema JSON committed (`room.schemaLocation`) for migration tests.

---

## 7. Habit engine

### 7.1 Responsibilities
1. **Generate** one task per active habit per day from a template.
2. **Schedule** exactly one reminder per habit per day (fixed or smart time).
3. **Reschedule** after reboot / time change / app update.

### 7.2 Scheduling — **WorkManager-first**, AlarmManager only when needed
- **Daily generation + (inexact) reminder:** `PeriodicWorkRequest`/daily `OneTimeWorkRequest` chain via WorkManager — survives reboots, battery-friendly, no special permission. Default.
- **Exact-time reminder** (user insists on "9:00 sharp"): `AlarmManager.setExactAndAllowWhileIdle` guarded by `SCHEDULE_EXACT_ALARM`. We default to inexact windows to avoid the permission and Doze issues; exact is opt-in.
- `BootReceiver` (`RECEIVE_BOOT_COMPLETED`) re-enqueues work after reboot.

### 7.3 Templates (on-device, no backend)
Templates are bundled Kotlin/`assets` definitions: `{ category, promptPool[], optionalParam }`. Examples:
- `WATER` → "Drink one glass of water now 💧"
- `SPANISH_VOCAB` → pick next from a bundled word list → "Learn: *agua* = water"
- `DECLUTTER` → "Find one item to toss/donate"
- `GRATITUDE` → rotating prompt from a pool
- `CUSTOM` (premium) → user-authored prompt pool

Generation = deterministic pick seeded by `(habitId, epochDay)` so the same day always yields the same task (idempotent re-runs), with rotation across the pool.

### 7.4 "Smart time" rules (MVP heuristic, no ML)
- Default smart slots per category (e.g., WATER spread across waking hours → pick mid-afternoon; GRATITUDE → evening).
- If screen-time access granted: bias reminder to a recent **active** window (avoid firing while phone unused).
- v1.1: shift toward the hour with the best historical completion rate (simple frequency table, still on-device).

---

## 8. UI / UX

### 8.1 Navigation (bottom bar, 4 tabs + modal flows)
```
Home (Today)  |  Insights  |  Habits  |  Settings
```
- **Home/Today:** quick-log chips for each metric (tap mood face, sleep stepper…), today's habit task card (Done/Skip), streak banner.
- **Insights:** metric picker + range toggle (7/30/90), line/area chart, correlation card (two-metric scatter + r), weekly summary cards.
- **Habits:** list with completion state + streaks; FAB → create/edit habit (title, template, reminder mode/time).
- **Settings:** theme, screen-time access toggle + status, notification settings, **Export** (CSV/JSON/[PDF premium]), privacy explainer, premium upsell.
- **Modal flows:** day editor (edit past entries), onboarding (privacy promise + optional permissions), paywall.

### 8.2 UX principles
- Logging is the hot path: everything < 15s, big tap targets, no typing required for core metrics.
- Permissions are contextual + reversible; privacy copy everywhere ("stays on this device").
- Empty/first-run states teach the value before asking for anything.

---

## 9. Monetization (no servers, Play Billing)

**Model:** free core + **one-time "Pro" unlock** (and/or optional subscription) via **Google Play Billing Library**. Entitlement is verified locally via Play's on-device `queryPurchasesAsync` + signature; no server needed (acceptable for low-risk consumer features).

**Free vs Pro**
| Free | Pro |
|---|---|
| All core tracking, basic line chart, CSV/JSON export, up to N habits | Premium chart pack (heatmap, correlation matrix), theme packs, **PDF/templated export**, unlimited custom habits, advanced insights |

- Use a one-time **in-app product** (`inAppProduct`) for "Pro lifetime" (simplest, server-free, user-friendly) and optionally a subscription for ongoing theme/insight drops.
- Gate features behind an `Entitlements` flag cached in encrypted prefs, re-checked on launch.
- **Caveat:** purely on-device entitlement can be tampered with on rooted devices — acceptable for these feature classes; do not gate anything safety-critical.

---

## 10. Export (local-only)

- **CSV:** one file per metric or a combined long-format CSV (`day,type,value,note`). Streamed write to a `Uri` from the system file picker (`ACTION_CREATE_DOCUMENT`) or share-sheet — no storage permission needed (SAF).
- **JSON:** full structured dump (metrics, symptoms, habits, tasks) with schema version — also the basis for future restore.
- **PDF (v1.1/Pro):** render summary + charts to PDF via `PdfDocument`/Compose capture. All on-device.
- Everything goes through `Storage Access Framework`; the app never needs broad file permissions.

---

## 11. Security & privacy

### 11.1 Threat model (STRIDE-lite)
| Threat | Mitigation |
|---|---|
| Device theft / offline DB extraction | SQLCipher AES-256; key in hardware Keystore, not in DB/file |
| Backup exfiltration | `allowBackup=false`, DB excluded from extraction rules |
| Other apps reading data | App-private storage; no exported components leaking data; no `INTERNET` |
| Memory scraping (rooted) | Passphrase only in memory while running; optional biometric gate (v2) |
| Tampered entitlement | Accepted risk for cosmetic/feature gates; nothing safety-critical gated |
| Notification content leak on lock screen | Use `VISIBILITY_PRIVATE` for habit notifications |
| Screen-time over-collection | Read daily totals only, opt-in, revocable, never leaves device |

### 11.2 Key handling — see §3.2. Keystore-wrapped random passphrase, GCM, non-exportable key.

### 11.3 Backup/restore
- MVP: no auto-backup (by design). User-driven JSON export is the manual backup.
- v2: passphrase-protected encrypted export file (`AES-GCM`, PBKDF2/Argon2-derived key from a user passphrase) + import/merge.

### 11.4 Privacy messaging
- One-screen privacy promise at onboarding; per-permission rationale; a "What we store / what we never do" section in Settings. No analytics SDKs, no crash reporting that exfiltrates data (or opt-in only, off by default).

---

## 12. Android Studio project plan

### 12.1 Folder structure (see scaffold in this repo)
```
MetricMind/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradle/libs.versions.toml          (version catalog)
├── gradle/wrapper/…
├── gradlew / gradlew.bat
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── res/ (values: strings, themes, colors; xml: data_extraction_rules)
        └── java/com/metricmind/
            ├── MetricMindApp.kt
            ├── core/crypto/KeyManager.kt
            ├── di/{DatabaseModule,AppModule}.kt
            ├── data/local/{MetricMindDatabase,Converters}.kt
            ├── data/local/entity/*.kt
            ├── data/local/dao/*.kt
            ├── data/repository/*.kt
            ├── domain/model/*.kt
            ├── domain/repository/*.kt        (interfaces)
            ├── domain/usecase/*.kt
            ├── habit/{HabitEngine,GenerateDailyTasksWorker,ReminderWorker,BootReceiver,Notifications}.kt
            ├── export/{CsvExporter,JsonExporter}.kt
            ├── billing/BillingManager.kt
            └── ui/
                ├── MainActivity.kt
                ├── navigation/MetricMindNav.kt
                ├── theme/{Color,Theme,Type}.kt
                ├── home/{HomeScreen,HomeViewModel}.kt
                ├── insights/{InsightsScreen,InsightsViewModel}.kt
                ├── habits/{HabitsScreen,HabitsViewModel}.kt
                └── settings/{SettingsScreen,SettingsViewModel}.kt
```

### 12.2 Key files
Provided as working snippets in the scaffold (`app/src/main/...`). They compile against the version catalog in `gradle/libs.versions.toml`. The scaffold favors *correct wiring* (DI, encrypted Room, WorkManager, Compose nav) over exhaustive feature completeness so you can extend each screen.

---

## 13. Build: release Android App Bundle (AAB)

### 13.1 Prerequisites
- Android Studio (Ladybug+), JDK 17, Android SDK with **compileSdk 34** (set `sdk.dir` in `local.properties` or `ANDROID_HOME`).

### 13.2 Create an upload keystore (one-time)
```bash
keytool -genkeypair -v \
  -keystore metricmind-upload.jks \
  -alias metricmind \
  -keyalg RSA -keysize 4096 -validity 10000
# Store metricmind-upload.jks OUTSIDE the repo. Never commit it.
```

### 13.3 Provide signing secrets (not committed)
`~/.gradle/gradle.properties` (or `keystore.properties` ignored by git):
```properties
MM_STORE_FILE=/abs/path/metricmind-upload.jks
MM_STORE_PASSWORD=********
MM_KEY_ALIAS=metricmind
MM_KEY_PASSWORD=********
```
The `app/build.gradle.kts` `signingConfigs.release` reads these (falls back to debug signing if absent, so the project still builds for new contributors).

### 13.4 Versioning
- `versionCode` = monotonically increasing integer (CI build number or `git rev-list --count HEAD`).
- `versionName` = semver `1.0.0`.

### 13.5 Build the AAB
```bash
# from project root
./gradlew clean :app:bundleRelease
# output: app/build/outputs/bundle/release/app-release.aab
```
Android Studio: **Build → Generate Signed Bundle/APK → Android App Bundle**, pick the keystore, choose `release`.

### 13.6 Verify before upload
```bash
# inspect the bundle
bundletool build-apks --bundle=app-release.aab --output=mm.apks \
  --ks=metricmind-upload.jks --ks-key-alias=metricmind
# upload app-release.aab to Play Console (internal testing track first)
```
- Enable Play App Signing (Google manages the app signing key; your upload key signs uploads).
- R8/ProGuard is on for release (`isMinifyEnabled=true`); keep rules in `proguard-rules.pro` (Room/SQLCipher/Hilt/Vico keep-rules included).

---

## 14. Testing strategy (low-maintenance)
- **Unit:** `domain` use-cases (correlation, streaks, smart-time, generation determinism) — pure Kotlin, fast.
- **Room migration tests** with `MigrationTestHelper` + exported schemas.
- **Instrumented:** DAO CRUD against an in-memory encrypted DB; WorkManager `TestDriver`.
- **UI:** a few Compose tests for the log flow and paywall gate.

---

## 15. Performance on mid-range
- Flows + paging for long histories; never load full history into memory for charts (query aggregated/downsampled ranges).
- Indexed range queries (§6.3); avoid N+1 in habit/streak computation (single windowed query).
- Compose: stable keys in lists, `derivedStateOf` for chart inputs, avoid recomposition storms.
- WorkManager batching; inexact alarms by default to spare battery.
