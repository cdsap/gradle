# Phase 5: Diagnostics Contract Hardening (Slice 1)

Date: 2026-04-08

## Scope

This phase hardens test coverage for lock-contention diagnostics introduced in earlier phases by validating operation-specific reason tags across all local build cache lock paths.

Focus:

- strengthen correctness of observability contracts
- reduce regression risk in machine-parsable concurrency-limited reason strings
- keep runtime behavior unchanged

---

## Implemented Changes

### Extended Local Build Cache Lock-Timeout Test Coverage

Updated:

- `platforms/core-execution/build-cache-local/src/test/groovy/org/gradle/caching/local/internal/DirectoryBuildCacheTest.groovy`

Added new tests verifying `LockTimeoutException` tagging for:

- `store` operation via `storeLocally(...)`
- `temp-file` operation via `withTempFile(...)`

Existing `load` operation tag test remains in place.

Validated reason formats:

- `concurrency-limited:lock-contention:local-build-cache:load:...`
- `concurrency-limited:lock-contention:local-build-cache:store:...`
- `concurrency-limited:lock-contention:local-build-cache:temp-file:...`

Each assertion also verifies that original lock-file metadata is preserved.

---

## Validation Performed

Environment:

- Java via asdf: `zulu-23.32.11`

Executed successfully:

- `:build-cache-local:test --tests org.gradle.caching.local.internal.DirectoryBuildCacheTest --no-configuration-cache -Dorg.gradle.unsafe.isolated-projects=false`
- `:build-cache-local:forkingIntegTest --tests org.gradle.caching.local.internal.ConcurrentLocalBuildCacheInvocationsIntegrationTest --no-configuration-cache -Dorg.gradle.unsafe.isolated-projects=false` (regression check for Phase 4 slice)

---

## Outcome

Phase 5 Slice 1 increases confidence that concurrency-limit diagnostics for local build cache locking remain stable and actionable across all relevant operations.

This improves compatibility for downstream parsing and supports further rollout of concurrency observability across additional shared-state components.
