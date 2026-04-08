# Phase 1: Fine-Grained Locking for Concurrent Invocations

## Overview
Phase 1 focused on refactoring Gradle's core locking infrastructure to support **shared read access** across multiple processes (daemons). This is the foundational requirement for "Approach A" (Daemon Pooling + Fine-Grained RW-Locking), enabling concurrent builds to read from shared caches (Configuration Cache, Dependency Metadata) without unnecessary serialization.
## Key Architectural Changes

### 1. Enhanced Locking Interfaces
...
### 4. Opt-in Feature Flag
Introduced a dedicated control for agentic concurrency features:
*   **`ConcurrencyMode`**: Manages the `org.gradle.internal.concurrency.agentic` system property.
*   **Backward Compatibility**: When the flag is `false` (default), all `Shared` lock requests in `DefaultCacheCoordinator` are automatically downgraded to `Exclusive`, ensuring zero behavioral change for standard workflows.

### 5. Contention Observability
Enhanced `DefaultFileLockManager` to provide actionable feedback during lock contention:
*   **Lifecycle Logging**: In `agentic` mode, Gradle now logs "Waiting to acquire..." messages at the `LIFECYCLE` level (visible to users and agents).
*   **PID Tracking**: Logs include the PID of the current lock holder, enabling easier troubleshooting of stuck builds.
*   **State Updates**: If the lock holder changes (e.g., Build A finishes but Build C grabs the lock before Build B), Gradle logs the transition.

Expanded the core synchronization interfaces to explicitly accept a `LockMode` (`Shared` vs. `Exclusive`):
*   **`ExclusiveCacheAccessCoordinator`**: Added `useCache(LockMode, Supplier)` and `withFileLock(LockMode, Supplier)`.
*   **`CrossProcessCacheAccess`**: Added `withFileLock(LockMode, Supplier)` and `acquireFileLock(LockMode)`.

### 2. Multi-Threaded Ownership Logic
Refactored `DefaultCacheCoordinator` to move beyond a single "owner" thread model:
*   **Shared Owners**: Introduced a `Set<Thread> sharedOwners` to track threads holding a shared lock.
*   **Exclusive Owner**: Maintained `Thread exclusiveOwner` for write operations.
*   **Concurrency Rules**: 
    *   Multiple threads can hold a `Shared` lock simultaneously.
    *   An `Exclusive` lock requires no other shared or exclusive owners.
    *   Re-entrancy is preserved for the current thread regardless of mode.

### 3. On-Demand File Locking
Updated `LockOnDemandCrossProcessCacheAccess` and `LockOnDemandEagerReleaseCrossProcessCacheAccess` to dynamically request the appropriate OS-level file lock from `FileLockManager`:
*   When a build requests `Shared` access, the daemon now acquires a **non-exclusive file lock**, allowing other daemons to do the same.
*   Write operations continue to use **exclusive file locks** to ensure data integrity.

## Subsystem Improvements

### Configuration Cache
*   **`ConfigurationCacheRepository`**: Modified `useForStateLoad` to use `Shared` locks.
*   **Impact**: Multiple Gradle daemons can now concurrently load the configuration cache for the same project, significantly reducing wait time during "burst" agent invocations.

### Dependency Management
*   **`WritableArtifactCacheLockingAccessCoordinator`**: Updated `IndexedCache.getIfPresent` to use `Shared` locks.
*   **`ReadOnlyArtifactCacheLockingAccessCoordinator`**: Updated internal transparent cache lookups to use `Shared` locks.
*   **Impact**: Lookups for dependency metadata (e.g., checking if a module is already in the cache) no longer block other builds from performing the same check.

## Modified Files
- `platforms/core-execution/persistent-cache/src/main/java/org/gradle/cache/ExclusiveCacheAccessCoordinator.java`
- `platforms/core-execution/persistent-cache/src/main/java/org/gradle/cache/CrossProcessCacheAccess.java`
- `platforms/core-execution/persistent-cache/src/main/java/org/gradle/cache/internal/DefaultCacheCoordinator.java`
- `platforms/core-execution/persistent-cache/src/main/java/org/gradle/cache/internal/LockOnDemandCrossProcessCacheAccess.java`
- `platforms/core-execution/persistent-cache/src/main/java/org/gradle/cache/internal/LockOnDemandEagerReleaseCrossProcessCacheAccess.java`
- `platforms/core-execution/persistent-cache/src/main/java/org/gradle/cache/internal/FixedExclusiveModeCrossProcessCacheAccess.java`
- `platforms/core-execution/persistent-cache/src/main/java/org/gradle/cache/internal/FixedSharedModeCrossProcessCacheAccess.java`
- `platforms/core-execution/persistent-cache/src/main/java/org/gradle/cache/internal/NoLockingCacheAccess.java`
- `platforms/core-configuration/configuration-cache/src/main/kotlin/org/gradle/internal/cc/impl/ConfigurationCacheRepository.kt`
- `platforms/software/dependency-management/src/main/java/org/gradle/api/internal/artifacts/ivyservice/DefaultArtifactCaches.java`
- `platforms/software/dependency-management/src/main/java/org/gradle/api/internal/artifacts/ivyservice/WritableArtifactCacheLockingAccessCoordinator.java`
- `platforms/software/dependency-management/src/main/java/org/gradle/api/internal/artifacts/ivyservice/ReadOnlyArtifactCacheLockingAccessCoordinator.java`

## Next Steps
1.  Implement a **Feature Flag** to make these changes opt-in.
2.  Enhance **Observability** by logging specific PID and lock-mode information when a build is waiting for a lock.
3.  Audit and optimize **Local Build Cache** and **Task History** for similar shared-read patterns.
