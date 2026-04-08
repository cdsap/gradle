# Phase 2: Observability for concurrent invocations

This phase builds on Phase 1 (`phase1.md`) and the spec’s **Diagnostics and Observability** requirement: when users opt into concurrent-style runs, Gradle should give **actionable** feedback when work is blocked on **file lock contention** (a common pattern with multiple processes sharing a Gradle user home).

## Summary

1. **`ConcurrentBuildInvocationContext`** — A per-thread **stack** of booleans, pushed when a `BuildTreeState` opens and popped when it closes (including on failure during service registry construction). The innermost value reflects `BuildModelParameters.isConcurrentInvocationsEnabled()`. Nested build trees compose naturally (inner scope wins for `isEnabled()`).

2. **Lifecycle log lines from `DefaultFileLockManager`** — While acquiring the **state-region** file lock, if the context is enabled and acquisition has not succeeded:
   - After **1 s** of waiting, emit one **`lifecycle`** line (visible at default log level) naming lock **mode**, **display name**, elapsed **ms**, and that another process may hold the user home.
   - Every **5 s** thereafter, emit a shorter **still waiting** line with elapsed time.

3. **Dependencies** — `persistent-cache` now depends on **`logging-api`** so `DefaultFileLockManager` can use `Logging.getLogger(...).lifecycle(...)` (Gradle console visibility without `--info`).

## Scope and limitations

- Diagnostics run only when **`org.gradle.concurrent` / `--concurrent`** is in effect for the **current build tree** (Phase 1 flag).
- The context is **thread-local**. Threads that never `enter` a build tree do not see the flag; most cross-process lock waits on the build-critical path still run on threads that entered via `BuildTreeState`.
- This phase does **not** add daemon-queue or TCP-level instrumentation; it focuses on **file locks**, which the spec and stress scenarios call out often.

## Files touched (conceptual)

- `platforms/core-runtime/concurrent` — `ConcurrentBuildInvocationContext`, unit tests
- `subprojects/core` — `BuildTreeState` enter/leave pairing
- `platforms/core-execution/persistent-cache` — `DefaultFileLockManager`, `build.gradle.kts`

## Suggested next phases

- Richer progress events (build operations) for lock wait histograms
- Optional threshold / rate limits driven by `InternalOptions`
- Thread inheritance or executor propagation if profiling shows misses on worker threads
