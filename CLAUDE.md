# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build (Windows)
./gradlew assembleDebug        # Debug APK
./gradlew assembleRelease      # Release APK
./gradlew build                # Full build (all variants)

# Testing
./gradlew test                 # Unit tests
./gradlew connectedAndroidTest # Instrumented tests (requires device/emulator)
./gradlew testDebugUnitTest    # Single variant unit tests

# Other
./gradlew lint                 # Run lint checks
./gradlew clean                # Clean build outputs
```

On Windows, use `gradlew.bat` if `./gradlew` doesn't work.

## Architecture

**MVVM** with a Repository pattern across four layers:

```
UI (Compose screens)
  ↓ observes StateFlow
ViewModels (TaskViewModel, ThemeViewModel, SplashViewModel, BackupViewModel)
  ↓ calls suspend funs / collects Flow
Repository (TaskRepository, CategoryRepository)
  ↓ delegates to DAOs
Room Database (AppDatabase, v8)
```

### Data Layer
- **Entities:** `Task`, `Subtask`, `Category`, `Reminder`
- **DAOs:** `TaskDao`, `SubtaskDao`, `CategoryDao`, `ReminderDao` — each has `getAllXxxSync()` and `deleteAllXxx()` methods for backup operations
- **AppDatabase** uses TypeConverters for `Date` serialization and has 8 tracked migrations

Key business rule: when all subtasks of a task are completed, the parent task auto-completes (logic lives in `TaskViewModel`).

### Dependency Injection (Hilt)
`DatabaseModule` (`di/`) is the single `@Module` with `@InstallIn(SingletonComponent::class)`. It provides `AppDatabase`, all DAOs, `TaskRepository`, and `CategoryRepository` as singletons. ViewModels use `@HiltViewModel` + `@Inject` constructor — screens obtain them via `hiltViewModel()`, `MainActivity` uses `by viewModels()`.

### Repository
`TaskRepository` and `CategoryRepository` handle their respective entity types. Reminder scheduling is delegated to `ReminderScheduler` (AlarmManager-based).

### ViewModels
- **`TaskViewModel`** — central ViewModel (extends `AndroidViewModel`); all task/subtask/reminder mutations go through here. Uses `flatMapLatest` to reactively switch the task list query when the filter changes. Includes quick reminder support (`setQuickReminder`, `deleteReminderForTask`).
- **`CategoryViewModel`** — category CRUD operations.
- **`ThemeViewModel`** — singleton for dark mode and language; persists to SharedPreferences (`"theme_pref"` / `"language_pref"`).
- **`SplashViewModel`** — controls the splash animation.
- **`BackupViewModel`** — export/import all app data as JSON via `BackupManager`. Injects DAOs directly (not repositories) to access sync queries and bulk delete.

### Navigation
NavHost start destination: `"category_list"`. Routes:
- `category_list` → `CategoryListScreen`
- `tasks_by_category/{categoryId}` → `TasksByCategoryScreen`
- `task_detail/{taskId}` → `TaskDetailScreen`
- `reminders`, `daily_reminders`, `theme_settings`, `language_settings`
- `about` → `AboutScreen`
- `backup` → `BackupScreen`

Navigation drawer (hamburger menu) is an Android `DrawerLayout` wrapping a `ComposeView`, not a pure Compose drawer. Drawer items: Theme, Language, Daily Reminders, Backup, About.

### Background Work
- `ReminderScheduler` — schedules exact alarms via `AlarmManager.setExactAndAllowWhileIdle()`. Falls back to `setAndAllowWhileIdle()` if exact alarms are not permitted.
- `ReminderReceiver` — BroadcastReceiver that handles alarm fires, triggers `NotificationService`, and reschedules recurring reminders. Uses `goAsync()` for async DB access.
- `DailyReminderService` — separate BroadcastReceiver (in `services/` package, note plural) for morning/evening daily reminders.
- Boot receiver (`RECEIVE_BOOT_COMPLETED`) re-schedules all reminders after device restart.
- **Notification channels:** `"taksy_reminders"` (task reminders) and `"daily_reminders"` (daily summaries), both IMPORTANCE_HIGH.

### Backup System
- **`BackupManager`** (`utils/`) — stateless utility that serializes/deserializes all entities to/from JSON using `org.json` (no external dependencies). Format includes `backupVersion`, `exportDate`, and arrays for categories, tasks, subtasks, reminders.
- **`BackupScreen`** — export creates a timestamped JSON file via SAF (`ActivityResultContracts.CreateDocument`); import reads via SAF (`ActivityResultContracts.OpenDocument`) with a confirmation dialog warning that all data will be replaced.
- Import clears all tables (reminders → subtasks → tasks → categories) then inserts from the JSON preserving original IDs.

### Splash Screen
- Native Android splash (`Theme.SplashScreen`) uses `splash_icon.xml` — an `InsetDrawable` wrapping `ticksy_icon.png` with 24dp inset so the icon fits inside the system's circular mask.
- After the native splash, the Compose `SplashScreen` shows animated icon (150dp, scale + pulse + alpha) with app name and subtitle.

## Key Tech Details

- **Min SDK 26**, Target SDK 35, Java 11, Kotlin 2.0.21
- **Compose BOM** 2024.09.00 — do not specify individual Compose library versions
- **Room** 2.6.1 with KSP for annotation processing (not kapt)
- **Hilt** 2.48 — `TaksyApplication` extends `HiltAndroidApp`; all DI wiring is in `DatabaseModule`
- **Localization:** Spanish (`values/strings.xml`) is the default; English in `values-en/strings.xml`. Locale changes are applied via `LocaleHelper` and require activity recreation. `locales_config.xml` declared for Android 13+ per-app language support.
- **Database name:** `ticksy_database` (note spelling differs from app name)
- **Manifest application class:** `.TicksyApplication` (note spelling differs from project name "Taksy")

- **Permissions:** `POST_NOTIFICATIONS`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`
- **ProGuard/R8** enabled for release builds
- **Room schema** exported to `app/schemas/` via KSP arg `room.schemaLocation`

## Adding Database Migrations

When modifying Room entities, always add a migration in `AppDatabase.kt` and increment the version. The current version is **8**. Never use `fallbackToDestructiveMigration` in production builds.

## Pending / Future Work

- **Unit tests** — no tests exist yet; add tests for `BackupManager` (round-trip JSON), `TaskViewModel` (auto-complete logic), and repositories
- **Backup: reschedule reminders on import** — after importing, active reminders should be rescheduled via `ReminderScheduler` so alarms fire correctly on the new device
- **Backup: error handling for malformed JSON** — add more granular user feedback for parse errors
- **Swipe-to-delete** on tasks and subtasks for better UX
- **Task priority levels** — add priority field to Task entity (requires DB migration v9)
- **Search across all categories** — global task search from the category list screen
- **Widget** — home screen widget showing today's pending tasks
- **Deprecation cleanup** — replace `Icons.Default.ArrowBack` with `Icons.AutoMirrored.Filled.ArrowBack` and other deprecated API usages flagged by the compiler
