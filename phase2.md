# Phase 2: Contention Diagnostics Expansion (Slice 1)

Date: 2026-04-08

## Scope

This phase extends observability for concurrency limits beyond the initial daemon-availability signal from Phase 1.

Implemented in this slice:

- lock contention diagnostics in configuration cache shared-state locking paths
- improved daemon-client aggregation by reason category, not only raw reason strings

The focus remains low-risk diagnostics and classification, with no default behavior change.

---

## Implemented Changes

### 1) Configuration Cache Lock Contention Diagnostics

Updated `ConfigurationCacheRepository` to surface contention in the concurrent invocation lock path:

- captures lock-acquisition duration for the `.concurrent-invocation-access` lock
- emits lifecycle message when wait exceeds threshold
- rewrites lock-timeout exception message with structured concurrency reason prefix

Reason format:

- `concurrency-limited:lock-contention:configuration-cache:<shared|exclusive>:...`

Changed file:

- `platforms/core-configuration/configuration-cache/src/main/kotlin/org/gradle/internal/cc/impl/ConfigurationCacheRepository.kt`

Why:

- makes lock/shared-state limits visible to users
- provides machine-parsable reason data for downstream diagnostics
- keeps read/write safety model from Phase 1 intact

---

### 2) Daemon Client Reason Category Aggregation

Updated daemon client diagnostics to include categorized summaries of concurrency-limited reasons:

- parses reasons matching `concurrency-limited:<category>:<detail>`
- reports aggregated category counts (e.g., `daemon-availability`, `lock-contention`)
- includes category summary in:
  - lifecycle fallback message when starting a new daemon
  - terminal `NoUsableDaemonFoundException` message

Changed files:

- `platforms/core-runtime/client-services/src/main/java/org/gradle/launcher/daemon/client/DaemonClient.java`
- `platforms/core-runtime/client-services/src/test/groovy/org/gradle/launcher/daemon/client/DaemonClientTest.groovy`

---

## Validation Performed

Environment:

- Java via asdf: `zulu-23.32.11`

Executed successfully:

- `configuration-cache:compileKotlin`
- `configuration-cache:test --tests org.gradle.internal.buildtree.BuildModelParametersProviderTest`
- `client-services:test --tests org.gradle.launcher.daemon.client.DaemonClientTest`

Run settings:

- `--no-configuration-cache`
- `-Dorg.gradle.unsafe.isolated-projects=false`

---

## Outcome

Phase 2 Slice 1 improves practical observability of concurrency limits:

- lock contention is now surfaced as a first-class concurrency-limited reason in configuration cache access
- daemon-client diagnostics now expose both reason details and category-level summaries

This sets up the next slice: extending structured contention diagnostics to additional shared-state lock points (for example build cache and task-history related locking paths).
