# Phase 9: Restore Configuration Cache Startup In Concurrent Invocation Mode

## Goal
Unblock configuration cache when concurrent invocation mode is enabled so parallel worktree builds can use the new Gradle distribution instead of failing during service creation.

## Problem
The previous iteration introduced a startup regression in configuration cache wiring:

- `ConfigurationCacheRepository` is a `BuildSession`-scoped service.
- Its constructor had been changed to require `BuildModelParameters`.
- `BuildModelParameters` is not available in the `BuildSession` service scope.

In practice, parallel validation runs failed immediately with:

- `Cannot create service of type ConfigurationCacheRepository ... BuildModelParameters for parameter #6 is not available`

This made the iteration invalid before file-contention behavior could even be evaluated.

## Evidence
- The failure reproduced in validation worktrees as soon as `--configuration-cache` and `-Dorg.gradle.concurrent.invocations=true` were used.
- Inspecting the service scopes showed that `ConfigurationCacheRepository` only needed the concurrent-invocation flag, not the full `BuildModelParameters` service.
- Running the built distribution directly after the fix confirmed configuration cache startup was restored.

## What Changed

### 1) Use `StartParameterInternal` in `ConfigurationCacheRepository`
- File: `platforms/core-configuration/configuration-cache/src/main/kotlin/org/gradle/internal/cc/impl/ConfigurationCacheRepository.kt`
- Replaced the constructor dependency on `BuildModelParameters` with `StartParameterInternal`.
- Kept the same behavior by reading:
  - `startParameter.isConcurrentInvocationModeEnabled`

This matches the service scope correctly because `StartParameterInternal` is available in the build session.

### 2) Add regression coverage
- File: `platforms/core-configuration/configuration-cache/src/integTest/groovy/org/gradle/internal/cc/impl/ConfigurationCacheIntegrationTest.groovy`
- Added an integration test that runs configuration cache twice with:
  - `-Dorg.gradle.concurrent.invocations=true`
- Verifies the first run stores the cache entry and the second run reuses it.

## Why This Approach
The concurrent-invocation mode check does not need the richer build-tree model parameters object. Injecting the narrower `StartParameterInternal` removes the scope mismatch while preserving the intended conditional behavior.

## Expected Result
- Configuration cache can start normally in concurrent invocation mode.
- Parallel worktree validation can proceed far enough to measure actual contention and total elapsed time instead of failing during bootstrap.
