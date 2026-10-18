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
            BackupViewModel, StatsViewModel)
  ↓ delegates to
Domain use cases (CompleteTaskUseCase, SubtaskUseCase,
                  TaskReminderUseCase, DeleteTaskUseCase,
                  DeleteCategoryUseCase,
                  RecurrenceCalculator)
  ↓ calls
Repositories (TaskRepository, CategoryRepository, PreferencesRepository)
  ↓ delegates to DAOs / SharedPreferences
Room Database (AppDatabase, v16)
```

### Data Layer
- **Entities:** `Task` (`TaskPrioridad`: NINGUNA/BAJA/MEDIA/ALTA, `TaskRecurrencia`: NINGUNA/DIARIA/SEMANAL/MENSUAL/ANUAL, `descripcion: String?`, `fechaCompletada: Date?`), `Subtask`, `Category` (has `orden` for drag-reorder), `Reminder` (always one-shot; no type field).
- **DAOs:** `TaskDao`, `SubtaskDao`, `CategoryDao`, `ReminderDao` — each exposes `getAllXxxSync()` and `deleteAllXxx()` for backup operations.
- **AppDatabase** uses TypeConverters for `Date` and has 16 tracked migrations. v15 dropped the legacy `tasks.archivada`, `tasks.orden` and `reminders.tipoRecordatorio` columns (table recreation, keeps the AUTOINCREMENT counter). v16 has no schema change: it deletes subtasks/reminders orphaned by task deletes before the cascade fix.
- **`data/preferences/PreferencesRepository`** — Hilt `@Singleton`. Owns all four SharedPreferences files (`theme_pref`, `language_pref`, `daily_reminder_prefs`, `onboarding_prefs`) behind typed getters/setters. Also constructible manually with a `Context` so `LocaleHelper.attachBaseContext()` can read the saved language before the Hilt graph is built. Holds the `DailyReminderPrefs` data class consumed by `DailyReminderManager` and the `ThemeMode` enum. `getThemeMode()` reads `theme_mode`; if absent it migrates the legacy `is_dark_mode` boolean (explicit choice kept), otherwise defaults to SYSTEM.

Task lists in a category (`TaskDao.getTasksByCategory` / `searchTasksByCategory`) hide completed occurrences of recurring tasks — only the next pending occurrence matters there; they stay in the DB for stats and backup. Order: pending first, then priority (ALTA→NINGUNA), then due date (no date last), then newest. There is no manual reordering of tasks. Completed rows show "Completed on <date>" (`fechaCompletada`).

Key business rule: when all subtasks of a task are completed, the parent task auto-completes (logic lives in `SubtaskUseCase`, surfaced via `TaskViewModel`).

### Domain Layer (`domain/`)
- **`CompleteTaskUseCase`** — single entry point for marking a task complete. Cancels reminders, advances recurring tasks via `RecurrenceCalculator`, carries the task's reminders over to the next occurrence (each advanced with the task's recurrence; a date still in the past is stored but not scheduled, since AlarmManager would fire it immediately), and writes the new clone + completed original in one Room transaction (`TaskRepository.completeRecurringTask`). `execute(task): Boolean` is idempotent via compare-and-swap: it returns `true` only if this call won the race and actually completed the task, `false` if another caller (radio button vs. subtask auto-complete) got there first. Called by both `TaskViewModel.toggleTaskStatus` (radio button path) and `SubtaskUseCase.toggle` (auto-complete branch) — the CAS, not the route collapse, is what prevents the recurring-task duplication bug.
- **`SubtaskUseCase`** — observe/add/toggle/delete on subtasks. `toggle()` returns `true` when the parent task auto-completed, so the caller can refresh the widget.
- **`TaskReminderUseCase`** — observe/setQuick/deleteAllForTask on reminders. Wraps `ReminderSchedulerContract` so reminder DB writes and alarm scheduling stay in sync. The only reminder is the quick one (one per task, one-shot); recurrence lives on the task, not the reminder.
- **`RecurrenceCalculator`** — `advance(Date, TaskRecurrencia): Date`. Covered by JVM unit tests.
- **`DeleteTaskUseCase`** — `delete(task): DeletedTask` removes the task with its subtasks and reminders in one transaction (`TaskRepository.deleteTaskWithChildren`), then cancels the alarms; `restore(deleted)` re-inserts all of it with the original ids (`restoreDeletedTask`) and reschedules active future reminders. `TaskViewModel` keeps the `DeletedTask` until the undo snackbar is used and serialises delete/restore with a `Mutex`, so a quick undo waits for the delete to commit. There is no foreign key, so deleting only the task row used to leave invisible orphans whose alarms kept firing.
- **`DeleteCategoryUseCase`** — deletes a category together with all its tasks (incl. completed), their subtasks and reminders in one transaction (`TaskRepository.deleteTasksInCategory`), then cancels the reminders' alarms after commit. Needed because there is no foreign key between `tasks.categoriaId` and `categories`: deleting only the category would leave invisible orphan tasks that still appear in the widget and keep firing alarms.

### Dependency Injection (Hilt)
`di/DatabaseModule` provides `AppDatabase`, all DAOs, both repositories, and `ReminderSchedulerContract` as `@Singleton`. Use cases (`CompleteTaskUseCase`, `SubtaskUseCase`, `TaskReminderUseCase`, `DeleteTaskUseCase`, `DeleteCategoryUseCase`) and `PreferencesRepository` use `@Inject` constructor with `@Singleton` and are auto-provided by Hilt — no module entry needed. ViewModels use `@HiltViewModel`; screens obtain them via `hiltViewModel()`; `MainActivity` uses `by viewModels()`. `BroadcastReceiver`s (`ReminderReceiver`, `DailyReminderService`) are `@AndroidEntryPoint` with `@Inject lateinit var` fields. The widget (`TaskWidgetProvider` and `TaskWidgetRemoteViewsFactory`) uses `WidgetEntryPoint` + `EntryPointAccessors.fromApplication()` because `RemoteViewsFactory` can't be a Hilt entry point.

### Repositories
- **`TaskRepository`** — task/subtask/reminder DAO facade. Exposes `completeTaskIfPending(taskId): Boolean` (atomic `UPDATE ... WHERE estado = 'PENDIENTE'` via `TaskDao.completeTaskIfPending`, compare-and-swap on the DB row) and `completeRecurringTask(task, clone, cloneReminders): List<Reminder>?`, which performs the same CAS inside `appDatabase.withTransaction { }` and only inserts the clone (and its inherited reminders) if the CAS won, returning the inserted reminders or `null` if it lost — this is what makes concurrent completion attempts safe. `deleteTaskWithChildren(task): DeletedTask` / `restoreDeletedTask(deleted)` back task delete and undo. `deleteTasksInCategory(categoryId): List<Long>` deletes a category's tasks/subtasks/reminders in one transaction and returns the reminder ids to cancel. `markTaskPending(taskId)` reverts completion and clears `fechaCompletada`. `cancelAllRemindersForTask(taskId)` flips `activo=false` on every reminder of a task via a single `UPDATE`.
- **`CategoryRepository`** — category CRUD + `initializeDefaultCategories(Context)`. The seeded names come from `DefaultCategories.getAll(Context)`, which reads `R.array.default_category_names` so the eight default categories localize correctly.
- **`PreferencesRepository`** — described in Data Layer above.

### ViewModels
- **`TaskViewModel`** — thin facade (extends `AndroidViewModel`). Owns task CRUD, the `togglingTaskIds` guard, and the `searchAllTasks` flow. Delegates subtask operations to `SubtaskUseCase`, reminder operations to `TaskReminderUseCase`, and task completion to `CompleteTaskUseCase`. `addTask(input: TaskInput)` is the single entry point — the four legacy overloads are collapsed. Calls `TaskWidgetProvider.refreshAll(context)` after mutations. Every mutation runs through `launchSafe(@StringRes errorMessage)`: a failure is logged and emitted on `errors: Flow<Int>` (a buffered `Channel`, one-shot).
- **`CategoryViewModel`** — same `launchSafe`/`errors` pattern as `TaskViewModel`; category CRUD (delete goes through `DeleteCategoryUseCase` + widget refresh); injects `@ApplicationContext` so `initializeDefaultCategories` can read the localized array.
- **`ThemeViewModel`** — `themeMode: StateFlow<ThemeMode>` (LIGHT/DARK/SYSTEM) + language. `MainActivity` resolves SYSTEM with `isSystemInDarkTheme()` and re-applies `enableEdgeToEdge(SystemBarStyle.auto { isDark })` so the status/navigation bars follow the app theme, not the device's. Injects `PreferencesRepository`; calls `AppCompatDelegate.setApplicationLocales()` on language change to trigger activity recreation.
- **`BackupViewModel`** — export/import via `BackupManager`. Injects `@ApplicationContext`, `AppDatabase` (for `withTransaction`), DAOs directly (not repositories), and `ReminderSchedulerContract` to reschedule reminders after import.
- **`StatsViewModel`** — combines `TaskRepository.getAllTasks()` and `CategoryRepository.getAllCategories()` into a `StatsUiState` via `stateIn`. Derives totals, completion rate, streak, last-7-days bucket counts (using `fechaCompletada`), and top 5 categories by task count.

### Navigation
`ui/navigation/AppNavGraph.kt` owns the `NavHost` and every `composable(...)` route. `ui/navigation/AppDrawer.kt` (`DrawerScreen`) holds the `DrawerLayout` + `ComposeView` hybrid, the `NavigationView` menu and the language-change confirm dialog. `MainActivity` only handles the activity lifecycle, theme/system bars and the error Toasts. `ThemeMode.isDark()` lives in `ui/theme/Theme.kt`.

Start destination: `"onboarding"` (first launch) or `"category_list"` (subsequent). Routes:
- `onboarding` → `OnboardingScreen`
- `category_list` → `CategoryListScreen` — home screen: time-of-day greeting (`GreetingUtils`), a row of tasks due today, and the category lists with done/total progress (the screen scrolls normally; holding a list for 1 s lifts it for drag-reorder, saved once on release). Own palette and font in `ui/theme/HomeTheme.kt` (`HomePalette.Light`/`Dark`, Plus Jakarta Sans in `res/font/`). Today cards whose due time has passed show the time and border in red (re-checked every minute; tasks without a time never do). A completed recurring occurrence in the today row can't be un-ticked: its next occurrence already exists, so reopening it would leave two pending.
- `tasks_by_category/{categoryId}` → `TasksByCategoryScreen` — same visual language as the home screen: header with the category icon and done/total progress, an "Add task" pill that expands into `InlineTaskInput` (title, priority, recurrence; stays open to add several; new tasks have no due date, it is set in the detail or via the reminder), Pending/Completed sections of `TaskListItem` cards (circle to complete, swipe left to delete with undo, long-press to rename, bell for the quick reminder). The category colour (`Category.accentColor()` in `HomeTheme.kt`) is the accent.
- `task_detail/{taskId}` → `TaskDetailScreen` — same visual language as the home screen. Due date (Material3 date picker, then time picker with a "No time" option stored as 00:00; clearable), quick reminder (same `QuickReminderDialog` as the list bell), priority and recurrence (`OptionPills`, shared with `InlineTaskInput`), and subtasks with progress. Every change is saved immediately. The route observes the task live (`getAllTasks().map { find }`) so completing the last subtask shows the task as completed without leaving the screen.
- `category_management` → `CategoryManagementScreen` — same visual language as Settings. "Add category" pill, rows with icon tile and task count (all tasks, incl. completed occurrences); tap a row to edit, trash icon to delete with a warning showing how many tasks go with it. `AddCategoryDialog` (name, 10 colours, 10 icons, live preview) and `DeleteCategoryDialog` share `HomeDialogSurface` / `DialogTitle` / `DialogButtons` (in `AddCategoryDialog.kt`) — reuse them for the remaining old-style dialogs.
- `settings` → `SettingsScreen` — same visual language as the home screen (`HomePalette` + Plus Jakarta Sans). Theme tiles: system/light/dark; language rows, which go through MainActivity's confirm dialog because it recreates the activity; a row to `daily_reminders` showing on/off and the two times (read via `DailyReminderManager.loadPrefs`).
- `daily_reminders` → `DailyReminderScreen` (reached from Settings) — same visual language as Settings. Every change (on/off switch, morning/evening time via a Material3 `TimePicker` dialog) is saved and rescheduled immediately through `DailyReminderManager.scheduleDailyReminders`, which also persists `enabled=false` so a disabled reminder is not revived on boot. Turning it on shows `ExactAlarmPermissionDialog` if the exact-alarm permission is missing.
- `about` → `AboutScreen`
- `backup` → `BackupScreen`
- `stats` → `StatsScreen` — same visual language as Settings. Completion-rate ring + streak, pending / active-category counters, last-7-days bars (today highlighted; day initials localized in the UI from `DayCompletion.day`), top 5 categories with icon tile and progress.

`AppNavGraph` defines a private `NavHostController.popOrHome()` extension that collapses the repeated "popBackStack or fall back to category_list" pattern used by every drawer screen.

Navigation drawer is an Android `DrawerLayout` wrapping a `ComposeView` (not a pure Compose drawer — kept as a documented architectural choice). Drawer items: Manage Categories, Settings, Stats, Backup, About. Drawer navigation uses `popUpTo("category_list")` + `launchSingleTop = true`.

### Background Work
- **`ReminderSchedulerContract`** — interface with `scheduleReminder()` and `cancelReminder()`. Implemented by `ReminderScheduler` and provided as `@Singleton` via Hilt.
- **`ReminderScheduler`** — delegates to `AlarmPolicy.scheduleExactOrFallback()` for the SDK check, exact/inexact branching, and `SecurityException` fallback.
- **`AlarmPolicy`** (`service/AlarmPolicy.kt`) — `object` with `scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)`. Also `needsExactAlarmPermission(context)` and `exactAlarmSettingsIntent(context)`: on Android 14+ `SCHEDULE_EXACT_ALARM` is denied by default, so without it reminders are inexact (observed up to ~1 h window). `TasksByCategoryScreen` shows `ExactAlarmPermissionDialog` after setting a reminder when the permission is missing. Single source of truth for the SDK 31+ `canScheduleExactAlarms()` check and the `setExactAndAllowWhileIdle` → `setAndAllowWhileIdle` fallback used by both `ReminderScheduler` and `DailyReminderManager`.
- **`ReminderReceiver`** — `@AndroidEntryPoint` `BroadcastReceiver`. `@Inject lateinit var scheduler: ReminderSchedulerContract` and `@Inject lateinit var database: AppDatabase`. Handles alarm fires (shows the notification via `NotificationService`) and reschedules all active future task reminders + `DailyReminderManager.rescheduleFromPrefs()` on `BOOT_COMPLETED` and on `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (alarms set before the grant would otherwise stay inexact).
- **`DailyReminderService`** — `@AndroidEntryPoint` `BroadcastReceiver` (in `service/`). `@Inject lateinit var taskRepository: TaskRepository`. Schedules via `AlarmPolicy` (exact only with the permission) and self-reschedules for the next day after firing.
- **`DailyReminderManager`** — stateless `object` for daily-reminder scheduling. Reads/writes config via `PreferencesRepository(context)`. Exposes `loadPrefs()`, `scheduleDailyReminders()`, `cancelDailyReminders()`, `scheduleExactAlarm()` (delegates to `AlarmPolicy`), `rescheduleFromPrefs()`. Called from `DailyReminderScreen`, `DailyReminderService` (self-reschedule), and `ReminderReceiver` (boot reschedule).
- Boot receiver (`RECEIVE_BOOT_COMPLETED`) re-schedules both task reminders and daily reminders after device restart.
- **Notification channels:** `"taksy_reminders"` (task reminders) and `"daily_reminders"` (daily summaries), both IMPORTANCE_HIGH.

### Backup System
- **`BackupManager`** (`utils/`) — stateless utility that serializes/deserializes all entities to/from JSON using `org.json`. Includes `backupVersion`, `exportDate`, and arrays for categories, tasks, subtasks, reminders. `BackupImportException` + `ImportErrorType` enum (INVALID_JSON, MISSING_SECTION, INVALID_CATEGORY/TASK/SUBTASK/REMINDER, INVALID_DATE) report the exact failing record index.
- **`BackupScreen`** — export via SAF `CreateDocument`; import via SAF `OpenDocument` with a "this will replace all data" confirmation dialog. Errors localized through `BackupState.ImportError`.
- Import clears all tables (reminders → subtasks → tasks → categories) then inserts from the JSON preserving original IDs, all inside a single `appDatabase.withTransaction { }` (a mid-import failure rolls back instead of leaving the DB empty/partial). After the transaction commits, active future reminders are rescheduled via `ReminderSchedulerContract`.

### Widget
- **`TaskWidgetProvider`** (`widget/`) — `AppWidgetProvider` that renders pending tasks. Updates every 30 minutes via `updatePeriodMillis`. Handles `ACTION_REFRESH`. Exposes `refreshAll(context)` used by `TaskViewModel` after task mutations. Resolves `TaskDao` via `EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)`.
- **`WidgetEntryPoint`** — `@EntryPoint @InstallIn(SingletonComponent::class)` exposing `taskDao()`.
- **`TaskWidgetService` / `TaskWidgetRemoteViewsFactory`** — `RemoteViewsService` that queries `TaskDao.getPendingTasksSync()` (non-suspend, sorted by priority then due date). Each item shows a color-coded priority dot, task title, and due date (red if overdue). `TaskDao` is fetched once via a lazy delegate that calls `EntryPointAccessors`.
- Widget layout: header (app name, task count plurals, refresh button) + `ListView` with empty state. Minimum 250×180 dp, resizable. Tapping header or items opens `MainActivity`. Registered in `AndroidManifest.xml` with `BIND_REMOTEVIEWS`.

### Splash Screen
- Native Android splash (`Theme.SplashScreen`) uses `splash_icon.xml` — an `InsetDrawable` wrapping `ticksy_icon.png` with 24 dp inset.
- There is no Compose splash: the native one is the only splash.

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

When modifying Room entities, always add a migration in `AppDatabase.kt` and increment the version. The current version is **16**. Never use `fallbackToDestructiveMigration` in production builds.

## Coding Conventions

- **No `!!`** anywhere in `src/main`. Use `?.let { }`, `mapNotNull`, `takeIf`, or `requireNotNull(x) { "context" }` with a message. Grep-enforced.
- **Use cases own domain logic.** Anything that mutates more than one entity or needs to coordinate the scheduler should be a use case. ViewModels are thin and delegate.
- **No SharedPreferences access outside `PreferencesRepository`.** If you need a new key, add it there.
- **No alarm scheduling outside `AlarmPolicy`.** New alarm sites call `AlarmPolicy.scheduleExactOrFallback()`.
- **Category icons live in `CategoryUtils`.** `selectableIcons` is the single map for both the picker and rendering; don't redefine icon lists in UI code.
- **Display category names via `CategoryUtils.getCategoryName(category)`**, never `category.nombre` directly. It shows `nombre` as-is, except an un-renamed default category (name matches a seeded/`category_*`/legacy name for its icon, in any supported locale) is shown translated to the current language.
- **No category names hardcoded in Kotlin.** Default category names live in the `default_category_names` string-array; only metadata (color, icon) stays inline.
- **Receivers and services that need DI** are `@AndroidEntryPoint` with `@Inject lateinit var` fields. Don't construct `TaskRepository(...)` or `ReminderScheduler(context)` manually.
- **ViewModel errors reach the user.** Wrap mutations in `launchSafe(R.string.error_…)`; `MainActivity` collects `taskViewModel.errors` + `categoryViewModel.errors` and shows a Toast. Don't swallow exceptions or launch bare `viewModelScope.launch` for writes.
- **Widget code** accesses DI via `EntryPointAccessors` because `RemoteViewsFactory` isn't an `@AndroidEntryPoint` target.

## Verifying on a Device

Unit tests can't reproduce concurrency or alarm timing; verify on a real device (a Moto g35, Android 16, was used). The debug build allows `run-as`:

```bash
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
# Pull the Room DB (copy all three files, the data is often still in the WAL)
for f in ticksy_database ticksy_database-wal ticksy_database-shm; do
  "$ADB" exec-out run-as com.example.taksy cat databases/$f > "$f"
done
# Scheduled alarms: window=0 + exactAllowReason=permission means exact
"$ADB" shell dumpsys alarm | grep -A2 ReminderReceiver
"$ADB" shell appops get com.example.taksy SCHEDULE_EXACT_ALARM
"$ADB" exec-out screencap -p > screen.png
```

## History / Status

**Recurring-task duplication bug — root-caused and fixed 2026-07-07 (commit `510876f`).** Root cause was check-then-act in `CompleteTaskUseCase.execute`; fixed via the atomic `completeTaskIfPending` compare-and-swap. **Device-verified 2026-10-17: no duplicates** (rapid taps and subtask auto-complete each produced exactly one next occurrence in the on-device DB). The user-perceived "duplicates" were completed occurrences piling up in the category list, fixed in `1bdf007`. Full writeup: `docs/debugging-taksy-duplication.md`.

**2026-10-17 session** (all pushed, through `eb96419`):
- Fixes: `ReminderReceiver` recurring-reminder advance made atomic (`26fad90`); category icon mapping unified in `CategoryUtils` (`d425b7e`); category names shown from `nombre` instead of derived from the icon (`e277ae6`).
- Simplification after a feature audit: category management wired into the drawer + `DeleteCategoryUseCase`, ~1,600 lines of unreachable code removed (`d937d8e`); recurring tasks carry their reminder to the next occurrence, reminder recurrence no longer creatable (`c6af391`); dead Compose splash and the archive feature removed (`346c786`); task drag-reorder removed (`033d980`); single Settings screen with system theme (`3f5d435`).
- ViewModel errors shown to the user (`d527367`); 98 unused strings removed (`b5857cd`); exact-alarm permission prompt — on Android 14+ reminders were firing 20–60 min late (`eb96419`).
- 109 unit tests pass. Kotlin in `src/main` went from ~10,900 to ~8,500 lines.
- DB v15 migration drops the legacy columns (`archivada`, task `orden`, `tipoRecordatorio`) and the recurring-reminder advance path; device-verified on the Moto g35 (32 tasks / 8 reminders kept). Old backups still import (legacy fields ignored; a DIARIO/SEMANAL/MENSUAL reminder becomes one-shot). 96 unit tests pass.
- Task delete now cascades to subtasks/reminders with full undo (`DeleteTaskUseCase`); DB v16 cleaned the existing orphans. Device-verified on the Moto g35. 100 unit tests pass.
