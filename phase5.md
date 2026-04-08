# Phase 5: File-lock build operation result

Phase 4 (`phase4.md`) wraps concurrent file-lock acquisition in a **build operation** with **details** and **progress**. Phase 5 attaches a structured **result** so consumers (build scans, diagnostics) can see how long acquisition took and whether any contention was observed.

## `AcquireGradleUserHomeFileLockResult`

Attached via `BuildOperationContext.setResult(...)` when the Phase 4 `CallableBuildOperation` completes **successfully** (lock acquired):

| Field | Meaning |
|-------|--------|
| `totalDurationMillis` | Wall time from entering the acquisition loop until the state-region lock succeeded (`System.nanoTime()` based). |
| `observedContention` | `true` if at least one `tryLockState` attempt failed before success (another process held the lock or a transient denial). |

Uncontended fast paths typically yield `observedContention == false` and a small duration.

## Scope

- Only the **build-operation** path (concurrent diagnostics on, `BuildOperationRunner` present) sets this result.
- Lifecycle-only and default paths are unchanged.

## Suggested Phase 6+

- **Integration tests:** two Gradle processes, shared user home, `--concurrent`, assert operation presence / result shape in a listener.
- **Finer-grained locking** or staging for a hot shared store under the same opt-in.
- Tie results to **TAPI / scan** examples once the type is stable for external tools.
