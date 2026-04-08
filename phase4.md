# Phase 4: Build operations for concurrent file-lock waits

Phase 2 (`phase2.md`) adds **lifecycle** logging while waiting on a cross-process file lock when concurrent invocations are enabled. Phase 4 adds a **build operation** around that wait so tools and build scans can attribute time and see structured **details** and **progress** events.

## Behavior

When **all** of the following hold:

- `ConcurrentBuildInvocationContext` is active (enter/leave from `BuildTreeState`, Phase 2),
- `org.gradle.internal.concurrent.lock-diagnostics` is not `false` (Phase 3),
- and a **`BuildOperationRunner`** was registered for the current build tree (typical daemon / full session setup),

then acquiring the **state-region** lock runs inside:

`BuildOperationRunner.call(CallableBuildOperation)` with:

- **Display name:** `Acquire file lock on '<target>'`
- **Details:** `AcquireGradleUserHomeFileLockDetails` (target display name, lock mode, lock file path, optional operation label)
- **Progress:** same human-readable contention strings as Phase 2 (first after 1 s, then every 5 s), emitted via `BuildOperationContext.progress`

If no `BuildOperationRunner` is available (for example some minimal global service setups), behavior falls back to **Phase 2 lifecycle logging** only.

## Types

| Type | Role |
|------|------|
| `org.gradle.cache.internal.operations.AcquireGradleUserHomeFileLockDetails` | Serializable-ish POJO attached as operation `details` for scan / introspection |

## Context stack

`ConcurrentBuildInvocationContext` now stores both the concurrent opt-in flag and the **`BuildOperationRunner`** resolved from the build session registry when the build tree opens.

## Suggested follow-ups

- Emit a **final status** or result object on successful acquire (elapsed wait time). (**Done in Phase 5 / `phase5.md`.**)
- Optional **parent operation** binding when lock wait happens off the main build thread.
- Extend the same pattern to other high-contention locks if profiling warrants it.
