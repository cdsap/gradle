# Phase 1: Concurrent Invocation Foundations

Date: 2026-04-08

## Scope

This phase implemented the first safe, opt-in foundation for concurrent Gradle invocations, aligned with `spec_gradle_agentic_mode.md`:

- opt-in feature switch for concurrent invocation mode
- initial safe read/write concurrency behavior for configuration cache access
- initial daemon-side concurrency observability for busy/unavailable daemon responses

Default behavior remains unchanged unless the opt-in property is enabled.

---

## Implemented Changes

### 1) Opt-in Flag and Parameter Propagation

Added a new property-based opt-in:

- `org.gradle.concurrent.invocations`

Wired from start parameters into build model parameters so internal services can branch behavior safely.

Changed files:

- `platforms/core-runtime/start-parameter/src/main/java/org/gradle/initialization/StartParameterBuildOptions.java`
- `platforms/core-runtime/start-parameter/src/main/java/org/gradle/api/internal/StartParameterInternal.java`
- `platforms/core-runtime/base-services/src/main/java/org/gradle/internal/buildtree/BuildModelParameters.java`
- `platforms/core-configuration/configuration-cache/src/main/kotlin/org/gradle/internal/buildtree/control/BuildModelParameters.kt`
- `platforms/core-configuration/configuration-cache/src/main/kotlin/org/gradle/internal/buildtree/control/BuildModelParametersProvider.kt`
- `platforms/core-configuration/configuration-cache/src/main/kotlin/org/gradle/internal/buildtree/control/DefaultBuildModelParametersFactory.kt`

Behavior:

- Feature is explicit opt-in.
- Existing CLI usage remains compatible.
- Default Gradle behavior is preserved.

---

### 2) Configuration Cache Access: Read vs Write Lock Semantics

Implemented initial lock-mode split for configuration cache operations under concurrent invocation mode:

- state load path uses shared lock
- store path uses exclusive lock

When opt-in is off, existing exclusive cache coordination remains in place.

Changed files:

- `platforms/core-configuration/configuration-cache/src/main/kotlin/org/gradle/internal/cc/impl/ConfigurationCacheRepository.kt`
- `platforms/core-configuration/configuration-cache/src/main/java/org/gradle/internal/cc/impl/CacheAccessOperations.java` (new helper)

Notes:

- Introduced a dedicated lock target under configuration cache base dir for concurrent-invocation access coordination.
- Kept write operations serialized for correctness.
- Removed Kotlin unchecked-cast warning under `-Werror` by moving write-lock return plumbing into Java helper code.

---

### 3) Daemon Availability Observability

Added first-stage diagnostics for concurrency-limited daemon availability:

- daemon busy/unavailable responses are categorized and reason-tagged
- client tracks and aggregates daemon-unavailable reasons during reuse attempts
- client logs lifecycle message when concurrency is limited and it falls back to starting a new daemon
- final "no usable daemon" error now includes aggregated unavailability reasons when present

Changed files:

- `platforms/core-runtime/launcher/src/main/java/org/gradle/launcher/daemon/server/exec/StartBuildOrRespondWithBusy.java`
- `platforms/core-runtime/client-services/src/main/java/org/gradle/launcher/daemon/client/DaemonUnavailableConnectException.java` (new class)
- `platforms/core-runtime/client-services/src/main/java/org/gradle/launcher/daemon/client/DaemonClient.java`

---

## Tests Updated

- `platforms/core-runtime/launcher/src/test/groovy/org/gradle/launcher/cli/converter/StartParameterConverterTest.groovy`
  - validates new property from CLI/system properties and gradle.properties
- `platforms/core-configuration/configuration-cache/src/test/groovy/org/gradle/internal/buildtree/BuildModelParametersProviderTest.groovy`
  - validates new build model parameter default in expected map
- `platforms/core-runtime/client-services/src/test/groovy/org/gradle/launcher/daemon/client/DaemonClientTest.groovy`
  - validates daemon unavailable exception typing and reason-included failure messaging

---

## Validation Performed

Environment:

- Java set via asdf: `zulu-23.32.11`

Executed successfully:

- `configuration-cache:compileKotlin`
- `client-services:test --tests org.gradle.launcher.daemon.client.DaemonClientTest`
- `launcher:test --tests org.gradle.launcher.cli.converter.StartParameterConverterTest`
- `configuration-cache:test --tests org.gradle.internal.buildtree.BuildModelParametersProviderTest`

Run settings used for repo compatibility in this environment:

- `--no-configuration-cache`
- `-Dorg.gradle.unsafe.isolated-projects=false`

---

## Constraints Maintained

- Opt-in only; default behavior unchanged.
- No unsafe concurrent mutation paths introduced.
- Writes remain exclusive where correctness requires serialization.
- Focused on implementation-agnostic requirements from spec, not prototype replication.

---

## Phase 1 Outcome

Phase 1 establishes a safe and test-validated baseline:

- concurrency mode can be enabled intentionally
- configuration-cache access is less serialized for read-heavy scenarios while preserving write safety
- daemon contention is more observable for users and diagnostics

This creates a stable base for Phase 2 work on broader contention diagnostics and additional shared-state concurrency boundaries.
