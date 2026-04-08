# Phase 3: Tune concurrent file-lock diagnostics

Phase 2 (`phase2.md`) adds **lifecycle** log lines when the concurrent-invocations opt-in is active and Gradle waits on a **state-region file lock**. Phase 3 makes that behavior **optional** for environments that need quiet logs or deterministic output.

## System property

| Property | Default | Meaning |
|----------|---------|--------|
| `org.gradle.internal.concurrent.lock-diagnostics` | `true` | When `false`, suppresses the Phase 2 lifecycle messages (locking still behaves the same). |

Example:

```bash
./gradlew help --concurrent -Dorg.gradle.internal.concurrent.lock-diagnostics=false
```

## Implementation

- **`DefaultFileLockManager.CONCURRENT_LOCK_DIAGNOSTICS_PROPERTY`** — public constant for the property name.
- **`DefaultFileLockManager.concurrentLockDiagnosticsEnabled()`** — `@VisibleForTesting` helper used before emitting diagnostics.

## Tests

- **`DefaultFileLockManagerConcurrentDiagnosticsPropertyTest`** — default `true`, `false` when the property is set.

## Suggested later work (Phase 4+)

- Finer-grained **read/write** locking or staging for a chosen shared store under the concurrent opt-in.
- **Daemon** scheduling or pooling experiments behind the same opt-in, driven by measurements.
- **Build operation** or structured events for lock wait times (for build scans and tooling).
