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
- **Entities:** `Task` (`TaskPrioridad`: NINGUNA/BAJA/MEDIA/ALTA, `TaskRecurrencia`: NINGUNA/DIARIA/SEMANAL/MENSUAL/ANUAL — ANUAL is not offered in the UI but still handled (old data, backups), `descripcion: String?`, `fechaCompletada: Date?`), `Subtask`, `Category` (has `orden` for drag-reorder), `Reminder` (always one-shot; no type field).
- **DAOs:** `TaskDao`, `SubtaskDao`, `CategoryDao`, `ReminderDao` — each exposes `getAllXxxSync()` and `deleteAllXxx()` for backup operations.
- **AppDatabase** uses TypeConverters for `Date` and has 16 tracked migrations. v15 dropped the legacy `tasks.archivada`, `tasks.orden` and `reminders.tipoRecordatorio` columns (table recreation, keeps the AUTOINCREMENT counter). v16 has no schema change: it deletes subtasks/reminders orphaned by task deletes before the cascade fix.
- **`data/preferences/PreferencesRepository`** — Hilt `@Singleton`. Owns all five SharedPreferences files (`theme_pref`, `language_pref`, `daily_reminder_prefs`, `onboarding_prefs`, `share_prefs` — last list used when sharing) behind typed getters/setters. Also constructible manually with a `Context` so `LocaleHelper.attachBaseContext()` can read the saved language before the Hilt graph is built. Holds the `DailyReminderPrefs` data class consumed by `DailyReminderManager` and the `ThemeMode` enum. `getThemeMode()` reads `theme_mode`; if absent it migrates the legacy `is_dark_mode` boolean (explicit choice kept), otherwise defaults to SYSTEM.

Task lists in a category (`TaskDao.getTasksByCategory` / `searchTasksByCategory`) hide completed occurrences of recurring tasks — only the next pending occurrence matters there; they stay in the DB for stats and backup. Order: pending first, then priority (ALTA→NINGUNA), then due date (no date last), then newest. There is no manual reordering of tasks. Completed rows show "Completed on <date>" (`fechaCompletada`).

Key business rule: when all subtasks of a task are completed, the parent task auto-completes (logic lives in `SubtaskUseCase`, surfaced via `TaskViewModel`).

### Domain Layer (`domain/`)
- **`CompleteTaskUseCase`** — single entry point for marking a task complete. Cancels reminders, advances recurring tasks via `RecurrenceCalculator`, carries the task's reminders over to the next occurrence (each advanced with the task's recurrence; a date still in the past is stored but not scheduled, since AlarmManager would fire it immediately), and writes the new clone + completed original in one Room transaction (`TaskRepository.completeRecurringTask`). `execute(task): Boolean` is idempotent via compare-and-swap: it returns `true` only if this call won the race and actually completed the task, `false` if another caller (radio button vs. subtask auto-complete) got there first. Called by both `TaskViewModel.toggleTaskStatus` (radio button path) and `SubtaskUseCase.toggle` (auto-complete branch) — the CAS, not the route collapse, is what prevents the recurring-task duplication bug.
- **`SubtaskUseCase`** — observe/add/toggle/delete on subtasks. `toggle()` returns `true` when the parent task auto-completed, so the caller can refresh the widget.
- **`TaskReminderUseCase`** — observe/setLead/updateDueDate/deleteAllForTask. A task has at most one one-shot reminder, defined relative to its due date by `ReminderLead` (`domain/ReminderLead.kt`): with a time — at the time, 10 min, 1 h, 1 day before; "no time" (00:00) — 9:00 that day or the day before. `updateDueDate` moves the reminder with the date keeping its lead (falls back to the first option of the new kind when switching between timed and all-day; removing the date removes the reminder). A reminder time already in the past is stored but not scheduled (AlarmManager would fire it at once). Older reminders whose offset matches no option show as "Custom" and keep their offset. Wraps `ReminderSchedulerContract` so reminder DB writes and alarm scheduling stay in sync; recurrence lives on the task, not the reminder.
- **`RecurrenceCalculator`** — `advance(Date, TaskRecurrencia): Date`. Covered by JVM unit tests.
- **`StreakCalculator`** — `compute(completedAt: List<Date>, now): Int`, consecutive days with a completed task (alive from yesterday if nothing is done yet today). Counts days with the calendar, not 24 h steps, so the DST change doesn't break it. Used by `StatsViewModel` and by the home screen.
- **`DeleteTaskUseCase`** — `delete(task): DeletedTask` removes the task with its subtasks and reminders in one transaction (`TaskRepository.deleteTaskWithChildren`), then cancels the alarms; `restore(deleted)` re-inserts all of it with the original ids (`restoreDeletedTask`) and reschedules active future reminders. `TaskViewModel` keeps the `DeletedTask` until the undo snackbar is used and serialises delete/restore with a `Mutex`, so a quick undo waits for the delete to commit. There is no foreign key, so deleting only the task row used to leave invisible orphans whose alarms kept firing.
- **`DeleteCategoryUseCase`** — deletes a category together with all its tasks (incl. completed), their subtasks and reminders in one transaction (`TaskRepository.deleteTasksInCategory`), then cancels the reminders' alarms after commit. Needed because there is no foreign key between `tasks.categoriaId` and `categories`: deleting only the category would leave invisible orphan tasks that still appear in the widget and keep firing alarms.

### Dependency Injection (Hilt)
`di/DatabaseModule` provides `AppDatabase`, all DAOs, both repositories, and `ReminderSchedulerContract` as `@Singleton`. Use cases (`CompleteTaskUseCase`, `SubtaskUseCase`, `TaskReminderUseCase`, `DeleteTaskUseCase`, `DeleteCategoryUseCase`) and `PreferencesRepository` use `@Inject` constructor with `@Singleton` and are auto-provided by Hilt — no module entry needed. ViewModels use `@HiltViewModel`; screens obtain them via `hiltViewModel()`; `MainActivity` uses `by viewModels()`. `BroadcastReceiver`s (`ReminderReceiver`, `DailyReminderService`) are `@AndroidEntryPoint` with `@Inject lateinit var` fields. The widget (`TaskWidgetProvider` and `TaskWidgetRemoteViewsFactory`) uses `WidgetEntryPoint` + `EntryPointAccessors.fromApplication()` because `RemoteViewsFactory` can't be a Hilt entry point.

### Repositories
- **`TaskRepository`** — task/subtask/reminder DAO facade. Exposes `completeTaskIfPending(taskId): Boolean` (atomic `UPDATE ... WHERE estado = 'PENDIENTE'` via `TaskDao.completeTaskIfPending`, compare-and-swap on the DB row) and `completeRecurringTask(task, clone, cloneReminders): List<Reminder>?`, which performs the same CAS inside `appDatabase.withTransaction { }` and only inserts the clone (and its inherited reminders) if the CAS won, returning the inserted reminders or `null` if it lost — this is what makes concurrent completion attempts safe. `deleteTaskWithChildren(task): DeletedTask` / `restoreDeletedTask(deleted)` back task delete and undo. `deleteTasksInCategory(categoryId): List<Long>` deletes a category's tasks/subtasks/reminders in one transaction and returns the reminder ids to cancel. `markTaskPending(taskId)` reverts completion and clears `fechaCompletada`. `cancelAllRemindersForTask(taskId)` flips `activo=false` on every reminder of a task via a single `UPDATE`. `getReallyPendingTasks()` (pending tasks, used by the daily summary notification).
- **`CategoryRepository`** — category CRUD + `initializeDefaultCategories(Context)`. The seeded names come from `DefaultCategories.getAll(Context)`, which reads `R.array.default_category_names` so the eight default categories localize correctly.
- **`PreferencesRepository`** — described in Data Layer above.

### ViewModels
- **`TaskViewModel`** — thin facade (extends `AndroidViewModel`). Owns task CRUD, the `togglingTaskIds` guard, and the `searchAllTasks` flow. Delegates subtask operations to `SubtaskUseCase`, reminder operations to `TaskReminderUseCase`, and task completion to `CompleteTaskUseCase`. `addTask(input: TaskInput)` is the single entry point — the four legacy overloads are collapsed. A task created with a due date also gets its default reminder (`ReminderLead.defaultFor(due)`: at the time if it has one, 9:00 the same day if "no time"), so the bell is on and the alarm scheduled without opening the schedule dialog; the composer has no reminder picker, it is changed afterwards from the bell. Dates and reminders go through `updateDueDate(task, due)`, `setReminderLead(task, lead)` and `setSchedule(task, due, lead)` (all `TaskReminderUseCase`); title/description edits through `renameTask(id, title)` / `updateTaskDescription(id, text)`, which change one field under a mutex. Calls `TaskWidgetProvider.refreshAll(context)` after mutations. Every mutation runs through `launchSafe(@StringRes errorMessage)`: a failure is logged and emitted on `errors: Flow<Int>` (a buffered `Channel`, one-shot).
- **`CategoryViewModel`** — same `launchSafe`/`errors` pattern as `TaskViewModel`; category CRUD (delete goes through `DeleteCategoryUseCase` + widget refresh); injects `@ApplicationContext` so `initializeDefaultCategories` can read the localized array.
- **`ThemeViewModel`** — `themeMode: StateFlow<ThemeMode>` (LIGHT/DARK/SYSTEM) + language. `MainActivity` resolves SYSTEM with `isSystemInDarkTheme()` and re-applies `enableEdgeToEdge(SystemBarStyle.auto { isDark })` so the status/navigation bars follow the app theme, not the device's. Injects `PreferencesRepository`; calls `AppCompatDelegate.setApplicationLocales()` on language change to trigger activity recreation.
- **`BackupViewModel`** — export/import via `BackupManager`. Injects `@ApplicationContext`, `AppDatabase` (for `withTransaction`), DAOs directly (not repositories), and `ReminderSchedulerContract` to reschedule reminders after import.
- **`StatsViewModel`** — combines `TaskRepository.getAllTasks()` and `CategoryRepository.getAllCategories()` into a `StatsUiState` via `stateIn`. Derives totals, completion rate, streak, last-7-days bucket counts (using `fechaCompletada`), and top 5 categories by task count.

### Navigation
`ui/navigation/AppNavGraph.kt` owns the `NavHost` and every `composable(...)` route. `ui/navigation/AppDrawer.kt` (`DrawerScreen`) holds the `DrawerLayout` + `ComposeView` hybrid, the drawer panel and the language-change confirm dialog. `MainActivity` only handles the activity lifecycle, theme/system bars and the error Toasts. `ThemeMode.isDark()` lives in `ui/theme/Theme.kt`.

Start destination: `"onboarding"` (first launch) or `"category_list"` (subsequent). Routes:
- `onboarding` → `OnboardingScreen` — same visual language as the home screen. Three pager pages, each with its own accent colour (also tints the button and the pill indicator); "Skip" / "Start" call `setOnboardingCompleted(true)`. To see it again on a debug build: `run-as com.example.taksy sed -i 's/value="true"/value="false"/' shared_prefs/onboarding_prefs.xml` and restart.
- `category_list` → `CategoryListScreen` — home screen: time-of-day greeting (`GreetingUtils`) with a 🔥 streak pill on the date line (only from 2 days; tap opens `stats`; computed in `AppNavGraph` with `StreakCalculator`), a row of tasks due today, and the category lists with done/total progress (the screen scrolls normally; holding a list for 1 s lifts it for drag-reorder, saved once on release). Own palette and font in `ui/theme/HomeTheme.kt` (`HomePalette.Light`/`Dark`, Plus Jakarta Sans in `res/font/`). Today cards whose due time has passed show the time and border in red (re-checked every minute; tasks without a time never do). A completed recurring occurrence in the today row can't be un-ticked: its next occurrence already exists, so reopening it would leave two pending. The search pill opens `HomeSearch` (in place, not a route; system Back closes it): with no text it shows the categories as shortcut chips, with text the results grouped into Pending/Completed with the match highlighted. `TaskDao.searchAllTasks` hides completed recurring occurrences, like the category list. The search field is the shared `ui/components/SearchField`, also used in the category list.
- `tasks_by_category/{categoryId}` → `TasksByCategoryScreen` — same visual language as the home screen: header with the category icon and done/total progress, an "Add task" pill that expands into `InlineTaskInput` (title; date pills No date / Today / Tomorrow / Pick a date — today and tomorrow are "no time" 00:00, the pick goes through `DueDatePickerFlow`; priority; recurrence). The "Add task" button saves and closes the composer and keyboard, then the list scrolls to the new task and highlights it briefly (detected as the id that wasn't in `tasks` before adding); the keyboard's Enter saves and keeps the composer open to add another, Pending/Completed sections of `TaskListItem` cards (circle to complete, swipe left to delete with undo, long-press to rename (`TaskViewModel.renameTask`), bell opens `TaskScheduleDialog`: date and reminder together, straight into the calendar when the task has no date). The category colour (`Category.accentColor()` in `HomeTheme.kt`) is the accent.
- `task_detail/{taskId}` → `TaskDetailScreen` — same visual language as the home screen. The title is editable in place and there is a multi-line description under it (`Task.descripcion`; its first line shows under the title in `TaskListItem`). Both save 600 ms after typing stops and on leaving, through `TaskViewModel.renameTask` / `updateTaskDescription`, which change only that field on the task read from the DB under a mutex — leaving the screen saves both at once and a whole-task write would overwrite the other. A blank title is not saved and reverts on losing focus. Subtasks only show their header and progress when there are some; otherwise just the "+ Add subtask" row. Due date (`DueDatePickerFlow`: date picker, then time picker with a "No time" option stored as 00:00; clearable) with "Remind me" chips below it (`ReminderLeadPicker`, shows the exact reminder time), priority and recurrence (`OptionPills`, shared with `InlineTaskInput`), and subtasks with progress. Every change is saved immediately. The route observes the task live (`getAllTasks().map { find }`) so completing the last subtask shows the task as completed without leaving the screen.
- `category_management` → `CategoryManagementScreen` — same visual language as Settings. "Add category" pill, rows with icon tile and task count (all tasks, incl. completed occurrences); tap a row to edit, trash icon to delete with a warning showing how many tasks go with it. `AddCategoryDialog` (name, 10 colours, 10 icons, live preview) and `DeleteCategoryDialog`.

- `settings` → `SettingsScreen` — same visual language as the home screen (`HomePalette` + Plus Jakarta Sans). Theme tiles: system/light/dark; language rows, which go through MainActivity's confirm dialog because it recreates the activity; a row to `daily_reminders` showing on/off and the two times (read via `DailyReminderManager.loadPrefs`).
- `daily_reminders` → `DailyReminderScreen` (reached from Settings) — same visual language as Settings. Every change (on/off switch, morning/evening time via `HomeTimePickerDialog`) is saved and rescheduled immediately through `DailyReminderManager.scheduleDailyReminders`, which also persists `enabled=false` so a disabled reminder is not revived on boot. Turning it on shows `ExactAlarmPermissionDialog` if the exact-alarm permission is missing.
- `about` → `AboutScreen` — same visual language as Settings. App icon, version read from the package (not hardcoded), description, feature list, developer.
- `backup` → `BackupScreen` — same visual language as Settings. Export / import rows; import asks for confirmation (shared dialog helpers) because it replaces everything.
- `stats` → `StatsScreen` — same visual language as Settings. Completion-rate ring + streak, pending / active-category counters, last-7-days bars (today highlighted; day initials localized in the UI from `DayCompletion.day`), top 5 categories with icon tile and progress.

**Dialogs:** every dialog is built from `ui/components/HomeDialog.kt` — `HomeDialogSurface`, `DialogTitle`, `DialogButtons` (optional `leading` slot and `dismissText`), `HomeMessageDialog` (title + message + action), `HomeTimePickerDialog` / `homeTimePickerColors` and `HomeDatePickerDialog`, all coloured with `HomePalette` + the screen accent. Don't use a bare Material `AlertDialog`. The date picker works in UTC midnight: convert with `DateUtils.toUtcDay(date)` / `DateUtils.combineUtcDay(dayUtc, hour, minute)` (passing local millis shows the previous day near midnight). Date + reminder UI lives in `ui/components/TaskSchedule.kt` (`DueDatePickerFlow`, `ReminderLeadPicker`, `TaskScheduleDialog`, `formatDueDateTime`); the list screen only opens the dialog once the reminder flow has emitted, so it never starts without the existing reminder.

`AppNavGraph` defines a private `NavHostController.popOrHome()` extension that collapses the repeated "popBackStack or fall back to category_list" pattern used by every drawer screen.

Navigation drawer is an Android `DrawerLayout` wrapping a `ComposeView` (not a pure Compose drawer — kept as a documented architectural choice). The panel is a second `ComposeView` (`DrawerPanel`, `HomePalette` + Plus Jakarta Sans; there is no `NavigationView` or menu XML). Drawer items: Manage Categories, Stats, Backup, Settings, About. Drawer navigation uses `popUpTo("category_list")` + `launchSingleTop = true`. Back closes an open drawer (`BackHandler`), and the drawer is locked closed on the `onboarding` route.

### Background Work
- **`ReminderSchedulerContract`** — interface with `scheduleReminder()` and `cancelReminder()`. Implemented by `ReminderScheduler` and provided as `@Singleton` via Hilt.
- **`ReminderScheduler`** — delegates to `AlarmPolicy.scheduleExactOrFallback()` for the SDK check, exact/inexact branching, and `SecurityException` fallback.
- **`AlarmPolicy`** (`service/AlarmPolicy.kt`) — `object` with `scheduleExactOrFallback(alarmManager, triggerAtMillis, pendingIntent)`. Also `needsExactAlarmPermission(context)` and `exactAlarmSettingsIntent(context)`: on Android 14+ `SCHEDULE_EXACT_ALARM` is denied by default, so without it reminders are inexact (observed up to ~1 h window). The list bell, the task detail and the daily-reminders screen show `ExactAlarmPermissionDialog` after setting a reminder when the permission is missing. Single source of truth for the SDK 31+ `canScheduleExactAlarms()` check and the `setExactAndAllowWhileIdle` → `setAndAllowWhileIdle` fallback used by both `ReminderScheduler` and `DailyReminderManager`.
- **`ReminderReceiver`** — `@AndroidEntryPoint` `BroadcastReceiver`. `@Inject lateinit var scheduler: ReminderSchedulerContract` and `@Inject lateinit var database: AppDatabase`. Handles alarm fires (shows the notification via `NotificationService`, titled with the task's current title and showing its description — `Reminder.titulo` is a copy that isn't updated on rename) and reschedules all active future task reminders + `DailyReminderManager.rescheduleFromPrefs()` on `BOOT_COMPLETED` and on `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (alarms set before the grant would otherwise stay inexact).
- **`ReminderActionReceiver`** — `@AndroidEntryPoint` `BroadcastReceiver` for the three buttons on a reminder notification: Done (`CompleteTaskUseCase`), +1 h and Tomorrow (`TaskReminderUseCase.snooze(task, SnoozeOption)`; `SnoozeOption` in `domain/`: now + 1 h, or tomorrow 9:00). Snoozing moves only the reminder, not the due date (it then shows as "Custom"); every button closes the notification and refreshes the widget. `NotificationService` gives each button its own `PendingIntent` request code (`reminderId * 3 + index`).
- **`DailyReminderService`** — `@AndroidEntryPoint` `BroadcastReceiver` (in `service/`). `@Inject lateinit var taskRepository: TaskRepository`. Schedules via `AlarmPolicy` (exact only with the permission) and self-reschedules for the next day after firing.
- **`DailyReminderManager`** — stateless `object` for daily-reminder scheduling. Reads/writes config via `PreferencesRepository(context)`. Exposes `loadPrefs()`, `scheduleDailyReminders()`, `cancelDailyReminders()`, `scheduleExactAlarm()` (delegates to `AlarmPolicy`), `rescheduleFromPrefs()`. Called from `DailyReminderScreen`, `DailyReminderService` (self-reschedule), and `ReminderReceiver` (boot reschedule).
- Boot receiver (`RECEIVE_BOOT_COMPLETED`) re-schedules both task reminders and daily reminders after device restart.
- `NotificationService` checks `POST_NOTIFICATIONS` (Android 13+) before notifying; without it the reminder is silently skipped.
- **Notification channels:** `"taksy_reminders"` (task reminders) and `"daily_reminders"` (daily summaries), both IMPORTANCE_HIGH.

### Backup System
- **`BackupManager`** (`utils/`) — stateless utility that serializes/deserializes all entities to/from JSON using `org.json`. Includes `backupVersion`, `exportDate`, and arrays for categories, tasks, subtasks, reminders. `BackupImportException` + `ImportErrorType` enum (INVALID_JSON, MISSING_SECTION, INVALID_CATEGORY/TASK/SUBTASK/REMINDER, INVALID_DATE) report the exact failing record index.
- **`BackupScreen`** — export via SAF `CreateDocument`; import via SAF `OpenDocument` with a "this will replace all data" confirmation dialog. Errors localized through `BackupState.ImportError`. File I/O lives in `BackupViewModel.exportBackup(uri)` / `importBackup(uri)` on `Dispatchers.IO` (an unreadable file reports an error instead of crashing; export success is set only after the write). `importBackupJson(json)` stays as the testable core.
- Import clears all tables (reminders → subtasks → tasks → categories) then inserts from the JSON preserving original IDs, all inside a single `appDatabase.withTransaction { }` (a mid-import failure rolls back instead of leaving the DB empty/partial). After the transaction commits, active future reminders are rescheduled via `ReminderSchedulerContract`.

### Widget
- **`TaskWidgetProvider`** (`widget/`) — `AppWidgetProvider` that renders pending tasks. Updates every 30 minutes via `updatePeriodMillis`. Handles `ACTION_REFRESH`. Exposes `refreshAll(context)` used by `TaskViewModel` after task mutations. `onUpdate` reads the pending count off the main thread (`goAsync` + `Dispatchers.IO`) — Room throws on main-thread reads, which crashed the app when the widget was added or, with a widget placed, on every task change (fixed 2026-10-18). Resolves `TaskDao` via `EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)`.
- **`WidgetEntryPoint`** — `@EntryPoint @InstallIn(SingletonComponent::class)` exposing `taskDao()`.
- **`TaskWidgetService` / `TaskWidgetRemoteViewsFactory`** — `RemoteViewsService` that queries `TaskDao.getPendingTasksSync()` (non-suspend, sorted by priority then due date). Each item shows a color-coded priority dot (an `ImageView` tinted with `setColorFilter` — a widget only accepts certain views, a plain `View` there is not allowed), task title, and due date (red if overdue). `TaskDao` is fetched once via a lazy delegate that calls `EntryPointAccessors`.
- Widget layout: header (app name, task count plurals, refresh button) + `ListView` with empty state. Minimum 250×180 dp, resizable. Tapping header or items opens `MainActivity`. Registered in `AndroidManifest.xml` with `BIND_REMOTEVIEWS`.

### Share into Taksy
- **`ShareActivity`** — target of `ACTION_SEND` `text/plain` from other apps. Translucent theme (`Theme.Ticksy.Share`), `taskAffinity=""` and `excludeFromRecents`, so a dialog floats over the source app and finishing returns to it; the full app is never opened. `@AndroidEntryPoint`, shares `TaskViewModel`/`CategoryViewModel`/`ThemeViewModel` and `PreferencesRepository` (remembers the last list). Dialog = `HomeDialogSurface` with editable title, description preview, list pills (`OptionPills` with `text` = display name) and Add.
- **`SharedText.parse(subject, text)`** (`domain/`) decides the task: with a subject (browsers send page title + URL) subject = title and text = description; without, first line = title and the rest = description; a title over 120 chars is cut at a word and the rest goes to the description. Nothing shared → the activity just finishes.
- `TaskViewModel.addTask` now returns its `Job` and `TaskInput` has `descripcion`: the activity waits for the `Job` before toasting and finishing, otherwise leaving would cancel the ViewModel scope mid-insert. The focus request must live inside the `Dialog` content (it is composed separately; requesting it outside crashed).
- Test without a second app: `adb shell "am start -a android.intent.action.SEND -t text/plain --es android.intent.extra.SUBJECT 'x' --es android.intent.extra.TEXT 'y' -n com.example.taksy/.ShareActivity"` (quote the whole remote command, or adb splits spaces; `MSYS_NO_PATHCONV=1` in Git Bash).

### Splash Screen
- Native Android splash (`Theme.SplashScreen`) uses `splash_icon.xml` — an `InsetDrawable` wrapping `ticksy_icon_splash.png` with 24 dp inset. The marks of `ticksy_icon.png` (check, lines, TASKS) are transparent, so they took the background colour (black on dark); `ticksy_icon_splash.png` is the same icon with a white disc baked in behind them (outside the circle stays transparent). In Compose the icon is always drawn with `ui/components/AppIcon` (white disc + the PNG scaled so its circle — the central 76 %, 780 of 1024 px — fills `size`).
- There is no Compose splash: the native one is the only splash.
- Background `@color/splash_background` = the app background (`HomePalette.background`): `#FBFAF6` in `values/`, `#121212` in `values-night/`. It follows the system dark mode, not the app's theme setting (the system draws it before the app runs).

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

**2026-10-23:** tasks created with a date had no reminder (bell off, no notification) until the bell was opened and a lead picked; `addTask` now creates the default reminder (`0eb181f`). Device-verified on the Moto g35: alarm exact at the chosen time. Gotcha when testing: Android 15+ notification cooldown mutes the sound of notifications fired in quick succession (they carry the system-added `SILENT` flag) — wait ~10 min between tests. Sharing text from other apps creates a task (`ShareActivity`). A home "overdue" banner was built and discarded (home too crowded, no per-task move); if revisited, prefer swipe-right-to-today in the category list over adding home elements. The home shows the streak. `computeStreak`/`buildLast7Days` subtracted 86 400 000 ms per day, wrong on DST-change days: the streak is fixed (`StreakCalculator`), `StatsViewModel.buildLast7Days` still has it. Reminder notifications now have Done / +1 h / Tomorrow buttons (device-verified: +1 h scheduled an exact alarm one hour ahead and closed the notification).

**2026-10-18 session** (pushed through `80eaf42`): redesign finished — Stats, Category management, Backup, About, Onboarding, Compose drawer panel, every dialog/picker (`HomeDialog.kt`), global search (`HomeSearch`). UX flows: composer closes after adding and highlights the new task; task description; date + reminder unified (`ReminderLead`, `TaskScheduleDialog`); editable title; date pills when creating; splash uses the app background. Closing audit: dead DAO/repository/notification code, `TaskFilter`, `HamburgerMenu`, unused colours/strings/launcher drawables and ~40 unused imports removed; lint errors fixed (widget `View` → `ImageView`, `drawer_background` only in night, notification permission check). 111 unit tests pass; lint has 0 errors. The device app was reset to a fresh install (`pm clear`).
