# Phase 1: Concurrent invocations (agent-friendly Gradle)

This document records what was implemented as **Phase 1** for the concurrent-invocation / agent-driven workflow work, aligned with `spec_gradle_agentic_mode.md` (opt-in, correctness, configuration cache interoperability, observability foundations).

## Summary

Phase 1 adds an **explicit opt-in** for “concurrent invocations” behavior, **threads that flag through the build model** so subsystems can branch safely later, applies **one concrete performance behavior** where the spec calls out configuration reuse (parallel configuration cache store when safe), **keeps configuration cache keys honest** when the mode changes, and **serializes the flag across the tooling/daemon protocol** with a small backward-compat read path.

## User-facing controls

| Mechanism | Value |
|-----------|--------|
| Gradle property | `org.gradle.concurrent=true` |
| CLI (incubating) | `--concurrent` / `--no-concurrent` |
| Help category | Performance |

Implementation: `StartParameterBuildOptions.ConcurrentInvocationsOption` → `StartParameterInternal.setConcurrentInvocationsEnabled(boolean)`.

## Build model

- **`BuildModelParameters`** (`base-services`): new `boolean isConcurrentInvocationsEnabled()`.
- **Concrete modes** (`configuration-cache` / `BuildModelParameters.kt`): `GradleVintageMode`, `GradleConfigurationCacheMode`, and `GradleIsolatedProjectsMode` each carry the flag.
- **`toDisplayMap()`**: includes entry `concurrentInvocations` so **operational logging** lists the effective value (foundation for spec diagnostics).

`BuildModelParametersProvider` passes `startParameter.isConcurrentInvocationsEnabled` into all three mode constructors (including nested build tree vintage parameters).

## Behavior changes (when opt-in is on)

### Configuration Cache (task-only CC path)

- **Parallel store** is enabled if `(org.gradle.configuration-cache.parallel || org.gradle.concurrent)` and the existing internal switch remains on:
  - `org.gradle.internal.configuration-cache.parallel-store` (default `true`)
- Rationale: spec asks to reduce unnecessary serialization of configuration-related work; parallel store is the existing knob for CC stores without changing default Gradle for users who do not opt in.

### Isolated Projects

- **Parallel configuration cache store** uses `(parallelIsolatedProjects || concurrentInvocations) && options[configurationCacheParallelStore]`, so **concurrent opt-in can enable parallel store** even when IP’s parallel flag would otherwise leave it off—again gated by the internal `parallel-store` option.

### Vintage / default

- No change to execution semantics beyond the flag being available on `BuildModelParameters` and in `toDisplayMap()`.

## Configuration cache correctness

- **`ConfigurationCacheStartParameter`**: exposes `isConcurrentInvocationsEnabled` from `StartParameterInternal`.
- **`ConfigurationCacheKey`**: hashes `putBoolean(startParameter.isConcurrentInvocationsEnabled)` so entries are not blindly shared between concurrent and non-concurrent modes as behavior diverges.

## Tooling / daemon protocol

- **`BuildActionSerializer`** (`StartParameterSerializer`): writes `isConcurrentInvocationsEnabled` **after** `nonInteractive`.
- **Compatibility**: if the stream ends after `nonInteractive`, `read` catches `EOFException` and defaults concurrent invocations to `false` (older payloads).

## Incubating notice

- **`DefaultBuildModelParametersFactory`**: when `modelParameters.isConcurrentInvocationsEnabled`, calls `IncubationLogger.incubatingFeatureUsed("Concurrent invocations")`.

## Tests

| Location | What was added or updated |
|----------|---------------------------|
| `ConfigurationCacheKeyTest` | `cache key honours concurrent invocations option` |
| `BuildModelParametersProviderTest` | `defaults()` includes `concurrentInvocations: false`; `concurrent invocations opt-in enables configuration cache parallel store for task-only runs`; `concurrent invocations enables configuration cache parallel store when isolated projects parallel-ip is off` |

## Files touched (by area)

- **Start parameter / CLI**: `StartParameterInternal.java`, `StartParameterBuildOptions.java`
- **Build model**: `BuildModelParameters.java`, `BuildModelParameters.kt`, `BuildModelParametersProvider.kt`, `DefaultBuildModelParametersFactory.kt`
- **Configuration cache**: `ConfigurationCacheStartParameter.kt`, `ConfigurationCacheKey.kt`
- **Tooling serialization**: `BuildActionSerializer.java`
- **Tests**: `ConfigurationCacheKeyTest.kt`, `BuildModelParametersProviderTest.groovy`

## Explicit non-goals for Phase 1

- No broad rewrite of global file locking or all shared stores.
- No new daemon pooling / multi-daemon policy (flag is ready for later work).
- No new user-facing lock-wait or daemon-queue diagnostics yet (only model + log map + incubation).

## Local development note

Building this Gradle repo may require **multiple JDKs** (e.g. Java 8 for some toolchains, Java 23 for running Gradle). Example:

```properties
org.gradle.java.installations.paths=/path/to/jdk8,/path/to/jdk23
```

(Often set in `~/.gradle/gradle.properties` or passed with `-Dorg.gradle.java.installations.paths=...`.)

## Suggested next phases (not implemented here)

See project planning: observability for lock contention and daemon limits; finer-grained RW patterns on chosen stores under the same opt-in; daemon strategy experiments; concurrent stress / corruption tests.
