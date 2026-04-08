# Phase 2 & 3: Daemon Allocation and Build Cache Concurrency

## Overview
Phase 2 and 3 focused on optimizing daemon allocation during concurrent bursts and enabling shared access to the local build cache. These changes reduce resource exhaustion and further eliminate serialization points between concurrent builds.

## Key Architectural Changes

### 1. Daemon Allocation Queuing (Phase 2)
To prevent "burst" invocations from starting an excessive number of JVMs, a queuing mechanism was added to the daemon connection logic:
*   **`DefaultDaemonConnector`**: In `agentic` mode, if no idle daemons are available but compatible busy daemons exist, the client will wait up to 5 seconds (`BUSY_WAIT_TIMEOUT`) for a daemon to become idle instead of immediately starting a new one.
*   **Impact**: Significantly reduces peak memory usage on build agents when multiple tasks or agents trigger Gradle simultaneously.

### 2. Local Build Cache Concurrency (Phase 3)
The local directory-based build cache was optimized to allow concurrent read and write operations across multiple processes:
*   **`DirectoryBuildCache`**: Updated to use `Shared` file locks for `loadLocally`, `storeLocally`, and `withTempFile`.
*   **Atomic Operations**: Leverages the fact that moving files into the cache is an atomic operation (`Files.move` with `ATOMIC_MOVE`), making it safe for multiple processes to "write" (move) into the cache while others are reading.
*   **In-Process Safety**: Maintained the `ReentrantReadWriteLock` to ensure thread safety within a single daemon.
*   **Impact**: Eliminates build cache lock contention, allowing multiple concurrent builds to benefit from local cache hits and stores simultaneously.

## Modified Files
- `platforms/core-runtime/client-services/src/main/java/org/gradle/launcher/daemon/client/DefaultDaemonConnector.java`
- `platforms/core-execution/build-cache-local/src/main/java/org/gradle/caching/local/internal/DirectoryBuildCache.java`

## Updated Implementation Roadmap
1.  **Phase 1 (Complete)**: Foundational RW-locking, Feature Flag, and Observability.
2.  **Phase 2 & 3 (Complete)**: Daemon Queuing and Build Cache Concurrency.
3.  **Phase 4 (Next)**: Final validation and comprehensive stress testing of all concurrent features.
