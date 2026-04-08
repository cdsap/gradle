# Phase 8: Multi-process tests and shared store

Phase 7 shipped user-facing documentation for concurrent invocations (`phase7.md`). Phase 8 adds **automated multi-process coverage** and tracks the **shared store** experiment.

## Status

### Done

- **Multi-process integration test** — `ConcurrentInvocationsSharedUserHomeIntegrationTest` in `persistent-cache`: two `GradleContextualExecuter` builds start in parallel with `--concurrent` and `--no-daemon`, share one `GRADLE_USER_HOME`, resolve the same Maven module into `caches/modules-2`, assert lifecycle text for concurrent invocations and successful `:compileJava`.

### Still open

1. **Stronger assertions** — Optional `BuildOperationListener` / forced lock contention (see original Phase 7 plan).
2. **Shared store (§2)** — One targeted path: RW or staging under `org.gradle.concurrent` + internal guard (`phase9.md`).
3. **Daemon (§3)** — Optional; only after metrics from overlapping invocations.

Next: **`phase9.md`**.
