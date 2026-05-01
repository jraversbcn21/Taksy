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
Room Database (AppDatabase, v9)
```

### Data Layer
- **Entities:** `Task` (with `TaskPrioridad` enum: NINGUNA/BAJA/MEDIA/ALTA), `Subtask`, `Category`, `Reminder`
- **DAOs:** `TaskDao`, `SubtaskDao`, `CategoryDao`, `ReminderDao` — each has `getAllXxxSync()` and `deleteAllXxx()` methods for backup operations
- **AppDatabase** uses TypeConverters for `Date` serialization and has 9 tracked migrations

Key business rule: when all subtasks of a task are completed, the parent task auto-completes (logic lives in `TaskViewModel`).

### Dependency Injection (Hilt)
`DatabaseModule` (`di/`) is the single `@Module` with `@InstallIn(SingletonComponent::class)`. It provides `AppDatabase`, all DAOs, `TaskRepository`, and `CategoryRepository` as singletons. ViewModels use `@HiltViewModel` + `@Inject` constructor — screens obtain them via `hiltViewModel()`, `MainActivity` uses `by viewModels()`.

### Repository
`TaskRepository` and `CategoryRepository` handle their respective entity types. Reminder scheduling is delegated to `ReminderScheduler` (AlarmManager-based).

### ViewModels
- **`TaskViewModel`** — central ViewModel (extends `AndroidViewModel`); all task/subtask/reminder mutations go through here. Uses `flatMapLatest` to reactively switch the task list query when the filter changes. Includes quick reminder support (`setQuickReminder`, `deleteReminderForTask`). The `QuickReminderDialog` includes both a `DatePicker` and `TimePicker` so reminders can be set for any future date+time. Exposes `searchAllTasks(query)` for global search across all categories. Calls `TaskWidgetProvider.refreshAll()` after task mutations to keep the widget in sync.
- **`CategoryViewModel`** — category CRUD operations.
- **`ThemeViewModel`** — singleton for dark mode and language; persists to SharedPreferences (`"theme_pref"` / `"language_pref"`).
- **`SplashViewModel`** — controls the splash animation.
- **`BackupViewModel`** — export/import all app data as JSON via `BackupManager`. Injects `@ApplicationContext` and DAOs directly (not repositories) to access sync queries and bulk delete. After import, reschedules active future reminders via `ReminderScheduler`.

### Navigation
NavHost start destination: `"category_list"`. Routes:
- `category_list` → `CategoryListScreen`
- `tasks_by_category/{categoryId}` → `TasksByCategoryScreen`
- `task_detail/{taskId}` → `TaskDetailScreen`
- `reminders`, `daily_reminders`, `theme_settings`, `language_settings`
- `about` → `AboutScreen`
- `backup` → `BackupScreen`

Navigation drawer (hamburger menu) is an Android `DrawerLayout` wrapping a `ComposeView`, not a pure Compose drawer. Drawer items: Theme, Language, Daily Reminders, Backup, About.

Drawer navigation uses `popUpTo("category_list")` + `launchSingleTop = true` to prevent stacking multiple destinations. All drawer screen `onBackClick` handlers guard against empty back stack by checking `popBackStack()` return value and falling back to navigating to `category_list`.

### Background Work
- `ReminderScheduler` — schedules exact alarms via `AlarmManager.setExactAndAllowWhileIdle()`. Falls back to `setAndAllowWhileIdle()` if exact alarms are not permitted.
- `ReminderReceiver` — BroadcastReceiver that handles alarm fires, triggers `NotificationService`, and reschedules recurring reminders. Uses `goAsync()` for async DB access. Also reschedules daily reminders on `BOOT_COMPLETED` via `ReminderViewModel.rescheduleFromPrefs()`.
- `DailyReminderService` — separate BroadcastReceiver (in `services/` package, note plural) for morning/evening daily reminders. Uses `setExactAndAllowWhileIdle()` (not `setRepeating()`) for reliable delivery. Each alarm self-reschedules for the next day after firing. Uses `goAsync()` for async DB access.
- `ReminderViewModel` — non-Hilt ViewModel for daily reminder scheduling. Persists configuration (enabled, morning/evening hours) to SharedPreferences (`"daily_reminder_prefs"`). Exposes static `scheduleExactAlarm()`, `loadPrefs()`, and `rescheduleFromPrefs()` for use by receivers.
- Boot receiver (`RECEIVE_BOOT_COMPLETED`) re-schedules both task reminders and daily reminders after device restart.
- **Notification channels:** `"taksy_reminders"` (task reminders) and `"daily_reminders"` (daily summaries), both IMPORTANCE_HIGH.

### Backup System
- **`BackupManager`** (`utils/`) — stateless utility that serializes/deserializes all entities to/from JSON using `org.json` (no external dependencies). Format includes `backupVersion`, `exportDate`, and arrays for categories, tasks, subtasks, reminders. Uses `BackupImportException` with `ImportErrorType` enum (INVALID_JSON, MISSING_SECTION, INVALID_CATEGORY/TASK/SUBTASK/REMINDER, INVALID_DATE) for granular error reporting on malformed files — errors identify the exact record index that failed.
- **`BackupScreen`** — export creates a timestamped JSON file via SAF (`ActivityResultContracts.CreateDocument`); import reads via SAF (`ActivityResultContracts.OpenDocument`) with a confirmation dialog warning that all data will be replaced. Import errors show localized messages via `BackupState.ImportError`.
- Import clears all tables (reminders → subtasks → tasks → categories) then inserts from the JSON preserving original IDs. After insertion, active future reminders are rescheduled via `ReminderScheduler`.

### Widget
- **`TaskWidgetProvider`** (`widget/`) — `AppWidgetProvider` that renders a `RemoteViews` list of pending tasks. Updates every 30 minutes (`updatePeriodMillis`). Handles `ACTION_REFRESH` for manual refresh via the header button. Exposes `refreshAll(context)` static method used by `TaskViewModel` to trigger updates on task mutations (add, delete, complete, toggle subtask).
- **`TaskWidgetService`** / **`TaskWidgetRemoteViewsFactory`** — `RemoteViewsService` that queries `TaskDao.getPendingTasksSync()` (non-suspend, sorted by priority then due date). Each item shows a color-coded priority dot, task title, and due date (red if overdue).
- Widget layout: header with app name, task count (plurals), and refresh button; `ListView` with empty state. Minimum size 250×180dp, resizable. Tapping header or items opens `MainActivity`.
- Registered in `AndroidManifest.xml` with `BIND_REMOTEVIEWS` permission on the service.

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

When modifying Room entities, always add a migration in `AppDatabase.kt` and increment the version. The current version is **9**. Never use `fallbackToDestructiveMigration` in production builds.

## Recently Completed

- **Swipe-to-delete** — Material3 `SwipeToDismissBox` on tasks (with confirmation dialog) and subtasks (direct delete).
- **Global search** — search across all categories from `CategoryListScreen` TopAppBar, results with urgency colors and category name.
- **UI cleanup** — removed redundant creation date and non-functional info icon from task list items.
- **Task priority levels** — `TaskPrioridad` enum (NINGUNA/BAJA/MEDIA/ALTA), DB migration v8→v9, priority sorting in all queries, FilterChip selector, colored dot indicators.
- **Backup error handling** — granular `BackupImportException` with `ImportErrorType` enum, per-record validation, localized error messages (ES/EN).
- **Backup: reschedule reminders on import** — active future reminders rescheduled via `ReminderScheduler` after import; past reminders skipped.
- **Deprecation cleanup** — replaced all deprecated Compose APIs: `Icons.AutoMirrored.Filled.*`, `animateItem()`, `BorderStroke`.
- **Home screen widget** — pending tasks list with priority dots, due dates, task count, refresh button, auto-sync on task changes.

## Pending / Future Work

### High Priority
- **Unit tests** — no tests exist yet; add tests for `BackupManager` (round-trip JSON), `TaskViewModel` (auto-complete logic), and repositories

### Medium Priority — Functionality
- **Task notes/description** — the UI hint exists but the field is not implemented in the Task entity
- **Drag & drop to reorder tasks** — similar to the existing category reordering
- **Task archiving** — archive completed tasks instead of only delete
- **Recurring task dates** — repeat tasks on a schedule (separate from reminder recurrence)

### Medium Priority — Quality
- **Remove debug logs** — many `android.util.Log.d` calls remain in `ReminderScheduler`, `DailyReminderScreen`, etc.
- **Widget dark mode** — current widget has hardcoded light colors, should respond to system theme
- **Widget preview image** — add `android:previewImage` to `widget_task_info.xml` for the widget picker

### Nice to Have
- **Undo on swipe-to-delete** — Snackbar with "Undo" instead of confirmation dialog for smoother UX
- **Onboarding flow** — guide new users to create their first category
- **Statistics/dashboard** — tasks completed per week, most active category, streaks
