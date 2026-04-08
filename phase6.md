# Phase 6 (planned): Validation and deeper concurrency work

This file kicks off the **next** iteration after Phase 5 (`phase5.md`). It is a **roadmap**, not implemented work yet.

## 1. Integration / stress coverage

- **Multi-process:** two or more `gradle` CLIs (or Tooling API clients) against the same `GRADLE_USER_HOME` with `--concurrent`, overlapping targets that share a known lock (e.g. dependency or metadata cache).
- **Assertions:** build finishes without corruption; optionally attach a test `BuildOperationListener` and expect an `Acquire file lock on` operation with `AcquireGradleUserHomeFileLockResult` when contention is forced.
- **Chaos:** kill one process while holding a lock (harder, flaky); optional soak in a separate job.

## 2. Shared store experiment (spec-aligned)

- Pick **one** high-value path (per design doc): e.g. a cache index or metadata file still guarded by a coarse lock.
- Under `org.gradle.concurrent` + internal guard: **read-shared / write-exclusive** or **staging + atomic publish**, with focused correctness tests.

## 3. Daemon / throughput (when metrics exist)

- Use Phase 2–5 **logs and build operations** to see where time goes under agent bursts.
- Prototype **daemon policy** (reuse, pool hints) only if measurements justify it; keep opt-in boundaries clear.

## 4. Documentation

- User-facing snippet for `--concurrent`, properties (`org.gradle.concurrent`, `org.gradle.internal.concurrent.lock-diagnostics`), and how to interpret lock operations in a **build scan**.
- Optionally fold `phase1.md`–`phase6.md` into a single contributor doc when the feature stabilizes.
