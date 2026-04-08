# Phase 7 (planned): Multi-process validation and product docs

Phase 6 introduced **automated coverage** for the concurrent file-lock build operation path (see `phase6.md`). Phase 7 is the next increment: **real multi-process** scenarios and **user-facing** documentation.

## 1. Multi-process integration tests

- Two JVMs / two `GradleExecuter` instances (or scripted shells) sharing a temporary `GRADLE_USER_HOME`.
- Enable `--concurrent` (and any required CC flags) so lock diagnostics and operations fire.
- Assertions: both builds succeed; no corrupt cache markers; optional listener asserts an `Acquire file lock on` operation under forced contention.

## 2. Shared store (continued)

- Execute the “one store” experiment from the agentic spec: targeted RW or staging under `org.gradle.concurrent` + an internal kill-switch.

## 3. User documentation

- Publish a short section (e.g. user manual or release notes) describing:
  - `org.gradle.concurrent` / `--concurrent`
  - `org.gradle.internal.concurrent.lock-diagnostics`
  - How lock wait appears in console and in **build scans** (operation display name + result type)

## 4. Daemon experiments (optional)

- Only after Phase 7 §1 metrics: revisit daemon pooling or scheduling hypotheses.
