# Phase 6: Configuration Cache Reason-Tagging Coverage (Slice 1)

Date: 2026-04-08

## Scope

This phase adds explicit unit coverage for configuration-cache lock-timeout reason tagging used by concurrent invocation mode.

Focus:

- stabilize observability contract for configuration-cache lock contention reasons
- keep behavior unchanged while improving testability

---

## Implemented Changes

### 1) Extracted Testable Tagging Helper

Updated:

- `platforms/core-configuration/configuration-cache/src/main/kotlin/org/gradle/internal/cc/impl/ConfigurationCacheRepository.kt`

Change:

- introduced `taggedLockTimeout(lockMode, e)` in the companion object
- reused helper in `withConcurrentInvocationLock(...)` lock-timeout handling path

Behavior remains equivalent:

- reason format stays:
  - `concurrency-limited:lock-contention:configuration-cache:<shared|exclusive>:...`
- original lock-file metadata is preserved

### 2) New Unit Tests for Shared/Exclusive Tagging

Added:

- `platforms/core-configuration/configuration-cache/src/test/kotlin/org/gradle/internal/cc/impl/ConfigurationCacheRepositoryLockTimeoutTaggingTest.kt`

Tests verify:

- shared lock mode maps to `...:configuration-cache:shared:...`
- exclusive lock mode maps to `...:configuration-cache:exclusive:...`
- lock-file path is retained on wrapped `LockTimeoutException`

---

## Validation Performed

Environment:

- Java via asdf: `zulu-23.32.11`

Executed successfully:

- `:configuration-cache:test --tests org.gradle.internal.cc.impl.ConfigurationCacheRepositoryLockTimeoutTaggingTest --no-configuration-cache -Dorg.gradle.unsafe.isolated-projects=false`

---

## Outcome

Phase 6 Slice 1 closes a test-coverage gap in configuration-cache contention diagnostics by making reason-tag generation directly verifiable for both lock modes.

This supports reliable downstream parsing of concurrency limits and reduces regression risk as concurrent-invocation behavior evolves.
