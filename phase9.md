# Phase 9 (planned): Shared store and deeper validation

Phase 8 added a **multi-process** integration test with shared Gradle user home (`phase8.md`). Phase 9 focuses on the **shared store** experiment and optional **build-operation** assertions under contention.

## 1. Shared store

- Pick one high-value path per `spec_gradle_agentic_mode.md` (e.g. metadata behind a coarse lock).
- Under `org.gradle.concurrent` + internal kill-switch: read-shared/write-exclusive or staging + atomic publish, with correctness tests.

## 2. Contention instrumentation in tests

- Extend multi-process coverage to assert lock-related build operations when contention is deliberately triggered (listener or log hooks), without flakiness.

## 3. Daemon policy (optional)

- Revisit only if measurements from real overlapping invocations justify changes to daemon reuse or scheduling.
