# Phase 7: Worktree Parallelism Reliability for Local Build Cache Contention

Date: 2026-04-16

## Scope

This phase addresses a concurrency failure mode observed under multiple parallel builds from separate worktrees sharing the same Gradle user home:

- lock contention on `~/.gradle/caches/build-cache-1`
- builds failing with `Timeout waiting to lock Build cache (...)`

Goal:

- preserve build correctness while preventing local-cache lock contention from failing the whole build
- degrade gracefully under contention so parallel build throughput is maintained

---

## Implemented Changes

### Local Build Cache Lock Contention Is Now Non-Fatal in Build Cache Controller

Updated:

- `platforms/core-execution/build-cache/src/main/java/org/gradle/caching/internal/controller/DefaultBuildCacheController.java`

Behavior changes:

- local cache **load** lock contention:
  - now treated as local cache miss
  - build continues to remote load path (if available)
- local cache **store** lock contention:
  - now suppressed (best-effort local store)
  - build continues without failure
- local **temp-file** lock contention (when local service is used as temp-file store):
  - now falls back to a non-local temp-file store
  - remote cache operations continue instead of aborting

Implementation notes:

- kept lock-contention detection scoped to local cache timeout fingerprints:
  - `concurrency-limited:lock-contention:local-build-cache`
  - `Timeout waiting to lock Build cache`
- non-contention errors remain build-fatal, preserving existing correctness/safety behavior

---

## Tests Added

Updated:

- `platforms/core-execution/build-cache/src/test/groovy/org/gradle/caching/internal/controller/DefaultBuildCacheControllerTest.groovy`

New coverage:

- suppress local load lock-timeout and continue to remote load
- suppress local store lock-timeout
- fallback temp-file strategy when local temp-file access is lock-contended

---

## Validation Performed

Environment:

- Java via asdf: `zulu-23.32.11`

Executed successfully:

- `:build-cache:test --tests org.gradle.caching.internal.controller.DefaultBuildCacheControllerTest --no-configuration-cache -Dorg.gradle.unsafe.isolated-projects=false`

---

## Outcome

Phase 7 makes parallel worktree builds significantly more robust when contending on the shared local build cache lock by turning local-cache lock contention into a degradable condition rather than a hard build failure.
