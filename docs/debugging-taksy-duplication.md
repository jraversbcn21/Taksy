# Debugging incident: recurring task duplication

Session: 2026-07-07. Status: bug root-caused and fixed in code (commit `510876f`,
not pushed). Device smoke-test still pending. Two of six Phase-2 findings fixed;
four remain, tracked below with priority order for the next session.

## Root cause (verified in code, not assumed)

`CompleteTaskUseCase.execute(task: Task)` was check-then-act: it received a
`Task` snapshot, checked `task.recurrencia`/implicit `estado`, then wrote. Two
call paths could reach it with the same stale PENDIENTE snapshot in an
overlapping window:

- `TaskViewModel.toggleTaskStatus` (radio button), guarded by `togglingTaskIds`
  — but the guard is a `Set<Long>` checked only on this path.
- `SubtaskUseCase.toggle` (last subtask completes → parent auto-completes) —
  **unguarded**, reads the task fresh from DB but still hands a snapshot to
  `execute()`.

Failure sequence: user completes the last subtask (coroutine A suspends
mid-flight in Room I/O) → user taps the radio button before A resumes
(coroutine B, guard set is empty, passes) → B completes the task and inserts
clone #1 → A resumes, re-reads siblings (all done), re-reads task — if that
read lands before B's write is visible, it sees PENDIENTE and inserts clone #2.

Collapsing both routes into one `CompleteTaskUseCase` (an earlier, already-shipped
fix documented in CLAUDE.md before this session) reduced frequency but did
**not** close the race — the check-then-act was still outside any transaction
and the guard didn't cover both routes.

## Fix applied (commit `510876f`)

- `TaskDao.completeTaskIfPending(id, fechaCompletada): Int` — new query,
  `UPDATE tasks SET estado='COMPLETADA', fechaCompletada=:f WHERE id=:id AND
  estado='PENDIENTE'`. Returns rows-affected (0 or 1). This is the
  compare-and-swap: only one concurrent caller can ever win it, enforced by
  SQLite's own row-level write.
- `TaskRepository.completeTaskIfPending(taskId): Boolean` and
  `completeRecurringTask(task, clone): Boolean` — the recurring path runs the
  CAS **inside** `appDatabase.withTransaction { }` and only inserts the clone
  if the CAS returned 1.
- `CompleteTaskUseCase.execute(task): Boolean` — now idempotent. Returns
  whether *this* call actually completed the task.
- `SubtaskUseCase.toggle` returns `completeTaskUseCase.execute(task)` directly
  instead of always returning `true` after calling it.
- Removed `TaskRepository.toggleTaskStatus` / `TaskDao.markTaskAsCompleted`
  (both read-modify-write without a CAS guard) — replaced by
  `completeTaskIfPending` / `markTaskPending`.
- Added `CompleteTaskUseCaseTest` (4 tests) that reproduces the exact race with
  a stale shared snapshot and asserts exactly one clone is ever inserted.
- Updated `TaskRepositoryTest` and `TaskViewModelTest` fakes for the new DAO
  contract. 94 unit tests total, 0 failures.

**Still needed:** physical-device smoke-test — recurring task with 2 subtasks,
complete last subtask and tap the radio button in quick succession, repeat
~10x, confirm exactly one next-occurrence. Also retest rapid double-tap on the
radio button alone.

## Phase 2 sweep — repo-wide findings (same risk pattern)

| # | Severity | Location | Finding | Status |
|---|----------|----------|---------|--------|
| 1 | Critical | `CompleteTaskUseCase.kt`, `SubtaskUseCase.kt`, `TaskRepository.kt` | Recurring task duplication (above) | **Fixed**, commit `510876f` |
| 2 | High | `BackupViewModel.kt` import | Wipe (4 deletes) + reinsert (N inserts) with no `withTransaction` — mid-import crash could leave DB empty or partial | **Fixed**, commit `510876f` (wrapped in `appDatabase.withTransaction`, `BackupViewModelTest` added) |
| 3 | Medium | `ReminderReceiver.kt:65-78`, `scheduleNextRecurringReminder` | Read-check-write on reminder recurrence without a transaction. Same pattern as #1 but lower risk — AlarmManager doesn't naturally double-fire a single alarm | **Fixed** — `TaskRepository.advanceRecurringReminder` (transaction + `ReminderDao.advanceReminderIfUnchanged` CAS on `fechaRecordatorio`/`activo`); `cancelAllRemindersForTask` now a single targeted `UPDATE` |
| 4 | Low | `CategoryUtils.kt:22` | `"school" -> Icons.Default.Settings` is the wrong icon (cosmetic); `favorite`/`health` icon names collide | **Fixed** — `school` → `Icons.Default.Create`; `CategoryUtils.selectableIcons` is now the single map used by `AddCategoryDialog` (picker offered `star`/`schedule`/`business`/`sports`, which previously rendered as `Info`); `health` kept as an intentional legacy alias of `favorite` (seeded by `MIGRATION_4_5`) |
| 5 | Info | `AppDatabase.kt:103-108` | Hardcoded category names in `MIGRATION_4_5` SQL — this is frozen migration SQL, not a live-code violation of the "no hardcoded category names" rule. No action needed. | Not a bug — documented for completeness |

Verified clean by grep during the sweep (no action needed): zero `!!` in
`src/main`; SharedPreferences access confined to `PreferencesRepository`
(`LocaleHelper`/`DailyReminderManager` manual-construct it, which is the
already-documented exception); all alarm scheduling goes through
`AlarmPolicy`; no `fallbackToDestructiveMigration`; no manual construction of
`TaskRepository`/`ReminderScheduler` outside `DatabaseModule`; migrations
current through v14.

## Next-session punch list, in priority order

> **Status 2026-10-17: all items below are done** (device smoke-test, `ReminderReceiver` CAS, `CategoryUtils` icons, push). Kept for history.

1. **Device smoke-test the duplication fix** (see repro steps above). If it
   still reproduces, something in the CAS wiring is wrong — re-check that
   `TaskViewModel.toggleTaskStatus` and `SubtaskUseCase.toggle` both go through
   `CompleteTaskUseCase.execute` and that no other write path bypasses it.
2. **`ReminderReceiver.scheduleNextRecurringReminder`** (medium, finding #3) —
   wrap the check-then-write in a transactional repository method, mirroring
   the `completeTaskIfPending` pattern. Verify with a unit test on a fake
   `ReminderDao`/`TaskDao` pair simulating overlapping alarm fires.
3. **`CategoryUtils` icon mapping** (low, finding #4) — fix `"school"` to map
   to a school-appropriate icon; resolve the `favorite`/`health` name
   collision. Verify visually.
4. **Push commit `510876f`** to origin/master once the above is done (or
   sooner, if the user wants incremental pushes) — currently local-only, one
   commit ahead of `origin/master`.

## Completion estimate

Original scope: root-cause the duplication bug, fix it, and sweep the repo for
the same non-atomic-write risk pattern (6 findings identified).

- **Done:** root cause traced and documented, critical fix (#1) shipped with
  regression tests, high-severity fix (#2) shipped with tests, CLAUDE.md
  synced to reflect the real fix (not the previously-inaccurate claim that
  route-collapsing alone had fixed it), 94/94 unit tests green.
- **Not done:** device smoke-test (can't be verified from unit tests alone —
  the whole point of this bug was that it needed a real device to reproduce
  the coroutine interleaving), medium finding #3, low finding #4, push to
  remote.

**~70% complete.** The two highest-severity, highest-confidence items (data
corruption on both the completion path and the backup/import path) are done
and test-covered. What's left is one medium-risk code fix, one cosmetic fix,
and — the part that actually closes the original incident — confirming on a
real device that the race is gone, since JVM unit tests can assert the CAS
logic but can't reproduce the exact Main-dispatcher coroutine interleaving
that caused the original bug report.

## Device smoke-test — 2026-10-17 (Moto g35 5G)

Result: **no duplication.** Verified by pulling the Room DB via `adb exec-out run-as`:
the "test 2" daily chain (ids 17→53) advanced one day per completion, never more
than one PENDIENTE row, no repeated `fechaVencimiento`. Covered subtask
auto-complete (task 17, 5 subtasks) and rapid radio taps (~1/s, two in the same
second). Logcat showed no crashes or SQLite errors.

The user-reported "duplicates" (screen recording `duplicado.mp4`) were completed
occurrences left in the category list with no date or recurrence icon, so they
looked identical to the pending one. Fixed in `1bdf007`: category list/search
queries exclude `estado = 'COMPLETADA' AND recurrencia != 'NINGUNA'`, and
completed rows show their completion date.

Still not exercised on device: a DIARIO reminder firing and advancing
(`advanceRecurringReminder`); only UNA_VEZ reminders were tested.
