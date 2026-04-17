# Phase 8: Fail-Fast Local Build Cache Lock Contention

## Goal
Reduce time spent waiting on `~/.gradle/caches/build-cache-1/build-cache-1.lock` when multiple Gradle invocations run concurrently from separate worktrees.

## Problem
Phase 7 made lock contention non-fatal by degrading gracefully when local build cache operations fail.
However, Gradle could still spend significant time waiting on the local build-cache lock before falling back, increasing overall "Build cache" time.

## What Changed

### 1) Fail-fast lock probe for local build cache
- File: `platforms/core-execution/build-cache-local/src/main/java/org/gradle/caching/local/internal/DirectoryBuildCache.java`
- Added an opt-in fail-fast path:
  - Before `persistentCache.withFileLock(...)`, perform a non-blocking probe (`FileChannel.tryLock()`) on the local cache lock file.
  - If the lock is already held by another process, throw a tagged lock-timeout immediately:
    - `concurrency-limited:lock-contention:local-build-cache:<operation>:...`
  - If probe cannot determine contention (IO issues, overlapping intra-process lock), continue with normal lock flow.

### 2) Enable fail-fast only in concurrent invocation mode
- File: `platforms/core-execution/build-cache-core/src/main/java/org/gradle/caching/local/internal/DirectoryBuildCacheServiceFactory.java`
- Injected `StartParameterInternal`.
- Wired `startParameter.isConcurrentInvocationModeEnabled()` into local build cache service creation so fail-fast is active only when:
  - `org.gradle.concurrent.invocations=true`

### 3) Constructor wiring updates
- File: `platforms/core-execution/build-cache-local/src/main/java/org/gradle/caching/local/internal/DirectoryBuildCacheService.java`
- File: `platforms/core-execution/build-cache-example-client/src/main/java/org/gradle/caching/example/BuildCacheClientModule.java`
- Updated constructor signatures to pass the new fail-fast toggle.

### 4) Test updates
- File: `platforms/core-execution/build-cache-core/src/test/groovy/org/gradle/caching/local/internal/DirectoryBuildCacheServiceFactoryTest.groovy`
- Updated service factory tests for new constructor dependency and builder-chain interactions.
- Existing `DirectoryBuildCacheTest` behavior remains valid and passing after constructor updates.

## Behavior After Phase 8
- Local build cache remains best-effort under contention (phase 7 behavior preserved).
- In concurrent mode, contention is detected sooner, so local cache operations skip faster instead of waiting on long lock timeout windows.
- Remote cache and task execution can proceed sooner when local lock is contended.

## Validation
Executed targeted tests with JDK 23 (Zulu via asdf):

- `:build-cache-local:test --tests org.gradle.caching.local.internal.DirectoryBuildCacheTest`
- `:build-cache-core:test --tests org.gradle.caching.local.internal.DirectoryBuildCacheServiceFactoryTest`

Both passed.

## Notes
- This phase intentionally optimizes latency under contention, not cache-hit semantics.
- If lock contention occurs, local cache may still report local-load failure and fall back, which is expected.
