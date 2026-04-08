# Phase 7: Multi-process validation and product docs

Phase 6 introduced automated coverage for the concurrent file-lock build operation path (see `phase6.md`). Phase 7 tracks **multi-process** scenarios, **shared-store** work, **user-facing documentation**, and optional **daemon** experiments.

## Status

### Done

- **User documentation (§3)** — Published in the user manual:
  - Performance options: `--concurrent` / `--no-concurrent` (`platforms/documentation/docs/src/docs/userguide/reference/runtime-configuration/command_line_interface.adoc`, anchor `sec:command_line_concurrent_invocations`).
  - Gradle properties reference: `org.gradle.concurrent`, `org.gradle.internal.concurrent.lock-diagnostics` (`build_environment.adoc`).
  - Dependency cache locking section cross-links to that CLI topic (`dependency_caching.adoc`).
  - Describes build-scan visibility (operations named like `Acquire file lock on ...` and contention in the structured result).

### Still open

1. **Multi-process integration tests** — Two JVMs / executors sharing a temporary `GRADLE_USER_HOME`, `--concurrent`; success + cache integrity; optional build-operation assertions under forced contention (see `phase8.md`).
2. **Shared store experiment** — Targeted RW or staging under `org.gradle.concurrent` + internal guard (spec-aligned).
3. **Daemon experiments** — Only after multi-process metrics: pooling or scheduling hypotheses.

Next increment: **`phase8.md`**.
