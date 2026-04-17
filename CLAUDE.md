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
ViewModels (TaskViewModel, ThemeViewModel, SplashViewModel)
  ↓ calls suspend funs / collects Flow
Repository (TaskRepository)
  ↓ delegates to DAOs
Room Database (AppDatabase, v8)
```

### Data Layer
- **Entities:** `Task`, `Subtask`, `Category`, `Reminder`
- **DAOs:** `TaskDao`, `SubtaskDao`, `CategoryDao`, `ReminderDao`
- **AppDatabase** uses TypeConverters for `Date` serialization and has 8 tracked migrations

Key business rule: when all subtasks of a task are completed, the parent task auto-completes (logic lives in `TaskViewModel`).

### Repository (`TaskRepository`)
Single repository handles all four entity types. Reminder scheduling is delegated to `ReminderScheduler` (AlarmManager-based).

### ViewModels
- **`TaskViewModel`** — central ViewModel; all task/subtask/category/reminder mutations go through here. Uses `flatMapLatest` to reactively switch the task list query when the filter changes.
- **`ThemeViewModel`** — singleton for dark mode and language; must call `initialize(context)` before use (done in `MainActivity.onCreate`). Persists to SharedPreferences.
- **`SplashViewModel`** — controls the 5.5-second splash animation.

### Navigation
NavHost start destination: `"category_list"`. Routes:
- `category_list` → `CategoryListScreen`
- `tasks_by_category/{categoryId}` → `TasksByCategoryScreen`
- `task_detail/{taskId}` → `TaskDetailScreen`
- `reminders`, `daily_reminders`, `theme_settings`, `language_settings`

Navigation drawer (hamburger menu) is an Android `DrawerLayout` wrapping a `ComposeView`, not a pure Compose drawer.

### Background Work
- `ReminderScheduler` — schedules exact alarms via `AlarmManager.setExactAndAllowWhileIdle()`
- `ReminderReceiver` — BroadcastReceiver that handles alarm fires and triggers `NotificationService`
- Boot receiver re-schedules reminders after device restart

## Key Tech Details

- **Min SDK 26**, Target SDK 35, Java 11, Kotlin 2.0.21
- **Compose BOM** 2024.09.00 — do not specify individual Compose library versions
- **Room** 2.6.1 with KSP for annotation processing (not kapt)
- **Hilt** 2.48 configured but minimally used — `TaksyApplication` extends `HiltAndroidApp`
- **Localization:** Spanish (`values/strings.xml`) is the default; English in `values-en/strings.xml`. Locale changes are applied via `LocaleHelper` and require activity recreation.
- **Database name:** `ticksy_database` (note spelling differs from app name)

## Adding Database Migrations

When modifying Room entities, always add a migration in `AppDatabase.kt` and increment the version. The current version is **8**. Never use `fallbackToDestructiveMigration` in production builds.
