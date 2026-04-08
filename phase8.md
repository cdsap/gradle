# Phase 8 (planned): Multi-process tests and shared store

Phase 7 shipped **user-facing documentation** for concurrent invocations (`phase7.md`). Phase 8 focuses on **automated multi-process coverage** and the **shared store** experiment; daemon work remains optional and metric-driven.

## 1. Multi-process integration tests

- Two `GradleExecuter` instances (or equivalent) sharing one temporary `GRADLE_USER_HOME`.
- Pass `--concurrent` (and any flags required for the scenario under test).
- Assert: both builds succeed; no corrupt cache markers; optional `BuildOperationListener` expects an `Acquire file lock on` operation with structured result when contention is forced.

## 2. Shared store

- Pick one high-value path (per `spec_gradle_agentic_mode.md`): e.g. cache index or metadata still behind a coarse lock.
- Under `org.gradle.concurrent` + internal guard: read-shared/write-exclusive or staging + atomic publish, with correctness tests.

## 3. Daemon (optional)

- Revisit only after §1 yields timing and lock-wait data from real overlapping invocations.
