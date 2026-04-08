# Phase 4: Concurrent Local Build Cache Integration Coverage (Slice 1)

Date: 2026-04-08

## Scope

This phase adds real concurrent invocation integration coverage for shared local build cache behavior under the opt-in concurrency mode.

Focus:

- validate that two concurrent Gradle invocations can run against a shared local build cache directory
- keep scope implementation-agnostic and aligned with the spec's correctness/isolation and observability goals

---

## Implemented Changes

### New Integration Test: Concurrent Shared Local Cache Access

Added:

- `platforms/core-execution/build-cache-local/src/integTest/groovy/org/gradle/caching/local/internal/ConcurrentLocalBuildCacheInvocationsIntegrationTest.groovy`

Test behavior:

- creates two separate projects (`project1`, `project2`) in the same integration test workspace
- configures both to use a single shared local build cache directory
- starts two Gradle builds concurrently via `executer.start()`
- enables:
  - `--build-cache`
  - `-Dorg.gradle.concurrent.invocations=true`
- waits for both builds to finish and verifies:
  - `:cacheable` task executed in both builds
  - shared local cache directory contains cache entry artifacts

---

## Stabilization Work During Phase

While validating this slice, two test-definition issues were fixed:

- Groovy DSL symbol collision between `@CacheableTask` annotation and task class name
  - task class renamed to `SharedCacheableTask`
- invalid task invocation in sample projects (`clean` task absent without base plugin)
  - invocation updated to execute only `cacheable`

These fixes make the test assert concurrent cache behavior rather than fail on unrelated script/task setup issues.

---

## Validation Performed

Environment:

- Java via asdf: `zulu-23.32.11` (for command execution)

Executed successfully:

- `:build-cache-local:forkingIntegTest --tests org.gradle.caching.local.internal.ConcurrentLocalBuildCacheInvocationsIntegrationTest --no-configuration-cache -Dorg.gradle.unsafe.isolated-projects=false`

Notes:

- test infrastructure reports cleanup of leftover daemon processes started by concurrent builds; run still completes successfully

---

## Outcome

Phase 4 Slice 1 adds concrete concurrent-invocation integration coverage at a key shared-state boundary (local build cache), complementing prior unit-level lock-timeout diagnostics from earlier phases.

This creates a practical base for the next phase to extend concurrent integration coverage into additional shared mutable state paths (for example execution history stores/repositories).
