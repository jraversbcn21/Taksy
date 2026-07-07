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

**MVVM** with Repository pattern + a thin domain layer:

```
UI (Compose screens, ui/components/*)
  ↓ observes StateFlow
ViewModels (TaskViewModel, CategoryViewModel, ThemeViewModel,
            SplashViewModel, BackupViewModel, StatsViewModel)
  ↓ delegates to
Domain use cases (CompleteTaskUseCase, SubtaskUseCase,
                  TaskReminderUseCase, RecurrenceCalculator)
  ↓ calls
Repositories (TaskRepository, CategoryRepository, PreferencesRepository)
  ↓ delegates to DAOs / SharedPreferences
Room Database (AppDatabase, v14)
```

### Data Layer
- **Entities:** `Task` (`TaskPrioridad`: NINGUNA/BAJA/MEDIA/ALTA, `TaskRecurrencia`: NINGUNA/DIARIA/SEMANAL/MENSUAL/ANUAL, `descripcion: String?`, `archivada: Boolean`, `orden: Int`, `fechaCompletada: Date?`), `Subtask`, `Category`, `Reminder`.
- **DAOs:** `TaskDao`, `SubtaskDao`, `CategoryDao`, `ReminderDao` — each exposes `getAllXxxSync()` and `deleteAllXxx()` for backup operations.
- **AppDatabase** uses TypeConverters for `Date` and has 14 tracked migrations.
- **`data/preferences/PreferencesRepository`** — Hilt `@Singleton`. Owns all four SharedPreferences files (`theme_pref`, `language_pref`, `daily_reminder_prefs`, `onboarding_prefs`) behind typed getters/setters. Also constructible manually with a `Context` so `LocaleHelper.attachBaseContext()` can read the saved language before the Hilt graph is built. Holds the `DailyReminderPrefs` data class consumed by `DailyReminderManager`.

Key business rule: when all subtasks of a task are completed, the parent task auto-completes (logic lives in `SubtaskUseCase`, surfaced via `TaskViewModel`).

### Domain Layer (`domain/`)
- **`CompleteTaskUseCase`** — single entry point for marking a task complete. Cancels reminders, advances recurring tasks via `RecurrenceCalculator`, and writes the new clone + completed original in one Room transaction (`TaskRepository.completeRecurringTask`). Called by both `TaskViewModel.toggleTaskStatus` (radio button path) and `SubtaskUseCase.toggle` (auto-complete branch); collapsing the two paths eliminated the recurring-task duplication bug.
- **`SubtaskUseCase`** — observe/add/toggle/delete on subtasks. `toggle()` returns `true` when the parent task auto-completed, so the caller can refresh the widget.
- **`TaskReminderUseCase`** — observe/add/update/delete/setActive/setQuick/deleteAllForTask on reminders. Wraps `ReminderSchedulerContract` so reminder DB writes and alarm scheduling stay in sync.
- **`RecurrenceCalculator`** — `advance(Date, TaskRecurrencia): Date` for tasks and `advance(Date, TipoRecordatorio): Date?` for reminders (returns null on UNA_VEZ). Covered by 10 JVM unit tests.

### Dependency Injection (Hilt)
`di/DatabaseModule` provides `AppDatabase`, all DAOs, both repositories, and `ReminderSchedulerContract` as `@Singleton`. Use cases (`CompleteTaskUseCase`, `SubtaskUseCase`, `TaskReminderUseCase`) and `PreferencesRepository` use `@Inject` constructor with `@Singleton` and are auto-provided by Hilt — no module entry needed. ViewModels use `@HiltViewModel`; screens obtain them via `hiltViewModel()`; `MainActivity` uses `by viewModels()`. `BroadcastReceiver`s (`ReminderReceiver`, `DailyReminderService`) are `@AndroidEntryPoint` with `@Inject lateinit var` fields. The widget (`TaskWidgetProvider` and `TaskWidgetRemoteViewsFactory`) uses `WidgetEntryPoint` + `EntryPointAccessors.fromApplication()` because `RemoteViewsFactory` can't be a Hilt entry point.

### Repositories
- **`TaskRepository`** — task/subtask/reminder DAO facade. Exposes `completeRecurringTask(task, clone)` which performs both writes inside `appDatabase.withTransaction { }` to keep Flow emissions atomic. `cancelAllRemindersForTask(taskId)` flips `activo=false` on every reminder of a task.
- **`CategoryRepository`** — category CRUD + `initializeDefaultCategories(Context)`. The seeded names come from `DefaultCategories.getAll(Context)`, which reads `R.array.default_category_names` so the eight default categories localize correctly.
- **`PreferencesRepository`** — described in Data Layer above.

### ViewModels
- **`TaskViewModel`** — thin facade (extends `AndroidViewModel`). Owns task CRUD, the filter `StateFlow`, the `togglingTaskIds` guard, and the `searchAllTasks` flow. Delegates subtask operations to `SubtaskUseCase`, reminder operations to `TaskReminderUseCase`, and task completion to `CompleteTaskUseCase`. `addTask(input: TaskInput)` is the single entry point — the four legacy overloads are collapsed. Calls `TaskWidgetProvider.refreshAll(context)` after mutations.
- **`CategoryViewModel`** — category CRUD; injects `@ApplicationContext` so `initializeDefaultCategories` can read the localized array.
- **`ThemeViewModel`** — dark mode + language. Injects `PreferencesRepository`; calls `AppCompatDelegate.setApplicationLocales()` on language change to trigger activity recreation.
- **`SplashViewModel`** — splash animation state.
- **`BackupViewModel`** — export/import via `BackupManager`. Injects `@ApplicationContext`, DAOs directly (not repositories), and `ReminderSchedulerContract` to reschedule reminders after import.
- **`StatsViewModel`** — combines `TaskRepository.getAllTasks()` and `CategoryRepository.getAllCategories()` into a `StatsUiState` via `stateIn`. Derives totals, completion rate, streak, last-7-days bucket counts (using `fechaCompletada`), and top 5 categories by task count.

### Navigation
`ui/navigation/AppNavGraph.kt` owns the `NavHost` and every `composable(...)` route. `MainActivity` only wires the `DrawerLayout` + `ComposeView` hybrid and the activity lifecycle.

Start destination: `"onboarding"` (first launch) or `"category_list"` (subsequent). Routes:
- `onboarding` → `OnboardingScreen`
- `category_list` → `CategoryListScreen`
- `tasks_by_category/{categoryId}` → `TasksByCategoryScreen`
- `task_detail/{taskId}` → `TaskDetailScreen`
- `reminders`, `daily_reminders`, `theme_settings`, `language_settings`
- `about` → `AboutScreen`
- `backup` → `BackupScreen`
- `stats` → `StatsScreen`

`AppNavGraph` defines a private `NavHostController.popOrHome()` extension that collapses the repeated "popBackStack or fall back to category_list" pattern used by every drawer screen.

Navigation drawer is an Android `DrawerLayout` wrapping a `ComposeView` (not a pure Compose drawer — kept as a documented architectural choice). Drawer items: Theme, Language, Daily Reminders, Stats, Backup, About. Drawer navigation uses `popUpTo("category_list")` + `launchSingleTop = true`.

### Background Work
- **`ReminderSchedulerContract`** — interface with `scheduleReminder()` and `cancelReminder()`. Implemented by `ReminderScheduler` and provided as `@Singleton` via Hilt.
- **`ReminderScheduler`** — delegates to `AlarmPolicy.scheduleExactOrFallback()` for the SDK check, exact/inexact branching, and `SecurityException` fallback.
- **`AlarmPolicy`** (`service/AlarmPolicy.kt`) — `object` with `scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)`. Single source of truth for the SDK 31+ `canScheduleExactAlarms()` check and the `setExactAndAllowWhileIdle` → `setAndAllowWhileIdle` fallback used by both `ReminderScheduler` and `DailyReminderManager`.
- **`ReminderReceiver`** — `@AndroidEntryPoint` `BroadcastReceiver`. `@Inject lateinit var scheduler: ReminderSchedulerContract` and `@Inject lateinit var database: AppDatabase`. Handles alarm fires (`NotificationService`, `RecurrenceCalculator.advance` for recurring reminders) and reschedules everything on `BOOT_COMPLETED` (active task reminders + `DailyReminderManager.rescheduleFromPrefs()`).
- **`DailyReminderService`** — `@AndroidEntryPoint` `BroadcastReceiver` (in `service/`). `@Inject lateinit var taskRepository: TaskRepository`. Uses `setExactAndAllowWhileIdle()` (via `AlarmPolicy`) and self-reschedules for the next day after firing.
- **`DailyReminderManager`** — stateless `object` for daily-reminder scheduling. Reads/writes config via `PreferencesRepository(context)`. Exposes `loadPrefs()`, `scheduleDailyReminders()`, `cancelDailyReminders()`, `scheduleExactAlarm()` (delegates to `AlarmPolicy`), `rescheduleFromPrefs()`. Called from `DailyReminderScreen`, `DailyReminderService` (self-reschedule), and `ReminderReceiver` (boot reschedule).
- Boot receiver (`RECEIVE_BOOT_COMPLETED`) re-schedules both task reminders and daily reminders after device restart.
- **Notification channels:** `"taksy_reminders"` (task reminders) and `"daily_reminders"` (daily summaries), both IMPORTANCE_HIGH.

### Backup System
- **`BackupManager`** (`utils/`) — stateless utility that serializes/deserializes all entities to/from JSON using `org.json`. Includes `backupVersion`, `exportDate`, and arrays for categories, tasks, subtasks, reminders. `BackupImportException` + `ImportErrorType` enum (INVALID_JSON, MISSING_SECTION, INVALID_CATEGORY/TASK/SUBTASK/REMINDER, INVALID_DATE) report the exact failing record index.
- **`BackupScreen`** — export via SAF `CreateDocument`; import via SAF `OpenDocument` with a "this will replace all data" confirmation dialog. Errors localized through `BackupState.ImportError`.
- Import clears all tables (reminders → subtasks → tasks → categories) then inserts from the JSON preserving original IDs. After insertion, active future reminders are rescheduled via `ReminderSchedulerContract`.

### Widget
- **`TaskWidgetProvider`** (`widget/`) — `AppWidgetProvider` that renders pending tasks. Updates every 30 minutes via `updatePeriodMillis`. Handles `ACTION_REFRESH`. Exposes `refreshAll(context)` used by `TaskViewModel` after task mutations. Resolves `TaskDao` via `EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)`.
- **`WidgetEntryPoint`** — `@EntryPoint @InstallIn(SingletonComponent::class)` exposing `taskDao()`.
- **`TaskWidgetService` / `TaskWidgetRemoteViewsFactory`** — `RemoteViewsService` that queries `TaskDao.getPendingTasksSync()` (non-suspend, sorted by priority then due date). Each item shows a color-coded priority dot, task title, and due date (red if overdue). `TaskDao` is fetched once via a lazy delegate that calls `EntryPointAccessors`.
- Widget layout: header (app name, task count plurals, refresh button) + `ListView` with empty state. Minimum 250×180 dp, resizable. Tapping header or items opens `MainActivity`. Registered in `AndroidManifest.xml` with `BIND_REMOTEVIEWS`.

### Splash Screen
- Native Android splash (`Theme.SplashScreen`) uses `splash_icon.xml` — an `InsetDrawable` wrapping `ticksy_icon.png` with 24 dp inset.
- After the native splash, the Compose `SplashScreen` shows the animated icon (150 dp, scale + pulse + alpha) with app name and subtitle.

## Key Tech Details

- **Min SDK 26**, Target SDK 35, Java 11, Kotlin 2.0.21, AGP 8.10.1
- **Compose BOM** 2024.09.00 — do not specify individual Compose library versions
- **Room** 2.6.1 with KSP (not kapt)
- **Hilt** 2.51.1 — `TicksyApplication` extends `HiltAndroidApp`; module wiring lives in `di/DatabaseModule`. Pre-2.49 versions break under AGP 8 because the Hilt Gradle plugin looks for the javac output at `intermediates/javac/debug/classes/...` instead of `intermediates/javac/debug/compileDebugJavaWithJavac/classes/...`, which trips `@AndroidEntryPoint` on `BroadcastReceiver`s during the ASM bytecode-transform pass — do **not** downgrade below 2.51.
- **Localization:** Spanish (`values/strings.xml`) is the default; English in `values-en/strings.xml`. Locale changes apply via `LocaleHelper.wrap()` (manually instantiates `PreferencesRepository`) and require activity recreation. `locales_config.xml` declared for Android 13+ per-app language support.
- **Database name:** `ticksy_database` (note spelling differs from app name)
- **Manifest application class:** `.TicksyApplication` (spelling differs from project name "Taksy")
- **Permissions:** `POST_NOTIFICATIONS`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`
- **ProGuard/R8** enabled for release builds
- **Room schema** exported to `app/schemas/` via KSP arg `room.schemaLocation`

## Adding Database Migrations

When modifying Room entities, always add a migration in `AppDatabase.kt` and increment the version. The current version is **14**. Never use `fallbackToDestructiveMigration` in production builds.

## Coding Conventions

- **No `!!`** anywhere in `src/main`. Use `?.let { }`, `mapNotNull`, `takeIf`, or `requireNotNull(x) { "context" }` with a message. Grep-enforced.
- **Use cases own domain logic.** Anything that mutates more than one entity or needs to coordinate the scheduler should be a use case. ViewModels are thin and delegate.
- **No SharedPreferences access outside `PreferencesRepository`.** If you need a new key, add it there.
- **No alarm scheduling outside `AlarmPolicy`.** New alarm sites call `AlarmPolicy.scheduleExactOrFallback()`.
- **No category names hardcoded in Kotlin.** Default category names live in the `default_category_names` string-array; only metadata (color, icon) stays inline.
- **Receivers and services that need DI** are `@AndroidEntryPoint` with `@Inject lateinit var` fields. Don't construct `TaskRepository(...)` or `ReminderScheduler(context)` manually.
- **Widget code** accesses DI via `EntryPointAccessors` because `RemoteViewsFactory` isn't an `@AndroidEntryPoint` target.

## Pending / Future Work

- Smoke-test the Step 5 fix on a device. If the duplication still reproduces, file a new bug against `TaskViewModel.toggleTaskStatus` + `togglingTaskIds` guard and look upstream of `CompleteTaskUseCase`.
- Optional: shrink `TasksByCategoryScreen` further. The drag-and-drop block is the main remaining inline section; it shares state heavily with the LazyColumn, so extraction is non-trivial.
- Optional: `MainActivity` bulk is the `DrawerLayout` AndroidView block; moving it into its own file is fine but offers little incremental value.
