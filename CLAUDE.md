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
ViewModels (TaskViewModel, CategoryViewModel, ThemeViewModel, SplashViewModel,
            BackupViewModel, StatsViewModel)
  ↓ calls suspend funs / collects Flow
Repository (TaskRepository, CategoryRepository)
  ↓ delegates to DAOs
Room Database (AppDatabase, v14)
```

### Data Layer
- **Entities:** `Task` (with `TaskPrioridad` enum: NINGUNA/BAJA/MEDIA/ALTA, `TaskRecurrencia` enum: NINGUNA/DIARIA/SEMANAL/MENSUAL/ANUAL, `descripcion: String?`, `archivada: Boolean`, `orden: Int`, `recurrencia: TaskRecurrencia`, `fechaCompletada: Date?`), `Subtask`, `Category`, `Reminder`
- **DAOs:** `TaskDao`, `SubtaskDao`, `CategoryDao`, `ReminderDao` — each has `getAllXxxSync()` and `deleteAllXxx()` methods for backup operations
- **AppDatabase** uses TypeConverters for `Date` serialization and has 14 tracked migrations

Key business rule: when all subtasks of a task are completed, the parent task auto-completes (logic lives in `TaskViewModel`).

### Dependency Injection (Hilt)
`DatabaseModule` (`di/`) is the single `@Module` with `@InstallIn(SingletonComponent::class)`. It provides `AppDatabase`, all DAOs, `TaskRepository`, `CategoryRepository`, and `ReminderSchedulerContract` as singletons. ViewModels use `@HiltViewModel` + `@Inject` constructor — screens obtain them via `hiltViewModel()`, `MainActivity` uses `by viewModels()`.

### Repository
`TaskRepository` and `CategoryRepository` handle their respective entity types. Reminder scheduling is abstracted via `ReminderSchedulerContract` interface (implemented by `ReminderScheduler`), injected through Hilt.

### ViewModels
- **`TaskViewModel`** — central ViewModel (extends `AndroidViewModel`); all task/subtask/reminder mutations go through here. Uses `flatMapLatest` to reactively switch the task list query when the filter changes. Receives `ReminderSchedulerContract` via Hilt injection for testability. Includes quick reminder support (`setQuickReminder`, `deleteReminderForTask`). The `QuickReminderDialog` includes both a `DatePicker` and `TimePicker` so reminders can be set for any future date+time. Exposes `searchAllTasks(query)` for global search across all categories. Calls `TaskWidgetProvider.refreshAll()` after task mutations to keep the widget in sync.
- **`CategoryViewModel`** — category CRUD operations.
- **`ThemeViewModel`** — singleton for dark mode and language; persists to SharedPreferences (`"theme_pref"` / `"language_pref"`).
- **`SplashViewModel`** — controls the splash animation.
- **`BackupViewModel`** — export/import all app data as JSON via `BackupManager`. Injects `@ApplicationContext`, DAOs directly (not repositories), and `ReminderSchedulerContract` to access sync queries, bulk delete, and reminder rescheduling after import.
- **`StatsViewModel`** — Hilt-injected `ViewModel`; combines `TaskRepository.getAllTasks()` and `CategoryRepository.getAllCategories()` into a single `StatsUiState` exposed as a `stateIn` `StateFlow`. Derives totals, completion rate, streak (consecutive days ending today/yesterday with at least one completion), last-7-days bucket counts using `fechaCompletada`, and top 5 categories by task count.

### Navigation
NavHost start destination: `"onboarding"` (first launch) or `"category_list"` (subsequent). Routes:
- `onboarding` → `OnboardingScreen`
- `category_list` → `CategoryListScreen`
- `tasks_by_category/{categoryId}` → `TasksByCategoryScreen`
- `task_detail/{taskId}` → `TaskDetailScreen`
- `reminders`, `daily_reminders`, `theme_settings`, `language_settings`
- `about` → `AboutScreen`
- `backup` → `BackupScreen`
- `stats` → `StatsScreen`

Navigation drawer (hamburger menu) is an Android `DrawerLayout` wrapping a `ComposeView`, not a pure Compose drawer. Drawer items: Theme, Language, Daily Reminders, Stats, Backup, About.

Drawer navigation uses `popUpTo("category_list")` + `launchSingleTop = true` to prevent stacking multiple destinations. All drawer screen `onBackClick` handlers guard against empty back stack by checking `popBackStack()` return value and falling back to navigating to `category_list`.

### Background Work
- `ReminderSchedulerContract` — interface defining `scheduleReminder()`, `cancelReminder()`, `cancelAllReminders()`. Implemented by `ReminderScheduler` and provided as singleton via Hilt. Enables unit testing ViewModels with fake schedulers.
- `ReminderScheduler` — implements `ReminderSchedulerContract`; schedules exact alarms via `AlarmManager.setExactAndAllowWhileIdle()`. Falls back to `setAndAllowWhileIdle()` if exact alarms are not permitted.
- `ReminderReceiver` — BroadcastReceiver that handles alarm fires, triggers `NotificationService`, and reschedules recurring reminders. Uses `goAsync()` for async DB access. Also reschedules daily reminders on `BOOT_COMPLETED` via `ReminderViewModel.rescheduleFromPrefs()`. Instantiates `ReminderScheduler` directly (not Hilt-managed).
- `DailyReminderService` — BroadcastReceiver (in `service/` package) for morning/evening daily reminders. Uses `setExactAndAllowWhileIdle()` (not `setRepeating()`) for reliable delivery. Each alarm self-reschedules for the next day after firing. Uses `goAsync()` for async DB access.
- `DailyReminderManager` — stateless `object` in `service/` for daily reminder scheduling. Persists configuration (enabled, morning/evening hours) to SharedPreferences (`"daily_reminder_prefs"`). Exposes `loadPrefs()`, `scheduleDailyReminders()`, `cancelDailyReminders()`, `scheduleExactAlarm()`, `rescheduleFromPrefs()`. Called from `DailyReminderScreen`, `DailyReminderService` (self-reschedule), and `ReminderReceiver` (boot reschedule).
- Boot receiver (`RECEIVE_BOOT_COMPLETED`) re-schedules both task reminders and daily reminders after device restart (via `DailyReminderManager.rescheduleFromPrefs()`).
- **Notification channels:** `"taksy_reminders"` (task reminders) and `"daily_reminders"` (daily summaries), both IMPORTANCE_HIGH.

### Backup System
- **`BackupManager`** (`utils/`) — stateless utility that serializes/deserializes all entities to/from JSON using `org.json` (no external dependencies). Format includes `backupVersion`, `exportDate`, and arrays for categories, tasks, subtasks, reminders. Uses `BackupImportException` with `ImportErrorType` enum (INVALID_JSON, MISSING_SECTION, INVALID_CATEGORY/TASK/SUBTASK/REMINDER, INVALID_DATE) for granular error reporting on malformed files — errors identify the exact record index that failed.
- **`BackupScreen`** — export creates a timestamped JSON file via SAF (`ActivityResultContracts.CreateDocument`); import reads via SAF (`ActivityResultContracts.OpenDocument`) with a confirmation dialog warning that all data will be replaced. Import errors show localized messages via `BackupState.ImportError`.
- Import clears all tables (reminders → subtasks → tasks → categories) then inserts from the JSON preserving original IDs. After insertion, active future reminders are rescheduled via `ReminderSchedulerContract`.

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

When modifying Room entities, always add a migration in `AppDatabase.kt` and increment the version. The current version is **14**. Never use `fallbackToDestructiveMigration` in production builds.

## Recently Completed

- **Phase 1 — Stability & Quality**: Removed 80 debug logs, moved 17 hardcoded strings to resources (ES/EN), extracted 12 widget colors to `colors.xml` + `values-night/`, 74 unit tests (BackupManager 31, TaskViewModel 14, TaskRepository 15, CategoryRepository 11, misc 3).
- **Task notes/description** — `descripcion: String?` field, migration v9→v10, editable notes section in TaskDetailScreen with debounced save, BackupManager support.
- **Undo swipe-to-delete** — swipe deletes immediately + Snackbar with "Undo" action. `restoreTask()` re-inserts via `@Insert(REPLACE)`. Removed confirmation dialog.
- **Task archiving** — `archivada: Boolean` field, migration v10→v11. Swipe left-to-right to archive. Archive toggle in TopAppBar shows archived section. All main queries filter `archivada = 0`.
- **Widget dark mode** — colors in `values-night/colors.xml`. Widget preview layout via `previewLayout`.
- **Splash duration** — reduced from 5.5s to 2.5s with tightened animations.
- **Drag & drop to reorder tasks** — `orden: Int` field, migration v11→v12, drag & drop in `TasksByCategoryScreen` reusing `CategoryListScreen` pattern. Queries sort by `orden ASC` after state. BackupManager support with backward-compatible import.
- **Recurring task dates** — `TaskRecurrencia` enum (NINGUNA/DIARIA/SEMANAL/MENSUAL/ANUAL), migration v12→v13. Completing a recurring task clones it with advanced `fechaVencimiento`. Recurrence selector in InlineTaskInput and TaskDetailScreen. Purple refresh icon indicator in TaskListItem.
- **Statistics dashboard** — `fechaCompletada: Date?` on Task (set by `TaskRepository.toggleTaskStatus` and the subtask auto-complete path in `TaskViewModel`), migration v13→v14. `StatsViewModel` (Hilt) combines tasks+categories flows. `StatsScreen` shows 6 stat cards, last-7-days bar chart (today on the right), and top categories (max 5) with completion progress bars. BackupManager serializes/parses `fechaCompletada` (backward-compatible: missing field treated as null).
- **Onboarding flow** — 3-page `HorizontalPager` onboarding (organize tasks, subtasks/recurrence, smart reminders). SharedPreferences flag `onboarding_completed` in `"onboarding_prefs"`. Conditional nav startDestination. Skip/Next/Get Started buttons with animated page indicators.
- **ReminderSchedulerContract interface** — extracted for testability; injected via Hilt.
- **Home screen widget** — pending tasks list with priority dots, due dates, task count, refresh button, auto-sync on task changes.

## Pending / Future Work

**Phase 3 is fully complete.** All v1.1 features (drag & drop, recurring tasks, onboarding, statistics dashboard) are merged and described in the "Recently Completed" section above.

### Phase 4 — Tech Debt
- ~~Consolidate hardcoded Compose colors~~ — DONE: 13 semantic constants in `ui/theme/Color.kt` (priority, due-date, recurrence, swipe, success/gold/dark-card).
- ~~Eliminate deprecated `LocaleHelper` APIs~~ — DONE: rewritten to use only `createConfigurationContext()` and `Configuration#setLocale()`. `MainActivity.attachBaseContext()` applies the saved locale.
- ~~Evaluate `ReminderViewModel` Hilt migration~~ — DONE (demoted, not migrated): converted to `object DailyReminderManager` in `service/`. It was not really a ViewModel (no `StateFlow`/`viewModelScope`), and `BroadcastReceiver`s don't benefit from Hilt for stateless helpers. Removed the dead `alarmManager` field and `initializeAlarmManager()` method.
